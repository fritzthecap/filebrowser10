package fri.gui.swing.hexeditor;

import java.awt.Point;
import javax.swing.JTable;
import javax.swing.DefaultListSelectionModel;

/**
	To simulate a textarea-like caret, this SelectionModel clears selection and
	leaves a focus border on selected cell when user clicks on a single selected
	cell for the second time. Even when the cell is selected, the caret remains
	to point to that cell, but by <i>table.isCellSelected()</i> one can find out
	if it is really selected. When it is not selected, a paste action inserts
	before the caret cell and does not replace it by clipboard contents.
*/

public class HexTableSelectionModel extends DefaultListSelectionModel
{
	private int lastRow = -2;
	private int lastColumn = -2;
	private Point caretCell;
	private JTable table;

	
	public HexTableSelectionModel(JTable table)	{
		this.table = table;
	}


	/** Overridden to clear selection at second click on a cell. */
	public void setSelectionInterval(int index0, int index1)	{
		//System.err.println("setSelectionInterval "+index0+", "+index1);
		if (index0 == index1 && index0 == lastRow && table.getSelectedColumn() == lastColumn)	{
			super.clearSelection();
			table.getColumnModel().getSelectionModel().clearSelection();
			
			lastRow = -2;
			lastColumn = -2;
		}
		else	{
			super.setSelectionInterval(index0, index1);
			
			if (index0 == index1 && table.getSelectedColumnCount() == 1)	{
				lastRow = index0;
				lastColumn = table.getSelectedColumn();
			}
		}
		
		setCaretCell(new Point(getLeadSelectionIndex(), table.getSelectedColumn()));
	}

	
	/** Returns caret cell (is selected or just focused). */
	public Point getCaretCell()	{
		return caretCell;
	}

	private void setCaretCell(Point p)	{
		if (p.x >= 0 && p.y >= 0)
			caretCell = p;
	}
	
	/** Returns the upper end of selection (inclusive). */
	public Point getSelectionStart()	{
		int minRow = getMinSelectionIndex();
		int columns = table.getColumnCount();
		for (int j = 0; minRow >= 0 && j < columns; j++)	{
			if (table.isCellSelected(minRow, j))
				return new Point(minRow, j);
		}
		return null;
	}
	
	/** Returns the lower end of selection (inclusive). */
	public Point getSelectionEnd()	{
		int maxRow = getMaxSelectionIndex();
		int columns = table.getColumnCount();
		for (int j = columns - 1; maxRow >= 0 && j >= 0; j--)	{
			if (table.isCellSelected(maxRow, j))
				return new Point(maxRow, j);
		}
		return null;
	}
	
}
