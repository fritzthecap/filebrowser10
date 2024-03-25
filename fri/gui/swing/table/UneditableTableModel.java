package fri.gui.swing.table;

import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class UneditableTableModel extends DefaultTableModel
{
	public UneditableTableModel()	{
		super();
	}
	
	public UneditableTableModel(Vector rows, Vector columns)	{
		super(rows, columns);
	}
	
	public UneditableTableModel(Object [][] rows, Object [] columns)	{
		super(rows, columns);
	}
	
	public boolean isCellEditable(int row, int col)	{	// not editable at all
		return false;
	}

	public Class getColumnClass(int col)	{	// show data types correctly
		return getValueAt(0, col).getClass();
	}

}
