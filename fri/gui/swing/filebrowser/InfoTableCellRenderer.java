package fri.gui.swing.filebrowser;

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Date;
import fri.gui.swing.table.sorter.*;
import fri.util.NumberUtil;

/**
	helper class responsible for converting
	sort terms like size and time to a human readable form
*/
public class InfoTableCellRenderer extends DefaultTableCellRenderer
{
	private int column = 0, row = 0;
	private TableSorter sorter;
	private FileTableData data;
	
	public InfoTableCellRenderer(FileTableData data, TableSorter sorter)	{
		super();
		this.sorter = sorter;
		this.data = data;
	}
	
	public Component getTableCellRendererComponent(
		JTable table,
		Object value,
		boolean selected,
		boolean focus,
		int row,
		int column)
	{
		this.column = table.convertColumnIndexToModel(column);
		this.row = row;
		Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
		return c;
	}
	
	protected void setValue(Object value) {
		setIcon(null);
		if (column == data.getTimeColumn())	{
			long m = ((Long)value).longValue();
			setText(FileNode.dateFormater.format(new Date(m)));
		}
		else
		if (column == data.getSizeColumn())	{
			long l = ((Long)value).longValue();
			setText(NumberUtil.getFileSizeString(l));
		}
		else
		if (column == data.getTypeColumn())	{
			setText("");
			if (((String)value).toLowerCase().equals("folder"))
				setIcon(UIManager.getIcon("Tree.closedIcon"));
			else
			if (((String)value).toLowerCase().equals("file"))
				setIcon(UIManager.getIcon("Tree.leafIcon"));
			else
				super.setValue(value);
		}
		else	{
			super.setValue(value);
		}
		// enable /disable item
		try	{
			NetNode n = (NetNode)data.getObjectAt(sorter.convertRowToModel(row));
			setEnabled(!n.getMovePending());
		}
		catch (ClassCastException e)	{
		}
	}

}
	