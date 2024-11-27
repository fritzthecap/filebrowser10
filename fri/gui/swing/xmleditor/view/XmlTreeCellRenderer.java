package fri.gui.swing.xmleditor.view;

import java.util.Hashtable;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.w3c.dom.Node;
import fri.gui.swing.xmleditor.model.MutableXmlNode;

/**
	Renders a typical icon for every Node type. This is done by a
	Icon helper class that paints a XML-Tag shortcut on the original icon.
	<p>
	Sets the icon and label enabled or disabled according to
	the move-pending state of the rendered XmlNode. Sets specific
	fonts for every type of XML node. Sets the label for document node
	and comment nodes.
	<p>
	Implements updateUI() to reset all settings and clear all icon caches.
	<p>
	This renderer only works with MutableXmNode as TreeNode.

	@author  Ritzberger Fritz
 */

public class XmlTreeCellRenderer extends DefaultTreeCellRenderer
{
	private static Hashtable cache = new Hashtable();	// icon cache
	private static Hashtable disabledCache = new Hashtable();	// disabled icon cache
	private MutableXmlNode item;	// buffered for getIcon()
	private Font elementFont, entityRefFont, othersFont;	// buffered fonts
	private Color origColor;	// buffered color


	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean selected,
		boolean expanded,
		boolean leaf,
		int row,
		boolean focus)
	{
		this.item = (MutableXmlNode)value;
		
		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, focus);

		// render cutten nodes disabled
		setEnabled(item.getMovePending() == false);

		// ensure colors have been created
		if (origColor == null)
			origColor = tree.getForeground();

		// set fonts according to rendered XML node type
		if (item.getNodeType() == Node.ELEMENT_NODE)	{
			if (elementFont == null)
				elementFont = tree.getFont().deriveFont(Font.BOLD);
			setFont(elementFont);
		}
		else
		if (item.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE || item.getNodeType() == Node.ENTITY_REFERENCE_NODE)	{
			if (entityRefFont == null)
				entityRefFont = tree.getFont().deriveFont(Font.ITALIC, (float)(tree.getFont().getSize() - 2));
			setFont(entityRefFont);
		}
		else	{	// set smaller font for #comment and #document
			if (othersFont == null)
				othersFont = tree.getFont().deriveFont((float)(tree.getFont().getSize() - 2));
			setFont(othersFont);
		}

		// render errors by red color
		if (item.getError() != null)
			setForeground(Color.red);
		else
			setForeground(origColor);

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

		othersFont = elementFont = entityRefFont = null;
		origColor = null;
		cache = new Hashtable();	// icon cache
		disabledCache = new Hashtable();	// disabled icon cache
	}


	/** Overridden to return an icon appropriate for the rendered XML node. */
	public Icon getIcon() {
		Icon original = super.getIcon();

		Integer key = Integer.valueOf(item.getNodeType());
		StringOverlayIcon icon = (StringOverlayIcon) cache.get(key);
		if (icon == null)	{
			icon = new StringOverlayIcon(item.getNodeType());
			cache.put(key, icon);
		}

		icon.setOriginal(original);

		return icon;
	}

	/** Overridden to return a disabled icon appropriate for the rendered XML node. */
	public Icon getDisabledIcon() {
		Integer key = Integer.valueOf(item.getNodeType());
		Icon icon = (Icon) disabledCache.get(key);
		if (icon == null)	{
			icon = getIcon();
			Image img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics g = img.getGraphics();		
			icon.paintIcon(this, g, 0, 0);
			icon = new ImageIcon(GrayFilter.createDisabledImage(img));
			disabledCache.put(key, icon);
		}
		return icon;
	}



	// Icon class that paints a XML-Tag shortcut on the original icon
	private class StringOverlayIcon implements Icon
	{
		private Icon original;
		private String symbol;

		StringOverlayIcon(short nodeType)	{
			this.symbol =
					nodeType == Node.TEXT_NODE ? "T"
				:	nodeType == Node.ELEMENT_NODE ? "E"
				:	nodeType == Node.COMMENT_NODE ? "C"
				:	nodeType == Node.CDATA_SECTION_NODE ? "CD"
				:	nodeType == Node.DOCUMENT_NODE ? "D"
				:	nodeType == Node.DOCUMENT_FRAGMENT_NODE ? "DF"
				:	nodeType == Node.ENTITY_NODE ? "EN"
				:	nodeType == Node.ENTITY_REFERENCE_NODE ? "ER"
				:	nodeType == Node.NOTATION_NODE ? "N"
				:	nodeType == Node.PROCESSING_INSTRUCTION_NODE ? "PI"
				:	nodeType == Node.ATTRIBUTE_NODE ? "A"
				:	nodeType == Node.DOCUMENT_TYPE_NODE ? "DT"
				:	"?";
		}

		void setOriginal(Icon original)	{
			this.original = original;
		}

		public void paintIcon(Component comp, Graphics g, int x, int y)	{
			original.paintIcon(comp, g, x, y);

			Color c = g.getColor();
			Font f = g.getFont();
			Font f1 = f.deriveFont(Font.BOLD, 8);	// 7 grad noch lesbar ...
			g.setFont(f1);
			g.setColor(Color.gray);
			FontMetrics fm = comp.getFontMetrics(f1);
			int w = fm.stringWidth(symbol);
			int h = fm.getAscent();

			g.drawString(symbol, x + getIconWidth() / 2 - w / 2, y + getIconHeight() / 2 + h / 2);

			g.setColor(c);
			g.setFont(f);
		}

		public int getIconWidth()	{
			return original.getIconWidth();
		}

		public int getIconHeight()	{
			return original.getIconHeight();
		}

	}

}
