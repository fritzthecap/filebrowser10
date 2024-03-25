package fri.util.file;

import java.io.File;

/**
	Count byte size of a file or folder, recursively. An extension can be passed
	optionally, then only files that end with that extension will be counted
	(case sensitive).
	
	@author Fritz Ritzberger
*/

public class FileSize extends RecursiveFileVisitor
{
	protected long length;
	
	/**
		Count bytes of passed File, recursively if it is a directory.
	*/
	public FileSize(File f)	{
		super(f);
	}
	
	/**
		Returns the recursive size.
	*/
	public long length()	{
		return length;
	}
	
	/**
		Implementation of: Count bytes of file.
	*/
	protected void visit(File f)	{
		length += f.length();
	}



	public static void main(String [] args)	{
		long len = 0L;
		for (int i = 0; i < args.length; i++)	{
			long l = 0L;
			File f = new File(args[i]);
			if (f.exists() == false)
				System.err.println("File not found: "+f);
			else
				System.out.println("  Bytes size of "+f+" is: "+(l = new FileSize(f).length()));
			len += l;
		}
		System.out.println("Sum of bytes is: "+len);
	}

}