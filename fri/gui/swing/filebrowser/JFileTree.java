package fri.gui.swing.filebrowser;

import java.awt.event.MouseEvent;
import javax.swing.tree.*;
import fri.gui.swing.tree.CustomJTree;

/**
	Workaround JDK 1.2 Bugs, that does not updateUI() for renderers
	and editors.<br>
	Provide a tooltip with file information.<br>
	Deny editing of root and nodes that are not editable (WINDOWS drives).
*/

public class JFileTree extends CustomJTree
{
	public JFileTree(TreeModel model)	{
		super(model);
		setRowHeight(14);
		putClientProperty("JTree.lineStyle", "Angled");
		setToolTipText("");	// must be set once for tooltips appearance
		setExpandedState(getPathForRow(0), false);
	}
	
	/** Retrieves the tooltip from NetNode of selected treenode. */
	public String getToolTipText(MouseEvent e)	{
		if (getRowForLocation(e.getX(), e.getY()) < 0)
			return null;
			
		TreePath curPath = getPathForLocation(e.getX(), e.getY());
		DefaultMutableTreeNode d = (DefaultMutableTreeNode)curPath.getLastPathComponent();
		
		try	{
			NetNode n = (NetNode)d.getUserObject();
			return n.getToolTipText();
		}
		catch (ClassCastException ex)	{
			return null;
		}
	}

	/** Overridden to check fro editing root or editing a node that is not manipulable. */
	public boolean isPathEditable(TreePath tp)	{
		if (tp.getPathCount() <= 1)	{	// root
			return false;
		}
		
		DefaultMutableTreeNode tn = (DefaultMutableTreeNode)tp.getLastPathComponent();
		NetNode n = (NetNode)tn.getUserObject();
		if (n.isManipulable() == false)	{
			return false;
		}

		return super.isPathEditable(tp);
	}

}
			