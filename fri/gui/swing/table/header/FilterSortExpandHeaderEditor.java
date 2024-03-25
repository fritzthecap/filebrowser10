package fri.gui.swing.table.header;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import fri.gui.swing.combo.history.HistCombo;
import fri.gui.swing.button.NoInsetsButton;
import fri.gui.swing.ComponentUtil;

/**
 * The HeaderCellEditor for filterable table columns. It takes
 * two Icons in the constructor that define ascending and descending
 * sort orders. It calls the <CODE>header.callHeaderChanged()</CODE>
 * method when a new sort order or a new filter is entered. It expands and collapses
 * the TableColumn (without any support from outside).
 * <p>
 * This HeaderEditor works only together with an EditableHeader!
 *
 * @author  Fritz Ritzberger 2001
 */

public class FilterSortExpandHeaderEditor extends FilterSortExpandHeaderRenderer implements
	ActionListener
{
	private HistCombo filters;
	private JButton sortbutton;	// the above button with the sort icon
	private int column = -1;	// we need the column where we edit
	private JTable table;	// we need the table column model where we edit


	/**
	 * Create a HeaderCellEditor with all possible options.
	 */
	public FilterSortExpandHeaderEditor()	{
		this(defaultOptions);
	}
	
	/**
	 * Create a HeaderCellEditor with the passed options.
	 */
	public FilterSortExpandHeaderEditor(int options)	{
		this(options, null, null);
	}
	
	/**
	 * Create a HeaderCellEditor with the sort icons to be used.
	 */
	public FilterSortExpandHeaderEditor(Icon sortAsc, Icon sortDesc)	{
		this(defaultOptions, sortAsc, sortDesc);
	}
	
	/**
	 * Create a HeaderCellEditor with the options and sort icons to be used.
	 */
	public FilterSortExpandHeaderEditor(int options, Icon sortAsc, Icon sortDesc)	{
		super(options, sortAsc, sortDesc);

		buildEditor();

		setClickCountToStart(1);
	}
	


	protected void buildEditor()	{
		if (checkSortPanel != null)	{
			if ((options & SORT) == SORT)	{	// make a button instead of a label
				sortbutton = new NoInsetsButton();
				sortbutton.setHorizontalTextPosition(SwingConstants.LEFT);
				sortbutton.setBorderPainted(false);
				sortbutton.setFocusPainted(false);
				sortbutton.setToolTipText(label.getToolTipText());

				ComponentUtil.replaceComponent(checkSortPanel, label, sortbutton);

				sortbutton.addActionListener(this);
			}

			if (checkbox != null)
				checkbox.addActionListener(this);
		}

		if (filterPanel != null)	{
			filters = ((HistCombo)editorComponent);
			filters.setToolTipText(filterText.getToolTipText());
	
			ComponentUtil.replaceComponent(filterPanel, filterText, filters);
	
			if (checkSortPanel == null)	{
				// left out goFilter button
				if (checkbox == null)	{	// add now filter button as we are an editor, not a renderer
					filterPanel.add(goFilter, BorderLayout.WEST);
				}
				else	{	// arrange checkbox and goFilter button on a panel
					JPanel p = new JPanel(new BorderLayout());
					p.add(checkbox, BorderLayout.WEST);
					p.add(goFilter, BorderLayout.CENTER);
					filterPanel.add(p, BorderLayout.WEST);

					checkbox.addActionListener(this);
				}
			}

			goFilter.addActionListener(this);
		}
	}

	/**
	 * Implements TableCellEditor. Sets all arriving values into Components.
	 */
	public Component getTableCellEditorComponent(
		JTable table, Object value, boolean selected, int row, int column)
	{
		//System.err.println("getTableCellEditorComponent, receiving value "+(value != null ? value.getClass().toString() : ""));
		
		// set current value
		// save filter text to superclass filter button
		getTableCellRendererComponent(table, value, selected, true, row, column);

		this.column = column;
		this.table = table;

		if (sortbutton != null)	{
			sortbutton.setText(label.getText());
			sortbutton.setIcon(getSortIcon());
		}
		
		if (filters != null)	{
			filters.setDataVector(current.getFilters());
		}
		
		current.clearChanged();

		setDefaultCursor();	// avoid RESIZE_CURSOR

		return panel;
	}

	public void cancelCellEditing()	{
		//System.err.println("HeaderEditor cancelCellEditing() = "+filterText.getText());
		super.cancelCellEditing();
		
		if (filters != null)
			filters.setText(filterText.getText());
	}
	
	public boolean stopCellEditing()	{
		//System.err.println("HeaderEditor stopCellEditing()");
		if (filters != null)
			filters.commit();
			
		return super.stopCellEditing();
	}
	
	/**
	 * Implements TableCellEditor. Sets all new values to the HeaderValueStruct.
	 */
	public Object getCellEditorValue()	{
		//System.err.println("HeaderEditor actionPerformed");
		if (filters != null)	{
			filters.commit();
			current.setFilters(filters.getDataVector());
		}
		return current;
	}

	private void setDefaultCursor()	{
		// JTableHeaderUI sets RESIZE_CURSOR when mouse enters header editor.
		// All editor components except textfield would show RESIZE_CURSOR!
		// Reset to normal cursor every time the editor is allocated
		if (checkbox != null)
			checkbox.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if (filters != null)
			filters.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if (sortbutton != null)
			sortbutton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if (goFilter != null)
			goFilter.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	private EditableHeader getTableHeader()	{
		return (EditableHeader)table.getTableHeader();
	}

	/**
	 * Implements ActionListener. Callback for Checkbox, Filter-Button & TextField, Sort-Button.
	 */
	public void actionPerformed(ActionEvent e)	{
		//System.err.println("HeaderEditor actionPerformed");
		if (filters != null)	{
			filters.commit();
			filterText.setText(filters.getText());	// save current filter to button when performed
		}
		
		if (e.getSource() == goFilter)	{
			current.setFilters(filters.getDataVector());
			callHeaderChanged();
		}
		else
		if (e.getSource() == sortbutton)	{
			current.setSort(current.toggleSort());	// toggle sort state
			resetSortFlags();
			callHeaderChanged();
			sortbutton.setIcon(getSortIcon());
		}
		else
		if (e.getSource() == checkbox)	{
			// update the header cell data struct
			TableColumn c = table.getColumnModel().getColumn(column);
			current.setExpanded(checkbox.isSelected());

			if (current.getExpanded())	{
				c.setMaxWidth(Integer.MAX_VALUE);
				c.setPreferredWidth(current.getWidth() > 0 ? current.getWidth() : 100);
				if (goFilter != null)
					goFilter.setEnabled(true);
			}
			else	{
				int w = c.getPreferredWidth();
				c.setMinWidth(MIN_COLLAPSED_WIDTH);
				c.setMaxWidth(MAX_COLLAPSED_WIDTH);
				current.setWidth(w);	// store old size
				if (goFilter != null)
					goFilter.setEnabled(false);
			}
			callHeaderChanged();

			// we are editing the header, stop this
			EditableHeader h = (EditableHeader)table.getTableHeader();
			current.clearChanged();	// no change in column semantics!
			h.editingStopped(null);	// current will be delivered to some subclass implementation
		}
	}


	private void callHeaderChanged()	{
		getTableHeader().callHeaderChanged(current, column);
		current.clearChanged();
	}

	private void resetSortFlags()	{
		if (table == null)
			return;

		TableColumnModel m = table.getColumnModel();
		
		for (int i = 0; i < m.getColumnCount(); i++)	{
			TableColumn c = m.getColumn(i);
			HeaderValueStruct hvs = (HeaderValueStruct)c.getHeaderValue();
			if (current != hvs)
				hvs.setSort(HeaderValueStruct.SORT_UNDEFINED);
		}
	}


	/**
	 * Set an filterable, sortable, one-click-expandable
	 * column header to the passed JTable.
	 * This convenience method works only for preset Strings in column headers.
	 */
	public static void setTableHeader(
		JTable table, 
		FilterSortExpandListener lsnr)
	{
		setTableHeader(
			table, 
			lsnr,
			defaultOptions, 
			null, 
			null);
	}

	/**
	 * Set an filterable, sortable, one-click-expandable
	 * column header to the passed JTable.
	 * This convenience method works only for preset Strings in column headers.
	 */
	public static void setTableHeader(
		JTable table, 
		FilterSortExpandListener lsnr,
		int options)
	{
		setTableHeader(
			table, 
			lsnr,
			options, 
			null, 
			null);
	}

	/**
	 * Set an optionally filterable, sortable, one-click-expandable
	 * column header to the passed JTable.
	 * This convenience method works only for preset Strings in column headers.
	 * @table table to decorate with a new header
	 * @lsnr  header editor listener
	 * @options settings for the header: with filter, with sort icon, ...
	 * @sortAsc ascending sort icon
	 * @sortDesc descending sort icon
	 */
	public static void setTableHeader(
		final JTable jtable, 
		final FilterSortExpandListener lsnr,
		int options, 
		Icon sortAsc, 
		Icon sortDesc)
	{
		TableCellEditor editor = new FilterSortExpandHeaderEditor(options, sortAsc, sortDesc);
		TableCellRenderer renderer = new FilterSortExpandHeaderRenderer(options, sortAsc, sortDesc);

		jtable.setTableHeader(new EditableHeader(jtable, editor, renderer)	{
			protected void headerChanged(Object value, int columnIndex)	{
				if (lsnr != null)	{
					HeaderValueStruct v = (HeaderValueStruct)value;
					columnIndex = jtable.convertColumnIndexToModel(columnIndex);
					//System.err.println("headerChanged (unchanged=0, sort=1, filter=2): "+v.getChanged()+", column "+columnIndex+", filter "+v.getFilters().get(0));
					lsnr.headerChanged(v, columnIndex);
				}
			}	
		});

		TableColumnModel m = jtable.getColumnModel();
		
		for (int i = 0; i < m.getColumnCount(); i++)	{
			TableColumn c = m.getColumn(i);
			c.setHeaderValue(new HeaderValueStruct(c.getHeaderValue().toString()));
		}

		// Workaround: need to tell parent about header, else scrollpane has old header (this is a Swing bug)
		Component parent = jtable.getParent();
		if (parent instanceof JViewport)	{	// already added to some scrollpane
			((JViewport)parent).setView(jtable);
		}
	}	

}