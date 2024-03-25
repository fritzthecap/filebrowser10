package fri.gui.mvc.model.swing;

import javax.swing.tree.*;
import fri.gui.mvc.util.swing.EventUtil;
import fri.gui.mvc.controller.CommandArguments;
import fri.gui.mvc.model.*;
import fri.gui.swing.tree.EditNotifyingTreeModel;

/**
 * Abstract implementation of a DefaultTreeModel with MVC framework capability.
 * The <i>doInsert</i> and <i>doDelete</i> methods are safe against being called
 * from a background thread by using <i>EventUtil.invokeSynchronous</i>.
 * 
 * @author Fritz Ritzberger
 */
public abstract class AbstractMutableTreeModel extends EditNotifyingTreeModel implements
	MutableModel
{
	public AbstractMutableTreeModel(TreeNode root)	{
		super(root);
	}
	
	public AbstractMutableTreeModel(TreeNode root, boolean askAllowsChildren)	{
		super(root, askAllowsChildren);
	}
	
	/** Subclasses must implement the allocation of a ModelItem from a MutableTreeNode. */
	public abstract ModelItem createModelItem(MutableTreeNode node);
	
	/** Returns the context of the passed item within its model. */
	public CommandArguments getModelItemContext(ModelItem item)	{
		MutableTreeNode node = (MutableTreeNode) item.getUserObject();
		MutableTreeNode pnt = (MutableTreeNode) node.getParent();
		Integer pos = TreeNodeUtil.getPosition(node);
		return new CommandArguments.Context(this, createModelItem(pnt), pos);
	}

	/** Inserts the passed item. */
	public ModelItem doInsert(final ModelItem item, final CommandArguments arg)	{
		Runnable r = new Runnable()	{
			public void run()	{
				MutableTreeNode newChild = (MutableTreeNode) item.getUserObject();
				MutableTreeNode parent = (MutableTreeNode) arg.getParent().getUserObject();
				Integer index = arg.getPosition();
				insertNodeInto(newChild, parent, index != null ? index.intValue() : parent.getChildCount() /*append to end*/);
			}
		};
		EventUtil.invokeSynchronous(r);
		return item;
	}

	/** Deletes the passed item. Sets changed to true. */
	public boolean doDelete(final ModelItem item)	{
		Runnable r = new Runnable()	{
			public void run()	{
				removeNodeFromParent((MutableTreeNode) item.getUserObject());
			}
		};
		EventUtil.invokeSynchronous(r);
		return true;
	}

	/**
	 * Returns the child if the passed name exists in child list of parent.
	 * <i>toString()</i> is used for that purpose.
	 */
	public MutableTreeNode getChildByName(MutableTreeNode parent, String name)	{
		for (int i = 0; i < getChildCount(parent); i++)	{
			MutableTreeNode child = (MutableTreeNode) getChild(parent, i);
			if (child.toString().equals(name))
				return child;
		}
		return null;
	}
	
	/**
	 * Returns the TreeNode with given path, searching from root (root itself MUST NOT be in path).
	 * The comparison is made by <i>treenode.toString()</i> method, so it does not work in trees
	 * that support non-unique child names.
	 * This call will not expand the whole tree hierarchy, but it will fail when cycles are in the tree.
	 */
	public TreeNode locate(String [] path)	{
		TreeNode root = (TreeNode) getRoot();
		if (path == null || path.length <= 0)
			return root;
		return locate(root, path, 0);
	}
	
	private TreeNode locate(TreeNode node, String [] path, int index)	{
		for (int i = 0; i < getChildCount(node); i++)	{
			TreeNode n = (TreeNode) getChild(node, i);
			
			if (n.toString().equals(path[index]))
				if (index == path.length - 1)	// at end
					return n;
				else	// have to search deeper
					return locate(n, path, index + 1);
		}
		return null;
	}

	/**
	 * CAUTION: this call may expand the whole hierarchy, so it should be used on small trees only!
	 * Returns the DefaultMutableTreeNode that contains passed userObject, searching from root.
	 * The comparison is made by <i>treenode.getUserObject().equals(userObject)</i>.
	 */
	public DefaultMutableTreeNode locate(Object userObject)	{
		return locate((DefaultMutableTreeNode) getRoot(), userObject);
	}
	
	private DefaultMutableTreeNode locate(DefaultMutableTreeNode node, Object userObject)	{
		if (node.getUserObject() != null && node.getUserObject().equals(userObject))
			return node;
			
		for (int i = 0; i < getChildCount(node); i++)	{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) getChild(node, i);
			DefaultMutableTreeNode found = locate(n, userObject);
			if (found != null)
				return found;
		}
		return null;
	}

}
