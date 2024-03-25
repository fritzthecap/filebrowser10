package fri.gui.swing.mailbrowser.addressbook;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import fri.gui.mvc.view.Selection;
import fri.gui.mvc.view.swing.TableSelectionDnd;
import fri.gui.swing.table.*;

public class AddressTable extends JPanel
{
	private JTable table;
	private Selection selection;
	
	public AddressTable()	{
		super(new BorderLayout());
		
		TableModel model = new AddressTableModel();
		table = new JTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		table.setDefaultRenderer(String.class, new AddressTableCellRenderer());

		if (PersistentColumnsTable.load(getSensorComponent(), AddressTable.class) == false)	{	// nothing persistent
			TableColumnModel cm = table.getColumnModel();
			TableColumn c;
			c = cm.getColumn(AddressTableModel.PERSON_COLUMN);
			c.setPreferredWidth(60);
			c = cm.getColumn(AddressTableModel.EMAIL_COLUMN);
			c.setPreferredWidth(80);
			c = cm.getColumn(AddressTableModel.PHONE_COLUMN);
			c.setPreferredWidth(50);
			c = cm.getColumn(AddressTableModel.ADDRESS_COLUMN);
			c.setPreferredWidth(100);
		}
		
		add(new JScrollPane(table), BorderLayout.CENTER);
	}


	public JTable getSensorComponent()	{
		return table;
	}
	
	public Selection getSelection()	{
		if (selection == null)
			selection = new TableSelectionDnd(table);
		return selection;
	}


	/** Returns the model for message requests. */
	public AddressTableModel getModel()	{
		return (AddressTableModel)table.getModel();
	}
	
	public void close()	{
		PersistentColumnsTable.store(getSensorComponent(), AddressTable.class);
		getModel().save();
	}



	private class AddressTableCellRenderer extends DefaultTableCellRenderer
	{
		public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean selected,
			boolean focus,
			int row,
			int col)
		{
			super.getTableCellRendererComponent(table, value, selected, focus, row, col);

			AddressTableRow msgRow = (AddressTableRow) ((TableSelectionDnd)getSelection()).getRow(row);
			setEnabled(msgRow.isMovePending() == false);

			return this;
		}
	}

}