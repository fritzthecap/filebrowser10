package fri.util.file.archive;

import java.util.Enumeration;
import java.util.Hashtable;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;

import fri.util.observer.CancelProgressObserver;

/**
	Wrapper around ZIP and TAR files, that lists elements,
	can close an archive opened by construction and can extract
	a specific entry.
*/

public interface Archive
{
	/**
		Enumerate all contained file and directory entries.
		The Enumeration gives ArchiveEntry instances.
	*/
	public Enumeration archiveEntries();

	/**
		Extract specified entries, or all of them when entries argument is null.
		Returns a map of entry names with their created File objects.
	*/
	public Hashtable extractEntries(File destFile, ArchiveEntry [] entries, CancelProgressObserver observer) throws Exception;

	/**
		Close the archive.
	*/
	public void close();

	/**
		Get error messages when opening archive.
	*/
	public String getError();
	
	/**
		Returns an InputStream for a passed entry name.
	*/
	public InputStream getInputStream(ArchiveEntry entry, CancelProgressObserver observer)
		throws IOException;

	/**
		Returns a File for a passed entry name. The file gets
		extracted temporarily (deleteOnExit).
	*/
	public File getFile(ArchiveEntry entry)
		throws IOException;


	/**
		Returns the root folder where the archive has been extracted.
	*/
	public File getExtractRootFolder();

}
