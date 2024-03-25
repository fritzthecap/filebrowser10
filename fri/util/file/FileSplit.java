package fri.util.file;

import java.io.*;
import fri.util.NumberUtil;
import fri.util.io.CopyStream;
import fri.util.observer.CancelProgressContinueConsole;
import fri.util.observer.CancelProgressContinueObserver;

/**
	Splits a file into splitSize parts. The names of the split files
	is <i>[filename].[number].split</i> in a directory named
	<i>[filename].split</i> or in a given directory.
	<p>
	A passed CancelProgressContinueObserver implementation 
	can tell progress and ask for next disk after each created file.
	<p>
	Usage:
	<pre>
		File splitDir = new FileSplit(new File(tooBigFile, 1509949)).split();	// 1.44M
	</pre>
	A FileSplit can be done to 1-n mediums that normally get one file each.
	After each split file written to a medium a dialog for inserting
	a new medium is launched.
*/

public class FileSplit
{
	public final static String EXTENSION = ".split";
	public final static String FLOPPY_SIZE = "1.38M";
	private File from, toDir;
	private long splitSize, len;
	private CancelProgressContinueObserver dlg;
	
	
	/**
		Split a file to many others in a directory.
		@param from source file to split
		@param splitSize size of created split files
	*/
	public FileSplit(File from, long splitSize)
		throws IOException
	{
		this(from, splitSize, null);
	}

	/**
		Split a file to many others in a directory. All files get closed after work.
		@param from source file to split
		@param splitSize size of created split files
		@param dlg Observer dialog for progress, cancel and continue comfirmation, can be null
	*/
	public FileSplit(File from, long splitSize, CancelProgressContinueObserver dlg)
		throws IOException
	{
		this(from, null, splitSize, dlg);
	}
	
	/**
		Split a file to many others in a directory. All files get closed after work.
		@param from source file to split
		@param splitSize size of created split files
		@param dlg Observer dialog for progress, cancel and continue comfirmation, can be null
	*/
	public FileSplit(File from, File toDir, long splitSize, CancelProgressContinueObserver dlg)
		throws IOException
	{
		// check arguments
		if (from.isDirectory())	{
			throw new IOException("Cannot split a directory: "+from);
		}
		
		if (toDir != null && toDir.exists() && toDir.isDirectory() == false)	{
			throw new IOException("Target directory exists and is a file: "+toDir);
		}
		
		if (toDir != null && toDir.isDirectory() && toDir.canWrite() == false)	{
			throw new IOException("Can not write to directory: "+toDir);
		}
		
		// name and create a target directory if not given
		if (toDir == null)	{
			toDir = makeSplitDir(from);
		}
		
		this.len = from.length();
		this.splitSize = splitSize;
		this.from = from;
		this.toDir = toDir;
		this.dlg = dlg;
	}
	
	
	/**
		Start to split file int directory. Cleans the target directory
		before creating the new set of files. All [filename].split will be
		removed.
		@return directory where split files are created.
	*/
	public File split()
		throws IOException
	{
		new DeleteFile(toDir, EXTENSION);
		toDir.mkdirs();

		// check target directory
		if (toDir.isDirectory() == false)	{
			throw new IOException("Could not find or create directory: "+toDir);
		}

		// open source file
		InputStream in  = new BufferedInputStream(new FileInputStream(from));

		// create target files
		try	{
			doSplit(dlg, from.getName(), toDir, in, len, splitSize);
		}
		finally	{
			try	{ in.close(); } catch (IOException e) {}
		}
		
		return toDir;
	}


	/** Returns a default split directory name for passed file. */
	public static File makeSplitDir(File src)	{
		return new File(src.getParent(), src.getName()+EXTENSION);
	}
	
	/** Returns true if passed directory name ends with FileSplit.EXTENSION. */
	public static boolean isSplitDir(File dir)	{
		return dir.getName().endsWith(FileSplit.EXTENSION);
	}
	

	private void doSplit(
		CancelProgressContinueObserver dlg,
		String basename,
		File directory,
		InputStream in,
		long fileSize,
		long splitSize)
		throws IOException
	{
		for (int i = 0; fileSize > 0L; fileSize -= splitSize, i++)	{
			File to = new File(directory, basename+"."+i+EXTENSION);
			long currentSize = (long)Math.min(splitSize, fileSize);
			CopyStream.bufsize = (int)currentSize;
			
			if (askContinue(dlg, to, currentSize))	{
				OutputStream out = new BufferedOutputStream(new FileOutputStream(to));
		
				if (dlg != null)
					dlg.setNote("Creating split file: "+to);
				
				new CopyStream(in, currentSize, out, dlg, true, false).copy();
			}
			else	{
				break;	// user canceled
			}
		}
		
		if (dlg != null)
			dlg.endDialog();
	}


	protected boolean askContinue(CancelProgressContinueObserver dlg, File dest, long splitSize)	{
		if (dlg != null)	{
			return dlg.askContinue("Write next "+NumberUtil.getFileSizeString(splitSize)+" to \""+dest+"\"?");
		}
		return true;
	}


	public static int splitSizeFromString(String len)	{
		len = len.trim();
		
		int factor = 1;
		
		if (len.equals("0"))	{
			len = FLOPPY_SIZE;
		}

		if (len.toUpperCase().endsWith("M"))	{
			factor = 1024 * 1024;
			len = len.substring(0, len.length() - 1).trim();
		}
		else
		if (len.toUpperCase().endsWith("K"))	{
			factor = 1024;
			len = len.substring(0, len.length() - 1).trim();
		}
		
		double splitSize = Double.valueOf(len).doubleValue();
		int size = (int)(((double)factor * splitSize) + 0.5);
		
		return size;
	}
	
	
	/** application main */
	
	public static void main(String [] args)	{
		if (args.length < 2 || args.length > 3)	{
			System.err.println("SYNTAX: java fri.util.file.FileSplit splitsize[M|K] filePath [targetDirectory]");
			System.err.println("	Splits file to parts of splitsize into given targetDirectory or a directory named \"[filename]_split\".");
			System.err.println("	Splitsize 0 (zero) is interpreted as "+FLOPPY_SIZE+" (floppy disk size).");
		}
		else	{
			String len = args[0];
			String src = args[1];
			String tgt = null;
			if (args.length > 2)
				tgt = args[2];
			
			int size = splitSizeFromString(len);
			
			CancelProgressContinueObserver dlg = new CancelProgressContinueConsole();
			File srcFile = new File(src);
			
			try	{
				FileSplit fs = (tgt == null) ?
						new FileSplit(srcFile, size, dlg) :
						new FileSplit(srcFile, new File(tgt), size, dlg);
						
				dlg.setNote("Split size is "+size+" bytes"+(tgt != null ? ", target directory is: "+tgt : ""));
				boolean del = dlg.askContinue("Delete \""+src+"\" after splitting?");
			
				fs.split();
				
				if (del)
					srcFile.delete();
			}
			catch (IOException e)	{
				e.printStackTrace();
			}
		}
	}
	
}