package fri.gui.mvc.view.swing;

import java.awt.Component;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import fri.gui.mvc.model.swing.AbstractTreeNode;

/**
	A tree cell renderer that enables and disables nodes when they were "cutten".
	It can be used with TreeModels that use AbstractTreeNode as node type.
*/

public class MovePendingTreeCellRenderer extends DefaultTreeCellRenderer
{
	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean selected,
		boolean expanded,
		boolean leaf,
		int row,
		boolean hasFocus)
	{
		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		
		if (value instanceof AbstractTreeNode == false)	// in error mode, else GUI dead
			return this;
			
		AbstractTreeNode n = (AbstractTreeNode)value;

		setEnabled(n.isMovePending() == false);

		if (n.isMovePending())	{
			if (leaf)
				setDisabledIcon(getLeafIcon());
			else
			if (expanded)
				setDisabledIcon(getOpenIcon());
			else
				setDisabledIcon(getClosedIcon());
		}

		return this;
	}

	/** Implement updateUI as it is not in DefaultTreeCellRenderer. */
	public void updateUI() {
		super.updateUI();
		setHorizontalAlignment(JLabel.LEFT);
		setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
		setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
		setOpenIcon(UIManager.getIcon("Tree.openIcon"));
		setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
		setTextNonSelectionColor(UIManager.getColor("Tree.textForeground"));
		setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
		setBackgroundNonSelectionColor(UIManager.getColor("Tree.textBackground"));
		setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor"));
		setTextSelectionColor(UIManager.getColor("Table.selectionForeground"));
		setBackgroundSelectionColor(UIManager.getColor("Table.selectionBackground"));
	}

}
