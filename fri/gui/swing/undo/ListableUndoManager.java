package fri.gui.swing.undo;

import javax.swing.undo.*;
import java.util.Enumeration;

/**
 * An UndoManager must be listable and changeable to remove dead undoable edits.
 * 
 * @author Fritz Ritzberger
 */
public class ListableUndoManager extends PreviewableUndoManager
{
	/** Returns all elements this UndoManager contains. */
	public Enumeration elements()	{
		return edits.elements();
	}

	/** Removes all edits this UndoManager contains which can be neither un-done nor re-done. */
	public void removeDeadEdits()	{
		for (int i = edits.size() - 1; i >= 0; i--) {	// loop from tail because edits might get removed
			UndoableEdit edit = (UndoableEdit) edits.elementAt(i);

			if (isDead(edit))
				trimEdits(i, i);
		}
	}

	private boolean isDead(UndoableEdit edit) {
		if (edit instanceof ListableCompoundEdit)	{
			for (Enumeration e = ((ListableCompoundEdit) edit).elements(); e.hasMoreElements(); )	{
				UndoableEdit child = (UndoableEdit) e.nextElement();
				if (isDead(child) == false)
					return false;
			}
			return true;
		}
		else	{
			return edit.canUndo() == false && edit.canRedo() == false;
		}
	}
	
}
