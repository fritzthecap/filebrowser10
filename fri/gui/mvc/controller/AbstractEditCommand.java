package fri.gui.mvc.controller;

import javax.swing.undo.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.UndoableCommand;
import fri.gui.mvc.controller.CommandArguments;

/**
	Basic command pattern for undoable actions.
	Holds the source and target nodes of e.g. a move command.
	The variable "newItem" can be used to store a newly created item.
	The variable "commandArguments" holds information about where the
	item got inserted by doit(). Contained are furthermore the source
	and target model, and support for editors that could be closed
	(if both source and target editor are closed, the edit is ready to
	be removed from undo manager).

	@author  Ritzberger Fritz
*/

public abstract class AbstractEditCommand extends AbstractUndoableEdit implements
	UndoableCommand
{
	protected ModelItem target, source;	// the copied or moved item, and its target item (new parent)
	protected ModelItem newItem;	// the item that will be created on a copy or move command
	protected CommandArguments commandArguments;
	protected Object sourceEditor, targetEditor;


	/** Store the source and target node to member variables. */
	protected AbstractEditCommand(ModelItem source, ModelItem target)	{
		this.source = source;
		this.target = target;
	}

	/**
		Store the source and target node to member variables.
		Store pasteInfo to member variable.
	*/
	protected AbstractEditCommand(ModelItem source, ModelItem target, CommandArguments commandArguments)	{
		this(source, target);
		this.commandArguments = commandArguments;
	}

	/**
		Additionally store the source and target editors. They are needed to decide when the edit dies.
	*/
	protected AbstractEditCommand(Object sourceEditor, Object targetEditor, ModelItem source, ModelItem target, CommandArguments commandArguments)	{
		this(source, target, commandArguments);
		this.sourceEditor = sourceEditor;
		this.targetEditor = targetEditor;
	}


	/** Default implementation of redo: Call doit() again. */
	public void redo() throws CannotRedoException	{
		super.redo();
		doit();
	}

	/**
		If the source editor is identical with the passed closed editor, it is set to null.
		If the target editor is identical with the passed closed editor, it is set to null.
		Returns true as soon as source and target editor are both null.
	*/
	public boolean diesWhenEditorClosed(Object closedEditor)	{
		if (sourceEditor == closedEditor)
			sourceEditor = null;
				
		if (targetEditor == closedEditor)
			targetEditor = null;

		return sourceEditor == null && targetEditor == null;
	}
	
	public Object getSourceEditor()	{
		return sourceEditor;
	}
	
	public Object getTargetEditor()	{
		return targetEditor;
	}

	/**
	 * Returns true if this edit can work when passed item is no more available.
	 * In this default implementation all three of source, target and newItem are checked
	 * if they match the passed item's user object. 
	 */
	public boolean canLiveWithoutModelItem(ModelItem disappeared)	{
		if (source != null && source.getUserObject().equals(disappeared.getUserObject()))
			return false;
		if (target != null && target.getUserObject().equals(disappeared.getUserObject()))
			return false;
		if (newItem != null && newItem.getUserObject().equals(disappeared.getUserObject()))
			return false;
		return true;
	}

}
