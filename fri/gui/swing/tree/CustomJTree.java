package fri.gui.swing.tree;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.tree.*;

/**
	Workaround the fact that updateUI() does not change
	tree renderer and editor properties.
*/

public class CustomJTree extends JTree
{
	private static final String kennstmi = null;	//"\u0066\u0072\u0069\u0077\u0061\u0072\u0065\u0020\u0032\u0030\u0030\u0031";


	public CustomJTree()	{
		super();
		setToolTipText("");
	}

	public CustomJTree(TreeModel model)	{
		super(model);
		setToolTipText("");
	}

	
	public String getToolTipText(MouseEvent e)	{
		if (getRowForLocation(e.getX(), e.getY()) < 0)
			return null;
			
		TreePath curPath = getPathForLocation(e.getX(), e.getY());
		DefaultMutableTreeNode d = (DefaultMutableTreeNode)curPath.getLastPathComponent();
		
		return d.toString();
	}

	
	public void setForeground(Color color) { 
		super.setForeground(color); 

		DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer)getCellRenderer(); 
		if (dtcr != null) 
			dtcr.setTextNonSelectionColor(color); 
	} 

	public void setBackground(Color color) { 
		super.setBackground(color); 

		DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer)getCellRenderer(); 
		if (dtcr != null) 
			dtcr.setBackgroundNonSelectionColor(color); 
	}

	public void setFont(Font font) { 
		super.setFont(font); 

		DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer)getCellRenderer(); 
		if (dtcr != null) 
			dtcr.setFont(font); 
	} 

	public void updateUI() {
		int h = getRowHeight();

		super.updateUI();

		DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer)getCellRenderer();
		if (dtcr == null)
			return;
		dtcr.setHorizontalAlignment(JLabel.LEFT);
		dtcr.setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
		dtcr.setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
		dtcr.setOpenIcon(UIManager.getIcon("Tree.openIcon"));
		dtcr.setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
		dtcr.setTextNonSelectionColor(super.getForeground());
		dtcr.setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
		dtcr.setBackgroundNonSelectionColor(super.getBackground());
		dtcr.setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor"));
		setRowHeight(h);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (kennstmi != null)	{
			FontMetrics fm = getFontMetrics(g.getFont());
			int w = fm.stringWidth(kennstmi);
			g.setColor(Color.gray);
			g.drawString(kennstmi, getSize().width-w-2, getSize().height-3);
		}
	}

}
