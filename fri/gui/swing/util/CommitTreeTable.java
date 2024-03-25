package fri.gui.swing.util;

import fri.gui.swing.treetable.JTreeTable;

/**
	Committing JTreeTable (stop cell editing).
	@auhor Ritzberger Fritz
*/

public abstract class CommitTreeTable
{
	/** Stop cell editing in a JTreeTable. */
	public static void commit(JTreeTable treetable)	{
		CommitTree.commit(treetable.getTree());
		CommitTable.commit(treetable);
	}

}