package fri.gui.mvc.controller;

import javax.swing.undo.*;
import fri.gui.mvc.model.ModelItem;

/**
	Abstract update command. This is not a standard edit, as it manipulates
	the ModelItem directly and the method to do this can not be standardized.
	Think: do it without triggering a new update edit by the controller's undo listener!

	@author  Ritzberger Fritz
*/

public abstract class AbstractUpdateCommand extends AbstractEditCommand
{
	protected Object oldValue, newValue;

	/**
		Create a UpdateCommand, does not execute it.
	*/
	public AbstractUpdateCommand(ModelItem item, Object oldValue, Object newValue)	{
		this(null, item, oldValue, newValue);
	}

	/**
		Additionally store the target editor.
	*/
	public AbstractUpdateCommand(Object targetEditor, ModelItem item, Object oldValue, Object newValue)	{
		super(null, targetEditor, null, item, null);

		this.oldValue = oldValue;
		this.newValue = newValue;
	}


	/**
		Set the new value directly to the editor, without notification of the undo listener.
		This doit() is called by redo() in superclass.
	*/
	public Object doit()	{
		setValue(target, newValue);
		return target;
	}

	/**
		Restore the value in specified column.
		The undo listener must not receive the event, as it would insert another edit.
	*/
	public void undo() throws CannotUndoException	{
		super.undo();
		setValue(target, oldValue);
	}

	/**
		Trick out the undo listener by calling the node directly.
		Refresh the View as this is a programmatical action.
		@param item the item to update.
		@param updateData the data that is the update, mostly a String name.
	*/
	protected abstract void setValue(ModelItem item, Object updateData);

}