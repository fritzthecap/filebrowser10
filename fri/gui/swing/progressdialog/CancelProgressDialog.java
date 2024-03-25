package fri.gui.swing.progressdialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import fri.util.observer.CancelProgressObserver;
import fri.util.NumberUtil;
import fri.gui.swing.ComponentUtil;

/**
	A Dialog for observing and canceling long lasting transactions.
	Mind that you must set <i>isByteSize</i> to false if your progress
	sum is anything other than bytes.
*/

public class CancelProgressDialog implements
	CancelProgressObserver,
	ActionListener
{
	private Component frame;
	private String label;
	private Runnable runnable;
	private Runnable finish;
	private long length;
	private long sum;
	private Thread thread;
	private Timer timer;
	private CancelMonitor delegate;
	private boolean ended;
	private boolean canceled;
	private boolean isByteSize = true;
	private boolean closeWhenMaximumReached = true;
	
	/**
		Create a progress/cancel dialog with aimed size but without startable procedure.
		This constructor does NOT run the thread!
	*/
	public CancelProgressDialog(
		Component frame,
		String label)
	{
		this(frame, label, 0L);
	}
	
	/**
		Create a progress/cancel dialog with aimed size but without startable procedure.
		This constructor does NOT run the thread!
	*/
	public CancelProgressDialog(
		Component frame,
		String label,
		long length)
	{
		this(frame, label, null, null, length);
	}
	
	/**
		Create a progress/cancel dialog without aimed size.
		This constructor does NOT run the thread!
	*/
	public CancelProgressDialog(
		Component frame,
		String label,
		Runnable runnable)
	{
		this(frame, label, runnable, null);
	}

	/**
		Create a progress/cancel dialog without aimed size.
		This constructor does NOT run the thread!
	*/
	public CancelProgressDialog(
		Component frame,
		String label,
		Runnable runnable,
		Runnable finish)
	{
		this(frame, label, null, finish, 0L);
		this.runnable = runnable;
	}

	/**
		Create a progress/cancel dialog and run it if runnable is not null.
	*/
	public CancelProgressDialog(
		Component frame,
		String label,
		Runnable runnable,
		Runnable finish,
		long length)
	{
		this.frame = frame;
		this.label = label;
		this.finish = finish;
		this.runnable = runnable;
		setTransferSize(length);
		
		if (runnable != null)
			start();
	}
	
	
	/** Sets the gloabl length to be transferred within this observation. */
	public void setTransferSize(long length)	{
		if (delegate != null)
			throw new IllegalStateException("Can not set size to dialog after it has been constructed!");
			
		this.length = length;
	}
	
	/** Call this method if progress length is anything other than bytes! */
	public void setIsByteSize(boolean isByteSize)	{
		this.isByteSize = isByteSize;
	}
	
	/** When this is set to false, the dialog does not close when the maximum count is reached. Call endDialog() to close then. */
	public void setCloseWhenMaximumReached(boolean closeWhenMaximumReached)	{
		this.closeWhenMaximumReached = closeWhenMaximumReached;
	}

	
	/** Creates and shows the dialog, in the case that another dialog depends on this. */
	public synchronized Component getDialog()	{
		return ended == false && delegate != null ? delegate.getDialog() : null;
	}
	
	
	/** start the procedure with passed Runnable */
	public void start(Runnable runnable)	{
		this.runnable = runnable;
		start();
	}
		
	/** start the procedure with passed runnable and finish objects. */
	public void start(Runnable runnable, Runnable finish)	{
		this.runnable = runnable;
		this.finish = finish;
		start();
	}
		
	/** start the procedure with passed aimed size */
	public void start(long length)	{
		setTransferSize(length);
		start();
	}
	
		
	/** start the procedure */
	public void start()	{
		frame = ComponentUtil.getWindowForComponent(frame);
		
		if (length > 0L)	{
			System.err.println("starting ProgressMonitor with size "+length);	//+", parent "+(frame != null ? frame .getClass().toString() : "null"));
			delegate = new ProgressMonitor(
					frame,
					label+(isByteSize ? " "+NumberUtil.getFileSizeString(length) : ""),
					"",
					0,
					100);
			((ProgressMonitor)delegate).setCloseWhenMaximumReached(closeWhenMaximumReached);
			delegate.setMillisToDecideToPopup(0);//500);	// else renameTo command would copy log but no dialog showing
			delegate.setMillisToPopup(1000);
			delegate.setProgress(0);

			timer = new Timer(200, this);	// show progress
		}
		else	{
			System.err.println("starting CancelMonitor, having no size");
			delegate = new CancelMonitor(
					frame,
					label,
					"");
			delegate.setMillisToDecideToPopup(0);
			delegate.setMillisToPopup(0);
		}

		thread = new Thread(runnable);
		thread.setPriority(Thread.MIN_PRIORITY);
		//System.err.println("CancelProgressDialog, starting thread ...");
		thread.start();
		
		if (timer != null)
			timer.start();
		//System.err.println("  Thread started ...");
	}


	/** Set canceled by application, not by Cancel Button. Needed to stop processes. */
	public synchronized void setCanceled()	{
		canceled = true;
	}
	
	
	/** Called by the observant when all sub-observants are through */
	public synchronized void endDialog()	{
		if (!ended)	{
			ended = true;

			if (timer != null)
				timer.stop();
			
			// close progress dialog
			if (SwingUtilities.isEventDispatchThread())	{
				closeDelegateRunFinish();
			}
			else	{
				//System.err.println("invoking finish-procedure");
				EventQueue.invokeLater(new Runnable()	{
					public void run()	{
						closeDelegateRunFinish();
					}
				});
			}
		}
	}


	private synchronized void closeDelegateRunFinish()	{
		if (delegate != null)	{
			CancelMonitor cm = delegate;
			delegate = null;
			cm.close();
			//System.err.println("CancelProgressDialog.closeDelegateRunFinish(), call close()");
		}
		
		if (finish != null)	{
			Runnable r = finish;
			finish = null;
			r.run();
		}
	}
	

	/** interface ActionListener: Timer is arriving, monitor the reached size. */
	public synchronized void actionPerformed(ActionEvent e)	{
		if (length > 0L && sum <= length && delegate != null)	{
			int p = (int)(sum * 100 / length);
			delegate.setProgress(p <= 0 && length > 1 ? 1 : p);	// progress to minimal 1
		}

		// FRi 2002-03-03: As some files have 0 bytes, let caller close dialog.
		// FRi 2002-06-26: Caller sometimes is slower than thread, so close it here???
		//if (sum >= length && length > 0L)	{
		//	endDialog();
		//}
	}
	
	
	// interface CancelProgressObserver

	/**
		Called by observed object checking for user cancel. 
		@return true if observed object should end interrupted
	*/
	public synchronized boolean canceled()	{
		boolean b = delegate != null && delegate.isCanceled();
		//System.err.println("CancelProgressDialog.canceled "+b);
		return b || canceled;
	}

	/**
		Called by observed object to tell the observer about done work.
		@param portion e.g. written bytes
	*/
	public void progress(long portion)	{
		sum += portion;
		//System.err.println("CancelProgressDialog progress to "+sum+" of "+length);
		//Thread.dumpStack();
	}

	/**
		Called when observed object changes, label changes to name of new object. 
	*/
	public void setNote(final String note)	{
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				synchronized(CancelProgressDialog.this)	{
					if (delegate != null)
						delegate.setNote(note);
						//System.err.println("CancelProgressDialog, setNote "+note);
				}
			}
		});
	}

	/**
		Returns the note of actual delegate dialog, null if none exists.
	*/
	public String getNote()	{
		return delegate != null ? delegate.getNote() : null;
	}
	
}
