package fri.gui.swing.ftpbrowser;

import java.io.File;
import javax.swing.tree.DefaultMutableTreeNode;
import fri.util.ftp.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.CommandArguments;

/**
	The FTP ModelItem implementing controller functionality.
	
	@author Fritz Ritzberger
*/

public class FtpServerTreeModelItem extends AbstractTreeModelItem
{
	/**
		Last used FtpClient for copy actions from a FTP server to the same FTP server.
		As every FtpServerTreeModelItem does its copy separated from others, we need
		a local buffer for the recently used client clone. Else every copied node
		would open a new FTP connection.
	*/
	public static FtpClient ftpClientClone;


	public FtpServerTreeModelItem(DefaultMutableTreeNode userObject)	{
		super(userObject);
	}
	
	/**
		Returns a new ModelItem representing a FTP server directory created within this MutableTreeNode userObject.
		The parent node is this node.
		@return null if new item was not created.
	*/
	protected ModelItem createInMedium(CommandArguments createInfo)	{
		FtpServerTreeNode fn = (FtpServerTreeNode)getUserObject();
		FtpClient ftpClient = fn.getFtpClient();
		String name = (String)createInfo.getCreateData();
		
		try	{
			ftpClient.chdir(fn.getAbsolutePath());
			ftpClient.mkdir(name);
			ftpClient.chdir(fn.getRootName());
			
			return new FtpServerTreeModelItem(fn.createTreeNode(name));
		}
		catch (Exception e)	{
			error(e);
			return null;
		}
	}
	
	/** Returns true if this item could be deleted on FTP server. */
	protected boolean deleteInMedium(CommandArguments deleteInfo)	{
		if (ProgressAndErrorReporter.getObserver() != null && ProgressAndErrorReporter.getObserver().canceled())
			return false;
		
		FtpServerTreeNode fn = (FtpServerTreeNode)getUserObject();
		ObservableFtpClient ftpClient = fn.getFtpClient();
		
		ftpClient.setObserver(ProgressAndErrorReporter.getObserver());
		
		try	{
			if (fn.getAllowsChildren() == false)
				ftpClient.deleteFile(fn.getAbsolutePath());
			else
				ftpClient.deleteDirectory(fn.getAbsolutePath());
				
			return true; 
		}
		catch (Exception e)	{
			error(e);
			return false;
		}
		finally	{
			ftpClient.setObserver(null);
		}
	}


	protected boolean isActionToSelf(ModelItem targetItem, boolean isCopy, boolean identical)	{
		FtpServerTreeNode fn = (FtpServerTreeNode) getUserObject();
		FtpServerTreeNode fnTarget = (FtpServerTreeNode) targetItem.getUserObject();
		FtpClient ftpClient = fn.getFtpClient();
		FtpClient ftpClientTarget = fnTarget.getFtpClient();

		if (ftpClient.equals(ftpClientTarget) == false)
			return false;	// is move from one FTP server to another
		
		return super.isActionToSelf(targetItem, isCopy, identical);
	}


