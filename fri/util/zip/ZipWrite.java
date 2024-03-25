package fri.util.zip;

import java.util.Hashtable;
import java.util.zip.*;
import java.io.*;
import fri.util.file.Link;
import fri.util.FileUtil;
import fri.util.os.OS;

/**
	Write files to a zip archive.
	This object accepts absolute pathes and makes them relative to
	the common parent path.
*/

public class ZipWrite
{
	/** set to false to put all entries as STORED (uncompressed) files. */
	public boolean bigFastZip = false;
	/** retrieve error message after invocation */
	public String error = null;
	private int bufsize = 1048576;	// 1 MB copy block size
	private ZipOutputStream zip;
	protected File [] files;
	private String commonPath = null;
	private Hashtable checkTable;

	protected ZipWrite()	{
	}

	/**
		Prepare writing of files to a ZIP archive by zipFilesTo().
		The default archive File is retrievable after that by getDefaultArchive().
		This constructor DOES NOT build the archive.
	*/
	public ZipWrite(String [] filenames)	{
		init(filenames);
	}

	/**
		Write files to an explicitely given ZIP archive by constructor.
		This constructor builds the archive.
	*/
	public ZipWrite(File archive, String [] filenames)
		throws Exception
	{
		init(filenames);
		zipFilesTo(archive);
	}


	/** Returns list of files to zip, for a delegate object. */
	public File [] getFiles()	{
		return files;
	}

	/** set the filenames and check for conflicts */
	protected void init(String [] filenames)	{
		files = buildCommonPath(filenames);
	}
		
	/** Returns a default name for the archive, made from common path. Includes extension. */
	public File getDefaultArchive()	{
		if (files.length <= 0)	{
			System.err.println("FEHLER: no files have been passed to zip.");
			return null;
		}
		
		if (files.length == 1)
			return new File(FileUtil.cutExtension(files[0].getPath())+".zip");
		
		String pathPart = commonPath != null ? commonPath : "";
		File a = new File(pathPart+".zip");
		if (a.getName().startsWith(".zip"))	// tricked by Windows
			return new File(FileUtil.cutExtension(files[0].getPath())+".zip");
		else
			return a;
	}
	

	/** Zip the files to the given archive name. */	
	public void zipFilesTo(File archive)
		throws Exception
	{
		OutputStream out = openOutputStream(archive);
		zip = openZipOutputStream(out);
		System.err.println("opened zip "+archive);
		
		try	{
			writeZip(files);
		}
		catch (Exception e)	{
			zip.close();
			System.err.println("close and delete zip at exception "+e);
			try	{ archive.delete(); }	catch (Exception ex)	{}
			throw e;
		}
		zip.close();
	}

	protected OutputStream openOutputStream(File file)
		throws Exception
	{
		return new BufferedOutputStream(new FileOutputStream(file));
	}
	
	/** Override this to get a jar output stream. @return an open zip output stream */
	protected ZipOutputStream openZipOutputStream(OutputStream os)
		throws Exception
	{
		return new ZipOutputStream(os);
	}


	/** @return the size of all files to copy */
	public long getRecursiveSize()	{
		long size = 0L;
		for (int i = 0; i < files.length; i++)	{
			size += getRecursiveSize(files[i]);
		}
		return size;
	}
	
	private long getRecursiveSize(File f)	{
		if (f.isDirectory())	{
			File [] files = getChildren(f, f.list());
			long size = 0L;
			if (files != null)
				for (int i = 0; i < files.length; i++)
					size += getRecursiveSize(files[i]);
			return size;
		}
		else
			return f.length();
	}


	/** Returns a pathname relative to common path and replace File.separatorChar by '/'. */
	protected String makeRelativePathName(File file)	{
		String name = commonPath != null
				? FileUtil.makeRelativePath(commonPath, file.getPath())
				: makeRelative(file);

		name = name.replace(File.separatorChar, '/');

		if (file.isDirectory() && name.endsWith("/") == false)
			name = name+"/";	// hardcoded like in ZipEntry.java

		return name;
	}
	
	/** Jeder fuehrende <i>File.separator</i> oder ein Windows-Drive wird entfernt. */
	private String makeRelative(File file)	{
		String path = file.getAbsolutePath();

		if (OS.isWindows && path.length() > 2)	{	// check for WINDOWS drive letter
			char drive = path.charAt(0);
			char driveEnd = path.charAt(1);
			char pathSep = path.charAt(2);
			if ((drive >= 'A' && drive <= 'Z' || drive >= 'a' && drive <= 'z') && driveEnd == ':' && pathSep == '\\')
				path = drive+"/"+path.substring(3);
		}

		if (path.startsWith(File.separator))	{
			while (path.startsWith(File.separator))
				path = path.substring(1);
		}

		return path;
	}

	
	// check for the common basedir and throw exception if none, returns arry of files to be written.
	private File [] buildCommonPath(String [] filenames)	{
		if (filenames.length <= 0)
			throw new IllegalArgumentException("Can not find common path for no files!");
			
		File [] files = new File [filenames.length];
		String [][] pathesByParts = new String [filenames.length] [];
		int min = Integer.MAX_VALUE;
		
		// make all files absolute, retrieve minimal path parts length
		for (int i = 0; i < filenames.length; i++)	{
			String resolvedDots = FileUtil.resolveLeadingDots(filenames[i]);
			String absolute = new File(resolvedDots).getAbsolutePath();
			files[i] = new File(absolute);
			pathesByParts[i] = FileUtil.getPathComponents(files[i], true, false);	// with root, not canonical
			min = Math.min(pathesByParts[i].length, min);
		}
		
		// loop over all files to retrieve a common path
		int commonCount = -1;

		for (int pos = 0; commonCount < 0 && pos < min; pos++)	{	// loop parts up to minimal path array length
			String comparePart = null;
			
			for (int i = 0; i < pathesByParts.length; i++)	{	// loop over all pathes
				String part = pathesByParts[i][pos];
				
				if (comparePart == null)
					comparePart = part;
				else
					if (comparePart.equals(part) == false)
						commonCount = pos;	// terminates loop
			}
		}
		
		if (OS.isWindows == false && commonCount > 0 || OS.isWindows == true && commonCount > 1)	{	// if there was a common path
			String [] common = new String[commonCount];
			System.arraycopy(pathesByParts[0], 0, common, 0, commonCount);	// take first as all are equal up to commonCount

			this.commonPath = FileUtil.makePath(common);
		}
		else
		if (filenames.length == 1)	{	// simply one file
			String [] common = new String[pathesByParts[0].length - 1];
			System.arraycopy(pathesByParts[0], 0, common, 0, common.length);
			
			this.commonPath = FileUtil.makePath(common);
		}
		
		return files;
	}
	

