package fri.gui.mvc.view;

import fri.gui.mvc.model.Model;

/**
	A View may be required to show many data Models of the same type.
	Its setModel() and refresh() methods output all Model data to the display.
	A View offers a Selection, that holds a selected ModelItem collection.

	@author  Ritzberger Fritz
*/

public interface View
{
	/**
		Return the Model the View visualizes. This is the Model that was passed
		by setModel(), which is NOT the view-adapter (like e.g. TableModel)!
		@return the displayed data model.
	*/
	public Model getModel();

	/**
		Display the data represented by the passed model and set the Model member variable.
		@param model the data model to display.
	*/
	public void setModel(Model model);
	
	/**
		Flush data from (in-memory) Model to the display (without loading new data).
	*/
	public void refresh();

	/**
		Return an object that can answer get-selected and set-selected requests in this View.
		@return a Selection that can return and set selected items.
	*/
	public Selection getSelection();

}
