package fri.gui.swing.text;

import java.awt.Component;
import javax.swing.JTextArea;
import fri.gui.awt.clipboard.*;
import fri.gui.text.TextHolder;

/**
	TextHolder implementation for JTextArea.
*/

public class ClipableJTextArea extends JTextArea implements
	ClipTextRenderer,
	TextHolder
{
	public ClipableJTextArea()	{
	}
	
	public ClipableJTextArea(int rows, int columns)	{
		super(rows, columns);
	}
	
	/** Implements TextHolder: returns this as Component. */
	public Component getTextComponent()	{
		return this;
	}
}
