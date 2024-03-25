package fri.util.file.archive;

import fri.util.tar.*;

/**
*/

public class TarEntry implements ArchiveEntry
{
	private SelectiveTarEntry delegate;
	
	
	public TarEntry(SelectiveTarEntry delegate)	{
		this.delegate = delegate;
	}
	
	
	public boolean isDirectory()	{
		return delegate.isDirectory();
	}

	public String getName()	{
		return delegate.getName();
	}

	public long getTime()	{
		return delegate.getModTime().getTime();
	}

	public long getSize()	{
		return delegate.getSize();
	}

	public String getInfo()	{
		return delegate.getUserName();
	}

	public Object getDelegate()	{
		return delegate;
	}

}
