package fri.gui.mvc.model;

import fri.gui.mvc.controller.CommandArguments;

/**
	An item within a model, that can be inserted, deleted,
	copied or moved. This interface is used by DefaultClipboard
	and its DefaultXXXCommands.
	<p>
	A ModelItem does not know about its parent, as it might
	be serialized for cut/copy and paste, i.e. drag and drop.
	So every method receives the necessary model(s) by CommandArguments.
	<p>
	The reason for implementing the contained methods in ModelItem (and not in Model)
	is the separation of element- and container-logic, whereas it is avoided
	to implement these methods twice, once in Model and once in ModelItem.<br>
	The only situation when no target ModelItem is present is an empty model, for
	this case the implementer could prepare a <i>getEmptyModelTargetItem()</i> method,
	the returned ModelItem could be used as non-existing dummy target. Another
	way to handle this is to simply return null from <i>commandArguments.getParent()</i>.

	@author  Ritzberger Fritz
*/

public interface ModelItem extends
	Cloneable,
	Movable
{
	/** Success error code from getError(), displayable instead of null. */
	public final static String NO_ERROR = "(no error)";

	/**
		Create a new item within, before or after "this" item.
		"This" item could be a temporary artificial item (empty lists), or the new item
		itself (created by "new XXXModelItem()"), or the creating parent item (hierarchies).
		<p />
		Convenience: If <i>getCreateData()</i> in CommandArguments returns non-null, this
		call will be the insertion of a newly created item (CreateCommand), else it comes
		from the undo of a RemoveCommand (or, implementation-dependent, from a Copy/MoveCommand).

		@param createInfo at least the identity for the new item, and the model.
		@return null if error happened, else the newly created item
			or "this" when insert was a undo from a RemoveCommand. But it is completely
			implementation dependent what to return. DefaultCreateCommand will call "doDelete"
			on it when undo is triggered, DefaultRemoveCommand will call "doDelete" on it when <i>doit()</i>
			is called. A "doMove" or "doCopy" implementation could use it for inserting after
			removing the move items or after cloning the copy items.
	*/
	public ModelItem doInsert(CommandArguments createInfo);

	/**
		Delete this item.

		@param deleteInfo information for deletion, at least the model
		@return true if deletion succeeded.
	*/
	public boolean doDelete(CommandArguments deleteInfo);

	/**
		Move this item within, before or after the passed item.
		The type of action is in CommandArguments.
		This method is called from <i>MoveCommand.doit()</i> with controller pasteInfo
		and from <i>MoveCommand.undo()</i> with sourcePasteInfo retrieved from sending model.
		
		@param pasteInfo at least the target model, optionally the
			target-item and -position.
		@return the moved item, or null if action failed.
			It could have a new identity, as a move sometimes
			is made by copy and delete.
	*/
	public ModelItem doMove(ModelItem target, CommandArguments pasteInfo);

	/**
		Copy this item within, before or after the passed item.
		The type of action is in CommandArguments.
		This method is not bidirectional like "doMove()" as <i>CopyCommand.undo()</i>
		uses <i>doDelete</i>.
		
		@param pasteInfo at least the target model, optionally the
			target-item and -position.
		@return the copied node, or null if action failed.
	*/
	public ModelItem doCopy(ModelItem target, CommandArguments pasteInfo);
	
	/**
		Optional method, not required by framework.
		Returns the last error message from the item, after an action took place.
		@return null or NO_ERROR if action succeeded.
	*/
	public String getError();

	/**
		Optional method, not required by framework, but will be needed by copy methods.
		Return a copy of this item. For hierarchies, set its parent to null.
	*/
	public Object clone();

	/**
		Optional method, not required by framework.
		Returns a user object (e.g. the TreeNode of a TreeModel).
	*/
	public Object getUserObject();
}