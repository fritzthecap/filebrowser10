package fri.gui.swing.ftpbrowser;

import java.io.File;
import java.io.IOException;
import javax.swing.tree.DefaultMutableTreeNode;
import fri.util.os.OS;
import fri.util.file.FileSize;

/**
	The filesystem tree node.
	Contains filesystem specific child list logic and retrieval of propeties (size, time).
	
	@author Fritz Ritzberger
*/

public class FilesystemTreeNode extends AbstractTreeNode
{
	private boolean isLink;
	private String linkName;

	
	/** File root constructor. */
	public FilesystemTreeNode()	{
		super(null);
	}

	/** File child constructor. */
	private FilesystemTreeNode(File child)	{
		super(child);
		
		try	{
			linkName = child.getCanonicalPath();
			isLink = linkName.equals(child.toString()) == false;
		}
		catch (IOException e)	{
			linkName = child.toString();
		}
	}


	public fri.gui.mvc.model.swing.AbstractTreeNode createTreeNode(Object createData)	{
		return new FilesystemTreeNode((File)createData);
	}


	protected boolean isDirectory()	{
		if (OS.isWindows)	{	// identify drives as A:\ continues to show dialog
			String name = toString();
			if (name.length() == 2 && name.endsWith(":") && getLevel() == 1)
				return true;
		}
		return ((File)getUserObject()).isDirectory();
	}

	protected void list()	{
		File [] farr;
		if (isRoot())	{
			System.err.println("Before listRoots in list()");
			farr = File.listRoots();
			System.err.println("After listRoots in list()");
		}
		else	{
			farr = ((File)getUserObject()).listFiles();
		}
		
		for (int i = 0; farr != null && i < farr.length; i++)	{
			add(createTreeNode(farr[i]));
		}
		
		if (children != null)
			children = sortChildren(children);
	}

	/** Returns true if this node is a symbolic link. */
	public boolean isLink()	{
		return isLink;
	}
	
	/** Returns the absolute path or the absolute path of the file this references (as a link). */
	public String getLinkName()	{
		return linkName;	/** Returns  displayable String about the file: size, modification date. */

	}
	
	/** Returns the recursive size (un-observed) of this node. */
	public long getRecursiveSize()	{
		return new FileSize((File)getUserObject()).length();
	}

	/** Returns  displayable String about the file: size, modification date. */
	public String getFileInfo()	{
		File f = (File)getUserObject();
		return FileInfo.getFileInfo(f.length(), f.lastModified());	// NullPointerException???
	}

	/** Returns the absolute path of this node. */
	public String getAbsolutePath()	{
		File f = (File)getUserObject();
		return f.getAbsolutePath();
	}

	/** Returns true this node can be dragged. This is true for all nodes except root and its immediate children. */
	public boolean isDragable()	{
		return isRoot() == false && ((DefaultMutableTreeNode)getParent()).isRoot() == false;
	}


	public String toString()	{
		if (getUserObject() instanceof File)	{	// when renamed, the TreeNode has a String userObject
			String s = ((File)getUserObject()).getName();

			if (s.length() <= 0)
				s = getUserObject().toString();	// UNIX root returns "" for getName()

			if (OS.isWindows && s.endsWith("\\") && getLevel() == 1)
				s = s.substring(0, s.length() - 1);
			
			return s;
		}
		else	{
			return getUserObject() != null ? getUserObject().toString() : "Computer";
		}
	}

}
