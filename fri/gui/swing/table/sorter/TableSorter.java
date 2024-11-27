package fri.gui.swing.table.sorter;

import java.util.*;
import java.text.Collator;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;
import java.awt.event.MouseAdapter;
import java.awt.Component;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import fri.gui.CursorUtil;
import fri.gui.swing.util.CommitTable;

/*
 * @(#)TableSorter.java 1.5 97/12/17
 *
 * Copyright (c) 1997 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */

/**
 * Usage (by FRi):
 * 
 * <pre>
 * TableModel model = new myModel(modelParams);
 * TableSorter sorter = new TableSorter(model, tableContainerWindow);
 * JTable table = new JTable(sorter);
 * sorter.addMouseListenerToHeaderInTable(table);
 * </pre>
 * 
 * A sorter for TableModels. The sorter has a model (conforming to TableModel)
 * and itself implements TableModel. TableSorter does not store or copy the data
 * in the TableModel, instead it maintains an array of integers which it keeps
 * the same size as the number of rows in its model. When the model changes it
 * notifies the sorter that something has changed eg. "rowsAdded" so that its
 * internal array of integers can be reallocated. As requests are made of the
 * sorter (like getValueAt(row, col) it redirects them to its model via the
 * mapping array. That way the TableSorter appears to hold another copy of the
 * table with the rows in a different order. The sorting algorthm used is stable
 * which means that it does not move around rows when its comparison function
 * returns 0 to denote that they are equivalent.
 * 
 * @version 1.5 12/17/97
 * @author Philip Milne
 * 
 * CHANGE: FRi August 1999: one click toggles sort order change. CHANGE: FRi
 * August 1999: hourglass cursor while sorting, on new parameter "component"
 * CHANGE: FRi August 1999: convertRowToModel() method to convert selected table
 * indices. CHANGE: FRi August 1999: reallocation of index at DELETE, INSERT,
 * UPDATE. CHANGE: FRi September 1999: end editing in mousePressed() before
 * sort. CHANGE: FRi April 2003: public sortByColumn. CHANGE: FRi June 2003:
 * reallocate indexes when row count changed. CHANGE: FRi October 2003: add
 * sortByColumn() with explicit direction, toggle default only when same column
 */

public class TableSorter extends TableMap
{
	private static Collator collator = Collator.getInstance();

	private int[] indexes;

	private int[] newindexes;

	private Vector sortingColumns = new Vector(1);

	private boolean ascending = true; // toggle sort order

	private Component component = null; // hourglass cursor

	/**
	 * Create a table sorter to be installed to JTable instead of TableModel.
	 * 
	 * @param model
	 *          basis table model
	 * @param component
	 *          Component on which the hour glass cursor is installed while
	 *          re-sorting.
	 */
	public TableSorter(TableModel model, Component component) {
		this.component = component;
		setModel(model);
	}

	/**
	 * Create a table sorter to be installed to JTable instead of TableModel.
	 * 
	 * @param model
	 *          basis table model
	 */
	public TableSorter(TableModel model) {
		setModel(model);
	}

	/** Set a new model to table */
	public void setModel(TableModel model) {
		super.setModel(model);
		allocateIndexes();
	}

	/** @return value by mapping screen row to model row. */
	public Object getValueAt(int aRow, int aColumn) {
		checkModel();
		return model.getValueAt(indexes[aRow], aColumn);
	}

	/** Sets the new value, mapping of screen row to model row. */
	public void setValueAt(Object aValue, int aRow, int aColumn) {
		checkModel();
		model.setValueAt(aValue, indexes[aRow], aColumn);
	}

	/**
	 * Convert a row value from screen row to model row
	 * 
	 * @param row
	 *          number of visible row on screen, starting at zero
	 */
	public int convertRowToModel(int row) {
		//System.err.println("mapping row "+row+" to "+indexes[row]+", length
		// "+indexes.length);
		if (row == 0 && indexes.length <= 0)
			return 0;

		if (row >= indexes.length)
			return indexes.length;

		return indexes[row];
	}

	private void allocateIndexes() {
		int rowCount = model.getRowCount();
		//System.err.println("allocateIndexes "+rowCount);
		indexes = new int[rowCount];
		for (int row = 0; row < rowCount; row++)
			indexes[row] = row;
	}

	private int reallocateIndexes(TableModelEvent e) {
		int rowCount = model.getRowCount();
		if (e.getType() == TableModelEvent.INSERT) {
			reallocate(e.getFirstRow(), e.getLastRow(), rowCount, false);
		}
		else
			if (e.getType() == TableModelEvent.DELETE) {
				reallocate(e.getFirstRow(), e.getLastRow(), rowCount, true);
			}
			else
				if (e.getType() == TableModelEvent.UPDATE) {
					//System.err.println("type UPDATE, first "+e.getFirstRow()+", last
					// "+e.getLastRow());
				}
				else {
					System.err.println("type " + e.getType() + ", first " + e.getFirstRow() + ", last " + e.getLastRow());
					allocateIndexes();
				}
		return e.getType();
	}