	/** Returns a new ModelItem when a move in data is possible without copying, else null. */
	protected ModelItem moveInMedium(ModelItem target, CommandArguments pasteInfo)	{
		if (ProgressAndErrorReporter.getObserver() != null && ProgressAndErrorReporter.getObserver().canceled())
			return null;
		
		if (target instanceof FtpServerTreeModelItem == false)
			return null;	// is move to local filesystem, must use copy/delete
			
		FtpServerTreeNode fn = (FtpServerTreeNode)getUserObject();
		FtpClient ftpClient = fn.getFtpClient();
		FtpServerTreeNode fnTarget = (FtpServerTreeNode)target.getUserObject();
		FtpClient ftpClientTarget = fnTarget.getFtpClient();

		if (ftpClient.equals(ftpClientTarget) == false)
			return null;	// is move from one FTP server to another, must use copy/delete
		
		String name = fn.toString();
		String newPath = targetPath(fnTarget, name);

		// test if node exists
		if (fnTarget.checkForOverwrite(fn, name) == false)	{
			error = "File exists, not overwritten on move: "+newPath;
			return null;
		}

		try	{
			// do the rename
			ftpClient.renameTo(fn.getAbsolutePath(), newPath);

			return new FtpServerTreeModelItem(fnTarget.createTreeNode(name));
		}
		catch (Exception e)	{
			error(e);
			return null;
		}
	}


	
	/** Copy this node. Target is in member variable. */
	public Object clone()	{
		if (ProgressAndErrorReporter.getObserver() != null && ProgressAndErrorReporter.getObserver().canceled())
			return null;

		FtpServerTreeNode fn = (FtpServerTreeNode)getUserObject();
		ObservableFtpClient ftpClient = fn.getFtpClient();
		boolean isDirectory = fn.getAllowsChildren();
		String name = getBaseName();
		AbstractTreeNode anTarget = (AbstractTreeNode)target.getUserObject();

		// check if file exists
		if (anTarget.checkForOverwrite(fn, name) == false)	{
			error = "File exists, not overwritten on copy: "+targetPath(anTarget, name);
			return null;
		}
		
		try	{
			if (target instanceof FtpServerTreeModelItem)	{
				// copy between two different FTP servers, or copy on the same FTP server
				FtpServerTreeNode fnTarget = (FtpServerTreeNode)target.getUserObject();
				FtpClient ftpClientTarget = fnTarget.getFtpClient();
	
				// if it is a copy within the same server, do it with a clone of the ftpClient
				if (ftpClient.equals(ftpClientTarget))	{	// copy on the same FTP server
					if (ftpClientClone == null || ftpClientClone.equals(ftpClientTarget) == false)	{
						if (ftpClientClone != null)	{
							try	{ ftpClientClone.disconnect(); }	catch (Exception e)	{}
						}
						ftpClientClone = (FtpClient)ftpClient.clone();	// open another connection to server
					}
					ftpClientTarget = ftpClientClone;	// target is second connection to server
				}

				// make source and target names
				String srcName = fn.getAbsolutePath();
				String tgtName = targetPath(fnTarget, name);
				
				// copy the file from server to server
				FtpClientToClient c2c = new FtpClientToClient(ftpClient, ftpClientTarget);
				ftpClient.setObserver(ProgressAndErrorReporter.getObserver());
				try	{
					if (isDirectory)
						c2c.copyDirectory(srcName, tgtName, true);
					else
						c2c.copyFile(srcName, tgtName);
				}
				finally	{
					ftpClient.setObserver(null);
				}
				
				return new FtpServerTreeModelItem(fnTarget.createTreeNode(name));
			}
			else
			if (target instanceof FilesystemTreeModelItem)	{
				// download to local filesystem
				FilesystemTreeNode fnTarget = (FilesystemTreeNode)target.getUserObject();
				File targetFolder = (File)fnTarget.getUserObject();
				File newPath = new File(targetFolder, name);
				
				ftpClient.setObserver(ProgressAndErrorReporter.getObserver());

				try	{
					if (isDirectory)
						ftpClient.downloadDirectory(fn.getAbsolutePath(), newPath.getPath(), true);
					else
						ftpClient.downloadFile(fn.getAbsolutePath(), newPath.getPath());
				}
				finally	{
					ftpClient.setObserver(null);
				}

				return new FilesystemTreeModelItem(fnTarget.createTreeNode(newPath));
			}
			else	{
				throw new IllegalArgumentException("ERROR in FtpServerTreeModelItem: Copy target ModelItem is of unknown class: "+target);
			}
		}
		catch (Exception e)	{
			error(e);
		}

		return null;
	}



	private String targetPath(AbstractTreeNode n, String name)	{
		String newPath = n.getAbsolutePath();
		if (newPath.endsWith("/") == false)
			newPath = newPath+"/";
		return newPath+name;
	}


	/** Method needed in FtpClipboard to free an FTP client's items. */
	FtpClient getFtpClient()	{
		return ((FtpServerTreeNode)getUserObject()).getFtpClient();
	}
	
}

