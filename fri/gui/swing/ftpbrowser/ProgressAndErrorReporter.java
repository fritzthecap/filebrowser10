package fri.gui.swing.ftpbrowser;

import java.awt.Component;
import javax.swing.*;
import fri.util.ftp.FtpResponseException;
import fri.util.observer.CancelProgressObservable;
import fri.util.observer.CancelProgressObserver;
import fri.gui.mvc.util.swing.EventUtil;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.progressdialog.CancelProgressDialog;
import fri.gui.swing.yestoalldialog.*;

/**
	An ProgressAndErrorReporter must render errors in a GUI dialog and
	show progress in a dialog, with cancel option.
	<p>
	This class is static and references <i>GuiApplication.globalFrame</i> as dialog parent.
	It is necessary to isolate the tree nodes of the tree models from the GUI, as models
	are kept as singletons and can be viewed by more than one window.
	
	@author Fritz Ritzberger
*/

public abstract class ProgressAndErrorReporter
{
	private static CancelProgressDialog observer;
	private static CancelProgressObservable observable;
	private static OverwriteLauncher overwriteConfirm;


	/** Show exception in a GUI dialog in event thread. */
	public static void error(Exception e)	{
		e.printStackTrace();
		error(e instanceof FtpResponseException ? e.getMessage() : e.toString());
	}

	/** Show an error string in a GUI dialog in event thread. */
	public static void error(final String msg)	{
		//System.err.println("before invokeLaterOrNow");
		EventUtil.invokeLaterOrNow(new Runnable()	{
			public void run()	{
				System.err.println("showing dialog with message "+msg);
				JOptionPane.showMessageDialog(getDialog() == null ? GuiApplication.globalFrame : getDialog(), msg, "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		//System.err.println("after invokeLaterOrNow");
	}

	
	/** Returns true if user confirmed overwrite of all files (save performance). */
	public static boolean isOverwriteAll()	{
		return overwriteConfirm != null && overwriteConfirm.isOverwriteAll();
	}
	
	/** Returns true if user confirms overwrite of passed file, or if overwrite all has been pressed once. */
	public static boolean overwrite(String tgt, String tgtInfo, String src, String srcInfo)	{
		Component pnt = getDialog() == null ? GuiApplication.globalFrame : getDialog();
		if (overwriteConfirm == null)	{
			overwriteConfirm = new OverwriteLauncher();
		}
		
		try	{
			int ret = overwriteConfirm.show(pnt, tgt, tgtInfo, src, srcInfo);
			return ret == YesToAllDialog.YES;
		}
		catch (UserCancelException e)	{
			System.err.println("User canceled at overwrite dialog");
		}
		
		return false;
	}



	/**
		Show an progress dialog for passed background thread procedures.
		Shows a cancel dialog if size is less equal zero.
		<i>finishOnSuccess</i> or <i>onException</i> will be called from event thread.
		The only caught exception is RunnablException, which the caller must use to wrap
		any really thrown exception.
	*/
	public synchronized static void createBackgroundMonitor(
		CancelProgressObservable observable,
		String label,
		long sizeTodo, 
		final Runnable todo,
		final Runnable onSuccess,
		final Runnable onException)
	{
		if (observer != null)	{
			error("Transaction or cleanup running, try later!");
			return;
		}
		
		// as FTP server has not finished when io-buffer was flushed, we need to wait longer than to progress finished
		if (sizeTodo > 0)
			sizeTodo++;	// increase size to unreachable and let endDialog() do the work.
		
		observer = new CancelProgressDialog(GuiApplication.globalFrame, label, sizeTodo);

		Runnable exceptionCatcher = new Runnable()	{
			public void run()	{
				try	{
					todo.run();	// here we are already in background, so run synchronously

					if (observer.canceled() == false)
						EventUtil.invokeLaterOrNow(onSuccess);
				}
				catch (RunnableException e)	{
					observer.endDialog();	// must end nonmodal dialog, else error dialog will not show (bug JDK 1.4 ?)

					if (observer.canceled() == false)
						error(e.realException);
					
					if (onException != null)
						EventUtil.invokeLaterOrNow(onException);
				}
				finally	{
					observer.endDialog();
					endTransaction();
				}
			}
		};
		
		if (observable != null)	{
			observable.setObserver(observer);
			ProgressAndErrorReporter.observable = observable;
		}
		// else might connect later to static observer context
		
		observer.start(exceptionCatcher);
		
		//if (sizeTodo <= 0L)
			observer.getDialog();	// else not showing on GUI: Bug?!?!
	}
	

	/** Returns the static observer, for clients that have no context with the observer (ModelItems). */
	public synchronized static CancelProgressObserver getObserver()	{
		return observer;
	}
	
	
	private synchronized static Component getDialog()	{
		return observer != null ? observer.getDialog() : null;
	}
	
	private synchronized static void endTransaction()	{
		observer = null;
		overwriteConfirm = null;
		
		if (observable != null)
			observable.setObserver(null);
	}
	



	/** Exception to be thrown in Runnable.run(), nesting the real exception. */
	public static class RunnableException extends RuntimeException
	{
		public final Exception realException;
		
		/** To be thrown within Runnable without violating its signature. */
		public RunnableException(Exception cause)	{
			this.realException = cause;
		}
		/** Not used, compatibility for older JDK's. */
		public RunnableException()	{
			realException = null;	// must initialize
		}
	}
	

	private ProgressAndErrorReporter()	{}

}
	
