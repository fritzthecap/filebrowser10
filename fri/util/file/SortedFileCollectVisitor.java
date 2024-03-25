package fri.util.file;

import java.io.File;
import java.util.*;

/**
	Collect all files contained in passed directory, sorted by name.
	@author Fritz Ritzberger, 2003
*/

public class SortedFileCollectVisitor extends RecursiveFileVisitor
{
	protected List list;
	
	/** Build a sorted collection of files contained in passed directory. */
	public SortedFileCollectVisitor(File dir, List list)	{
		this(list);
		loop(dir);
	}

	/** Stores the list, does NOT start the file loop. */
	protected SortedFileCollectVisitor(List list)	{
		this.list = list;
	}

	protected void visit(File f)	{
		list.add(f);
	}

	/** Sort directories' contents before visiting. */
	protected String [] getDirectoryFilenames(File dir)	{
		String [] names = dir.list();
		if (names != null)
			Arrays.sort(names);
		return names;
	}

	/** Overridden because directories itself are not needed in visit(). */
	protected boolean directoryVisitCondition(String extension)	{
		return false;
	}

}
