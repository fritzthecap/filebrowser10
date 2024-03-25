package fri.util.file.archive;

import java.io.*;
import java.util.*;
import java.util.zip.ZipException;
import fri.util.file.*;
import fri.util.io.CopyStream;
import fri.util.observer.CancelProgressObserver;
import fri.util.os.OS;

/**
	Subclasses ZipFile to implement Archive.
*/

public class ZipFile extends java.util.zip.ZipFile implements Archive
{
	private Vector elements = null;
	private File file;
	private File extractRootFolder = null;
	private StringBuffer errors = null;

	
	/**
		Construct a ZIP archive by calling super(f).
	*/
	public ZipFile(File f)
		throws java.util.zip.ZipException, IOException
	{
		super(f);
		this.file = f;
	}
	
	
	/**
		Returns newline separated error messages of files that could
		not be opened when extracting.
	*/
	public String getError()	{
		return errors == null ? null : errors.toString();
	}
	
	
	/**
		List all entries from the already opened archive
		or return buffered enumeration.
	*/
	public Enumeration archiveEntries()	{
		if (elements != null)
			return elements.elements();
		
		elements = new Vector();
		for (Enumeration e = entries(); e.hasMoreElements(); )	{
			java.util.zip.ZipEntry entry = (java.util.zip.ZipEntry)e.nextElement();
			elements.add(new ZipEntry(entry));
		}
		
		return elements.elements();
	}
	
	
	/**
		Closes the archive and catches and reports all Exceptions.
	*/
	public void close()	{
		try	{
			super.close();
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
	}


	/**
		Extract passed entries from already opened archive.
		Ensures that folders of file entries exist.
		Creates folder entries contained in archive even if
		they contain no files.
		<br>MIND: This method sets the filetime to that of the archive entry!
		@param f target directory or file
		@param entries entries to extract. All entries when null.
		@param observer cancel- and progress-observer interface
	*/
	public Hashtable extractEntries(File f, ArchiveEntry [] entries, CancelProgressObserver observer)
	throws
		IOException,
		ZipException
	{
		Enumeration e;
		int cap;
		
		if (entries == null)	{	// extract all entries
			e = entries();
			cap = size();
		}
		else	{	// extract selected entries
			Vector v = new Vector(cap = entries.length);
			
			for (int i = 0; i < entries.length; i++)
				v.add(entries[i].getDelegate());
				
			e = v.elements();
		}

		Hashtable h = new Hashtable(cap, 1.0f);
		
		while (e.hasMoreElements())	{
			if (observer != null && observer.canceled())
				return h;
				
			java.util.zip.ZipEntry z = (java.util.zip.ZipEntry)e.nextElement();

			if (z.isDirectory() == false)	{	// do file
				File tgt = new File(f, z.getName());

				// ensure directory exists
				String dirName = tgt.getParent();
				if (dirName != null)	{
					new File(dirName).mkdirs();
				}

				// ensure filename is platform compatible
				String baseName = ValidFilename.correctFilename(tgt.getName());
				tgt = new File(dirName, baseName);
				
				// extract entry
				BufferedInputStream bis = new BufferedInputStream(getInputStream(z));
				FileOutputStream os = null;
				
				try	{
					os = new FileOutputStream(tgt);
				}
				catch (FileNotFoundException ex)	{	// access denied when file readonly, do not break loop!
					if (errors == null)
						errors = new StringBuffer();
					errors.append(ex.getMessage()+OS.newline());
					
					observer.progress(z.getSize());
				}
				
				if (os != null)	{
					h.put(z.getName(), tgt);	// File was created
					extractRootFolder = f;
	
					new CopyStream(bis, z.getSize(), os, observer, true, true).copy();
					
					tgt.setLastModified(z.getTime());	// set the time of the zip entry to file
				}
			}
			else	{	// do directory
				File tgt = new File(f, z.getName());
				tgt.mkdirs();

				h.put(z.getName(), tgt);	// Directory was created
			}
		}
		
		return h;
	}

	
	
	/**
		Returns an InputStream for a passed entry name.
		The whole archive gets extracted to "/tmp/archiveName/..." and
		a FileInputStream gets returned.
	*/
	public InputStream getInputStream(ArchiveEntry entry, CancelProgressObserver observer)
		throws IOException
	{
		return getInputStream((java.util.zip.ZipEntry)entry.getDelegate());
	}


	/**
		Returns a File for a passed entry name. The file must get
		extracted temporarily.
	*/
	public File getFile(ArchiveEntry entry)
		throws IOException
	{
		File tmp = new File(file.getParent(), "."+file.getName());
		try	{
			Hashtable h = extractEntries(tmp, new ArchiveEntry [] { entry }, null);
			File f = (File)h.elements().nextElement();
			//System.err.println("ZipFile.getFile "+entry.getName()+" -> file is: "+f);
			return f;
		}
		catch (ZipException e)	{
			e.printStackTrace();
		}
		catch (IOException e)	{
			throw e;
		}
		return null;
	}


	/**
		Returns the root folder where the archive has been extracted.
	*/
	public File getExtractRootFolder()	{
		return extractRootFolder;
	}

}