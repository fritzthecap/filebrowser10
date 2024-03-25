package fri.gui.mvc.view.swing;

import java.awt.Point;
import javax.swing.JTree;
import javax.swing.tree.*;
import fri.gui.mvc.view.swing.SelectionDnd;
import fri.gui.mvc.view.MultipleSelection;
import fri.gui.swing.tree.TreeSelection;

/**
	Tree selection, returning selected DefaultMutableTreeNode(s), always in a List.
	There are two different methods: one for all nodes, one containing no dependent descendants.
	<p>
	CAUTION: Setting the selection expects DefaultMutableTreeNode in passed List!
	
	@author Fritz Ritzberger
*/

public class TreeSelectionDnd extends TreeSelection implements
	SelectionDnd,
	MultipleSelection
{
	public TreeSelectionDnd(JTree tree)	{
		super(tree);
	}

	private DefaultMutableTreeNode getTreeNode(TreePath tp)	{
		if (tp == null)
			return null;
		return (DefaultMutableTreeNode)tp.getLastPathComponent();
	}

	
	/** Returns selected TreeNodes in a List where descendants have been removed, or null if nothing is selected. */
	public Object getSelectedObject()	{
		return getSignificantSelection();
	}
	
	/** Returns all selected TreeNodes in a List, or null if nothing is selected. */
	public Object getAllSelectedObject()	{
		return getAllSelectedNodes();
	}
	
	/**
		Set selection in the view. @param o node to select, must be instanceof DefaultMutableTreeNode.
	*/
	public void setSelectedObject(Object o)	{
		DefaultMutableTreeNode d = (DefaultMutableTreeNode)o;
		TreePath tp = new TreePath(d.getPath());
		tree.setSelectionPath(tp);
	}

	/**
		Add a selection to current selected items of view.
		@param o node to add to selection, must be instanceof DefaultMutableTreeNode.
	*/
	public void addSelectedObject(Object o)	{
		DefaultMutableTreeNode d = (DefaultMutableTreeNode)o;
		TreePath tp = new TreePath(d.getPath());
		tree.addSelectionPath(tp);
	}

	/** Clear selection of the view. */
	public void clearSelection()	{
		tree.clearSelection();
	}

	/** Get a node from a Point in the view. */
	public Object getObjectFromPoint(Point p)	{
		TreePath tp = tree.getPathForLocation(p.x, p.y);
		return getTreeNode(tp);
	}

}
