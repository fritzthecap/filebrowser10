package fri.gui.swing.filebrowser;

import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.tree.*;
import java.util.Vector;

/**
	(1) JTree requesting and setting methods.<br>
	(2) Delegation to other selection Components (like JTable).
*/

public class TreeRequester
{
	protected JTree tree;
	// not null if another Component than tree is requested
	protected boolean delegateActive = false;
	protected DefaultMutableTreeNode [] delegateSelection = null;	
	protected NodeRenamer renamer = null;
	protected NodeInserter inserter = null;
	protected NodeSelecter selecter = null;
	protected DefaultMutableTreeNode delegateFolder = null;
	protected JFrame delegateFrame = null;
	
	
	public TreeRequester(JTree tree)	{
		this.tree = tree;
	}


	/** set other nodes than those selected in JTree */
	public void setDelegateSelection(
		JFrame delegateFrame,
		DefaultMutableTreeNode [] delegateSelection,
		NodeRenamer renamer,
		NodeInserter inserter,
		NodeSelecter selecter,
		DefaultMutableTreeNode delegateFolder)
	{
		//System.err.println("setDelegateSelection "+delegateSelection+", inserter "+(inserter != null)+", folder "+delegateFolder);
		delegateActive = true;
		this.delegateFrame = delegateFrame;
		this.delegateSelection = delegateSelection;
		this.renamer = renamer;
		this.inserter = inserter;
		this.selecter = selecter;
		this.delegateFolder = delegateFolder;
	}

	/** reset selection delegation */
	public void resetDelegateSelection()	{
		//System.err.println("resetDelegateSelection");
		delegateActive = false;
		this.delegateFrame = null;
		this.delegateSelection = null;
		this.renamer = null;
		this.inserter = null;
		this.selecter = null;
		this.delegateFolder = null;
	}
	
	
	protected int getSelectionCount()	{
		if (delegateActive)
			return delegateSelection != null ? delegateSelection.length : 0;
		return tree.getSelectionCount();
	}
	
	
	protected DefaultMutableTreeNode [] cloneArray(DefaultMutableTreeNode [] d)	{
		DefaultMutableTreeNode [] clone = new DefaultMutableTreeNode [d.length];
		System.arraycopy(d, 0, clone, 0, d.length);
		return clone;
	}


	/**
		@return a list of all selected nodes in tree.
			Selected descendants of selected container nodes are not in list!
	*/
	public DefaultMutableTreeNode [] getSelectedTreeNodes()	{
		if (delegateActive)
			return delegateSelection != null ? delegateSelection : new DefaultMutableTreeNode [0];
			
		TreePath [] tp = tree.getSelectionPaths();
		Vector v = null;
		for (int i = 0; tp != null && i < tp.length; i++)	{
			if (v == null)
				v = new Vector(tp.length);
			v.addElement(getTreeNode(tp[i]));
		}
		if (v == null || v.size() <= 0)
			return new DefaultMutableTreeNode [0];
			
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
		
		DefaultMutableTreeNode [] d = new DefaultMutableTreeNode [v.size()];
		v.copyInto(d);
		
		return d;
	}
	

	protected DefaultMutableTreeNode getSelectedTreeNode()	{
		if (delegateActive)
			return delegateSelection != null ? delegateSelection[0] :
				delegateFolder != null ? delegateFolder : null;	// only for "find"
		
		TreePath tp = tree.getSelectionPath();
		return getTreeNode(tp);
	}

	protected DefaultMutableTreeNode getTreeNode(TreePath tp)	{
		if (tp == null)
			return null;
		return (DefaultMutableTreeNode)tp.getLastPathComponent();
	}

	protected NetNode getSelectedNode()	{
		DefaultMutableTreeNode d = getSelectedTreeNode();
		return d == null ? null : (NetNode)d.getUserObject();
	}

	public NetNode [] getSelectedNodes()	{
		DefaultMutableTreeNode [] d = getSelectedTreeNodes();
		NetNode [] nn = new NetNode [d.length];
		for (int i = 0; i < d.length; i++)
			nn[i] = (NetNode)d[i].getUserObject();
		return nn;
	}


	public NetNode [] getSelectedLeafNodes()	{
		TreePath [] tp = tree.getSelectionPaths();
		Vector v = new Vector(tp != null ? tp.length : 0);
		for (int i = 0; tp != null && i < tp.length; i++)	{
			DefaultMutableTreeNode dn = getTreeNode(tp[i]);
			if (dn != null && dn.getAllowsChildren() == false)
				v.addElement(dn.getUserObject());
		}
		NetNode [] nn = new NetNode [v.size()];
		for (int i = 0; i < v.size(); i++)
			nn[i] = (NetNode)v.elementAt(i);
		return nn;
	}


