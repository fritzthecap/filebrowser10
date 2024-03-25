package fri.gui.swing.filebrowser;

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

public class FileTreeCellRenderer extends DefaultTreeCellRenderer
{
	private Font fontLink = null, fontNormal = null;
	private Icon recycler = new ImageIcon(getClass().getResource("images/Recycler.gif"));


	public Dimension getPreferredSize()	{
		Dimension d = super.getPreferredSize();
		d.width += d.width / 16;	// 10% breiter wegen geneigtem Font
		return d;
	}
	

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
		
		// handle disabled items
		DefaultMutableTreeNode d = (DefaultMutableTreeNode)value;
		boolean movePending = false, islink = false, isRecycler = false;
		try	{
			NetNode n = (NetNode)d.getUserObject();
			movePending = n.getMovePending();
			islink = n.isLink();
			NetNode wb = n.getWastebasket();
			isRecycler = wb != null && n.equals(wb);
		}
		catch (ClassCastException e)	{
		}
					
		setEnabled(movePending == false);

		if (fontNormal == null)	{
			fontNormal = tree.getFont();
			fontLink = new Font(fontNormal.getName(), Font.ITALIC, fontNormal.getSize());
		}

		if (islink)	{
			setFont(fontLink);
		}
		else	{
			setFont(fontNormal);
		}
		
		if (movePending)	{
			if (leaf)
				setDisabledIcon(getLeafIcon());
			else
			if (expanded)
				setDisabledIcon(getOpenIcon());
			else
				setDisabledIcon(getClosedIcon());
		}
		else
		if (isRecycler)	{
			setIcon(recycler);
		}
		
		return this;
	}
}
