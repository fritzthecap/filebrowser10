package fri.gui.swing.progressdialog;

import javax.swing.JProgressBar;
import java.awt.Component;

/**
 * Copied from javax.swing.ProgressMonitor and changed strongly.
 * <p>
 * A class to monitor the progress of some operation. If it looks
 * like the operation will take a while, a progress dialog will be popped up.
 * When the ProgressMonitor is created it is given a numeric range and a
 * descriptive string. As the operation progresses, call the setProgress method
 * to indicate how far along the [min,max] range the operation is.
 * Initially, there is no ProgressDialog. After the first millisToDecideToPopup
 * milliseconds (default 500) the progress monitor will predict how long
 * the operation will take.  If it is longer than millisToPopup (default 2000,
 * 2 seconds) a ProgressDialog will be popped up.
 * <p>
 * From time to time, when the Dialog box is visible, the progress bar will
 * be updated when setProgress is called.  setProgress won't always update
 * the progress bar, it will only be done if the amount of progress is
 * visibly significant.
 * 
 * @author James Gosling
 * @author Fritz Ritzberger
 * @version $Revision$  $Date$
 */

class ProgressMonitor extends CancelMonitor
{
    private JProgressBar    myBar;
    private int             min;
    private int             max;
    private int             v;
    private int             lastDisp;
    private int             reportDelta;
    private boolean         closeWhenMaximumReached = true;


    /**
     * Constructs a graphic object that shows progress, typically by filling
     * in a rectangular bar as the process nears completion.
     *
     * @param parentComponent the parent component for the dialog box
     * @param message a descriptive message that will be shown
     *        to the user to indicate what operation is being monitored.
     *        This does not change as the operation progresses.
     *        See the message parameters to methods in
     *        {@link JOptionPane#message}
     *        for the range of values.
     * @param note a short note describing the state of the
     *        operation.  As the operation progresses, you can call
     *        setNote to change the note displayed.  This is used,
     *        for example, in operations that iterate through a
     *        list of files to show the name of the file being processes.
     *        If note is initially null, there will be no note line
     *        in the dialog box and setNote will be ineffective
     * @param min the lower bound of the range
     * @param max the upper bound of the range
     * @see JDialog
     * @see JOptionPane
     */
    public ProgressMonitor(Component parentComponent,
                           Object message,
                           String note,
                           int min,
                           int max) {
        super(parentComponent, message, note);
        
        this.min = min;
        this.max = max;

        reportDelta = (max - min) / 100;
        if (reportDelta < 1)
        	    reportDelta = 1;
        v = min;
    }

    public void setCloseWhenMaximumReached(boolean close)	{
        this.closeWhenMaximumReached = close;
    }

    /** Overridden to leave the decision to popup to setProgress(). */
    public void setNote(String note) {
        this.note = note;
        
        if (noteLabel != null) {
            noteLabel.setText(note);
        }
    }


    /** Builds a dialog with a progress bar. */
    protected synchronized void showDialog()	{
    	   if (pane == null)	{
	        ensureNoteLabel();
	        
	        myBar = new JProgressBar();
	        myBar.setMinimum(min);
	        myBar.setMaximum(max);
	        myBar.setValue(v);
    	   }
        
        createOptionPane("Progress ...", new Object[] {message, noteLabel, myBar});
    }


    /** 
     * Indicate the progress of the operation being monitored.
     * If the specified value is >= the maximum, the progress
     * monitor is closed. 
     * @param nv an int specifying the current value, between the
     *        maximum and minimum specified for this component
     * @see #setMinimum
     * @see #setMaximum
     * @see #close
     */
    public void setProgress(int nv) {
        v = nv;
        boolean doClose = false;

        if (nv >= max) {
            doClose = true;
        }
        else if (nv >= lastDisp + reportDelta) {
            lastDisp = nv;
            if (myBar != null) {
                myBar.setValue(nv);
            }
            else {
                long T = System.currentTimeMillis();
                long dT = (int)(T-T0);

                if (dT >= getMillisToDecideToPopup()) {
                    int predictedCompletionTime;
                    if (nv > min) {
                        predictedCompletionTime = (int)((long)dT *
                                                        (max - min) /
                                                        (nv - min));
                    }
                    else {
                        predictedCompletionTime = getMillisToPopup();
                    }

                    if (predictedCompletionTime >= getMillisToPopup()) {
                        showDialog();
                    }
                }
            }
        }
        
        if (doClose && closeWhenMaximumReached)
            close();
    }



    /** 
     * Indicate that the operation is complete.  This happens automatically
     * when the value set by setProgress is >= max, but it may be called
     * earlier if the operation ends early.
     */
    public void close() {
      //if (dialog.isVisible())
      //  Thread.dumpStack();
      super.close();
      myBar = null;
    }


    /**
     * Returns the minimum value -- the lower end of the progress value.
     *
     * @return an int representing the minimum value
     * @see #setMinimum
     */
    public int getMinimum() {
        return min;
    }


    /**
     * Specifies the minimum value.
     *
     * @param m  an int specifying the minimum value
     * @see #getMinimum
     */
    public void setMinimum(int m) {
        min = m;
    }


    /**
     * Returns the maximum value -- the higher end of the progress value.
     *
     * @return an int representing the maximum value
     * @see #setMaximum
     */
    public int getMaximum() {
        return max;
    }


    /**
     * Specifies the maximum value.
     *
     * @param m  an int specifying the maximum value
     * @see #getMaximum
     */
    public void setMaximum(int m) {
        max = m;
    }

}