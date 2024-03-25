package fri.gui.swing.tree;

import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

/**
	Utility methods to get selected nodes from a JTree.
*/
public class TreeSelection
{
	protected JTree tree;
	
	public TreeSelection(JTree tree)	{
		this.tree = tree;
	}

	/**
		Returns all selected tree nodes.
	*/
	public List getAllSelectedNodes()	{
		TreePath [] tp = tree.getSelectionPaths();
		Vector v = null;
		
		for (int i = 0; tp != null && i < tp.length; i++)	{
			TreeNode node = (TreeNode) tp[i].getLastPathComponent();
			if (isNodeVisible(node))	{
				if (v == null)
					v = new Vector(tp.length);
				
				v.addElement(node);
			}
		}
		return v;
	}

	/**
		Returns getSignificantSelection().
	*/
	public List getSelectedNodes()	{
		return getSignificantSelection();
	}
	
	/**
		Get array of all DefaultMutableTreeNodes in tree without descendants
		of selected container nodes, as many actions are recursive in trees.
		This call works only for DefaultMutableTreeNodes!
		@return List of selected DefaultMutableTreeNodes or null if none is selected.
	*/
	public List getSignificantSelection()	{
		Vector v = (Vector) getAllSelectedNodes();
		
		Vector objects = null;
		
		if (v != null && v.size() > 0)	{
			// search for descendants in list
			Vector descendants = new Vector();
			for (int i = v.size() - 1; i >= 0; i--)	{
				DefaultMutableTreeNode n = (DefaultMutableTreeNode)v.elementAt(i);
	
				for (int j = v.size() - 1; j >= 0; j--)	{
					DefaultMutableTreeNode n1 = (DefaultMutableTreeNode)v.elementAt(j);
					
					if (n.equals(n1) == false && n.isNodeDescendant(n1))	// n1 is descendant of n
						descendants.addElement(n1);
				}
			}
			
			// collect all node-userobjects except descendants
			for (int i = 0; i < v.size(); i++)	{
				DefaultMutableTreeNode n = (DefaultMutableTreeNode)v.elementAt(i);
				
				if (descendants.indexOf(n) < 0 && isNodeVisible(n))	{
					if (objects == null)
						objects = new Vector();
						
					objects.addElement(n);
				}
			}
		}
		
		return objects;
	}
	
	private boolean isNodeVisible(TreeNode node)	{
		return node.getParent() != null || tree.getModel().getRoot().equals(node);
	}
	
}