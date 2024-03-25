package fri.gui.swing.util;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public abstract class TextFieldUtil
{
	public static void scrollToPosition(JTextField tf, int i)	{
		try	{
			Rectangle r = tf.modelToView(i);
			if (r != null)
				tf.scrollRectToVisible(r);
		}
		catch (BadLocationException exc)	{
		}
	}
}
