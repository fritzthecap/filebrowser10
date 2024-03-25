package fri.gui.swing.treetable;

import javax.swing.tree.*;
import javax.swing.*;
import java.util.Vector;
import java.awt.Point;

/**
	Helpers to request and set selected nodes from and to a JTreeTable.
	This class holds the treetable as member variables.

	@author  Ritzberger Fritz
*/

public class TreeTableSelection
{
	protected JTreeTable treetable;


	/**
		Create a selection requester and setter for a treetable.
	*/
	public TreeTableSelection(JTreeTable treetable)	{
		setTreeTable(treetable);
	}

	/** Set another treetable to this selection object. */
	public void setTreeTable(JTreeTable treetable)	{
		this.treetable = treetable;
	}
	
	/**
		Returns a list of all selected DefaultMutableTreeNode nodes in treetable.
			Descendants of selected container nodes ARE in list!
	*/
	public Vector getAllSelectedNodes()	{
		if (treetable == null)
			return null;

		ListSelectionModel sm = treetable.getSelectionModel();
		int min = sm.getMinSelectionIndex();
		int max = sm.getMaxSelectionIndex();
		Vector v = null;
		for (int i = min; min != -1 && max != -1 && i <= max; i++)	{
			if (sm.isSelectedIndex(i))	{
				DefaultMutableTreeNode n = getNodeForRow(i);
				if (n != null)	{
					if (v == null)
						v = new Vector();
					v.addElement(n);
				}
			}
		}
		return v;
	}

	/**
		Returns a list of selected DefaultMutableTreeNode nodes in treetable.
			Descendants of selected container nodes are NOT in list!
	*/
	public Vector getSelectedNodes()	{
		Vector v = getAllSelectedNodes();
		if (v == null)
			return null;
			
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
		//System.err.println("  selected with descendants "+v);
		// delete descendants from list
		for (int i = descendants.size() - 1; i >= 0; i--)	{
			v.removeElement(descendants.elementAt(i));
		}
		//System.err.println("  selected without descendants "+v);
		
		return v;
	}
	

	/** Returns the last selected node in treetable (lead selection). */
	public DefaultMutableTreeNode getSelectedNode()	{
		if (treetable == null)
			return null;

		int i = treetable.getSelectionModel().getLeadSelectionIndex();
		return getNodeForRow(i);
	}

	/** Returns node in passed row of treetable. */
	public DefaultMutableTreeNode getNodeForRow(int row)	{
		if (treetable == null)
			return null;

		TreePath tp = treetable.getTree().getPathForRow(row);
		if (tp == null)
			return null;

		return (DefaultMutableTreeNode)tp.getLastPathComponent();
	}

	/** Returns the row of passed node from treetable. */
	public int getRowForNode(DefaultMutableTreeNode node)	{
		if (treetable == null)
			return -1;

		int row = treetable.getTree().getRowForPath(new TreePath(node.getPath()));
		if (row < 0)	{
			treetable.getTree().expandPath(new TreePath(((DefaultMutableTreeNode)node.getParent()).getPath()));
			row = treetable.getTree().getRowForPath(new TreePath(node.getPath()));
		}
		return row;
	}

	/** Returns the node at given Point coordinates in treetable. */
	public DefaultMutableTreeNode getNodeFromPoint(Point p)	{
		if (treetable == null)
			return null;

		int i = treetable.rowAtPoint(p);
		return getNodeForRow(i);
	}

	/** Drag&Drop support: Returns a row for a Point (MouseEvent). */
	public int getRowForPoint(Point p)	{
		if (treetable == null)
			return -1;

		return treetable.rowAtPoint(p);
	}
	
	/**
		Returns selected node, if it is a container, else its parent.
	*/
	public DefaultMutableTreeNode getSelectedContainerNode()	{
		DefaultMutableTreeNode n = getSelectedNode();
		if (n != null && n.isLeaf())
			n = (DefaultMutableTreeNode)n.getParent();
		return n;
	}

	/**
		Returns only selected container nodes, selected leafs get
		converted to its parent, the resulting list is unique.
	*/
	public Vector getSelectedContainerNodes()	{
		Vector v = getSelectedNodes();
		if (v == null)
			return new Vector(0);
			
		Vector w = new Vector(v.size());
		for (int i = 0; i < v.size(); i++)	{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode)v.elementAt(i);
			
			if (n.isLeaf())
				n = (DefaultMutableTreeNode)n.getParent();
				
			if (w.indexOf(n) < 0)
				w.addElement(n);
		}
		return w;
	}



	/** Sets or adds selection to treetable, according to passed flag set (false for add). */
	private void doSelection(final int row, final boolean set)	{
		if (treetable == null || row < 0)
			return;

		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				ListSelectionModel sm = treetable.getSelectionModel();
				int i = Math.min(row, treetable.getRowCount());
				//System.err.println("setting selection to row "+row);
				if (set)	{
					sm.setSelectionInterval(i, i);
				}
				else
					sm.addSelectionInterval(i, i);
			}
		});
	}
	
	/** Set a selection to treetable. */
	public void setSelection(int row)	{
		doSelection(row, true);
	}

	/** Add a selection to treetable. */
	public void addSelection(int row)	{
		doSelection(row, false);
	}

	/** Set a selection to treetable. */
	public void setSelection(DefaultMutableTreeNode newnode)	{
		int row = getRowForNode(newnode);
		setSelection(row);
	}
	
	/** Add a selection to treetable. */
	public void addSelection(DefaultMutableTreeNode newnode)	{
		int row = getRowForNode(newnode);
		addSelection(row);
	}

	/** Clear all selections of treetable */
	public void clearSelection()	{
		if (treetable == null)
			return;

		treetable.getSelectionModel().clearSelection();
	}

}
