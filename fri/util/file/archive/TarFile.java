package fri.util.file.archive;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import com.aftexsw.util.bzip.CBZip2InputStream;
import fri.util.observer.CancelProgressObserver;
import fri.util.tar.*;

/**
	Implementing Archive.
	Provide a buffered index of all the tar archive entries.
*/

public class TarFile implements Archive
{
	private ObserveableTarArchive delegate;
	private File file;
	private boolean isGZip, isBZip2;
	private Vector elements = null;
	private Hashtable files = null, noDeleteOnClose;
	private File extractRootFolder = null;
	private String error = null;
	private boolean needAllEntries;
	
	
	/**
		Create a tar archive object. This does no open or read. The needAllEntries setting is false.
	*/
	public TarFile(File file, boolean isGZip, boolean isBZip2)	{
		this.file = file;
		this.isGZip = isGZip;
		this.isBZip2 = isBZip2;
	}
	
	/**
		Create a tar archive object. This does no open or read.
		@param willNeedAllEntries set this to true when all entries will be needed, default is false.
				True will be useful when searching all entries for some pattern, false when peeking only one entry.
	*/
	public TarFile(File file, boolean isGZip, boolean isBZip2, boolean willNeedAllEntries)	{
		this(file, isGZip, isBZip2);
		this.needAllEntries = willNeedAllEntries;
	}
	
	
	private void openDelegate()	
		throws Exception
	{
		InputStream is = new FileInputStream(file);
		
		if (isBZip2)	{
			byte [] magic = new byte[2];	// must contain "BZ"
			int cnt = is.read(magic);
			if (cnt != 2 || magic[0] != 'B' || magic[1] != 'Z')
				throw new IOException("This is not a BZIP2 archive: "+file.getName());
			is = new CBZip2InputStream(is);
		}
		else
		if (isGZip)	{
			is = new GZIPInputStream(is);
		}
		
		delegate = new ObserveableTarArchive(is);
	}

	private void closeDelegate()	{
		if (delegate == null)
			return;

		try	{
			delegate.closeArchive();
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
		delegate = null;
	}
	

	/**
		Closes the archive if ever opened and catches and reports IOException.
		Deletes temporarily extracted files except those for that getFile()
		was called at least once.
	*/
	public void close()	{
		closeDelegate();
		
		// delete temporarily created files
		if (files != null)	{
			// delete files
			for (Enumeration e = files.elements(); e.hasMoreElements(); )	{
				File f = (File)e.nextElement();
				
				if (false == f.isDirectory() && (noDeleteOnClose == null || noDeleteOnClose.get(f) == null))
					deleteTmpFile(f);
			}
			
			// delete folders
			for (Enumeration e = files.elements(); e.hasMoreElements(); )	{
				File f = (File)e.nextElement();
				if (f.isDirectory() && (noDeleteOnClose == null || noDeleteOnClose.get(f) == null))
					deleteTmpFile(f);
			}
			
			noDeleteOnClose = files = null;
		}
	}

	private void deleteTmpFile(File f)	{
		f.delete();	// delete it
					
		String parent = null;	// seek to parents for empty directories
		do	{
			parent = f.getParent();

			if (parent != null)	{
				f = new File(parent);
				String [] list = f.list();
				if (list == null || list.length <= 0)	{
					f.delete();
				}
			}
		}
		while (parent != null);
	}
	
	/**
		List buffered entries or opens and reads the archive and provides it.
	*/
	public Enumeration archiveEntries()	{
		if (elements != null)
			return elements.elements();

		elements = new Vector();
		
		try	{
			openDelegate();	// input stream is only once useable
			for (Enumeration e = delegate.entries(); e.hasMoreElements(); )	{
				SelectiveTarEntry entry = (SelectiveTarEntry)e.nextElement();
				elements.add(new TarEntry(entry));
			}
			closeDelegate();	// input stream is only once useable
		}
		catch (Exception e)	{
			error = e.toString();
			e.printStackTrace();
		}

		return elements.elements();
	}
	

	/**
		Extract passed entries. This call opens and reads the entire archive again,
		as TarArchive does not provide a <code>entry.getInputStream()</code> method.
		@param f target directory or file
		@param entries entries to extract. All entries when null.
		@param observer cancel- and progress-observer interface
	*/
	public Hashtable extractEntries(File f, ArchiveEntry [] entries, CancelProgressObserver observer)
		throws Exception
	{
		openDelegate();	// input stream is only once useable

		// create a delegate object array
		SelectiveTarEntry [] e = null;
		if (entries != null)	{
			e = new SelectiveTarEntry[entries.length];
			for (int i = 0; i < entries.length; i++)
				e[i] = (SelectiveTarEntry)entries[i].getDelegate();
		}
		
		extractRootFolder = f;

		// extract entries
		Hashtable h = delegate.extractEntries(f, e, observer);

		close();	// input stream is only once useable
			
		return h;
	}
	

	/**
		Get error messages from opening or reading the archive (not from constructing object).
	*/
	public String getError()	{
		return error != null ? error : delegate != null ? delegate.getError() : null;
	}


	/**
		Returns an InputStream for a passed entry name.
		The whole archive gets extracted when option needAllEntries is active,
		else only the passed entry.
		<br>MIND: This method sets the filetime to that of the archive entry!
	*/
	public InputStream getInputStream(ArchiveEntry entry, CancelProgressObserver observer)
		throws IOException
	{
		if (entry.isDirectory())
			return null;
			
		ensureFiles(entry, observer);
		
		// now we can return the input stream from file
		File f = (File) files.get(entry.getName());

		return new FileInputStream(f);
	}


	private void ensureFiles(ArchiveEntry entry, CancelProgressObserver observer)
		throws IOException
	{
		File f = files != null ? (File) files.get(entry.getName()) : null;

		// TAR does not provide an input stream from archive, we must extract temporarily
		if (f == null)	{
			File tmp = new File("."+file.getParent(), "."+file.getName());
			System.err.println("extracting tar archive to "+tmp);
			
			try	{
				Hashtable h = extractEntries(tmp, needAllEntries ? null : new ArchiveEntry [] { entry }, observer);
				if (files == null)
					files = h;
				else
					files.putAll(h);
			}
			catch (IOException e)	{
				throw e;
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
			System.err.println("... extracted from tar archive");
		}
	}


	/**
		Returns a File for a passed entry name. The file gets
		extracted temporarily. If this method was called, the file will
		not be deleted on <code>archive.close()</code> call.
	*/
	public File getFile(ArchiveEntry entry)
		throws IOException
	{
		ensureFiles(entry, null);
		
		File f = (File) files.get(entry.getName());
		//System.err.println("TarFile.getFile "+entry.getName()+" -> file is: "+f);
		
		if (noDeleteOnClose == null)
			noDeleteOnClose = new Hashtable();
		noDeleteOnClose.put(f, f);
		
		return f;
	}


	/**
		Returns the root folder where the archive has been extracted.
	*/
	public File getExtractRootFolder()	{
		return extractRootFolder;
	}

}