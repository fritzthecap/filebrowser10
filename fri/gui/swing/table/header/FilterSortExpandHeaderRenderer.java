package fri.gui.swing.table.header;

import java.awt.*;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.table.*;
import fri.gui.swing.combo.history.HistCombo;
import fri.gui.swing.button.NoInsetsButton;

/**
 * The renderer of the filter header editor. It derives CellEditor to
 * provide basic layouts for the cell editor.
 *
 * @author  Fritz Ritzberger 2001
 */

public class FilterSortExpandHeaderRenderer extends DefaultCellEditor implements
	TableCellRenderer
{
	public static final int FILTER = 1;	// 0001
	public static final int SORT = 2;	// 0010
	public static final int EXPAND = 4;	// 0100
	public static final int ALL_OPTIONS = FILTER|SORT|EXPAND;
	protected static final int defaultOptions = FILTER|SORT|EXPAND;
	protected static final int MIN_COLLAPSED_WIDTH = 0;
	protected static final int MAX_COLLAPSED_WIDTH = 14;
	protected int options;
	protected HeaderValueStruct current;
	protected JPanel panel;
	protected JPanel checkSortPanel;
	protected JCheckBox checkbox;
	protected JButton label;
	protected JPanel filterPanel;
	protected JButton goFilter;
	protected JButton filterText;
	private Icon sortDesc, sortAsc;


	/**
	 * Create a HeaderCellRenderer with all possible options.
	 */
	public FilterSortExpandHeaderRenderer()	{
		this(defaultOptions);
	}
	
	/**
	 * Create a HeaderCellRenderer with the passed options.
	 */
	public FilterSortExpandHeaderRenderer(int options)	{
		this(options, null, null);
	}
	
	/**
	 * Create a HeaderCellRenderer with passed Icons to be used.
	 */
	public FilterSortExpandHeaderRenderer(Icon sortAsc, Icon sortDesc)	{
		this(defaultOptions, sortAsc, sortDesc);
	}
	
	/**
	 * Create a HeaderCellRenderer with passed options and Icons to be used.
	 */
	public FilterSortExpandHeaderRenderer(int options, Icon sortAsc, Icon sortDesc)	{
		super(new HistCombo());	// will be filters for editor

		this.options = options;
		this.sortAsc = sortAsc;
		this.sortDesc = sortDesc;

		if ((options & SORT) == SORT)	{
			if (sortAsc == null)
				this.sortAsc  = new ImageIcon(getClass().getResource("images/up.gif"));

			if (sortDesc == null)
				this.sortDesc = new ImageIcon(getClass().getResource("images/down.gif"));
		}

		buildRenderer();
	}


	/**
		Returns true if header should display "Click To Edit Header" tooltip.
		This is when SORT or FILTER was set by options.
	public boolean needsEditTooltip()	{
		return (options & SORT) == SORT || (options & FILTER) == FILTER;
	}
	*/


	protected void buildRenderer()	{
		buildSortPanel();
		buildFilterPanel();
		
		panel = new TooltipProvidingPanel();
		panel.setBorder(BorderFactory.createRaisedBevelBorder());
		
		if (checkSortPanel != null && filterPanel != null)
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		else
			panel.setLayout(new BorderLayout());

		if (checkSortPanel != null)
			panel.add(checkSortPanel);
			
		if (filterPanel != null)
			panel.add(filterPanel);
	}
	
	protected void buildSortPanel()	{
		if ((options & SORT) != SORT && options != EXPAND)	{
			return;
		}
		
		buildCheckBox();

		label = new NoInsetsButton();	// column header label
		label.setBorderPainted(false);
		label.setFocusPainted(false);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setHorizontalTextPosition(SwingConstants.LEFT);
		if ((options & SORT) == SORT)
			label.setToolTipText("Sort Column");

		checkSortPanel = new JPanel(new BorderLayout());
		
		if (checkbox != null)
			checkSortPanel.add(checkbox, BorderLayout.WEST);
			
		checkSortPanel.add(label, BorderLayout.CENTER);
	}
		
	protected void buildFilterPanel()	{
		if ((options & FILTER) != FILTER)	{
			return;
		}

		buildCheckBox();	// if sort panel was not built
		
		goFilter = new NoInsetsButton("Filter");
		goFilter.setToolTipText("Apply Row Filter");
		goFilter.setBorderPainted(false);
		goFilter.setFocusPainted(false);

		filterText = new NoInsetsButton();	
		filterText.setBorderPainted(false);
		filterText.setHorizontalAlignment(checkSortPanel == null ? SwingConstants.CENTER : SwingConstants.LEFT);
		filterText.setToolTipText("Edit Row Filter");

		filterPanel = new JPanel(new BorderLayout());

		if (checkSortPanel == null)	{
			// leave out goFilter button
			if (checkbox != null)
				filterPanel.add(checkbox, BorderLayout.WEST);
		}
		else	{
			filterPanel.add(goFilter, BorderLayout.WEST);
		}

		filterPanel.add(filterText, BorderLayout.CENTER);
	}

	protected void buildCheckBox()	{
		if ((options & EXPAND) != EXPAND)	{
			return;
		}

		if (checkbox != null)	{
			return;	// do not create twice
		}
		
		checkbox = new NoInsetsCheckBox();	// no label for checkbox, to NOT close column when clicking on label
		checkbox.setBorderPaintedFlat(true);
		checkbox.setBorder(null);
		checkbox.setSelected(true);
		checkbox.setToolTipText("Collapse Or Expand Column");
	}
	
	public Component getTableCellRendererComponent(
		JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		if (value == null)
			return panel;

		//System.err.println("getTableCellRendererComponent, receiving value "+(value != null ? value.getClass().toString() : ""));
		if (value instanceof HeaderValueStruct == false)
			return panel;
			
		current = (HeaderValueStruct) value;

		if (checkbox != null)	{
			checkbox.setSelected(current.getExpanded());

			if (current.getExpanded() == false)	{
				TableColumnModel m = table.getColumnModel();
				TableColumn c = m.getColumn(column);
				c.setMinWidth(MIN_COLLAPSED_WIDTH);
				c.setMaxWidth(MAX_COLLAPSED_WIDTH);
			}
		}
		
		if (label != null)	{	// existing checkSortPanel
			label.setText(current.toString());
			label.setIcon(getSortIcon());
		}

		if (label == null)	{	// no checkSortPanel
			filterText.setText(current.toString());
		}
		else
		if (filterText != null)	{
			String f = current.getFilter();
			filterText.setText(f == null || f.length() <= 0 ? " " : f);	// one blank as label, would be zero height else
		}
		
		if (goFilter != null)
			goFilter.setEnabled(current.getExpanded());

		return panel;
	}

	protected Icon getSortIcon()	{
		return
			current.getSort() == HeaderValueStruct.SORT_DESC ? sortDesc :
			current.getSort() == HeaderValueStruct.SORT_ASC ? sortAsc :
			null;
	}



	class NoInsetsCheckBox extends JCheckBox
	{
		public Insets getInsets()	{
			return new Insets(0, 0, 0, 0);
		}
	}
	

	protected class TooltipProvidingPanel extends JPanel
	{
		public String getToolTipText(MouseEvent e)	{
			Point p = e.getPoint();
			Component c = SwingUtilities.getDeepestComponentAt(this, p.x, p.y);

			if (c == null && checkSortPanel != null)	{
				Point p2 = SwingUtilities.convertPoint(this, p, checkSortPanel);
				c = SwingUtilities.getDeepestComponentAt(checkSortPanel, p2.x, p2.y);
			}

			if (c == null && filterPanel != null)	{
				Point p2 = SwingUtilities.convertPoint(this, p, filterPanel);
				c = SwingUtilities.getDeepestComponentAt(filterPanel, p2.x, p2.y);
			}
				
			if (c instanceof JComponent)	{
				return ((JComponent)c).getToolTipText(e);
			}
			
			return getToolTipText();
		}
	}

}