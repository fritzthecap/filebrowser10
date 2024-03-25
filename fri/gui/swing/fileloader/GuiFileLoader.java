package fri.gui.swing.fileloader;

import java.io.*;
import java.awt.BorderLayout;
import javax.swing.*;

/**
	Loading a file in a background thread.
	Manages GUI controls like a progress bar and an error dialog.
*/

abstract class GuiFileLoader extends FileLoader
{
	private JComponent panel;
	private JProgressBar progress;

	/**
		Create a background FileLoader with a progress bar showing on passed panel.
		@param file file to load from disk
		@param panel panel with BorderLayout where to add progressbar at SOUTH, nullable
		@param loadObserver listener which is interested in status of loading.
			At start setLoading(true) is called, at end setLoading(false). Can be null.
		@param waiter object to notify() when finished loading, nullable
	*/
	public GuiFileLoader(
		File file,
		JComponent panel,
		LoadObserver loadObserver,
		Object waiter)
	{
		super(file, loadObserver, waiter);
		this.panel = panel;
	}



	protected void beforeWork()	{
		addProgressBar(true, getLength());
	}

	protected void afterWork()	{
		addProgressBar(false, 0);
	}



	
	// Adds a progress bar to panel (BorderLayout.SOUTH) if the passed length > 0 and the panel is not null.
	private void addProgressBar(final boolean add, int len)	{
		if (panel == null)
			return;

		if (len > 0)	{
			progress = new JProgressBar();
			progress.setStringPainted(true);
			progress.setMinimum(0);
			progress.setMaximum(len);
		}
		else
			if (progress == null)
				return;
		
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				if (add)
					panel.add(progress, BorderLayout.SOUTH);
				else
					panel.remove(progress);
				panel.revalidate();
			}
		});
	}


	/** Overridden to output the error message in a dialog. */
	protected void error(Exception e)	{
		e.printStackTrace();
		reportProgressAndError(null, 0, e.toString());
	}
	
		
	/**
		Do synchronized in event thread:
		if errorMsg is not null, open a dialog, else call <i>insertProgress(s, bytesRead)</i>.
	*/
	protected final void reportProgressAndError(final Object data, final int bytesRead, final String errorMsg)	{
		if (SwingUtilities.isEventDispatchThread())	{
			process(data, bytesRead, errorMsg);
		}
		else	{
			try	{
				SwingUtilities.invokeAndWait(new Runnable()	{
					public void run()	{
						process(data, bytesRead, errorMsg);
					}
				});
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
	}


	private void process(Object data, int bytesRead, String errorMsg)	{
		if (errorMsg == null)
			insertTextProgress(data, bytesRead);
		else
			JOptionPane.showMessageDialog(panel, "Error was: "+errorMsg, "Error", JOptionPane.OK_OPTION);
	}

	/**
		This is called from event thread.
		If there was allocated a progress bar, set its new value to <i>progress.getValue() + bytesRead</i>.
	*/
	protected void insertTextProgress(Object data, int bytesRead)	{	
		if (progress != null)	{
			progress.setValue(progress.getValue() + bytesRead);
		}
	}

}
