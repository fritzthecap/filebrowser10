package fri.gui.swing.util;

import javax.swing.*;

/**
	Utility for refreshing JTree.
	@auhor Ritzberger Fritz
*/

public abstract class RefreshTree
{
	/** Use this in refreshDisplay() */
	public static void refresh(JTree tree)	{
		tree.treeDidChange();
	}

}
