package fri.gui.swing.treetable;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.EventObject;

/**
	Placeholder for the JTreeTable CellEditor when
	the tree column is not editable.
*/

public class PassiveCellEditor implements
	TableCellEditor
{
	protected EventListenerList listenerList = new EventListenerList();
	protected JTreeTable treetable;


	public PassiveCellEditor(JTreeTable treetable)	{
		this.treetable = treetable;
	}
		
	public Component getTableCellEditorComponent(
		JTable table,
		Object value,
		boolean isSelected,
		int row,
		int col)
	{
		return treetable.getTree();
	}
	
	public Object getCellEditorValue() {
		return null;
	}

	/**
	 * Overridden to return false, and if the event is a mouse event
	 * it is forwarded to the tree.<p>
	 * The behavior for this is debatable, and should really be offered
	 * as a property. By returning false, all keyboard actions are
	 * implemented in terms of the table. By returning true, the
	 * tree would get a chance to do something with the keyboard
	 * events. For the most part this is ok. But for certain keys,
	 * such as left/right, the tree will expand/collapse where as
	 * the table focus should really move to a different column. Page
	 * up/down should also be implemented in terms of the table.
	 * By returning false this also has the added benefit that clicking
	 * outside of the bounds of the tree node, but still in the tree
	 * column will select the row, whereas if this returned true
	 * that wouldn't be the case.
	 * <p>By returning false we are also enforcing the policy that
	 * the tree will never be editable (at least by a key sequence).
	 */
	public boolean isCellEditable(EventObject e) {
		if (e instanceof MouseEvent) {
			MouseEvent me = (MouseEvent)e;
			if (me.getModifiers() == 0 || me.getModifiers() == InputEvent.BUTTON1_MASK) {
				for (int counter = treetable.getColumnCount() - 1; counter >= 0; counter--) {
					if (treetable.getColumnClass(counter) == TreeTableModel.class) {
						MouseEvent	newME = new MouseEvent(
								treetable.getTree(),
								me.getID(),
								me.getWhen(),
								me.getModifiers(),
								me.getX() - treetable.getCellRect(0, counter, true).x, me.getY(),
								me.getClickCount(),
								me.isPopupTrigger());
						treetable.getTree().dispatchEvent(newME);
					}
				}
			}
		}
		return false;
	}


	public boolean shouldSelectCell(EventObject anEvent) {
		return false;
	}

	public boolean stopCellEditing() {
		return true;
	}

	public void cancelCellEditing() {
	}

	public void addCellEditorListener(CellEditorListener l) {
		listenerList.add(CellEditorListener.class, l);
	}

	public void removeCellEditorListener(CellEditorListener l) {
		listenerList.remove(CellEditorListener.class, l);
	}

	/*
	 * Notify all listeners that have registered interest for
	 * notification on this event type.
	 * @see EventListenerList
	 */
	protected void fireEditingStopped() {
		// Guaranteed to return a non-null array
		Object[]	listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == CellEditorListener.class) {
				((CellEditorListener) listeners[i + 1]).editingStopped(new ChangeEvent(this));
			}
		}
	}

	/*
	 * Notify all listeners that have registered interest for
	 * notification on this event type.
	 * @see EventListenerList
	 */
	protected void fireEditingCanceled() {
		// Guaranteed to return a non-null array
		Object[]	listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == CellEditorListener.class) {
				((CellEditorListener) listeners[i + 1]).editingCanceled(new ChangeEvent(this));
			}
		}
	}

}