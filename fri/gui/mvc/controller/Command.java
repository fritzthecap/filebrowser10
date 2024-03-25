package fri.gui.mvc.controller;

/**
	Implementers must be able to do a command.
	Not every Command must be undoable, use <i>UndoableCommand</i> for that purpose.

	@author  Ritzberger Fritz
*/

public interface Command
{
	/**
		Execute a command.
		@return a resulting object (e.g. for a move-command the moved nodes).
	*/
	public Object doit();
}
