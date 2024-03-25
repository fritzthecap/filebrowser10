package fri.gui.swing.mailbrowser;

/**
	Thread that receives mails periodically by calling a controller.
*/

public class ReceiveThread extends Thread
{
	private boolean stopped;
	private long interval;
	private FolderController controller;
	
	public ReceiveThread(FolderController controller, int minutes)	{
		this.interval = minutes * 60 * 1000;	// calculate milliseconds to sleep
		this.controller = controller;	// needed to receive mails
		setPriority(Thread.MIN_PRIORITY);	// do not slow down the GUI
		start();	// starts thread
	}

	public void run()	{
		System.err.println("===================> mail watcher thread started.");
		boolean isFirst = true;
		
		while (stopped == false)	{
			try	{
				sleep(isFirst ? 5000 : interval);
				isFirst = false;
			}
			catch (InterruptedException e)	{
			}
			
			if (stopped == false && controller.isReceiving() == false)
				controller.cb_Receive(null);
		}
		System.err.println("===================> mail watcher thread was stopped.");
	}

	public void setStopped()	{
		stopped = true;
	}

}
