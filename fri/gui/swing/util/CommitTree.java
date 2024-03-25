package fri.gui.swing.util;

import javax.swing.*;

/**
	Committing JTree (stop cell editing).
	@auhor Ritzberger Fritz
*/

public abstract class CommitTree
{
	/** Stop cell editing in a JTree. */
	public static void commit(JTree tree)	{
		tree.stopEditing();
	}

}