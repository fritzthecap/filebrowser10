package fri.gui.swing.mailbrowser.rules.editor;

import java.awt.Component;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import fri.gui.swing.combo.*;

/**
	Render a List as JComboBox in JTable.
	Use a WideComboBox to diplay the strings in their full horizontal length.
	Set the selection and background color to that of JTable.
*/

public class ListTableCellEditor extends DefaultCellEditor
{
	private WideComboBox combo;
	private JTable table;

	public ListTableCellEditor()	{
		super(new WideComboBox());
		
		this.combo = (WideComboBox)editorComponent;
		this.combo.setRenderer(new ListTableComboRenderer());
		this.combo.setMaximumRowCount(25);
		
		setClickCountToStart(1);
	}
	
	public Component getTableCellEditorComponent(
		JTable table,
		Object value,
		boolean selected,
		int row,
		int column)
	{
		this.table = table;
		
		super.getTableCellEditorComponent(table, value, selected, row, column);
		System.err.println("getTableCellEditorComponent value is: "+value.getClass()+", "+value);

		Vector v = (Vector)value;
		combo.setModel(new DefaultComboBoxModel(v));

		combo.takePopupSize();
		combo.setBackground(table.getBackground());
		combo.setFont(table.getFont());
		
		return combo;
	}

	/** Overridden to return a reordered list. */
	public Object getCellEditorValue()	{
		DefaultComboBoxModel m = (DefaultComboBoxModel)combo.getModel();
		Vector v = new Vector(m.getSize());
		String s = (String)m.getSelectedItem();
		v.add(s);

		for (int i = 0; i < m.getSize(); i++)	{
			Object o = m.getElementAt(i);
			if (o.equals(s) == false)
				v.add(o);
		}
		System.err.println("ListTableCellEditor choice values are now: "+v);

		return v;
	}



	// A ComboBoxRenderer that gives the Popup-Selection the color of JTable-Selection.
	private class ListTableComboRenderer extends BasicComboBoxRenderer
	{
		public Component getListCellRendererComponent(
			JList list, 
			Object value,
			int index, 
			boolean isSelected, 
			boolean cellHasFocus)
		{
			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (isSelected) {
				setBackground(UIManager.getColor("Table.selectionBackground"));
				setForeground(UIManager.getColor("Table.selectionForeground"));
			}
			else {
				setBackground(table != null ? table.getBackground() : UIManager.getColor("Table.background"));
				setForeground(table != null ? table.getForeground() : UIManager.getColor("Table.foreground"));
			}
			return c;
		}
	
	}

}
