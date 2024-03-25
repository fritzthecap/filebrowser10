package fri.gui.mvc.controller;

import javax.swing.undo.UndoableEdit;

/**
	Implementers must be able to do, undo and redo a command.
	They can use <code>fri.gui.mvc.controller.clipboard.AbstractEditCommand</code>
	or <code>javax.swing.undo.AbstractUndoableEdit</code> to implement
	UndoableEdit.
	<p>
	This class defines no methods, it is melting Command and UndoableEdit.

	@author  Ritzberger Fritz
*/

public interface UndoableCommand extends
	Command,
	UndoableEdit
{
	// merging Command and UndoableEdit.
}