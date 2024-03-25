package fri.gui.swing.progressdialog;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
	A dialog for long lasting transactions that can be canceled.
	No progress bar is shown, no progress is observed.
	<p>
	This is super-class for ProgressMonitor. Both dialogs
	can not be reused, i.e. if once close() was called it
	never shows again.
	
	@see javax.swing.ProgressMonitor
	@see fri.gui.swing.progressdialog.ProgressMonitor
*/

class CancelMonitor
{
	protected JDialog         dialog;
	protected JOptionPane     pane;
	protected JLabel          noteLabel = null;
	protected Component       parentComponent;
	protected String          note;
	protected long            T0;
	protected Object          message;
	private Object[]        cancelOption = null;
	private int             millisToPopup = 200;
	private int             millisToDecideToPopup = 200;
	private boolean closed = false;
	
	
	/**
	* Constructs a dialog that appears delayed and shows a Cancel Button.
	*
	* @param parentComponent the parent component for the dialog box
	* @param message a descriptive message that will be shown
	*        to the user to indicate what operation is being monitored.
	*        This does not change as the operation progresses.
	* @param note a short note describing the state of the
	*        operation.  As the operation progresses, you can call
	*        setNote to change the note displayed.  This is used,
	*        for example, in operations that iterate through a
	*        list of files to show the name of the file being processes.
	*        If note is initially null, there will be no note line
	*        in the dialog box and setNote will be ineffective
	*/
	public CancelMonitor(Component parentComponent, Object message, String note)	{
		this.parentComponent = parentComponent;
		this.message = message;
		this.note = note;
		
		cancelOption = new Object[] { UIManager.getString("OptionPane.cancelButtonText") };
		T0 = System.currentTimeMillis();
	}


	/**
	 * Specifies the additional note that is displayed along with the
	 * message. Used, for example, to show which file the
	 * is currently being copied during a multiple-file copy.
	 *
	 * @param note  a String specifying the note to display
	 */
	public synchronized void setNote(String note) {
		this.note = note;
		
		if (dialog == null) {
			long T = System.currentTimeMillis();
			long dT = (int)(T - T0);
			if (dT >= millisToPopup) {
				showDialog();
			}
		}

		if (noteLabel != null)	{
			noteLabel.setText(note);
		}
	}


	protected void ensureNoteLabel()	{
		if (noteLabel == null)	{
			String note = getNote();
			if (note == null)
				note = " ";
				
			noteLabel = new JLabel(note);
		}
	}
	
	/** Does nothing, just to be compatible with ProgressMonitor */
	public void setProgress(int nv) {
	}


	protected synchronized void showDialog()	{
		if (closed == false)	{
			ensureNoteLabel();
			createOptionPane("Monitor ...", new Object[] { message, noteLabel });
		}
	}
	

	/** Creates and shows the dialog. */
	protected void createOptionPane(String title, Object [] objs)	{
		if (pane == null)	{
			pane = new ProgressOptionPane(objs);
			dialog = pane.createDialog(parentComponent, title);
			dialog.show();
		}
	}


	/**
		Shows the dialog and returns it, in the case that some dialog
		that depends on this needs its parent to show.
	*/
	public synchronized JDialog getDialog()	{
		if (dialog == null && closed == false)
			showDialog();
		return dialog;
	}


	/** 
	* Close the dialog.
	*/
	public synchronized void close() {
		if (closed == false) {
			if (dialog != null)	{
				dialog.setVisible(false);
				dialog.dispose();
			}
			closed = true;
		}
	}


	/** 
	* Returns true if the user has hit the Cancel button in the progress dialog.
	*/
	public boolean isCanceled() {
		if (pane == null)
			return false;
		Object v = pane.getValue();
		return ((cancelOption.length == 1) && (v != null && v.equals(cancelOption[0])));
	}



	/**
	 * Specifies the amount of time to wait before deciding whether or
	 * not to popup a progress monitor.
	 *
	 * @param millisToDecideToPopup  an int specifying the time to wait,
	 *        in milliseconds
	 * @see #getMillisToDecideToPopup
	 */
	public void setMillisToDecideToPopup(int millisToDecideToPopup) {
	    this.millisToDecideToPopup = millisToDecideToPopup;
	}
	
	
	/**
	 * Returns the amount of time this object waits before deciding whether
	 * or not to popup a progress monitor.
	 *
	 * @param millisToDecideToPopup  an int specifying waiting time,
	 *        in milliseconds
	 * @see #setMillisToDecideToPopup
	 */
	public int getMillisToDecideToPopup() {
	    return millisToDecideToPopup;
	}
	
	
	/**
	 * Specifies the amount of time it will take for the popup to appear.
	 * (If the predicted time remaining is less than this time, the popup
	 * won't be displayed.)
	 *
	 * @param millisToPopup  an int specifying the time in milliseconds
	 * @see #getMillisToPopup
	 */
	public void setMillisToPopup(int millisToPopup) {
	    this.millisToPopup = millisToPopup;
	}
	
	
	/**
	 * Returns the amount of time it will take for the popup to appear.
	 *
	 * @param millisToPopup  an int specifying the time in milliseconds
	 * @see #setMillisToPopup
	 */
	public int getMillisToPopup() {
	    return millisToPopup;
	}
	
	
	/**
	 * Specifies the additional note that is displayed along with the
	 * progress message.
	 *
	 * @return a String specifying the note to display
	 * @see #setNote
	 */
	public String getNote() {
	    return note;
	}





	private class ProgressOptionPane extends JOptionPane
	{
		ProgressOptionPane(Object messageList) {
			super(
				messageList,
				JOptionPane.INFORMATION_MESSAGE,
				JOptionPane.DEFAULT_OPTION,
				null,
				CancelMonitor.this.cancelOption,
				null);
		}

		public int getMaxCharactersPerLineCount() {
			return 100;
		}

		// Equivalent to JOptionPane.createDialog,
		// but create a modeless dialog.
		// This is necessary because the Solaris implementation doesn't
		// support Dialog.setModal yet.
		public JDialog createDialog(Component parentComponent, String title) {
			Frame frame = JOptionPane.getFrameForComponent(parentComponent);
			final JDialog dialog = new JDialog(frame, title, false);
			Container contentPane = dialog.getContentPane();
			
			contentPane.setLayout(new BorderLayout());
			contentPane.add(this, BorderLayout.CENTER);

			dialog.pack();
			dialog.setLocationRelativeTo(parentComponent);
			
			dialog.addWindowListener(new WindowAdapter() {
				boolean gotFocus = false;
				public void windowClosing(WindowEvent we) {
					setValue(null);
				}				
				public void windowActivated(WindowEvent we) {
					if (!gotFocus) {
						selectInitialValue();
						gotFocus = true;
					}
				}
			});
				
			addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					if (dialog.isVisible() && 
							event.getSource() == ProgressOptionPane.this &&
							(event.getPropertyName().equals(VALUE_PROPERTY) ||
							event.getPropertyName().equals(INPUT_VALUE_PROPERTY)))
					{
						close();
						//dialog.setVisible(false);
						//dialog.dispose();
					}
				}
			});

			return dialog;
		}
	}
}