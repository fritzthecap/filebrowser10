package fri.gui.swing.yestoalldialog;

import java.awt.EventQueue;

/**
	Show a modal dialog, optional from a background thread.
	Return the dialog result synchronously, by EventQueue.invokeAndWait().
*/

public class YesToAllLauncher
{
	protected YesToAllDialog dialog = null;
	private int ret;
	private UserCancelException exception;
	
	
	public void reset()	{
		dialog = null;
	}
	
	/** Show dialog synchronously, by invokeAndWait() if necessary. */
	public int show()
		throws UserCancelException
	{
		ret = -1;
		exception = null;
		
		if (EventQueue.isDispatchThread())	{
			startDialog();
		}
		else	{
			try	{
				EventQueue.invokeAndWait(new Runnable()	{
					public void run()	{
						startDialog();
					}
				});
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
		
		if (exception != null)	{
			throw exception;
		}
			
		return ret;
	}
	
	
	/** Override this and set a dialog to variable "dialog", else NullPointerException! */
	protected void startDialog()	{
		//Thread.yield();
		try	{
			ret = dialog.show();
		}
		catch (UserCancelException e)	{
			exception = e;
		}
	}

	public boolean isOverwriteAll()	{
		return dialog != null && dialog.isOverwriteAll();
	}

}