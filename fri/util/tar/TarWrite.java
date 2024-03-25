package fri.util.tar;

import java.io.*;
import java.util.zip.*;
import com.ice.tar.*;
import fri.util.zip.*;
import fri.util.FileUtil;

/**
	Generate a G-zipped TAR archive instead of a ZIP,
	whereby the relative path capabilities of superclass is used.
*/

public class TarWrite extends ZipWrite
{
	private SelectiveTarArchive tArchive;
	
	protected TarWrite()	{
		super();
	}

	/**
		Prepare writing of files to a TAR archive by zipFilesTo().
		The default archive File is retrievable after that by getDefaultArchive().
	*/
	public TarWrite(String [] filenames)	{
		super(filenames);
	}


	/**
		Writing files to a TAR archive by constructor.
		The archive File is explicitely given.
	*/
	public TarWrite(File archive, String [] filenames)
		throws Exception
	{
		super(archive, filenames);
	}


	/**
		Create a file with extension ".tgz" instead of ".zip".
	*/
	public File getDefaultArchive()	{
		File f = super.getDefaultArchive();
		return new File(FileUtil.cutExtension(f.getPath())+archiveExtension());
	}

	/** Returns an extension for the archive output. To be overridden for BZip2 compression. */
	protected String archiveExtension()	{
		return ".tar.gz";
	}
	

	/** Tar the files to the given archive name. */	
	public void zipFilesTo(File archive)
		throws Exception
	{
		OutputStream fos = openOutputStream(archive);
		OutputStream cos = openCompressedStream(fos);
		tArchive = createTarArchive(cos);

		try	{
			writeZip(files);
		}
		catch (Exception e)	{
			System.err.println("ERROR: close and delete tar at exception "+e);
			try	{ tArchive.closeArchive(); archive.delete(); }	catch (Exception ex)	{}
			throw e;
		}

		tArchive.closeArchive();
	}

	/** Returns a compression stream from passed FileOutputStream. To be overridden for BZip2 compression. */
	protected OutputStream openCompressedStream(OutputStream fos)
		throws IOException
	{
		return new GZIPOutputStream(fos);
	}


	/** Method to create TarArchive. Override to create observed TarArchive. */
	protected SelectiveTarArchive createTarArchive(OutputStream tar)	{
		return new SelectiveTarArchive(tar);
	}
	

	/** Write one entry to zip, reading from file (can be directory) */
	protected void writeEntry(File file)
		throws Exception
	{
		String name = makeRelativePathName(file);
		
		TarEntry t = new TarEntry(file);
		t.setName(name);
		
		tArchive.writeEntry(t, false);
	}


	/** test main */
	public static void main(String [] args)
		throws Exception
	{
		if (args.length < 1)	{
			System.err.println("SYNTAX: TarWrite file file folder ...");
			System.exit(0);
		}
		new TarWrite(new File("TarWrite.tgz"), args);
	}

}