	/** 
		Copy inut stream to output stream. Outputstream is not closed as it is reused.
	*/
	protected void doCopy(InputStream in, long fileSize, OutputStream out)
		throws IOException
	{
		try	{
			copyInStreamToOutStream(in, fileSize, out, this.bufsize);
		}
		catch (IOException e)	{
			error = e.toString();
			throw e;
		}
	}
	
	/** A general copy method for input to output stream, out is not closed, in is closed. */
	private static void copyInStreamToOutStream(
		InputStream in,
		long fileSize,
		OutputStream out,
		int bufsize)
		throws IOException
	{
		try	{
			long readBytes = 0;
			byte [] blob = null;
			long diff = fileSize;

			while (diff > 0)	{
				if (diff < bufsize)
					bufsize = (int)diff;

				if (blob == null)
					blob = new byte[bufsize];

				if (in.read(blob, 0, bufsize) != bufsize)	{
					in.close();
					String error = "failed reading "+bufsize+" bytes at "+readBytes;
					throw new IOException(error);
				}
				
				out.write(blob, 0, bufsize);
				readBytes += bufsize;
				diff = fileSize - readBytes;
				
			}	// end while
				
			in.close();
			
		}	// end try
		catch (IOException e)	{
			try	{ in.close(); }	catch (Exception ex)	{}
			throw e;
		}
	}



	/**
		Get the list of Files from parent File and children Strings.
		Override this to filter files.
	*/
	protected File [] getChildren(File parent, String [] list)	{
		if (list == null)
			return null;
			
		if (Link.isLink(parent))
			return null;
			
		File [] files = new File [list.length];
		for (int i = 0; i < list.length; i++)
			files[i] = new File(parent, list[i]);
			
		return files;
	}
	
	// write a subdir to zip
	private void writeDirToZip(File parent, String [] list)
		throws Exception
	{
		writeZip(getChildren(parent, list));
	}


	/** Loop over all files and zip them, if directory, go into it. */
	protected void writeZip(File [] files)
		throws Exception
	{
		for (int i = 0; files != null && i < files.length; i++)	{
			//System.err.println("zipping "+files[i]);
			if (checkTable == null)	{
				checkTable = new Hashtable();
			}

			if (checkTable.get(files[i]) == null)	{
				checkTable.put(files[i], files[i]);
				writeEntry(files[i]);
				
				if (files[i].isDirectory())	{
					writeDirToZip(files[i], files[i].list());
				}
			}
		}
	}


	/** Write one entry to zip, reading from file (can be directory) */
	protected void writeEntry(File file)
		throws Exception
	{
		InputStream in = null;
		if (file.isDirectory() == false)	{
			in = new BufferedInputStream(new FileInputStream(file));
		}
		
		String name = makeRelativePathName(file);
		
		ZipEntry z = getArchiveEntry(name);
		
		z.setSize(file.isDirectory() ? 0 : file.length());
		z.setTime(file.lastModified());

		if (file.isDirectory() || bigFastZip)	{
			z.setMethod(ZipEntry.STORED);
			z.setCompressedSize(z.getSize());	// muss gesetzt werden
			z.setCrc(0);	// irgendein Wert zwischen 0 und FFF...
		}
		else	{
			z.setMethod(ZipEntry.DEFLATED);
		}

		try	{
			zip.putNextEntry(z);
		}
		catch (ZipException e)	{
			e.printStackTrace();
			return;
		}

		if (in != null)
			doCopy(in, file.length(), zip);
			
		//zip.closeEntry();
	}


	/** Override this to return a jar entry.
		@return new Entry for archive containing passed file name.
	*/
	protected ZipEntry getArchiveEntry(String name)	{
		return new ZipEntry(name);
	}

	


	/** test main */
	public static void main(String [] args)
		throws Exception
	{
		if (args.length < 1)	{
			System.err.println("SYNTAX: ZipWrite file file folder ...");
			System.exit(0);
		}
		new ZipWrite(new File("ZipWrite.zip"), args);

		/*
		ZipWrite zw = new ZipWrite(args);
		File [] files = zw.files;
		for (int i = 0; i < files.length; i++)	{
			System.err.println("file="+files[i]+", zipname="+zw.makeRelativePathName(files[i]));
		}
		System.err.println("commonPath="+zw.commonPath);
		*/
	}
}