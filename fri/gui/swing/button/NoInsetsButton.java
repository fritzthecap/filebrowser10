package fri.gui.swing.button;

import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.Action;

/**
 * A JButton with no empty space around the label text.
 *
 * @author  Fritz Ritzberger
 * @version $Revision: 1.1 $  $Date: 2001/10/05 13:42:11 $
 */

public class NoInsetsButton extends JButton
{
	public NoInsetsButton()	{
		super();
	}
	public NoInsetsButton(Action action)	{
		super(action);
	}
	public NoInsetsButton(String text)	{
		super(text);
	}
	public NoInsetsButton(String text, Icon icon)	{
		super(text, icon);
	}
	public NoInsetsButton(Icon icon)	{
		super(icon);
	}
	
	public Insets getInsets()	{
		return new Insets(0, 2, 0, 2);	// top, left, bottom, right
	}
}
