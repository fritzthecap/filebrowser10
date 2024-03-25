package fri.gui.mvc.model;

import fri.gui.mvc.controller.CommandArguments;

/**
	MutableModel describes an editable collection of ModelItems of any structure
	(list, tree, graph). It provides methods to insert and delete items.
	No listener mechanism is provided here, as model changes are specific to
	the structure of the model.

	@author  Ritzberger Fritz
*/

public interface MutableModel extends Model
{
	/**
		Insert a new item.
		@param position information about where to insert the new item.
			This could contain a parent item or a integer position or both.
		@return inserted item, or null if action failed.
	*/
	public ModelItem doInsert(ModelItem item, CommandArguments position);

	/**
		Delete an item.
		@param item the item to delete from this model.
		@return true if delete succeeded.
	*/
	public boolean doDelete(ModelItem item);

}
