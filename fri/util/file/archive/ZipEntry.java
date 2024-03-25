package fri.util.file.archive;

/**
	Marker class for interface ArchiveEntry. Implements
	getMethod to return a String value.
*/

public class ZipEntry implements ArchiveEntry
{
	private java.util.zip.ZipEntry delegate;
	
	
	public ZipEntry(java.util.zip.ZipEntry delegate)	{
		this.delegate = delegate;
	}
	

	public boolean isDirectory()	{
		return delegate.isDirectory();
	}

	public String getName()	{
		return delegate.getName();
	}
	
	public long getTime()	{
		return delegate.getTime();
	}
	
	public long getSize()	{
		return delegate.getSize();
	}
	
	public String getInfo()	{
		return delegate.getMethod() == java.util.zip.ZipEntry.DEFLATED ? "DEFLATED" : "STORED";
	}

	public Object getDelegate()	{
		return delegate;
	}

}