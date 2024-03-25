package fri.gui.swing.treetable;

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.border.Border;

/**
 * A TreeCellRenderer that is a JTree.
 */

public class TreeTableCellRenderer extends JTree implements
	TableCellRenderer
{
	/** Last table/tree row asked to renderer. */
	protected int visibleRow;
	protected JTreeTable treetable;
	/** Border to draw around the tree, if this is non-null, it will
	 * be painted. */
	protected Border highlightBorder;


	public TreeTableCellRenderer(
		JTreeTable treetable,
		TreeModel model,
		TreeCellRenderer treeRenderer)
	{
		super(model);
		this.treetable = treetable;

		if (treeRenderer != null)
			setCellRenderer(treeRenderer);
	}

	/** Set a new tree model to treetabel cell renderer */
	public void setModel(
		TreeModel model,
		TreeCellRenderer treeRenderer)
	{
		super.setModel(model);
		
		if (treeRenderer != null)
			setCellRenderer(treeRenderer);
	}


	/**
	 * TreeCellRenderer method. Overridden to update the visible row.
	 */
	public Component getTableCellRendererComponent(
		JTable table,
		Object value,
		boolean selected,
		boolean focus,
		int row,
		int col)
	{
		Color background;
		Color foreground;
		
		if(selected) {
			background = table.getSelectionBackground();
			foreground = table.getSelectionForeground();
		}
		else {
			background = table.getBackground();
			foreground = table.getForeground();
		}
		
		highlightBorder = null;
		
		if (treetable.realEditingRow() == row && table.getEditingColumn() == col) {
			background = UIManager.getColor("Table.focusCellBackground");
			foreground = UIManager.getColor("Table.focusCellForeground");
		}
		else
		if (focus) {
			highlightBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
			if (table.isCellEditable(row, col)) {
				background = UIManager.getColor("Table.focusCellBackground");
				foreground = UIManager.getColor("Table.focusCellForeground");
			}
		}
		
		visibleRow = row;
		
		setBackground(background);
		
		TreeCellRenderer tcr = getCellRenderer();
		if (tcr instanceof DefaultTreeCellRenderer) {
			DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer)tcr); 
			if (selected) {
				dtcr.setTextSelectionColor(foreground);
				dtcr.setBackgroundSelectionColor(background);
			}
			else {
				dtcr.setTextNonSelectionColor(foreground);
				dtcr.setBackgroundNonSelectionColor(background);
			}
		}
		
		return this;
	}


	/**
	 * Sets the row height of the tree, and forwards the row height to
	 * the table.
	 */
	public void setRowHeight(int rowHeight) {
		if (rowHeight > 0) {
			super.setRowHeight(rowHeight);
			if (treetable != null &&
					treetable.getRowHeight() != rowHeight) {
				treetable.setRowHeight(getRowHeight());
			}
		}
	}

	/**
	 * This is overridden to set the height to match that of the JTable.
	 */
	public void setBounds(int x, int y, int w, int h) {
		super.setBounds(x, 0, w, treetable.getHeight());
	}

	/**
	 * Sublcassed to translate the graphics such that the last visible
	 * row will be drawn at 0,0.
	 */
	public void paint(Graphics g) {
		g.translate(0, -visibleRow * treetable.getRowHeight());
		super.paint(g);
		// Draw the Table border if we have focus.
		if (highlightBorder != null) {
			highlightBorder.paintBorder(this,
					g,
					0, 
					visibleRow * getRowHeight(),
					getWidth(),
					getRowHeight());
		}
	}


	/**
	 * updateUI is overridden to set the colors of the Tree's renderer
	 * to match that of the table.
	 */
	public void updateUI() {
		//System.err.println("TreeTableCellRenderer.updateUI()");
		super.updateUI();
		// Make the tree's cell renderer use the table's cell selection
		// colors.
		TreeCellRenderer tcr = getCellRenderer();
		if (tcr instanceof DefaultTreeCellRenderer) {
			DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer)tcr;
			
			dtcr.setHorizontalAlignment(JLabel.LEFT);
			dtcr.setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
			dtcr.setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
			dtcr.setOpenIcon(UIManager.getIcon("Tree.openIcon"));
			dtcr.setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
			dtcr.setTextNonSelectionColor(UIManager.getColor("Tree.textForeground"));
			dtcr.setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
			dtcr.setBackgroundNonSelectionColor(UIManager.getColor("Tree.textBackground"));
			dtcr.setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor"));
	
			dtcr.setTextSelectionColor(UIManager.getColor("Table.selectionForeground"));
			dtcr.setBackgroundSelectionColor(UIManager.getColor("Table.selectionBackground"));
		}
	}

}