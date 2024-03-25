package fri.gui.swing.undo;

import java.util.Enumeration;
import javax.swing.undo.*;

/**
	Undo manager must be previewable, i.e. it must provide
	the edit that is about to be undone/redone.
*/

public class PreviewableUndoManager extends UndoManager
{
	/** Returns the edit that is about to be undone. This could be a CompoundEdit (several edits). */
	public UndoableEdit getEditToBeUndone()	{
		return editToBeUndone();
	}

	/** Returns the edit that is about to be redone. This could be a CompoundEdit (several edits). */
	public UndoableEdit getEditToBeRedone()	{
		return editToBeRedone();
	}


	/** Returns the edit that is about to be undone. This is an atomic edit. */
	public UndoableEdit getFirstEditToBeUndone()	{
		return getFirstEdit(getEditToBeUndone());
	}

	/** Returns the edit that is about to be redone. This is an atomic edit. */
	public UndoableEdit getFirstEditToBeRedone()	{
		return getFirstEdit(getEditToBeRedone());
	}

	private UndoableEdit getFirstEdit(UndoableEdit edit)	{
		if (edit instanceof ListableCompoundEdit)	{
			ListableCompoundEdit lce = (ListableCompoundEdit) edit;
			Enumeration e = lce.elements();

			if (e != null && e.hasMoreElements())	{
				return getFirstEdit((UndoableEdit) e.nextElement());
			}
		}
		return edit;
	}

}
