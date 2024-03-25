package fri.gui.mvc.controller.clipboard;

import javax.swing.undo.*;
import fri.gui.mvc.controller.AbstractEditCommand;
import fri.gui.mvc.model.Model;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.CommandArguments;

/**
	Basic command pattern for a move action.
	On <i>doit()</i> it stores the parent of the moved item by
	<i>pasteInfo.getParent()</i>, to be able to move it back when
	undo is triggered. This implementation delegates to doit()
	when redo is triggered.

	@author  Ritzberger Fritz
*/

public class DefaultMoveCommand extends AbstractEditCommand
{
	private CommandArguments sourcePasteInfo;
	private ModelItem sourceParent;

	/**
		Create a MoveCommand, do not execute it. It will be undoable after calling doit().
		
		@param source item that is to be moved, non-null
		@param target container item where the new copy should live, can be null
		@param pasteInfo info about where to insert, and the source Model
	*/
	public DefaultMoveCommand(ModelItem source, ModelItem target, CommandArguments pasteInfo)	{
		this(null, null, source, target, pasteInfo);
	}

	/**
		Additionally store source and target editor.
	*/
	public DefaultMoveCommand(Object sourceEditor, Object targetEditor, ModelItem source, ModelItem target, CommandArguments pasteInfo)	{
		super(sourceEditor, targetEditor, source, target, pasteInfo);
	}


	/**
		Retrieves the sending model from CommandArguments.
		Retrieves the model item context of moving item (source) from sending model.
		Retrieves an optional parent from the model item context and stores it to a membervariable for undo.
		Retrieves the receiving model and sets it as sending model to the undo information.
		Then calls <i>source.doMove(target, pasteInfo)</i> to move the source item(s) to target, and stores the
		returned item into "newItem" membervariable.
		Now the Command it will be undoable.
		
		@return the newItem returned from <i>doMove</i> call.
	*/
	public Object doit()	{
		// retrieve information about where the item lives, for undo
		Model srcModel = commandArguments.getSendingModel();
		
		// Must check for null. Undo will not work when null.
		// Sometimes the source model can not be provided, e.g. when the source model
		// is a temporary Swing model, like in a right-side detail view of a left-side chooser list.
		// Then, in the moment of paste, this model does not contain the source item anymore.
		if (srcModel != null)	{
			this.sourcePasteInfo = srcModel.getModelItemContext(source);
			this.sourceParent = this.sourcePasteInfo.getParent();	// information where it would be inserted when undoing

			// set the counterpart model to the undo information
			this.sourcePasteInfo.setSendingModel(commandArguments.getReceivingModel());
		}
		else	{
			System.err.println("WARNING: DefaultMoveCommand - Undo will not work as source model was null in command argument.");
		}

		// move the item
		newItem = source.doMove(target, commandArguments);

		return newItem;
	}

	/**
		Move back the node that was created by move action.
		Calls <i>newItem.doMove(sourceParent, sourcePasteInfo)</i> where sourceParent is the
		return from <i>getParent()</i> call on sourcePasteInfo, and sourcePasteInfo is the
		retrieved source model item context,  both done in <i>doit()</i>.
	*/
	public void undo() throws CannotUndoException	{
		super.undo();
		
		source = newItem.doMove(sourceParent, sourcePasteInfo);
	}

}