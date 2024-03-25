package fri.util.file;

import java.io.File;

/**
	Delete a file or folder, recursively. An extension can be passed
	optionally, then only files that end with that extension will be deleted
	(case sensitive).
	
	@author Fritz Ritzberger
*/

public class DeleteFile extends RecursiveFileVisitor
{
	private boolean success = true;
	
	/**
		Delete passed File, recursively if it is a directory.
	*/
	public DeleteFile(File f)	{
		this(f, null);
	}

	/**
		Delete passed File, recursively if it is a directory.
		If extension is not null, only files that end with this
		extension will be deleted (case sensitive).
	*/
	public DeleteFile(File f, String extension)	{
		super(f, extension);
	}
	
	/** Need a protected no-arg constructor to enable storing constructor paramters before loop(). */
	protected DeleteFile()	{
	}
	

	/**
		Returns true if all files were deleted
	*/
	public boolean getSuccess()	{
		return success;
	}
	
	/**
		Implementation of: Visit a file or directory.
	*/
	protected void visit(File f)	{
		//System.err.println("DeleteFile visiting file "+f);
		success = f.delete() && success;
	}

	/** Overridden to restrict deletion of directories when an extension exists. */
	protected boolean directoryVisitCondition(String extension)	{
		return extension == null || extension.length() <= 0;
	}

}