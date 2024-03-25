package fri.gui.swing.spinnumberfield;

import java.awt.event.ActionListener;

/**
	Responsibilities of editing a numerical value in a textfield.
*/
public interface NumberEditor
{
	/** Returns the current numeric value from the textfield. */
	public long getValue();
	/** Sets the current numeric value in the textfield to the passed value. */
	public void setValue(long value);
	/** Sets the current numeric value, minimum and maximum. */
	public void setRange(long min, long max);
	/** Add a listener for every numeric changes in textfield. */
	public void addNumberEditorListener(NumberEditorListener lsnr);
	/** Remove a listener for every numeric changes in textfield. */
	public void removeNumberEditorListener(NumberEditorListener lsnr);
	/** Add a listener for committing changes in textfield. */
	public void addActionListener(ActionListener lsnr);
	/** Remove a listener for committing changes in textfield. */
	public void removeActionListener(ActionListener lsnr);
}