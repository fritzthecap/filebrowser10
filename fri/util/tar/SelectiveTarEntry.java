package fri.util.tar;

import com.ice.tar.TarEntry;
import java.util.Date;
import fri.util.Equals;

/**
	Wrapper around TarEntry, as TarArchive seems to be
	unaccessible by entries.
*/

public class SelectiveTarEntry
{
	private boolean directory;
	private long size;
	private String name;
	private Date modified;
	private String userName, groupName;
	
	protected SelectiveTarEntry()	{
	}

	public SelectiveTarEntry(TarEntry entry)	{
		size = entry.getSize();
		name = entry.getName();
		modified = entry.getModTime();
		directory = entry.isDirectory();
		userName = entry.getUserName();
		groupName = entry.getGroupName();
	}
	
	
	public boolean equals(Object o)	{
		if (o instanceof TarEntry)	{
			TarEntry te = (TarEntry)o;
			return
				Equals.equals(getModTime(), te.getModTime()) &&
				Equals.equals(getName(), te.getName()) &&
				Equals.equals(new Long(getSize()), new Long(te.getSize()));
		}
		return super.equals(o);
	}
	
	public boolean isDirectory()	{
		return directory;
	}
	
	public Date getModTime()	{
		return modified;
	}
	
	public String getName()	{
		return name;
	}
	
	public long getSize()	{
		return size;
	}

	public String getUserName()	{
		return userName+"/"+groupName;
	}

}
