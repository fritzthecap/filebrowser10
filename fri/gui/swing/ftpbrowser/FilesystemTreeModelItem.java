package fri.gui.swing.ftpbrowser;

import java.io.File;
import javax.swing.tree.DefaultMutableTreeNode;
import fri.util.file.*;
import fri.util.ftp.*;
import fri.util.io.CopyStream;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.CommandArguments;

/**
	The filesystem ModelItem implementing controller functionality.
	
	@author Fritz Ritzberger
*/

public class FilesystemTreeModelItem extends AbstractTreeModelItem
{
	public FilesystemTreeModelItem(DefaultMutableTreeNode userObject)	{
		super(userObject);
	}
	
	/**
		Returns a new ModelItem representing a filesystem directory created within this MutableTreeNode userObject.
		The parent node is this node.
		@return null if new item was not created.
	*/
	protected ModelItem createInMedium(CommandArguments createInfo)	{
		FilesystemTreeNode fn = (FilesystemTreeNode)getUserObject();
		File f = (File)fn.getUserObject();
		String name = (String)createInfo.getCreateData();
		File newFile = new File(f, name);
		
		if (newFile.mkdir())	{
			return new FilesystemTreeModelItem(fn.createTreeNode(newFile));
		}
		else	{
			error(new Exception("Create Directory failed in:\n"+f));
			return null;
		}
	}

	/** Returns true if this item could be deleted within filesystem. */
	protected boolean deleteInMedium(CommandArguments deleteInfo)	{
		if (ProgressAndErrorReporter.getObserver() != null && ProgressAndErrorReporter.getObserver().canceled())
			return false;

		FilesystemTreeNode fn = (FilesystemTreeNode)getUserObject();
		File f = (File)fn.getUserObject();
		
		DeleteFileObserved delete = new DeleteFileObserved(f);
		delete.setObserver(ProgressAndErrorReporter.getObserver());
		delete.delete();
		
		if ((ProgressAndErrorReporter.getObserver() == null || ProgressAndErrorReporter.getObserver().canceled() == false) &&
				(delete.getSuccess() != true || f.exists()))
		{
			error(new Exception("Delete failed:\n"+f));
			return false;
		}
		
		return true;
	}


	/** Returns a new ModelItem when the target item is in filesystem, else null. */
	protected ModelItem moveInMedium(ModelItem target, CommandArguments pasteInfo)	{
		if (ProgressAndErrorReporter.getObserver() != null && ProgressAndErrorReporter.getObserver().canceled())
			return null;

		if (target instanceof FilesystemTreeModelItem == false)
			return null;	// move to another medium must use copy/delete
		
		FilesystemTreeNode fn = (FilesystemTreeNode)getUserObject();
		File f = (File)fn.getUserObject();
		FilesystemTreeNode fnTarget = (FilesystemTreeNode)target.getUserObject();
		File fTarget = (File)fnTarget.getUserObject();
		String name = f.getName();
		File movedFile = new File(fTarget, name);
		
		// check if file exists
		if (fnTarget.checkForOverwrite(fn, name) == false)	{
			error = "File exists, not overwritten on move: "+movedFile;
			return null;
		}
		
		// do the rename
		f.renameTo(movedFile);
		
		// a rename between drives fails on Windows, must use copy and delete

		if (f.exists() == true)	{
			System.err.println("Move source file still exists, will try copy/delete for: "+f);
			return null;
		}
		if (movedFile.exists() == false)	{
			System.err.println("Move target file is not existing, will try copy/delete for: "+f);
			return null;
		}
			
		return new FilesystemTreeModelItem(fnTarget.createTreeNode(movedFile));
	}



	/** Copy this node. Target is in member variable. */
	public Object clone()	{
		if (ProgressAndErrorReporter.getObserver() != null && ProgressAndErrorReporter.getObserver().canceled())
			return null;

		FilesystemTreeNode fn = (FilesystemTreeNode)getUserObject();
		File f = (File)fn.getUserObject();
		String name = getBaseName();	//f.getName();
		AbstractTreeNode anTarget = (AbstractTreeNode)target.getUserObject();

		// check if file exists
		if (anTarget.checkForOverwrite(fn, name) == false)	{
			error = "File exists, not overwritten on copy: "+anTarget.getAbsolutePath()+File.separator+name;
			return null;
		}
				
		try	{
			if (target instanceof FilesystemTreeModelItem)	{	// copy within filesystem
				FilesystemTreeNode fnTarget = (FilesystemTreeNode)target.getUserObject();
				File newFile = new File((File)fnTarget.getUserObject(), name);
				
				CopyStream.bufsize = CopyStream.ONE_MB;
				new CopyFile(f, newFile, ProgressAndErrorReporter.getObserver()).copy();
				
				return new FilesystemTreeModelItem(fnTarget.createTreeNode(newFile));
			}
			else
			if (target instanceof FtpServerTreeModelItem)	{	// upload to FTP server
				FtpServerTreeNode fnTarget = (FtpServerTreeNode)target.getUserObject();
				ObservableFtpClient ftpClient = fnTarget.getFtpClient();
				String targetPath = fnTarget.getAbsolutePath()+"/"+name;

				ftpClient.setObserver(ProgressAndErrorReporter.getObserver());
				
				try	{
					if (fn.getAllowsChildren())
						ftpClient.uploadDirectory(f.getPath(), targetPath, true);
					else
						ftpClient.uploadFile(f.getPath(), targetPath);
				}
				finally	{
					ftpClient.setObserver(null);
				}
					
				return new FilesystemTreeModelItem(fnTarget.createTreeNode(name));
			}
			else	{
				throw new IllegalArgumentException("ERROR in FilesystemTreeModelItem: Copy target ModelItem is of unknown class: "+target);
			}
		}
		catch (Exception e)	{
			error(e);
		}
		
		return null;
	}

}
