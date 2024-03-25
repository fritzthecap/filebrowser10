package fri.gui.mvc.model;

import fri.gui.mvc.controller.CommandArguments;

/**
	Model describes a collection of ModelItems of any structure
	(list, tree, graph). It provides a method to locate an item
	and return its positional information (hierachial parent or list position).
	<p>
	The creation and loading of a Model is left to some factory,
	as constructors of Models could be very different.

	@author  Ritzberger Fritz
*/

public interface Model
{
	/**
		Returns the environment the item is living in, i.e.
		its Model (this) and optionally its parent item and/or its position.
		This should cover trees and lists.
		@param item the ModelItem for which data are requested.
		@return data about Model, parent item and position.
	*/
	public CommandArguments getModelItemContext(ModelItem item);

}