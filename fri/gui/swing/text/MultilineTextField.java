package fri.gui.swing.text;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import fri.gui.swing.ComponentUtil;
//import fri.gui.swing.CursorUtil;

/**
	A textfield that opens an editor on mousclick or F2 when it contains
	a text that has newlines. If it has no newlines, a double click
	or F2 opens the editor, too.

	@author  Ritzberger Fritz
*/

public class MultilineTextField extends JTextField implements
	MouseListener,
	KeyListener,
	FocusListener
{
	private String title;

	/** Create a textfield that opens a dialog on click or F2 when text is multiline. */
	public MultilineTextField()	{
		super();

		addMouseListener(this);
		addKeyListener(this);
		addFocusListener(this);
		
		// needed in JDK 1.4 to switch off newline filtering
		getDocument().putProperty("filterNewlines", Boolean.FALSE);
	}


	/** Implements MouseListener to launch editor on simple click when text is non-empty and mulitline. */
	public void mouseReleased(MouseEvent e)	{
		String text = getText().trim();
		if (text.length() > 0 && text.indexOf("\n") >= 0)	{
			openEditor();
		}
	}
	public void mousePressed(MouseEvent e)	{
	}
	/** Implements MouseListener to launch editor on double click. */
	public void mouseClicked(MouseEvent e)	{
		if (e.getClickCount() >= 2)	{
			openEditor();
		}
	}
	public void mouseEntered(MouseEvent e)	{
	}
	public void mouseExited(MouseEvent e)	{
	}


	/** Returns the title to be used on shown dialog. */
	public String getDialogTitle()	{
		return title;
	}
	/** Sets the title to be used on shown dialog. */
	public void setDialogTitle(String title)	{
		this.title = title;
	}

	/** Opens the modal editor dialog with passed title and sets the new text when closed. */
	public void openEditor()	{
		MultilineEditDialog edi = createEditDialog(ComponentUtil.getWindowForComponent(this), getText(), getDialogTitle());
		setText(edi.getText());
	}

	/** Override this to create another MulitlineEditDialog instance. */
	protected MultilineEditDialog createEditDialog(Window parent, String text, String title)	{
		if (parent instanceof Frame)
			return new MultilineEditDialog((Frame) parent, this, text, title, true);
		else
			return new MultilineEditDialog((Dialog) parent, this, text, title, true);
	}

	/** interface KeyListener to catch F2 for start editing */
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_F2)	{
			mouseReleased(null);
		}
	}
	public void keyReleased(KeyEvent e)	{
	}
	public void keyTyped(KeyEvent e)	{
	}

	/** Select all text when getting focus. */
	public void focusGained(FocusEvent e)	{
		selectAll();
	}
	public void focusLost(FocusEvent e)	{
	}

}