	protected DefaultMutableTreeNode [] getSelectedContainerTreeNodes()	{
		DefaultMutableTreeNode [] d = getSelectedTreeNodes();
		int k = 0, i = 0;
		
		for (; i < d.length; i++)	{	// convert to container if necessary and collect unique
			DefaultMutableTreeNode dd = getContainerTreeNode(d[i]);

			NetNode n2 = (NetNode)dd.getUserObject();
			boolean unique = true;

			for (int j = 0; unique && j < i; j++)	{
				NetNode n1 = (NetNode)d[j].getUserObject();
				if (n1.equals(n2))
					unique = false;
			}
			
			if (unique)	{
				d[k] = dd;
				k++;
			}
		}
		
		if (i > k)	{	// shorten array
			DefaultMutableTreeNode [] dnew = new DefaultMutableTreeNode[k];
			System.arraycopy(d, 0, dnew, 0, k);
			return dnew;
		}
		
		return d;
	}


	protected DefaultMutableTreeNode getSelectedContainerTreeNode()	{
		DefaultMutableTreeNode d = getSelectedTreeNode();
		return getContainerTreeNode(d);
	}



	protected DefaultMutableTreeNode getParentNode(DefaultMutableTreeNode d)	{
		DefaultMutableTreeNode d1 = (DefaultMutableTreeNode)d.getParent();
		if (d1 == null)
			return d;
		return d1;
	}
	
	protected DefaultMutableTreeNode getContainerTreeNode(DefaultMutableTreeNode d)	{
		NetNode n = (NetNode)d.getUserObject();	// only node knows whether it is a container
		if (n.isLeaf())	{	// seek parent container
			d = getParentNode(d);
		}
		return d;
	}

	protected NetNode getSelectedContainerNode()	{
		DefaultMutableTreeNode d = getSelectedContainerTreeNode();
		return (NetNode)d.getUserObject();
	}

	protected NetNode getNodeFromMouseEvent(MouseEvent e)	{
		TreePath tp = getTreePathFromMouseEvent(e);
		if (tp == null)
			return null;
		DefaultMutableTreeNode d = (DefaultMutableTreeNode)tp.getLastPathComponent();
		NetNode n = null;
		if (d != null)
			n = (NetNode)d.getUserObject();
		return n;
	}

	public TreePath getTreePathFromMouseEvent(MouseEvent e)	{
		if (tree.getRowForLocation(e.getX(), e.getY()) < 0)
			return null;
		return tree.getPathForLocation(e.getX(), e.getY());
	}

	/**
		@return true if all nodes in the passed array are container nodes.
	*/
	protected boolean areAllNodesContainers(DefaultMutableTreeNode [] nodes)	{
		for (int i = 0; i < nodes.length; i++)	{
			NetNode n = (NetNode)nodes[i].getUserObject();
			if (n.isLeaf())
				return false;
		}
		return true;
	}
	
	/**
		@return true if d1 is a child (not grandchild) of d, else false
		@param d parent node
		@param might be a child of d
	*/
	public boolean isIn(DefaultMutableTreeNode parent, DefaultMutableTreeNode node)	{
		int max = parent.getChildCount();
		for (int i = 0; i < max; i++)	{
			NetNode c = (NetNode)((DefaultMutableTreeNode)parent.getChildAt(i)).getUserObject();
			if (c.equals(node.getUserObject()))
				return true;
		}
		return false;
	}

	/**
		@return node that can be selected after removing passed node.
		@param d node that is to be removed.
	*/
	public DefaultMutableTreeNode selectionAfterRemove(DefaultMutableTreeNode d)	{
		DefaultMutableTreeNode d1 = null;
		d1 = d.getNextSibling();
		if (d1 == null)
			d1 = d.getPreviousSibling();
		if (d1 == null)
			d1 = (DefaultMutableTreeNode)d.getParent();
		return d1;
	}
	
	
	/**
		@param d node to add to current selection
	*/
	public void addSelection(DefaultMutableTreeNode dn)	{
		TreePath tp = new TreePath(dn.getPath());
		System.err.println("  adding to selection node "+tp);
		tree.addSelectionPath(tp);
	}
	
	public void clearSelection()	{
		tree.clearSelection();
	}
	
}