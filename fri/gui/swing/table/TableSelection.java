package fri.gui.swing.table;

import javax.swing.table.*;
import javax.swing.*;
import java.util.Vector;

/**
	Helpers to request a JTable's selection.
*/

public class TableSelection
{
	protected JTable table;

	/**
		Create a selection requester for a treetable
	*/
	public TableSelection(JTable table)	{
		this.table = table;
	}
	
	/**
		Returns a list of all selected table lines as Vector of Vectors.
	*/
	public Vector getAllSelectedRows()	{
		ListSelectionModel sm = table.getSelectionModel();
		int min = sm.getMinSelectionIndex();
		int max = sm.getMaxSelectionIndex();

		Vector v = new Vector();

		for (int i = min; min != -1 && max != -1 && i <= max; i++)	{
			if (sm.isSelectedIndex(i))	{
				Object o = getRow(i);
				if (o != null)
					v.addElement(o);
			}
		}
		return v.size() > 0 ? v : null;
	}

	/**
		Returns the given row as Vector.
		This Vector is taken from JTable data-Vector if table has a DefaultTableModel,
		else it is built with a new Vector, filled by <i>table.getValueAt()</i>.
	*/
	public Object getRow(int row)	{
		return getRow(row, table.getModel());
	}
	
	protected Object getRow(int row, TableModel m)	{
		if (m instanceof DefaultTableModel)	{
			DefaultTableModel dm = (DefaultTableModel)m;
			Vector dataVector = dm.getDataVector();
			return dataVector != null && dataVector.size() > row && row >= 0 ? dataVector.get(row) : null;
		}

		Vector v = new Vector(table.getColumnCount());
		for (int i = 0; i < table.getColumnCount(); i++)	{
			v.add(table.getValueAt(row, i));
		}
		return v;
	}
	
}
