package fri.gui.swing.hexeditor;

import java.awt.Point;
import javax.swing.table.*;
import fri.gui.mvc.model.*;
import fri.gui.mvc.controller.CommandArguments;

/**
	The 16-byte row layout of the table (settable), column header strings and additional
	address column management. Offers methods to insert, update and delete bytes.
*/

public class HexTableModel extends AbstractTableModel implements
	MutableModel
{
	/** The number of the address column, for setting preferred width. */
	public final static int ADDRESS_COLUMN = 0;	// where the ADDRESS_COLUMN is
	private final static int ADDITIONAL_LEADING_COLUMNS = 1;	// how many columns the ADDRESS_COLUMN takes
	private final static String ADDRESS_COLUMN_NAME = "Address";	// name of the ADDRESS_COLUMN
	private static int columnCount = Config.getColumnCount();
	private static String [] columns;
	private byte [] bytes = new byte [] { (byte)0 };	// This is the default model for new hexfiles: at least one byte
	
	
	/** Create a HexTableModel (do-nothing constructor). */
	public HexTableModel()	{
	}


	/** Set data into the empty model. This is only for initial model loading. Must not be null. */
	public void setBytes(byte [] bytes)	{
		if (bytes == null)
			throw new IllegalArgumentException("Null is not an allowed value for setBytes: "+bytes);
		this.bytes = bytes;
	}
	
	/** Returns data from the model. This is only for storing to disk. */
	public byte [] getBytes()	{
		return bytes;
	}
	
	
	/** Sets a new column count to table model. Columns get allocated newly. */
	public void setColumnCount(int columnCount)	{
		HexTableModel.columnCount = columnCount;
		HexTableModel.columns = null;
	}
  
	
	// TableModel implementation
	
	public int getRowCount()	{
		int cc = getHexColumnCount();
		return bytes.length / cc + (bytes.length % cc > 0 ? 1 : 0);
	}
	
	public int getColumnCount()	{
		return ensureColumns().length;
	}
  
	public String getColumnName(int column)	{
		return ensureColumns()[column];
	}
	
	public Class getColumnClass(int column)	{
		return ensureColumns()[column].getClass();
	}
	
	private String [] ensureColumns()	{
		if (HexTableModel.columns == null)	{
			HexTableModel.columns = new String[HexTableModel.columnCount + ADDITIONAL_LEADING_COLUMNS];
			for (int i = 0; i < HexTableModel.columns.length; i++)	{
				if (i < ADDITIONAL_LEADING_COLUMNS)
					if (i == ADDRESS_COLUMN)
						HexTableModel.columns[i] = ADDRESS_COLUMN_NAME;
					else
						throw new IllegalArgumentException("No other additional column than Address implemented!");
				else
					HexTableModel.columns[i] = Integer.toHexString(i - ADDITIONAL_LEADING_COLUMNS).toUpperCase();
			}
		}
		return HexTableModel.columns;
	}
	
	/** Returns true if position is not a address cell and is not behind last byte. */
	public boolean isCellEditable(int row, int column)	{
		return
			column >= ADDITIONAL_LEADING_COLUMNS &&
			column < columns.length &&
			pointToPosition(row, column) >= 0;
	}

	/**
		Sets the passed Byte value to the specified cell.
	*/
	public void setValueAt(Object value, int row, int column)	{
		int pos = pointToPosition(row, column);
		bytes[pos] = ((Byte)value).byteValue();
		
		fireTableCellUpdated(row, column);
	}

	/** Returns the Byte of the specified cell, or an address String for the address column. */
	public Object getValueAt(int row, int column)	{
		if (column < ADDITIONAL_LEADING_COLUMNS)	{
			if (column == ADDRESS_COLUMN)	{
				return Integer.toHexString(row * getHexColumnCount()).toUpperCase();
			}
			throw new IllegalArgumentException("No other leading column implemented, no value for column "+column);
		}
		else	{
			int pos = pointToPosition(row, column);
			if (pos < 0)
				return null;

			return new Byte(bytes[pos]);
		}
	}


	public boolean isHexByteColumn(int column)	{
		return column >= ADDITIONAL_LEADING_COLUMNS && column < getColumnCount();
	}
	
	private int getHexColumnCount()	{
		return getColumnCount() - ADDITIONAL_LEADING_COLUMNS;
	}


	public int pointToPosition(int row, int column)	{
		if (row < 0 || isHexByteColumn(column) == false)
			return -1;
		int offset = row * getHexColumnCount() + column - ADDITIONAL_LEADING_COLUMNS;
		if (offset >= bytes.length)
			return -1;
		return offset;
	}

	public Point positionToPoint(int pos)	{
		int cc = getHexColumnCount();
		int x = pos / cc;
		int y = pos % cc + ADDITIONAL_LEADING_COLUMNS;
		return new Point(x, y);
	}



	// Model implementation

	/** Implements Model. Returns CommandArguments that hold this Model. Offsets are contained in ControllerModelItem. */
	public CommandArguments getModelItemContext(ModelItem item)	{
		ControllerModelItem cmi = (ControllerModelItem)item;
		return new ByteCommandArguments(this, cmi.getStart());
	}


	// MutableModel implementation

	/**
		Implements MutableModel. Insert a new item which is an array of bytes.
		@param item start offset where to insert, and the bytes to insert
		@param position is null, according to <i>ControlerModelItem.doInsert()</i>
		@return inserted item (is passed ModelItem), or null if action failed.
	*/
	public ModelItem doInsert(ModelItem item, CommandArguments position)	{
		ControllerModelItem cmi = (ControllerModelItem)item;
		
		byte [] insertBytes = cmi.getBytesToInsert();
		int pos = Math.max(0, cmi.getStart());
		byte [] newBytes = new byte[getBytes().length + insertBytes.length];
		
		System.err.println("arraycopy of "+getBytes().length+" bytes to "+newBytes.length+" bytes with length "+pos+", bytes to insert length is "+insertBytes.length);
		System.arraycopy(getBytes(),  0,   newBytes, 0,                        pos);
		System.arraycopy(insertBytes, 0,   newBytes, pos,                      insertBytes.length);
		System.arraycopy(getBytes(),  pos, newBytes, pos + insertBytes.length, getBytes().length - pos);
		this.bytes = newBytes;
	
		Point p = positionToPoint(pos);
		fireTableRowsInserted(p.x, p.x);
		
		return item;
	}

	/**
		Implements MutableModel. Delete an item which is an array of bytes.
		@param item the item to delete from this model.
		@return true if delete succeeded.
	*/
	public boolean doDelete(ModelItem item)	{
		ControllerModelItem cmi = (ControllerModelItem)item;
		
		int start = cmi.getStart();
		int end = cmi.getEnd();
		int toDelete = end - start;
		byte [] newBytes = new byte[getBytes().length - toDelete];

		System.arraycopy(getBytes(), 0,   newBytes, 0,     start);
		System.arraycopy(getBytes(), end, newBytes, start, getBytes().length - end);
		this.bytes = newBytes;

		Point pStart = positionToPoint(start);
		Point pEnd   = positionToPoint(end);
		fireTableRowsDeleted(pStart.x, pEnd.x);
		
		return true;
	}

}
