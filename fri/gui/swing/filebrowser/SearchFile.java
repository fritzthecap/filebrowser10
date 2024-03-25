package fri.gui.swing.filebrowser;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import fri.util.file.archive.Archive;
import fri.util.file.archive.ArchiveEntry;
import fri.util.observer.CancelProgressObserver;

/**
	A wrapper for a transparent search across files and archive entries.
*/

public class SearchFile
{
	private File file = null;
	private ArchiveEntry entry;
	private Archive archive;
	
	
	public SearchFile(ArchiveEntry entry, Archive archive)	{
		this.entry = entry;
		this.archive = archive;
	}

	public SearchFile(File file)	{
		this.file = file;
	}
	

	public boolean isDirectory()	{
		if (file != null)
			return file.isDirectory();
			
		return entry.isDirectory();
	}
	
	public String getName()	{
		if (file != null)
			return file.getName();
			
		File f = new File(entry.getName());
		return f.getName();
	}
	
	public long getSize()	{
		if (file != null)
			return file.length();
			
		return entry.getSize();
	}
	
	public long getTime()	{
		if (file != null)
			return file.lastModified();
			
		return entry.getTime();
	}
	
	/** Extracts temporarily if it is an archive entry. */
	public File getFile()
		throws IOException
	{
		if (file != null)
			return file;
			
		return archive.getFile(entry);
	}
	
	public boolean isTemporaryFile()	{
		return file == null;
	}
	
	public InputStream getInputStream(CancelProgressObserver observer)
		throws IOException
	{
		if (file != null)
			return new FileInputStream(file);
		
		return archive.getInputStream(entry, observer);	
	}
	
}