package fri.gui.mvc.model;

/**
	Any implementer is able to store its move state: move pending or not move pending.
	This is needed for cut and paste. Implemented by DefaultTableRow and AbstractTreeNode.

	@author  Ritzberger Fritz
*/

public interface Movable
{
	/** Returns the current move state. */
	public boolean isMovePending();

	/** Sets the move state to the passed value. */
	public void setMovePending(boolean movePending);
}