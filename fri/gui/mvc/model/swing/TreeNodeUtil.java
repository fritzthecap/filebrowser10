package fri.gui.mvc.model.swing;

import javax.swing.tree.*;

/**
	Utilities in conjunction with tree nodes.
*/

public abstract class TreeNodeUtil
{
	/**
		Find a node in the passed root by comparing via <i>treenode.toString().equals(pathPart)</i>.
		The passed path does not contain the root node.
		@return the node that was idientified by passed path, or null if not found.
	*/
	public static TreeNode locate(TreeNode root, String [] path)	{
		TreeNode current = root;
		
		for (int i = 0; i < path.length; i++)	{
			//System.err.println("... locating path part "+path[i]+" in parent node "+current);
			TreeNode n = locateInChildren(current, path[i]);

			if (n == null)	// child not found
				return null;
			else
			if (i == path.length - 1)	// at end, identified
				return n;
			else
				current = n;
		}
		return null;
	}

	private static TreeNode locateInChildren(TreeNode node, String pathPart)	{
		for (int i = 0; i < node.getChildCount(); i++)	{
			TreeNode n = (TreeNode)node.getChildAt(i);
			if (n.toString().equals(pathPart))
				return n;
		}
		return null;
	}

	
	/** Returns the parent when node does not allow children. This is for pasting on leafs. */
	public static DefaultMutableTreeNode getParentWhenLeaf(DefaultMutableTreeNode node)	{
		if (node.getAllowsChildren() == false)
			return (DefaultMutableTreeNode)node.getParent();
		return node;
	}


	/** Returns the Integer position of the passed node within its parent. This is for locating insert positions. */
	public static Integer getPosition(TreeNode node)	{
		TreeNode parent = node.getParent();
		return parent != null ? new Integer(parent.getIndex(node)) : null;
	}


	private TreeNodeUtil()	{}
}
