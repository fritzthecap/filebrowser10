package fri.gui.mvc.view.swing;

import java.awt.Point;
import fri.gui.mvc.view.Selection;

/**
	Implementers must be able to get a node object from a Point.
	@author  Ritzberger Fritz
*/
public interface SelectionDnd extends Selection
{
	/** Get a node from a Point in the view. */
	public Object getObjectFromPoint(Point point);
}