	private void reallocate(int first, int last, int count, boolean delete) {
		newindexes = new int[count];
		int diff = last - first + 1;

		if (delete)
			reallocateDelete(first, last, count, diff);
		else
			reallocateInsert(first, last, count, diff);

		indexes = newindexes;
	}

	private void reallocateInsert(int first, int last, int count, int diff) {
		//System.err.println("type INSERT, first "+first+", last "+last+", count "+count);
		int i = 0;
		for (int j = 0; j < newindexes.length; j++) {
			if (j >= first && j <= last) { // new items are where they are
				newindexes[j] = j;
			}
			else
			if (indexes[i] >= first) {
				newindexes[j] = indexes[i] + diff;
				i++; // increment only if in range
			}
			else { // indexes[i] < first
				newindexes[j] = indexes[i];
				i++; // increment only if in range
			}
		}
	}

	private void reallocateDelete(int first, int last, int count, int diff) {
		//System.err.println("type DELETE, first "+first+", last "+last+", count "+count);
		int j = 0;
		for (int i = 0; i < indexes.length && j < newindexes.length; i++) {
			if (indexes[i] > last) {
				newindexes[j] = indexes[i] - diff;
				j++; // increment only if in range
			}
			else
			if (indexes[i] < first) {
				newindexes[j] = indexes[i];
				j++; // increment only if in range
			}
		}
	}

	/** Event implementation to reallocate mapping when table content has changed */
	public void tableChanged(TableModelEvent e) {
		//System.err.println("tableChanged in TableSorter, reallocate index "+e);
		if (reallocateIndexes(e) == TableModelEvent.UPDATE) {
			TableModelEvent te = new TableModelEvent((TableModel) e.getSource(), searchIndex(e.getFirstRow(), true), searchIndex(e.getLastRow(), false), e.getColumn(), e.getType());
			e = te;
		}
		super.tableChanged(e);
	}

	private int searchIndex(int row, boolean first) {
		for (int i = 0; i < indexes.length; i++)
			if (indexes[i] == row)
				return i;
		if (first)
			return 0;
		return Integer.MAX_VALUE;
	}

	private void checkModel() {
		if (indexes.length != model.getRowCount()) {
			allocateIndexes();
		}
	}

