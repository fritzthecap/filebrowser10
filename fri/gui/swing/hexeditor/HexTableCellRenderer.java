package fri.gui.swing.hexeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class HexTableCellRenderer extends DefaultTableCellRenderer
{
	public static final int horizontalAlignment = CENTER;
	private JLabel addr;
	private Border movePendingBorder;
	
	public HexTableCellRenderer()	{
		setHorizontalAlignment(horizontalAlignment);
	}

	public Component getTableCellRendererComponent(
		JTable table,
		Object value,
		boolean isSelected,
		boolean hasFocus,
		int row,
		int column)
	{
		boolean hasFoc = hasFocus;
		hasFocus = false;	// select even the (editable) focus cell
		
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		if (addr == null)	{	// allocate extra label to render address column
			addr = new JLabel();
			addr.setForeground(table.getTableHeader().getForeground());
			addr.setBackground(table.getTableHeader().getBackground());
			addr.setFont(table.getTableHeader().getFont());
			addr.setBorder(table.getTableHeader().getBorder());
			addr.setHorizontalAlignment(RIGHT);
			addr.setOpaque(true);
		}
		
		HexTableModel m = (HexTableModel)table.getModel();

		if (m.isHexByteColumn(column) == false)	{	// is the address column
			addr.setText(getText());
			return addr;
		}
		else	{
			// manage cutten cells
			HexTable t = (HexTable)table;
			boolean movePending = t.isMovePending(row, column);
			setEnabled(movePending == false);

			if (hasFoc)	{	// now add the focus border that was not done in super
				setBorder(UIManager.getBorder("Table.focusCellHighlightBorder") );
			}
			else
			if (movePending)	{
				if (movePendingBorder == null)
					movePendingBorder = BorderFactory.createLineBorder(Color.lightGray);
				setBorder(movePendingBorder);
			}
			else	{
				setBorder(null);
			}
		}
		
		return this;
	}

}
