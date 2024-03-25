package fri.gui.mvc.view.swing;

import java.awt.Point;
import javax.swing.JTable;
import fri.gui.mvc.view.MultipleSelection;
import fri.gui.mvc.view.swing.SelectionDnd;
import fri.gui.swing.table.TableSelection;

/**
	JTable selection that provides do-nothing methods for adding some
	selected table items (as the type of model items can not be predicted).
	It returns Vector of Vectors from <i>getSelectedObject()</i>.
	In the case of DefaultTableModel the contained rows are identical
	with TableModel rows, else they are Vectors created on the fly by calling
	<i>getValueAt()</i> for each selected row and every column.
	
	@author Fritz Ritzberger
*/
public class TableSelectionDnd extends TableSelection implements
	SelectionDnd,
	MultipleSelection
{
	public TableSelectionDnd(JTable table)	{
		super(table);
	}


	/** Get selection from the view. This returns null if nothing is selected. */
	public Object getSelectedObject()	{
		return getAllSelectedRows();
	}
	
	/**
		Set selection in the view.
		This method does nothing. Override to set selection from the passed Object.
	*/
	public void setSelectedObject(Object o)	{
	}

	/**
		Add a selection to current selected items of view.
		This method does nothing. Override to add selection from the passed Object.
	*/
	public void addSelectedObject(Object o)	{
	}


	/** Clear selection of the view. */
	public void clearSelection()	{
		table.getSelectionModel().clearSelection();
	}

	/**
		Get a item from a Point in the view. This return no list of rows but a TableModel row.
		Override if a TableSorter creates view row numbers different from model row numbers.
	*/
	public Object getObjectFromPoint(Point p)	{
		int i = table.rowAtPoint(p);
		Object item = getRow(i);
		return item;
	}

}
