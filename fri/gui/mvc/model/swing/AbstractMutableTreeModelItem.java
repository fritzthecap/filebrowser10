package fri.gui.mvc.model.swing;

import javax.swing.tree.MutableTreeNode;
import fri.gui.mvc.model.*;
import fri.gui.mvc.controller.CommandArguments;

/**
	ModelItem for AbstractMutableTreeModel that performs default behaviour for create, delete, copy and move.
	The ModelItem represents a temporary wrapper for a TreeNode, providing methods for a controller.
	This is NOT a view TreeNode!
	
	@author  Ritzberger Fritz
*/
public abstract class AbstractMutableTreeModelItem extends AbstractModelItem
{
	/** Create an item that performs copy, move, delete or insert. */
	public AbstractMutableTreeModelItem(MutableTreeNode userObject)	{
		super(userObject);
	}

	/**
		Assumes that <i>getUserObject()</i> returns AbstractTreeNode.
		Set this item as "cutten". This is done when action "Cut" takes place
		and has effect on the GUI, which requests this state from the item.
	*/
	public void setMovePending(boolean movePending)	{
		AbstractTreeNode n = (AbstractTreeNode) getUserObject();
		n.setMovePending(movePending);
	}

	/**
		Assumes that <i>getUserObject()</i> returns AbstractTreeNode.
		Return the "cutten" state of this item.
		Called by the GUI to set enabled/disabled the item renderer.
	*/
	public boolean isMovePending()	{
		AbstractTreeNode n = (AbstractTreeNode) getUserObject();
		return n.isMovePending();
	}

	/**
	 * Returns a new ModelItem from createInfo, under the assumption that there is no
	 * other medium than the TreeModel. As this is not realistic you should override this.
	 */
	protected ModelItem createInMedium(CommandArguments createInfo)	{
		AbstractTreeNode thisNode = (AbstractTreeNode) getUserObject();
		Object createData = createInfo.getCreateData();
		// a subclass would create a File or ... now and here
		AbstractTreeNode newNode = thisNode.createTreeNode(createData);
		AbstractMutableTreeModel model = (AbstractMutableTreeModel)createInfo.getModel();
		return model.createModelItem(newNode);
	}

	/** Just returns true under the assumption that there is no other medium than the TreeModel. */
	protected boolean deleteInMedium(CommandArguments deleteInfo)	{
		return true;
	}

	/** Returns a ModelItem clone of this item by calling cloneInMedium(). */
	public Object clone()	{
		AbstractTreeNode n = (AbstractTreeNode) getUserObject();
		Object treeNodeUserObject = cloneInMedium(n.getUserObject());
		if (treeNodeUserObject == null)
			return null;	// could not be copied
		
		AbstractMutableTreeModel model = (AbstractMutableTreeModel) pasteInfo.getReceivingModel();
		return model.createModelItem(((AbstractTreeNode) target.getUserObject()).createTreeNode(treeNodeUserObject));
	}

	/** Override to clone the userObject from tree node. This implementation just returns the passed object. */
	protected Object cloneInMedium(Object treeNodeUserObject)	{
		return treeNodeUserObject;
	}

}
