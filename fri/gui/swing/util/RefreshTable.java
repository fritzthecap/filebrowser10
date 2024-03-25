package fri.gui.swing.util;

import javax.swing.*;

/**
	Utility for refreshing JTable
	@auhor Ritzberger Fritz
*/

public abstract class RefreshTable
{
	/** Use this in refreshDisplay() */
	public static void refresh(JTable table)	{
		table.revalidate();
		table.repaint();
	}

}
