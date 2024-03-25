package fri.gui.swing.ftpbrowser;

import java.awt.*;
import javax.swing.*;
import fri.gui.mvc.view.swing.MovePendingTreeCellRenderer;

public class TreeCellRenderer extends MovePendingTreeCellRenderer
{
	private Font fontLink, fontNormal;


	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean selected,
		boolean expanded,
		boolean leaf,
		int row,
		boolean hasFocus)
	{
		Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		
		if (value instanceof AbstractTreeNode == false)	// in error mode, else GUI dead
			return c;
		
		if (fontNormal == null)	{
			fontNormal = tree.getFont();
			fontLink = new Font(fontNormal.getName(), Font.ITALIC, fontNormal.getSize());
		}

		AbstractTreeNode n = (AbstractTreeNode)value;
		if (n.isLink())	{
			setFont(fontLink);
		}
		else	{
			setFont(fontNormal);
		}

		return this;
	}


	public Dimension getPreferredSize()	{
		Dimension d = super.getPreferredSize();
		d.width += d.width / 16;	// 10% breiter wegen geneigtem Font
		return new Dimension(d);
	}

}