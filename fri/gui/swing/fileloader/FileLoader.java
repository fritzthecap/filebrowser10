package fri.gui.swing.fileloader;

import java.io.File;
import java.awt.EventQueue;

/**
	Loading a file in a background thread.
*/

public abstract class FileLoader extends Thread
{
	protected File file;
	private LoadObserver loadObserver;
	private Object waiter;
	protected boolean interrupted = false;

	/**
		Create new FileLoader for a JTextComponent.
		@param file file to load from disk
		@param loadObserver listener which is interested in status of loading.
			At start setLoading(true) is called, at end setLoading(false). Can be null.
		@param waiter object to notify() when finished loading, nullable
	*/
	public FileLoader(
		File file,
		LoadObserver loadObserver,
		Object waiter)
	{
		this.file = file;
		this.loadObserver = loadObserver;
		this.waiter = waiter;
	}



	/** Break the read loop. */
	public void interrupt()	{
		interrupted = true;
	}
	
	
	/** implements Runnable */
	public void run() {
		startLoading();
		
		setPriority(Thread.MIN_PRIORITY);
		
		int len = getLength();
		
		beforeWork();

		System.err.println("start working on "+file);

		try {
			work(len);
		}
		catch (Exception e) {
			error(e);
		}
		finally	{
			System.err.println("FileLoader finished loading "+file);
			
			if (loadObserver != null && interrupted == false)	{
				EventQueue.invokeLater(new Runnable()	{
					public void run()	{
						stopLoading();
					}
				});
			}
	
			afterWork();

			if (waiter != null)	{	// if someone is waiting, wake up
				synchronized(waiter)	{
					waiter.notifyAll();
				}
			}
		}
	}


	/** Does nothing. Called before loading starts, after startLoading(). */
	protected void beforeWork()	{
	}

	/** Does nothing. Called after loading ended, after stopLoading(). */
	protected void afterWork()	{
	}


	/**
		Implement this method that opens a FileReader or FileInputStream and does the loading work.
		Any thrown exception will be caught and delegated to <i>error()</i>.
	*/
	protected abstract void work(int len) throws Exception;



	/** Returns the length of the file to load. */
	protected int getLength()	{	
		return (int)file.length();
	}
	
	/** Notifies a LoadObserver by calling its <i>setLoading(true)</i> method. */
	protected void startLoading()	{
		if (loadObserver != null)
			loadObserver.setLoading(true);
	}
	
	/** Notifies a LoadObserver by calling its <i>setLoading(false)</i> method. */
	protected void stopLoading()	{
		if (loadObserver != null)
			loadObserver.setLoading(false);
	}
	
	
	/** Any exception thrown in <i>work()</i> comes to here. */
	protected void error(Exception e)	{
		System.err.println("FileLoader.error(), Exception is:");
		e.printStackTrace();
	}
	
}