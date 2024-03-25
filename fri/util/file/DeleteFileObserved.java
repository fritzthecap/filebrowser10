package fri.util.file;

import java.io.File;
import java.io.IOException;

import fri.util.observer.CancelProgressObservable;
import fri.util.observer.CancelProgressObserver;

/**
	Extends DeleteFile by adding CancelProgressObserver facility.
	CAUTION: call <i>delete()</i> to get this running! The constructor does
	nothing as this class implements <i>setObservable()</i> interface.
	<p>
	If files are passed to this class, it only calls <i>progress()</i> and
	<i>canceled()</i> and <i>setNote()</i> methods of the observer, not <i>endDialog()</i>.
	<p>
	Errors: ".." path parts make the isLink() method fail, such directories are not looped.
	
	@author Fritz Ritzberger
*/

public class DeleteFileObserved extends DeleteFile implements
	CancelProgressObservable
{
	private CancelProgressObserver observer;
	private File f;
	private String extension;
	
	
	public DeleteFileObserved(File f)	{
		this(f, null);
	}
	
	public DeleteFileObserved(File f, String extension)	{
		this.f = f;
		this.extension = extension;
	}

	/** Implements CancelProgressObservable: set the observer object. */
	public void setObserver(CancelProgressObserver observer)	{		
		this.observer = observer;
	}
	
	/** Start to delete recursively. */
	public void delete()	{
		try	{
			super.loop(f, extension);
		}
		catch (RuntimeException e)	{
			System.err.println("Canceled delete!");
		}
	}
	
	/** Overridden to let store the observer in constructor. */
	protected void loop(File f, String extension)	{
	}
	
	
	/**
		Implementation of: Visit a file or directory.
	*/
	protected void visit(File f)	{
		if (observer != null && observer.canceled())	{
			throw new RuntimeException();
		}
		else	{
			long size = -1L;
			if (observer != null)	{
				observer.setNote(f.getName());
				size = getObserverProgressSize(f);
			}
				
			super.visit(f);

			if (size >= 0L)
				observer.progress(size);
		}
	}
	
	/**
	 * Returns -1 to NOT report progress on deletion (is faster).
	 * To be overridden by systems that want to set file-length delete progress to observer.
	 * @param f the file about to be deleted
	 * @return the size of the file when progress should be reported, else less than 0.
	 */
	protected long getObserverProgressSize(File f)	{
		return -1L;
	}




	/** Deleted all pased file arguments, despite of they are directories or files. */
	public static void main(String [] args)
		throws IOException
	{
		CancelProgressObserver observer = new CancelProgressObserver()	{
			public boolean canceled()	{ return false; }
			public void progress(long portion)	{ System.out.print(""+portion+" "); }
			public void setNote(String note)	{ System.out.print("\ndeleting "+note+" "); }
			public void endDialog()	{ System.out.println("\nFinished delete."); }
		};
		
		for (int i = 0; i < args.length; i++)	{
			DeleteFileObserved delete = new DeleteFileObserved(new File(args[i]));
			delete.setObserver(observer);
			delete.delete();
		}
		observer.endDialog();
	}

}
