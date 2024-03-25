package fri.gui.swing.filebrowser;

import javax.swing.table.*;

/**
	Responsibilities:
		enable column class rendering,
		get columns from data,
		decide which column is editable.
*/

public class ZipInfoTableModel extends DefaultTableModel
{
	public ZipInfoTableModel(FileTableData data)	{
		super(data, data.getColumns());
	}
	
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	public boolean isCellEditable(int row, int col)	{
		return false;
	}
	
}