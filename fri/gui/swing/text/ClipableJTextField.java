package fri.gui.swing.text;

import java.awt.Component;
import javax.swing.JTextField;
import fri.gui.awt.clipboard.*;
import fri.gui.text.TextHolder;

/**
	TextHolder and ClipTextRenderer implementation for JTextField.
*/

public class ClipableJTextField extends JTextField implements
	ClipTextRenderer,
	TextHolder
{
	public ClipableJTextField()	{
		super();
		addKeyListener(new ClipKeyListener(this, this));
	}

	public ClipableJTextField(int cols)	{
		super(cols);
		addKeyListener(new ClipKeyListener(this, this));
	}

	public ClipableJTextField(String s)	{
		super(s);
		addKeyListener(new ClipKeyListener(this, this));
	}

	public ClipableJTextField(String s, int cols)	{
		super(s, cols);
		addKeyListener(new ClipKeyListener(this, this));
	}

	public void replaceRange(String pasteText, int selectionStart, int selectionEnd)	{
		if (selectionStart >= 0 && selectionEnd > selectionStart)	{
			String neu;
			String old = getText();
			neu = old.substring(0, selectionStart) + pasteText + old.substring(selectionEnd);
			setText(neu);
		}
		else	{
			int max = Math.max(selectionStart, selectionEnd);
			if (max >= 0)
				insert(pasteText, max);
		}
	}

	public void insert(String pasteText, int caretPosition)	{
		String neu;
		String old = getText();
		if (caretPosition < old.length())
			neu = old.substring(0, caretPosition) + pasteText + old.substring(caretPosition);
		else
			neu = old + pasteText;
		setText(neu);
	}

	/** Implements TextHolder: returns this as Component. */
	public Component getTextComponent()	{
		return this;
	}
}
