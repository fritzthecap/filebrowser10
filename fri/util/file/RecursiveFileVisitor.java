package fri.util.file;

import java.io.File;
import fri.util.file.Link;
import fri.util.os.OS;

/**
	Loop recursively through directories and files. An extension can be passed
	optionally, then only files that end with that extension (case sensitive)
	will be passed to abstract method <i>visit()</i>. This is a depth first loop.
	<p>
	All files and directories in a directory are visited before the directory itself
	is passed to <i>visit()</i> method. This can be changed by overridding
	<code>loop(File f, String extension)</code> and calling <code>loop(f, extension, true)</code>.
	<p>
	Symbolic link directories (UNIX) are visited, but not looped recursively,
	i.e. visit() gets the link directory, but not the contained files.
	<p>
	Errors: getCanconicalPath() does not work correctly for pathes that contain "..".
	
	@author Fritz Ritzberger
*/

public abstract class RecursiveFileVisitor
{
	/**
		Loop through passed File, recursively if it is a directory.
	*/
	public RecursiveFileVisitor(File f)	{
		this(f, null);
	}
	
	/**
		Loop through passed File, recursively if it is a directory.
		If extension is not null, only files that end with this
		extension will be passed to <i>visit()</i> method (case sensitive).
	*/
	public RecursiveFileVisitor(File f, String extension)	{
		loop(f, extension);
	}
	

	/** Need a protected no-arg constructor to enable storing constructor paramters before loop(). */
	protected RecursiveFileVisitor()	{
	}
	

	protected abstract void visit(File f);
	
	
	protected void loop(File f)	{
		loop(f, null);
	}
	
	protected void loop(File f, String extension)	{
		loop(f, extension, false);
	}
	
	protected void loop(File f, String extension, boolean visitDirectoryBeforeContents)	{
		//System.err.println("loop with file "+f+", extension "+extension+", visitDirectoryBeforeContents "+visitDirectoryBeforeContents);

		if (f.isFile())	{	// is file or other
			if (extension == null || f.getName().endsWith(extension))	{
				visit(f);
			}
			// others are ignored
		}
		else	{	// is directory
			// test if it is a symbolic link (non-Windows)
			boolean isLink = OS.isWindows ? false : Link.isLink(f);

			if (visitDirectoryBeforeContents)	{
				visitDirectory(f, extension);
				loopDirectory(f, extension, isLink, visitDirectoryBeforeContents);
			}
			else	{
				loopDirectory(f, extension, isLink, visitDirectoryBeforeContents);
				visitDirectory(f, extension);
			}
		}
	}


	/**
		Override this to sort directories' contents before visiting.
		This implementation simply returns <i>dir.list()</i>. 
		@return an array of file names.
	*/
	protected String [] getDirectoryFilenames(File dir)	{
		return dir.list();
	}
	
	private void loopDirectory(File f, String extension, boolean isLink, boolean visitDirectoryBeforeContents)	{
		//System.err.println("loopDirectory "+f+", isLink "+isLink);
		if (isLink == false)	{
			String [] list = getDirectoryFilenames(f);

			for (int i = 0; list != null && i < list.length; i++)	{
				loop(new File(f, list[i]), extension, visitDirectoryBeforeContents);
			}
		}
	}

	private void visitDirectory(File f, String extension)	{
		if (directoryVisitCondition(extension))	{
			visit(f);
		}
	}

	/**
		Returns true. Override if directories itself should not be visited.
		Nevertheless the contents of the directory WILL be visited!
	*/
	protected boolean directoryVisitCondition(String extension)	{
		return true;
	}

}
