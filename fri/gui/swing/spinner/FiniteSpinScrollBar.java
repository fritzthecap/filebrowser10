package fri.gui.swing.spinner;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JScrollBar;

/**
	A JScrollBar that scrolls in a defined range. Its size fits to
	passed editor JComponent.
*/

public class FiniteSpinScrollBar extends JScrollBar
{
	private JComponent editor;
	
	/** Creates a scrollbar with zero range. */
	public FiniteSpinScrollBar(JComponent editor, int initial, int min, int max)	{
		super(JScrollBar.VERTICAL, initial, 0, min, max);
		this.editor = editor;
	}

	public JComponent getEditor()	{
		return editor;
	}
	
	public void setEditor(JComponent editor)	{
		this.editor = editor;
	}
	
	/** Delegate to enclosing class that holds editor. */
	public Dimension getPreferredSize()	{
		Dimension d = super.getPreferredSize();
		return new Dimension(2 * d.width / 3, getEditor().getPreferredSize().height);
	}

}