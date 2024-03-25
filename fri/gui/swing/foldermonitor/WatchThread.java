package fri.gui.swing.foldermonitor;

import java.io.File;

class WatchThread extends Thread
{
	private int pauseMillis;
	private boolean stopped;
	private boolean suspended;
	private File [] roots;
	private EventRenderer renderer;
	
	
	WatchThread(File [] roots, EventRenderer renderer, int pauseMillis)	{
		this.roots = roots;
		this.renderer = renderer;
		this.pauseMillis = pauseMillis;
	}


	public void setStopped()	{
		this.stopped = true;
	}
	
	public void setSuspended(boolean suspended)	{
		this.suspended = suspended;
	}
	
	public boolean isInterrupted()	{
		return stopped || suspended;
	}

	
	public void run()	{
		FileCache cache = new FileCache(renderer, this);
		boolean doCheck = false;
		
		while (stopped == false)	{
			if (suspended == false)	{
				//System.err.println("WatchThread "+hashCode()+" working after pause of "+pauseMillis+" millis ...");
				for (int i = 0; i < roots.length; i++)	{	// fill cache or check for new and modified files
					new Visitor(renderer, this, roots[i], cache, doCheck);
				}
				doCheck = true;
				cache.checkFiles();	// check for deleted files
			}
				
			if (pauseMillis > 0)
				try	{ Thread.sleep(pauseMillis); }	catch (Exception e)	{}
		}
		//System.err.println("WatchThread "+hashCode()+" has been finished!");
	}

}