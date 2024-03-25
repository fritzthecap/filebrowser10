package fri.gui.mvc.controller;

import javax.swing.undo.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.MutableModel;

/**
	Default remove command. Binds the model and the item and calls
	<i>item.doDelete(arguments)</i> when executed.

	@author  Ritzberger Fritz
*/

public class DefaultRemoveCommand extends AbstractEditCommand
{
	/**
		Create a RemoveCommand, do not execute it. It will be undoable after calling doit().
		@param item item to be removed
	*/
	public DefaultRemoveCommand(ModelItem item, MutableModel model)	{
		this(null, item, model);
	}
	
	/**
		Additionally store the target editor.
	*/
	public DefaultRemoveCommand(Object targetEditor, ModelItem item, MutableModel model)	{
		super(null, targetEditor, null, item, model.getModelItemContext(item));
	}


	/**
		Execute the RemoveCommand and call <i>node.doDelete()</i>.
		This is called by default in method AbstractEditCommand.redo().
	*/
	public Object doit()	{
		if (target.doDelete(commandArguments))
			return target;
		return null;
	}

	/**
		Re-insert the node that was deleted.
	*/
	public void undo() throws CannotUndoException	{
		super.undo();
		target.doInsert(commandArguments);
	}

}