package fri.gui.swing.util;

import javax.swing.*;

/**
	Committing JTable (stop cell editing).
	@auhor Ritzberger Fritz
*/

public abstract class CommitTable
{
	/** Stop cell editing in a JTable. */
	public static void commit(JTable table)	{
		try	{
			DefaultCellEditor ded = (DefaultCellEditor)table.getCellEditor();
			if (ded != null)
				ded.stopCellEditing();
		}
		catch (ClassCastException e)	{
			e.printStackTrace();
		}
	}

}