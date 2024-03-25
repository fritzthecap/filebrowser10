package fri.gui.swing.table.header;

import java.util.Vector;

/**
 * Struct that holds the data for every column header: filter list,
 * sort state, column-checked state (column shown or not), name of
 * column (label text).
 * Furthermore it holds the state what was changed recently in data.
 *
 * @author  Fritz Ritzberger 2001
 */

public class HeaderValueStruct
{
	public static final int SORT_UNDEFINED = 0;
	public static final int SORT_DESC = -1;
	public static final int SORT_ASC = 1;
	public static final int NOT_CHANGED = 0;
	public static final int CHANGED_SORT = 1;
	public static final int CHANGED_FILTER = 2;
	public static final int CHANGED_EXPANSION = 3;
	private int width;
	private Object name;
	private Vector filters;
	private int sort = SORT_UNDEFINED;	// 1 down, -1 up, 0 undefined
	private int change = NOT_CHANGED;
	private boolean expanded;

	/**
	 * Constructor for creating a struct with default values from the tableheader
	 * column labels (only name of column is knonw).
	 */
	public HeaderValueStruct(Object name)	{
		this(name, true, null);
	}

	/**
	 * Constructor for creating a struct with new values from the cell editor.
	 */
	private HeaderValueStruct(Object name, boolean expanded, Vector filters)	{
		this.name = name;
		this.expanded = expanded;
		this.filters = filters;
		if (this.filters == null)	{
			this.filters = new Vector();
		}
		if (this.filters.size() <= 0)	{
			this.filters.add("*");
		}
	}


	/** Change column header display label dynamically. */
	public void setName(String name)	{
		this.name = name;
	}
	
	
	/** Returns the column header label text. */
	public String toString()	{
		return name == null ? "" : name.toString();
	}

	/** Changes the sort flag. */
	public void setSort(int sort)	{
		change = CHANGED_SORT;
		this.sort = sort;
	}

	/** Stores the expansion state. */
	public void setExpanded(boolean expanded)	{
		change = CHANGED_EXPANSION;
		this.expanded = expanded;
	}

	/** Returns the column width. */
	public void setWidth(int width)	{
		this.width = width;
	}

	/** Sets a new list of column filters (String). */
	public void setFilters(Vector filters)	{
		change = CHANGED_FILTER;
		this.filters = filters;
	}

	/** Returns the sort flag. */
	public int getSort()	{
		return sort;
	}

	/** Returns the expansion state. */
	public boolean getExpanded()	{
		return expanded;
	}

	/** Returns the column width. */
	public int getWidth()	{
		return width;
	}


	/** Toggles the sort flag from ascending to descending or vice versa. */
	public int toggleSort()	{
		if (sort == SORT_UNDEFINED)
			sort = SORT_ASC;
		else
		if (sort == SORT_ASC)
			sort = SORT_DESC;
		else	//if (sort == SORT_DESC)
			sort = SORT_ASC;
		return sort;
	}

	/** Returns the list of filters (String). */
	public Vector getFilters()	{
		return filters;
	}
	
	/** Returns the first element from the list of filters or null if list is empty. */
	public String getFilter()	{
		return filters.size() > 0 ? filters.get(0).toString() : null;
	}
	
	/** Clears the change flag that indicates the last happened change to NOT_CHANGED. */
	public void clearChanged()	{
		change = NOT_CHANGED;
	}

	/** Returns the change flag that indicates the last happened change. */
	public int getChanged()	{
		return change;
	}

}
