package fri.gui.swing.scroll;

import java.awt.Component;
import javax.swing.JScrollPane;

/**
	Utilities in conjunction with scrollpane.
*/

public abstract class ScrollPaneUtil
{
	/** Returns the scrollp ane for a given Component, or null if no <i>JScrollPane</i> parent was found. */
	public static JScrollPane getScrollPane(Component c)	{
		while (c != null && c instanceof JScrollPane == false)
			c = c.getParent();
		return (JScrollPane)c; 
	}


	private ScrollPaneUtil()	{}	
}