	/**
	 * Add a mouse listener to the Table to trigger a table sort when a column
	 * heading is clicked in the JTable.
	 */
	public void addMouseListenerToHeaderInTable(final JTable table) {
		final TableSorter sorter = this;
		final JTable tableView = table;

		if (component == null)
			component = table;

		tableView.setColumnSelectionAllowed(false);

		MouseAdapter listMouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				TableColumnModel columnModel = tableView.getColumnModel();
				int viewColumn = columnModel.getColumnIndexAtX(e.getX());
				int column = tableView.convertColumnIndexToModel(viewColumn);

				if (e.getClickCount() >= 1 && column != -1) {
					CommitTable.commit(tableView);
					sorter.sortByColumn(column);
				}
			}
		};

		JTableHeader th = tableView.getTableHeader();
		th.addMouseListener(listMouseListener);
	}

	/** Reorder after an insertion. */
	public void resort() {
		if (sortingColumns.size() <= 0)
			sortByColumn(0);
		else
			sortByColumn(((Integer) sortingColumns.get(0)).intValue(), getSortOrder());
	}
	
	/** Sort the table rows from outside. */
	public void sortByColumn(int column) {
		boolean newDirection = !ascending; // default toggle sort order
		if (sortingColumns.size() > 0) {
			Integer i = (Integer) sortingColumns.get(0);
			if (column != i.intValue()) // if we do not sort the same column as before, keep sort direction
				newDirection = ascending;
		}
		sortByColumn(column, newDirection);
	}

	/** Sort the table rows from outside, passing explicit direction. */
	public void sortByColumn(int column, boolean ascending) {
		if (component != null)
			CursorUtil.setWaitCursor(component);

		try	{
			this.ascending = ascending;
	
			sortingColumns.removeAllElements();
			sortingColumns.addElement(Integer.valueOf(column));
	
			sort();
	
			super.tableChanged(new TableModelEvent(this));
		}
		finally	{
			if (component != null)
				CursorUtil.resetWaitCursor(component);
		}
	}

	/** Returns the current sort order. */
	public boolean getSortOrder() {
		return ascending;
	}

	private void sort() {
		checkModel();

		// shuttlesort((int[])indexes.clone(), indexes, 0, indexes.length);
		// jdk1.3beta: access error when invoking indexes.clone()
		int[] cloned = new int[indexes.length];
		System.arraycopy(indexes, 0, cloned, 0, indexes.length);
		shuttlesort(cloned, indexes, 0, indexes.length);
	}

	// This is a home-grown implementation which we have not had time
	// to research - it may perform poorly in some circumstances. It
	// requires twice the space of an in-place algorithm and makes
	// NlogN assigments shuttling the values between the two
	// arrays. The number of compares appears to vary between N-1 and
	// NlogN depending on the initial order but the main reason for
	// using it here is that, unlike qsort, it is stable.
	private void shuttlesort(int from[], int to[], int low, int high) {
		if (high - low < 2) {
			return;
		}
		int middle = (low + high) / 2;
		shuttlesort(to, from, low, middle);
		shuttlesort(to, from, middle, high);

		int p = low;
		int q = middle;

		/*
		 * This is an optional short-cut; at each recursive call, check to see if
		 * the elements in this subset are already ordered. If so, no further
		 * comparisons are needed; the sub-array can just be copied. The array must
		 * be copied rather than assigned otherwise sister calls in the recursion
		 * might get out of sinc. When the number of elements is three they are
		 * partitioned so that the first set, [low, mid), has one element and and
		 * the second, [mid, high), has two. We skip the optimisation when the
		 * number of elements is three or less as the first compare in the normal
		 * merge will produce the same sequence of steps. This optimisation seems to
		 * be worthwhile for partially ordered lists but some analysis is needed to
		 * find out how the performance drops to Nlog(N) as the initial order
		 * diminishes - it may drop very quickly.
		 */

		if (high - low >= 4 && compare(from[middle - 1], from[middle]) <= 0) {
			for (int i = low; i < high; i++) {
				to[i] = from[i];
			}
			return;
		}

		// A normal merge.

		for (int i = low; i < high; i++) {
			if (q >= high || (p < middle && compare(from[p], from[q]) <= 0)) {
				to[i] = from[p++];
			}
			else {
				to[i] = from[q++];
			}
		}
	}

	private int compare(int row1, int row2) {
		for (int level = 0; level < sortingColumns.size(); level++) {
			Integer column = (Integer) sortingColumns.elementAt(level);
			int result = compareRowsByColumn(row1, row2, column.intValue());
			if (result != 0)
				return ascending ? result : -result;
		}
		return 0;
	}

	private int compareRowsByColumn(int row1, int row2, int column) {
		Class type = model.getColumnClass(column);
		TableModel data = model;

		// Check for nulls

		Object o1 = data.getValueAt(row1, column);
		Object o2 = data.getValueAt(row2, column);

		// If both values are null return 0
		if (o1 == null && o2 == null) {
			return 0;
		}
		else
			if (o1 == null) { // Define null less than everything.
				return -1;
			}
			else
				if (o2 == null) {
					return 1;
				}

		/*
		 * We copy all returned values from the getValue call in case an optimised
		 * model is reusing one object to return many values. The Number subclasses
		 * in the JDK are immutable and so will not be used in this way but other
		 * subclasses of Number might want to do this to save space and avoid
		 * unnecessary heap allocation.
		 */
		if (type.getSuperclass() == java.lang.Number.class) {
			Number n1 = (Number) data.getValueAt(row1, column);
			double d1 = n1.doubleValue();
			Number n2 = (Number) data.getValueAt(row2, column);
			double d2 = n2.doubleValue();

			if (d1 < d2)
				return -1;
			else
				if (d1 > d2)
					return 1;
				else
					return 0;
		}
		else
		if (type == java.util.Date.class) // Date is to be sorted from youngest to oldest
		{
			Date d1 = (Date) data.getValueAt(row1, column);
			long n1 = d1.getTime();
			Date d2 = (Date) data.getValueAt(row2, column);
			long n2 = d2.getTime();

			if (n1 < n2)
				return +1;
			else
				if (n1 > n2)
					return -1;
				else
					return 0;
		}
		else
		if (type == Boolean.class) {
			Boolean bool1 = (Boolean) data.getValueAt(row1, column);
			boolean b1 = bool1.booleanValue();
			Boolean bool2 = (Boolean) data.getValueAt(row2, column);
			boolean b2 = bool2.booleanValue();

			if (b1 == b2)
				return 0;
			else
				if (b1) // Define false < true
					return 1;
				else
					return -1;
		}
		else {
			Object v1 = data.getValueAt(row1, column);
			String s1 = v1.toString().toLowerCase();
			Object v2 = data.getValueAt(row2, column);
			String s2 = v2.toString().toLowerCase();
			//int result = s1.compareTo(s2);
			int result = collator.compare(s1, s2);

			if (result < 0)
				return -1;
			else
				if (result > 0)
					return 1;
				else
					return 0;
		}
	}

	/*
	public void n2sort() {
		for(int i = 0; i < getRowCount(); i++) {
			for(int j = i+1; j < getRowCount(); j++) {
				if (compare(indexes[i], indexes[j]) == -1) {
					swap(i, j);
				}
			}
		}
	}

	private void swap(int i, int j) {
		int tmp = indexes[i];
		indexes[i] = indexes[j];
		indexes[j] = tmp;
	}
	*/

}