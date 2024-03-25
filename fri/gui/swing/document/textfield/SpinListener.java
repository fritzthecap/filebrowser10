package fri.gui.swing.document.textfield;

import javax.swing.text.JTextComponent;

/**
	A Listener for spin boxes or cursor keys that increments and decrements
	numbers and string lists in MaskingElements.
*/

public interface SpinListener
{
	/**
		Figures out the MaskingElement in textfield and
		decrements its current value. Rewrites the textfield.
	*/
	public void decrement(JTextComponent textfield);

	/**
		Figures out the MaskingElement in textfield and
		increments its current value. Rewrites the textfield.
	*/
	public void increment(JTextComponent textfield);
}