package fri.util.concordance.filenames;

import java.io.File;

/**
	Wraps a file to detect files with same names within directories.
	The methods hashCode() and equals() reference the name of the wrapped file.
*/
public class FileWrapper
{
	public final File file;
	private final Object key;
	
	public FileWrapper(File file, Object key)	{
		this.file = file;
		this.key = key;
	}
	
	public boolean equals(Object o)	{
		FileWrapper other = (FileWrapper)o;
		return other.key.equals(key);
	}

	public int hashCode()	{
		return key.hashCode();
	}

	/** Returns the hashing object for Concordance algorithm. */
	public String toString()	{
		return file.getName()+":	"+file.getParent();
	}

}
