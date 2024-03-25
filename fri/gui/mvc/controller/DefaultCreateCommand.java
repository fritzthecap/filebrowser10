package fri.gui.mvc.controller;

import javax.swing.undo.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.controller.CommandArguments;

/**
	Default create command.

	@author  Ritzberger Fritz
*/

public class DefaultCreateCommand extends AbstractEditCommand
{
	/**
		Create a CreateCommand, do not execute it. It will be undoable after calling doit().
		@param parent ModelItem that will create i.e. <i>doInsert()</i> the new item using createData
		@param model the MutableModel where to insert i.e. <i>doInsert()</i> the new item
		@param createData the name or value of the new item
	*/
	public DefaultCreateCommand(ModelItem parent, MutableModel model, Object createData)	{
		this(null, parent, model, createData);
	}
	
	/**
		Create a CreateCommand, do not execute it. It will be undoable after calling doit().
		@param parent ModelItem that will create i.e. <i>doInsert()</i> the new item using createData
		@param model the MutableModel where to insert i.e. <i>doInsert()</i> the new item
		@param createData the name or value of the new item
		@param position the position where to create the new item
	*/
	public DefaultCreateCommand(ModelItem parent, MutableModel model, Object createData, Integer position)	{
		this(null, parent, model, createData, position);
	}
	
	/** Additionally store the target editor. */
	public DefaultCreateCommand(Object targetEditor, ModelItem parent, MutableModel model, Object createData)	{
		this(targetEditor, parent, model, createData, null);
	}
	
	/** Additionally store the target editor. */
	public DefaultCreateCommand(Object targetEditor, ModelItem parent, MutableModel model, Object createData, Integer position)	{
		super(null, targetEditor, null, parent, new Arguments(model, createData, position));
	}
	

	/**
		Execute the CreateCommand.  This is called by default from redo().
	*/
	public Object doit()	{
		source = (ModelItem)target.doInsert(commandArguments);
		return source;
	}

	/**
		Remove the created item.
	*/
	public void undo() throws CannotUndoException	{
		super.undo();

		if (source != null)
			source.doDelete(commandArguments);
	}



	private static class Arguments extends CommandArguments
	{
		Arguments(MutableModel model, Object createData, Integer position)	{
			this.model = model;
			this.createData = createData;
			this.position = position;
		}
	}

}