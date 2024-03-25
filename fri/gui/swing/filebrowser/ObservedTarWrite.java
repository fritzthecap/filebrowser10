package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.io.*;
import com.aftexsw.util.bzip.CBZip2OutputStream;

import fri.util.io.CopyStream;
import fri.util.tar.*;

/**
	Delegates to a TarWrite object to write TAR archives.
*/

public class ObservedTarWrite extends ObservedZipWrite
{	
	private TarWrite delegateTarWrite;	
	private boolean bZip2;	
	
	public ObservedTarWrite(
		Component parent,
		NetNode [] n,
		boolean bZip2,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
		throws Exception
	{
		this.filter = filter;
		this.include = include;
		this.showfiles = showfiles;
		this.showhidden = showhidden;
		this.bZip2 = bZip2;
		
		init(parent, n);
	}

	
	public ObservedTarWrite(Component parent, NetNode [] n, boolean bZip2)
		throws Exception
	{
		this.bZip2 = bZip2;
		
		init(parent, n);
	}


	/** Overridden allocate delegate and prepare file list for counting bytes. */
	protected void init(String [] filenames)	{
		this.delegateTarWrite = new DelegateTarWrite(filenames);
		this.files = delegateTarWrite.getFiles();	// for getRecursiveSize(), with wait cursor
	}
	
	// overriding ObservedZipWrite methods
	
	/** Overridden to set title "Tar" */
	protected String getProgressDialogTitle()	{
		return "Tar";
	}

	// overriding ZipWrite methods, delegate to TarWrite
	
	/** Delegate to TarWrite. */
	public File getDefaultArchive()	{
		return delegateTarWrite.getDefaultArchive();
	}
	

	/** Delegate to TarWrite. */
	public void zipFilesTo(File archive)
		throws Exception
	{
		delegateTarWrite.zipFilesTo(archive);
	}



	// TarArchve must learn to be observed: copy-method is overridden.
	// TarArchve can be compressed by either GZip or BZip2.
	
	private class DelegateTarWrite extends TarWrite
	{
		public DelegateTarWrite(String [] filenames)	{
			super(filenames);
		}
		
		/** Overridden to create observed TarArchive. */
		protected SelectiveTarArchive createTarArchive(OutputStream tar)	{
			return new SelectiveTarArchive(tar)	{
				/** Overridden to use CopyStream for transfering bytes. */
				protected void copyInputStreamToOutputStream(
					InputStream in,
					long size,
					OutputStream out)
				throws IOException
				{
					new CopyStream(in, size, out, dlg, false, false).copy();
					// Copy without closing out-stream, as next entry gets written to it.
					// Do not close in-stream, as TarArchive does this.
				}
			};
		}

		/** Returns a compression stream from passed FileOutputStream. To be overridden for BZip2 compression. */
		protected OutputStream openCompressedStream(OutputStream fos)
			throws IOException
		{
			if (bZip2)	{
				String bzHeader = "BZ";	// BZip2 file header characters
				fos.write(bzHeader.getBytes());
				return new CBZip2OutputStream(fos);
			}
			return super.openCompressedStream(fos);
		}

		/** Returns an extension for the archive output. To be overridden for BZip2 compression. */
		protected String archiveExtension()	{
			return bZip2 ? ".tar.bz2" : super.archiveExtension();
		}
	
	}

}
