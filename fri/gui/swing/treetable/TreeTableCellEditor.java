package fri.gui.swing.treetable;

import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.awt.Component;
import java.awt.Rectangle;
import javax.swing.*;
import javax.swing.tree.*;
import java.util.EventObject;

/**
 * An editor that can be used to edit the tree column. This extends
 * DefaultCellEditor and uses a JTextField (actually, TreeTableTextField)
 * to perform the actual editing.
 * <p>To support editing of the tree column we can not make the tree
 * editable. The reason this doesn't work is that you can not use
 * the same component for editing and renderering. The table may have
 * the need to paint cells, while a cell is being edited. If the same
 * component were used for the rendering and editing the component would
 * be moved around, and the contents would change. When editing, this
 * is undesirable, the contents of the text field must stay the same,
 * including the caret blinking, and selections persisting. For this
 * reason the editing is done via a TableCellEditor.
 * <p>Another interesting thing to be aware of is how tree positions
 * its render and editor. The render/editor is responsible for drawing the
 * icon indicating the type of node (leaf, branch...). The tree is
 * responsible for drawing any other indicators, perhaps an additional
 * +/- sign, or lines connecting the various nodes. So, the renderer
 * is positioned based on depth. On the other hand, table always makes
 * its editor fill the contents of the cell. To get the allusion
 * that the table cell editor is part of the tree, we don't want the
 * table cell editor to fill the cell bounds. We want it to be placed
 * in the same manner as tree places it editor, and have table message
 * the tree to paint any decorations the tree wants. Then, we would
 * only have to worry about the editing part. The approach taken
 * here is to determine where tree would place the editor, and to override
 * the <code>reshape</code> method in the JTextField component to
 * nudge the textfield to the location tree would place it. Since
 * JTreeTable will paint the tree behind the editor everything should
 * just work. So, that is what we are doing here. Determining of
 * the icon position will only work if the TreeCellRenderer is
 * an instance of DefaultTreeCellRenderer. If you need custom
 * TreeCellRenderers, that don't descend from DefaultTreeCellRenderer, 
 * and you want to support editing in JTreeTable, you will have
 * to do something similiar.
 */

public class TreeTableCellEditor extends DefaultCellEditor
{
	private JTreeTable treetable;
	private static final int CLICK_COUNT_TO_START = 2;
	
	
	/**
		Create a treetable cell editor using a JTextfield.
		@param treetable the treetabel hosting this renderer.
	*/
	public TreeTableCellEditor(JTreeTable treetable) {
		super(new TreeTableTextField());

		this.treetable = treetable;

		setClickCountToStart(CLICK_COUNT_TO_START);
	}

	/**
	 * Overriden to determine an offset that tree would place the
	 * editor at. The offset is determined from the
	 * <code>getRowBounds</code> JTree method, and additionaly
	 * from the icon DefaultTreeCellRenderer will use.
	 * <p>The offset is then set on the TreeTableTextField component
	 * created in the constructor, and returned.
	 */
	public Component getTableCellEditorComponent(
		JTable table,
		Object value,
		boolean isSelected,
		int r,
		int c)
	{
		Component component = super.getTableCellEditorComponent(table, value, isSelected, r, c);
		
		JTree t = treetable.getTree();
		int offsetRow = r;	//t.isRootVisible() ? r : r - 1;	// geht sonst nicht bei false!!!
		Rectangle bounds = t.getRowBounds(offsetRow);
		int offset = bounds.x + getTreeTableColumnOffset();
		//System.err.println("editing treetable row "+offsetRow+", x="+offset);
		
		TreeCellRenderer tcr = t.getCellRenderer();
		
		if (tcr instanceof DefaultTreeCellRenderer) {
			DefaultTreeCellRenderer dtcr = (DefaultTreeCellRenderer)tcr;
			Object node = t.getPathForRow(offsetRow).getLastPathComponent();
			Icon icon;
			if (t.getModel().isLeaf(node))
				icon = dtcr.getLeafIcon();
			else
			if (t.isExpanded(offsetRow))
				icon = dtcr.getOpenIcon();
			else
				icon = dtcr.getClosedIcon();

			if (icon != null) {
				offset += ((DefaultTreeCellRenderer)tcr).getIconTextGap() + icon.getIconWidth();
				//System.err.println("offset of tree icon is: "+offset);
			}
		}
		
		((TreeTableTextField)getComponent()).offset = offset;
		
		return component;
	}

	/**
	 * This is overriden to forward the event to the tree. This will
	 * return true if the click count >= 3, or the event is null.
	 */
	public boolean isCellEditable(EventObject e) {
		if (e instanceof MouseEvent) {
			MouseEvent me = (MouseEvent)e;

			int tfOffs = ((TreeTableTextField)getComponent()).offset;

			// if start editing, do not forward event to tree
			if (me.getClickCount() >= getClickCountToStart() && tfOffs <= me.getX())	{
				return true;
			}

			// If the modifiers are not 0 (or the left mouse button),
			// tree may try and toggle the selection, and table
			// will then try and toggle, resulting in the
			// selection remaining the same. To avoid this, we
			// only dispatch when the modifiers are 0 (or the left mouse
			// button).
			if (me.getModifiers() == 0 || me.getModifiers() == InputEvent.BUTTON1_MASK) {
				int relOffs = me.getX() - getTreeTableColumnOffset();

				MouseEvent newME = new MouseEvent(
						treetable.getTree(),
						me.getID(),
						me.getWhen(),
						me.getModifiers(),
						relOffs,
						me.getY(),
						me.getClickCount(),
						me.isPopupTrigger());
				treetable.getTree().dispatchEvent(newME);
			}
		}
		else
		if (e == null)	{	// not mouse event
			return true;	// F2 pressed
		}

		return false;
	}


	private int getTreeTableColumnOffset()	{
		for (int i = treetable.getColumnCount() - 1; i >= 0; i--) {
			if (treetable.getColumnClass(i) == TreeTableModel.class) {
				return treetable.getCellRect(0, i, true).x;
			}
		}
		return -1;
	}

	/**
	 * Component used by TreeTableCellEditor. The only thing this does
	 * is to override the <code>reshape</code> method, and to ALWAYS
	 * make the x location be <code>offset</code>.
	 */
	static class TreeTableTextField extends JTextField {
		public int offset;
	
		public void reshape(int x, int y, int w, int h) {
			int newX = Math.max(x, offset);
			super.reshape(newX, y, w - (newX - x), h);
		}
	}

}
