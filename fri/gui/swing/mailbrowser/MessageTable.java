package fri.gui.swing.mailbrowser;

import java.awt.*;
import java.util.List;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.mail.*;
import fri.util.mail.*;
import fri.gui.CursorUtil;
import fri.gui.mvc.view.Selection;
import fri.gui.mvc.view.swing.TableSelectionDnd;
import fri.gui.swing.table.*;
import fri.gui.swing.table.header.*;
import fri.gui.swing.table.sorter.*;

/**
	Panel that holds the message table and constructs its filter header.
*/

public class MessageTable extends JPanel implements
	ListSelectionListener	// to fill status panel
{
	private Selection selection;
	private JTable table;
	private JLabel status;
	private int sortColumn = MessageTableModel.SENT_DATE_COLUMN;
	private boolean sortOrder = false;

	
	/** Create a table panel, allocate filter/sort header and status label. Listen to selection. */
	public MessageTable()	{
		super(new BorderLayout());
		
		MessageTableModel model = new MessageTableModel();
		TableSorter sorter = new TableSorter(model);
		table = new JTable(sorter);
		table.setDefaultRenderer(String.class, new MessageTableCellRenderer());
		
		FilterSortExpandHeaderEditor.setTableHeader(table, new FilterSortExpandListener()	{
			public void headerChanged(HeaderValueStruct v, int columnIndex)	{
				CursorUtil.setWaitCursor(MessageTable.this);
				try	{
					if (v.getChanged() == HeaderValueStruct.CHANGED_SORT)	{
						MessageTable.this.sortColumn = columnIndex;
						MessageTable.this.sortOrder = v.getSort() != HeaderValueStruct.SORT_DESC;
						if (sortColumn == MessageTableModel.SENT_DATE_COLUMN)
							MessageTable.this.sortOrder = ! MessageTable.this.sortOrder;	// as date is String and should be sorted reverse at start
						MessageTable.this.getSorter().sortByColumn(MessageTable.this.sortColumn, MessageTable.this.sortOrder);
					}
					else
					if (v.getChanged() == HeaderValueStruct.CHANGED_FILTER)	{
						String filter = v.getFilter().trim();
						if (MessageTable.this.getModel().filterChanged(filter, columnIndex))	{
							MessageTable.this.getModel().setFilter(filter, columnIndex);
							MessageTable.this.getSorter().sortByColumn(MessageTable.this.sortColumn, MessageTable.this.sortOrder);
							MessageTable.this.valueChanged(null);
						}
					}
				}
				finally	{
					CursorUtil.resetWaitCursor(MessageTable.this);
				}
			}
		});
		
		if (PersistentColumnsTable.load(getSensorComponent(), MessageTableModel.class) == false)	{	// nothing persistent
			TableColumnModel cm = table.getColumnModel();
			TableColumn c;
			c = cm.getColumn(MessageTableModel.SUBJECT_COLUMN);
			c.setPreferredWidth(160);
			c = cm.getColumn(MessageTableModel.FROM_COLUMN);
			c.setPreferredWidth(80);
			c = cm.getColumn(MessageTableModel.SENT_DATE_COLUMN);
			c.setPreferredWidth(110);
			c = cm.getColumn(MessageTableModel.TEXT_COLUMN);	// Make small text column
			c.setPreferredWidth(10);
		}

		add(new JScrollPane(table), BorderLayout.CENTER);
		
		status = new JLabel(" ");
		add(status, BorderLayout.SOUTH);
		
		table.getSelectionModel().addListSelectionListener(this);
	}


	/** Install popup menu on sensor component. */
	public JTable getSensorComponent()	{
		return table;
	}

	/** Pass the selection to controller. */
	public Selection getSelection()	{
		if (selection == null)
			selection = new MessageSelection(table);
		return selection;
	}

	/** Returns the model for message requests. */
	public MessageTableModel getModel()	{
		return (MessageTableModel)getSorter().getModel();
	}
	
	private TableSorter getSorter()	{
		return (TableSorter)table.getModel();
	}
	
	
	private MessageTableRow getMessageTableViewRow(int visualRow)	{
		int modelRow = getSorter().convertRowToModel(visualRow);
		return getModel().getMessageTableRow(modelRow);
	}

	
	public void clear()	{
		getModel().clear();	// clear current table
		status.setText(" ");
	}
	
	
	/** Called by the FolderController when all messages were loaded into table. Shows sum of messages and sorts by date. */
	public void finishedMessageLoading(boolean reSort)	{
		if (reSort)
			getSorter().sortByColumn(sortColumn, sortOrder);
		
		valueChanged(null);	// render message count in status bar
		
		// change column header label for "sent" folder
		TableColumn col = getSensorComponent().getColumnModel().getColumn(MessageTableModel.FROM_COLUMN);
		HeaderValueStruct hv = (HeaderValueStruct)col.getHeaderValue();
		if (getModel().rendersToAdress())
			hv.setName(Language.get("To"));
		else
			hv.setName(Language.get("From"));
	}
	
	public void finishedNewMessageLoading(int newCount)	{
		status.setText(""+newCount+" "+Language.get("New_Messages"));
	}
	
	/** Implements ListSelectionListener to fill status label with selection information. */
	public void valueChanged(ListSelectionEvent e)	{
		if (e != null && e.getValueIsAdjusting())
			return;
		
		List sel = (List)getSelection().getSelectedObject();
		int cnt = sel != null ? sel.size() : 0;
		
		if (cnt > 0)
			status.setText(""+cnt+" "+Language.get("Selected_Of")+" "+getModel().getRowCount()+" "+Language.get("Messages"));
		else
			status.setText(""+getModel().getRowCount()+" "+Language.get("Messages"));
	}
	
	
	/* Store the column widths. */
	public void close()	{
		PersistentColumnsTable.store(getSensorComponent(), MessageTableModel.class);
	}





	/** Overrides <i>getRow()</i> method to convert view row to model row. */
	public static class MessageSelection extends TableSelectionDnd
	{
		MessageSelection(JTable table)	{
			super(table);
		}

		public Object getRow(int viewRow)	{
			TableSorter sorter = (TableSorter)MessageSelection.this.table.getModel();
			int modelRow = sorter.convertRowToModel(viewRow);
			return super.getRow(modelRow, sorter.getModel());
		}
	}




	private class MessageTableCellRenderer extends DefaultTableCellRenderer
	{
		private Font font, boldFont;
		private Color color, selectedColor;
		private JTable table;
		
		public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean selected,
			boolean focus,
			int row,
			int col)
		{
			this.table = table;
			Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, col);
			
			if (font == null)	{
				font = c.getFont();
				boldFont = font.deriveFont(Font.BOLD);
			}
			
			MessageTableRow msgRow = getMessageTableViewRow(row);
			setEnabled(msgRow.isMovePending() == false);
			
			Message msg = msgRow.getMessage();
			try	{
				if (MessageUtil.isNewMessage(msg))
					c.setFont(boldFont);
				else
					c.setFont(font);
			}
			catch (MessagingException e)	{
				e.printStackTrace();
			}
			
			if (selected == false && color == null)
				color = c.getBackground();
			if (selected == true && selectedColor == null)
				selectedColor = c.getBackground();
				
			if (selected)
				setBackground(selectedColor);
			else
			if (table.convertColumnIndexToModel(col) == MessageTableModel.TEXT_COLUMN)
				setBackground(Color.lightGray);
			else
				setBackground(color);
			
			return c;
		}
		
		public String getToolTipText(MouseEvent e)	{
			int row = table.rowAtPoint(e.getPoint());
			MessageTableRow msgRow = getMessageTableViewRow(row);
			Message msg = msgRow.getMessage();
			
			try	{
				String s = "";

				if (msg.isSet(Flags.Flag.RECENT))
					s = Language.get("Recent");
				if (msg.isSet(Flags.Flag.SEEN))
					s = s+(s.length() > 0 ? ", " : "")+Language.get("Seen");
				if (msg.isSet(Flags.Flag.ANSWERED))
					s = s+(s.length() > 0 ? ", " : "")+Language.get("Answered");
				if (msg.isSet(Flags.Flag.DRAFT))
					s = s+(s.length() > 0 ? ", " : "")+Language.get("Draft");

				if (s.length() > 0)
					return s;
			}
			catch (MessagingException ex)	{
				ex.printStackTrace();
			}
			return super.getToolTipText(e);
		}

	}

}