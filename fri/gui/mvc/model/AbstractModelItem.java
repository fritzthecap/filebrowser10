package fri.gui.mvc.model;

import fri.gui.mvc.controller.CommandArguments;

/**
	AbstractModelItem performs default behaviour for create, delete, copy and move.
	The ModelItem represents a temporary wrapper for a view item (like TreeNode),
	providing executable methods for a controller. The controller must pass the
	MutableModel wrapped into CommandArgument subclass instances. So this temporary
	ModelItem does not know its Model except during certain method calls. Only the
	CommandArguments can bind a ModelItem to its Model (exchangeable ModelItem).
	<p>
	This implementation simplifies the insert/delete operations on MutableModel
	when performing copy/move. It provides CommandArguments (inner classes) for
	undoable Commands executed on the MutableModel, but it does neither create nor
	manage such Commands.
	
	@author  Ritzberger Fritz
*/

public abstract class AbstractModelItem implements
	ModelItem
{
	/** Argument that was received by <i>doCopy()</i> is stored to this variable before calling <i>clone()</i>. */
	protected CommandArguments pasteInfo;
	
	/** Target item that was received by <i>doCopy()</i> is stored to this variable before calling <i>clone()</i>. */
	protected ModelItem target;
	
	/** This gets set to true when a move happens, as <i>clone()</i> might need to know that. */
	protected boolean isMove;

	/** Contains the last happened error. MUST be set by copy/move implementations if an error happens! */
	protected String error;

	private final Object userObject;


	/** Create an item that performs copy, move, delete or insert. */
	public AbstractModelItem(Object userObject)	{
		this.userObject = userObject;
	}


	/**
		Returns a new ModelItem subclass from an object created within the specific data.
		The parent node is this node. This method MUST NOT insert the created item into the model!
		@return null if new item was not created.
	*/
	protected abstract ModelItem createInMedium(CommandArguments createInfo);
	
	
	/**
		This implementation returns null. Can be override if the position within child list is significant.
		If this method returns null (default), then the position is retrieved from createArguments.
	*/
	protected Integer createdPositionInMedium(ModelItem createdItem)	{
		return null;
	}

	/**
		If <i>createInfo.getCreateData()</i> returns non-null, this
		call will be the insertion of a newly created item (createInMedium) into this one, else it
		will be the insertion of this ModelItem into the parent retrieved from createInfo.
		@return null if error happened, else the newly created item (or this).
	*/
	public ModelItem doInsert(CommandArguments createArguments)	{
		//System.err.println("AbstractModelItem.doInsert, inserting item is: "+getUserObject()+", createArguments "+createArguments);
		CommandArguments args = null;
		ModelItem actor = null;

		if (createArguments.getCreateData() != null)	{	// create new item within this one
			actor = createInMedium(createArguments);
			if (actor == null)
				return null;
				
			Integer pos = createdPositionInMedium(actor);
			if (pos == null)
				pos = createArguments.getPosition();
				
			args = new CommandArguments.Context(createArguments.getModel(), this, pos);
		}
		else	{	// insert this item into parent from createArguments (is a undo from RemoveCommand)
			actor = this;
			args = createArguments;
		}

		return doInsert(args, actor);
	}

	/**
		This insert method is called by insert, move and copy. It just inserts into the receiving model.
		It calls <i>args.getReceivingModel().doInsert(actor, args)</i>.
	*/
	protected ModelItem doInsert(CommandArguments args, ModelItem actor)	{
		//System.err.println("protected AbstractModelItem.doInsert, inserting item is: "+getUserObject()+", arguments "+args+", actor "+actor.getUserObject());
		MutableModel m = args.getReceivingModel();
		m.doInsert(actor, args);
		return actor;
	}
	
	
	/** Returns true if this item could be deleted within the specific data. */
	protected abstract boolean deleteInMedium(CommandArguments deleteInfo);

	/**
		Delete this item. This method must be overridden if the implementation supports a wastebasket
		for deleted items, in this case the item must be moved to this wastebasket by calling <i>doMove()</i>.
		<p>
		This method calls <i>deleteInMedium()</i>. If this returns true, <i>model.doDelete(this)</i> gets called.
		@param deleteInfo information for deletion, containing at least the MutableModel
		@return true if deletion succeeded.
	*/
	public boolean doDelete(CommandArguments deleteInfo)	{
		//System.err.println("AbstractModelItem.doDelete, item to delete is: "+getUserObject()+", deleteInfo "+deleteInfo);
		boolean ret = false;
		
		if (deleteInMedium(deleteInfo))	{
			ret = true;
			deleteInfo.getModel().doDelete(this);
		}
		
		return ret;
	}


	/**
		Returns a new ModelItem when a move in data is possible without copying, else null.
		This default implementation returns null (no move available).
	*/
	protected ModelItem moveInMedium(ModelItem target, CommandArguments pasteInfo)	{
		return null;
	}

	/**
		Returns true if the created target would be identical with this item.
		This returns comparison of userObjects without regarding pasteInfo, to be overridden.
		If this returns true, no copy or move action is executed.
		<p>
		This method can be used to create a save-copy, which means to clone the node under
		another name in the same folder. If this is needed, a save copy name can be made by
		this method and left within a member variable, which can be used by <i>doCopy</i>
		to create a save-copy. Mind that the target must change to its parent in that case!
		@param targetItem the target node for trees, can be null for tables, then the result is always false.
		@param pasteInfo information about models and positions
		@param isCopy true when called by doCopy(), false when called by doMove().
		@return true if targetItem is equal to this ModelItem's user object.
	*/
	protected boolean isActionToSelf(ModelItem targetItem, CommandArguments pasteInfo, boolean isCopy)	{
		//System.err.println("AbstractModelItem.isActionToSelf, isCopy "+isCopy+", target = "+targetItem.getUserObject()+", moving = "+getUserObject());
		if (targetItem != null && targetItem.getUserObject().equals(getUserObject()))
			return true;
		
		return false;	// can not cover case child to its parent
	}
	
	/**
		Move this item to the target item. If <i>moveInMedium()</i> returns null and no error occured,
		this is done by copy and delete (the <i>isMove</i> flag gets set to true).
		Else the moved item gets inserted in the receiving model and deleted from the sending model.
		@param target the target node for trees, can be null for tables.
		@param pasteInfo the target model and the position.
		@return the moved item, or null if action failed.
	*/
	public ModelItem doMove(ModelItem target, CommandArguments pasteInfoArg)	{
		//System.err.println("AbstractModelItem.doMove, target = "+target.getUserObject()+", moving = "+getUserObject()+", pasteInfoArg "+pasteInfoArg);
		pasteInfoArg = setParentToCopyMoveArguments(pasteInfoArg, target);

		if (isActionToSelf(target, pasteInfoArg, false))
			return null;	// is move to itself
			
		ModelItem movedItem = moveInMedium(target, pasteInfoArg);
		
		if (error == null)	{
			if (movedItem == null)	{
				isMove = true;
				movedItem = doCopy(target, pasteInfoArg);
				
				if (movedItem != null)
					doDelete(new CommandArguments.Delete(pasteInfoArg.getSendingModel()));

				isMove = false;
			}
			else	{	// was moved within data, no copy/delete
				movedItem = doInsert(pasteInfoArg, movedItem);
	
				if (movedItem != null)	{	// successfully moved, remove the invalid old item from Model
					MutableModel sendingModel = pasteInfoArg.getSendingModel();
					if (sendingModel == null)
						throw new IllegalArgumentException("The clipoard source-model might not have been set to a valid value (use clipborad.setSourceModel): "+sendingModel);
					
					sendingModel.doDelete(this);	// do not call doDelete(), as the data are no more there!
				}
			}
		}
		
		return movedItem;
	}


	/**
		Copy this item to the passed item. This is done by inserting a clone() of this item.
		Before cloning this item the variable <i>copyPasteInfo</i> is evaluated with CommandArguments.
		@param target the target node for trees, can be null for tables.
		@param pasteInfo the target model and the position.
		@return the copied node, or null if action failed.
	*/
	public ModelItem doCopy(ModelItem target, CommandArguments pasteInfoArg)	{
		//System.err.println("AbstractModelItem.doCopy, target = "+target.getUserObject()+", moving = "+getUserObject()+", pasteInfo "+pasteInfoArg);
		pasteInfoArg = setParentToCopyMoveArguments(pasteInfoArg, target);
		
		this.pasteInfo = pasteInfoArg;
		this.target = target;
		
		if (isActionToSelf(target, pasteInfoArg, true))
			return null;	// is copy to itself
			
		ModelItem copiedItem = (ModelItem) clone();

		this.pasteInfo = null;
		this.target = null;
		
		return (copiedItem != null) ? doInsert(pasteInfoArg, copiedItem) : null;
	}


	private CommandArguments setParentToCopyMoveArguments(CommandArguments copyMoveArgs, ModelItem parent)	{
		copyMoveArgs.setParent(parent);
		return copyMoveArgs;
	}


	/**
		Returns the last error message from the item, after an action took place.
		@return null if action succeeded, else the error reason.
	*/
	public String getError()	{
		return error;
	}


	/**
		Must be implemented by subclasses. This gets called when an item is copied.
		This produces a deep copy that exists physically in the medium within the
		target node, which can be retrieved from member variable <i>target</i> ModelItem.
		The clone gets returned wrapped into a ModelItem.
		@return a deep copy ModelItem of this item.
	*/
	public abstract Object clone();
	
	
	/**
		Optional method, not required by framework.
		Returns a user object (e.g. the TreeNode of a TreeModel).
	*/
	public Object getUserObject()	{
		return userObject;
	}
	
	/**
		Delegates to useObject if not null.
	*/
	public String toString()	{
		return userObject != null ? userObject.toString() : super.toString();
	}

}
