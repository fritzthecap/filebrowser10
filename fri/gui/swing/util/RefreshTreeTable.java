package fri.gui.swing.util;

import fri.gui.swing.treetable.JTreeTable;

/**
	Utilitiy for refreshing JTreeTable.
	@auhor Ritzberger Fritz
*/

public abstract class RefreshTreeTable
{
	/** Use this in refreshDisplay() */
	public static void refresh(JTreeTable treetable)	{
		RefreshTree.refresh(treetable.getTree());
		RefreshTable.refresh(treetable);
	}

}
