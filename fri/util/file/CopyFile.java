package fri.util.file;

import java.io.*;
import fri.util.io.CopyStream;
import fri.util.observer.CancelProgressObserver;

/**
	Copy the contents of a file (or directory) to another file (or directory).
	<p>
	MIND: you must call copy() to get this class running!
	<p>
	If files are passed to this class, it only calls <i>progress()</i> and
	<i>canceled()</i> methods of the observer, not <i>setNote()</i> or <i>endDialog()</i>.
	The <i>setNote()</i> method is called when passed arguments are directories.
	
	@author Fritz Ritzberger
*/

public class CopyFile extends CopyStream
{
	private File to;

	/**
	 Copy a file to another one. Both files get closed at end.
	 Both arguments can be directories or files. If <i>to</i> is a directory
	 and <i>from</i> is a file, the destination is assumed to be a new File in <i>to</i> directory.
	 When <i>to</i> is a file and <i>from</i> is a directory an IOEception is thrown.
	 
	 @param from the source file or directory to be copied
	 @param to the target file or directory to copy to
	 @param dlg Observer dialog for progress and cancel, can be null
	 @exception IOException for read/write errors, or if "from" is a directory and "to" an existing file.
	 */
	public CopyFile(
		File from,
		File to,
		CancelProgressObserver dlg)
		throws IOException
	{
		this.to = to;
		this.dialog = dlg;
		
		if (from.isFile() == false)	{
			if (to.isFile())	{
				throw new IOException("Can not overwrite a existing file with a directory, copying "+from+" to "+to);
			}
			new CopyDirectory(from, to, dlg).copy();
		}
		else	{
			if (to.isDirectory())	{
				to = new File(to, from.getName());
			}
	
			InputStream in  = new BufferedInputStream(new FileInputStream(from));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(to));
			
			this.size = from.length();
			this.out = out;
			this.in = in;
			this.doCloseOut = true;
			this.doCloseIn = true;
		}
	}

	/**
		To be overridden by subclasses that must perform cleanup on error.
		This gets called only when IOException was thrown.
	*/
	protected void finalActionOnError()	{
		try	{ to.delete(); } catch (Exception ex)	{}
	}



	/** Copies files arg[0] to arg[1], despite of they are directories or files. */
	public static void main(String [] args)
		throws IOException
	{
		CancelProgressObserver observer = new CancelProgressObserver()	{
			public boolean canceled()	{ return false; }
			public void progress(long portion)	{ System.out.print(""+portion+" "); }
			public void setNote(String note)	{ System.out.print("\ncopying "+note+" "); }
			public void endDialog()	{ System.out.println("\nFinished copying."); }
		};
		
		new CopyFile(new File(args[0]), new File(args[1]), observer).copy();
		observer.endDialog();
	}
	
}



class CopyDirectory extends RecursiveFileVisitor
{
	private String sourcePath;
	private String targetPath;
	private CancelProgressObserver dlg;
	private IOException exception;
	
	
	CopyDirectory(File sourceDir, File targetDir, CancelProgressObserver dlg)
		throws IOException
	{
		this.dlg = dlg;
		this.sourcePath = Link.getCanonicalPath(sourceDir);
		this.targetPath = Link.getCanonicalPath(targetDir) + File.separator + sourceDir.getName();

		targetDir = new File(targetPath);
		if (targetDir.exists() == false && targetDir.mkdirs() == false)
			throw new IOException("Could not create target directory: "+targetDir);
	}

	public void copy()
		throws IOException
	{
		try	{
			super.loop(new File(sourcePath), null, true);	// true: visit directories before contained files!
		}
		catch (RuntimeException e)	{
			throw exception;
		}
	}
	
	protected void visit(File f)	{
		String relativeSrc = Link.getCanonicalPath(f).substring(sourcePath.length());
		String target = targetPath + File.separator + relativeSrc;
		File newFile = new File(target);
		
		if (f.isFile())	{
			try	{
				if (dlg != null)
					dlg.setNote(f.getName());
				new CopyFile(f, newFile, dlg).copy();
			}
			catch (IOException e)	{
				exception = e;
				throw new RuntimeException("IOException occured: "+e.getMessage());
			}
		}
		else	{
			if (newFile.exists() == false && newFile.mkdirs() == false)	{
				exception = new IOException("Could not create subdirectory: "+newFile);
				throw new RuntimeException();
			}
		}
	}

}
