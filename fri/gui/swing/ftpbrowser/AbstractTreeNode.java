package fri.gui.swing.ftpbrowser;

import java.util.Vector;
import fri.util.sort.quick.*;

/**
	TreeNode base class that contains methods for the move pending state
	and provides a abstract list() method that gets called only once.
	
	@author Fritz Ritzberger
*/

public abstract class AbstractTreeNode extends fri.gui.mvc.model.swing.AbstractTreeNode
{
	protected Boolean isDirectory;


	public AbstractTreeNode(Object userObject)	{
		super(userObject, true);
	}


	/** Returns  displayable String about the file: size, modification date. */
	public abstract String getFileInfo();
	
	/** Returns true if this node is a symbolic link. */
	public abstract boolean isLink();

	/** Returns the recursive size (un-observed) of this node. */
	public abstract long getRecursiveSize();

	/** Returns the absolute path of this node. */
	public abstract String getAbsolutePath();

	protected abstract boolean isDirectory();


	public boolean getAllowsChildren() {
		if (isRoot())
			return true;

		if (isDirectory == null)
			isDirectory = new Boolean(isDirectory());
		
		return isDirectory.booleanValue();
	}

	public void releaseChildren()	{
		super.releaseChildren();
		isDirectory = null;
	}
	
	/**
		Returns true if a child of passed name can be found within children
		and overwrite dialog is confirmed, false if child name does not exist
		or overwrite is not confirmed.
	*/
	public boolean checkForOverwrite(AbstractTreeNode src, String name)	{
		if (ProgressAndErrorReporter.isOverwriteAll())	// already confirmed overwrite for all
			return true;

		AbstractTreeNode target = (AbstractTreeNode)existsChild(name);

		System.err.println("checking for overwrite in "+getAbsolutePath()+" on "+src.getAbsolutePath()+" with name "+name);
		
		if (target != null)	{
			return ProgressAndErrorReporter.overwrite(
						src.getAbsolutePath(),
						src.getFileInfo(),
						target.getAbsolutePath(),
						target.getFileInfo());
		}
		
		return true;
	}


	/** Sorts a child list in a file manner (folders first), considering the extension. */
	protected Vector sortChildren(Vector v)	{
		return new QSort(new FileComparator()).sort(v);
	}




	private static class FileComparator implements Comparator
	{
		public int compare(Object o1, Object o2)	{
			AbstractTreeNode n1 = (AbstractTreeNode) o1;
			AbstractTreeNode n2 = (AbstractTreeNode) o2;

			// sort directories to top
			boolean isDir1 = n1.getAllowsChildren();
			boolean isDir2 = n2.getAllowsChildren();
			
			if (isDir1 && isDir2 == false)
				return -1;
			if (isDir1 == false && isDir2)
				return 1;

			// get names
			String s1 = n1.toString().toLowerCase();
			String s2 = n2.toString().toLowerCase();

			// sort directories by name, ignoring extensions
			if (isDir1 && isDir2)
				return s1.compareTo(s2);

			// sort hidden files to top
			boolean isHidden1 = s1.startsWith(".");
			boolean isHidden2 = s2.startsWith(".");
			if (isHidden1 && isHidden2 == false)
				return -1;
			if (isHidden1 == false && isHidden2)
				return 1;
			
			// get extensions
			String [] nameAndExtension1 = split(s1);
			String name1 = nameAndExtension1[0];
			String ext1 = nameAndExtension1[1];
			String [] nameAndExtension2 = split(s2);
			String name2 = nameAndExtension2[0];
			String ext2 = nameAndExtension2[1];
			
			// group by extensions
			if (ext1 == null && ext2 != null)
				return -1;
			if (ext1 != null && ext2 == null)
				return 1;

			if (ext1 != null && ext2 != null && ext1.equals(ext2) == false)
				return ext1.compareTo(ext2);

			return s1.compareTo(s2);
		}
		
		private String [] split(String name)	{
			int i = name.lastIndexOf(".");
			if (i > 0 && i < name.length() - 1)	// dot at head or tail is not a valid extension
				return new String [] { name.substring(0, i), name.substring(i + 1) };
			return new String [] { name, null };
		}
	}
	
}