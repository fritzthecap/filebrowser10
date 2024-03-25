package fri.util.file.archive;

/**
	Wrapper around ZIP and TAR entries, that mirrors
	common attributes like time, size, name.
*/

public interface ArchiveEntry
{
	public String getName();
	public long getSize();
	public long getTime();
	public String getInfo();
	public boolean isDirectory();
	public Object getDelegate();
}