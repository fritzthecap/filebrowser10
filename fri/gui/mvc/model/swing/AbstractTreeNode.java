package fri.gui.mvc.model.swing;

import javax.swing.tree.DefaultMutableTreeNode;
import fri.gui.mvc.model.Movable;

/**
	TreeNode base class that contains methods for the 'move pending' state
	and provides an abstract <i>list()</i> method that gets called on node
	expansion. The list() method must fill the protected <i>children</i>
	member variable with TreeNodes.
	
	@author Fritz Ritzberger
*/

public abstract class AbstractTreeNode extends DefaultMutableTreeNode implements Movable
{
	private boolean movePending;
	private boolean listed;

	public AbstractTreeNode(Object userObject)	{
		super(userObject);
	}

	public AbstractTreeNode(Object userObject, boolean askAllowsChildren)	{
		super(userObject, askAllowsChildren);
	}

	/**
		Factory method.
		This should be called from <i>list()</i> overrides to create child nodes.
		Creates a new AbstractTreeNode subclass from passed creation data, but
		does not insert or add the node to this node's children.
	 */
	public abstract AbstractTreeNode createTreeNode(Object createData);

	/** Called when a tree node expands for the first time. */
	public int getChildCount() {
		listNode();
		return super.getChildCount();
	}

	private void listNode()	{
		if (listed == false)	{
			listed = true;
			list();
		}
	}
	
	/** Subclasses must implement the listing of the node children. */
	protected abstract void list();
	
	/**
	 * Set this item as "cutten". This is done when action "Cut" takes place
	 * and has effect on the GUI, which requests this state from the item.
	 */
	public void setMovePending(boolean movePending)	{
		this.movePending = movePending;
	}

	/**
	 * Return the "cutten" state of this item.
	 * Called by the GUI to set the item renderer enabled/disabled.
	 */
	public boolean isMovePending()	{
		return movePending;
	}

	/**
	 * Sets the <i>children</i> list to null and <i>listed</i> to false,
	 * next <i>getChildCount()</i> call will refresh this view node.
	 */
	public void releaseChildren()	{
		children = null;
		listed = false;
	}

	/**
	 * Convenience method to search for a TreeNode by comparing its string representation.
	 */
	public AbstractTreeNode existsChild(String childName)	{
		for (int i = 0; i < getChildCount(); i++)	{
			if (getChildAt(i).toString().equals(childName))
				return (AbstractTreeNode)getChildAt(i);
		}
		return null;
	}

	/**
	 * Returns true this node can be dragged. Default this is true for all nodes except the root.
	 */
	public boolean isDragable()	{
		return isRoot() == false;
	}

}
