package fri.gui.mvc.view;

/**
	Implementers are required to prepare multiple selection for requesters.
	Implicitely the inherited method <i>getSelectedObject()</i> will return a List of objects,
	and <i>setSelectedObject()</i> will accept a List as argument.

	@author  Ritzberger Fritz
*/

public interface MultipleSelection extends Selection
{
	/** Add an item to currently selected items of the associated view. */
	public void addSelectedObject(Object o);

}
