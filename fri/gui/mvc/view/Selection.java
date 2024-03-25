package fri.gui.mvc.view;

/**
	Implementers are required to prepare the data selection for requesters.
	This is a single selection type.

	@author  Ritzberger Fritz
*/
public interface Selection
{
	/** Get selection from the view. Returns null if nothing is selected. */
	public Object getSelectedObject();
	
	/** Set selection in the view. Passing null will clear selection. */
	public void setSelectedObject(Object o);

	/** Clear selection of the view. */
	public void clearSelection();

}
