package fri.gui.swing.table.header;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

/**
 * This is a JTableHeader that manages CellEditors for header columns
 * by setting EditableHeaderUI as user interface class.
 * Editing starts immediately with first mouse click. Editing stops when
 * table is focused or another header.
 * <p>
 * Example:
 <pre>
 	FilterSortExpandListener lsnr = new FilterSortExpandListener()	{
		public void headerChanged(HeaderValueStruct v, int columnIndex)	{
			if (v.getChanged() == HeaderValueStruct.CHANGED_SORT)	{
				boolean ascending = (v.getSort() == HeaderValueStruct.SORT_ASC);
				sort(ascending);
			}
			else
			if (v.getChanged() == HeaderValueStruct.CHANGED_FILTER)	{
				String filter = v.getFilter();
				filter(filter, columnIndex);
			}
			else
			if (v.getChanged() == HeaderValueStruct.CHANGED_EXPANSION)	{
				boolean expanded = v.getExpanded();
				storeExpansion(expanded, columnIndex);
			}
		}	
	};

	FilterSortExpandHeaderEditor.setTableHeader(
		jtable,
		lsnr,
		FilterSortExpandHeaderEditor.SORT|FilterSortExpandHeaderEditor.FILTER);

 </pre>

 * @author Nobuo Tamemasa 1999
 * @author Fritz Ritzberger 2001
 */

public class EditableHeader extends JTableHeader implements
	CellEditorListener
{
	/** the default cell editor for all columns */
	protected TableCellEditor defaultHeaderEditor = null;
	/** the default cell renderer for all columns */
	protected TableCellRenderer defaultHeaderRenderer = null;

	private static final int HEADER_ROW = -10;	// getTableCellEditorComponent() is called with this row
	private int editingColumn;
	private TableCellEditor cellEditor = null;	// the currently active cell editor this object listens to
	private Component editorComp;
	private MouseListener mouseLsnr = new MouseAdapter()	{	// table mouse listener that ends cell editing
		public void mousePressed(MouseEvent e)	{
			if (isEditing())
				getCellEditor().cancelCellEditing();
		}
	};
	
	/**
	 * Create the header from a JTable with already set columns.
	 */
	public EditableHeader(JTable table) {
		this(table, null);
	}

	/**
	 * Create the header from a JTable with already set columns.
	 * A default editor for all columns is passed.
	 */
	public EditableHeader(JTable table, TableCellEditor defaultHeaderEditor) {
		super(table.getColumnModel());
		this.defaultHeaderEditor = defaultHeaderEditor;
		init(table.getColumnModel());
	}

	/**
	 * Create the header from a JTable with already set columns.
	 * A default editor and renderer for all columns is passed.
	 */
	public EditableHeader(JTable table, TableCellEditor defaultHeaderEditor, TableCellRenderer defaultHeaderRenderer) {
		super(table.getColumnModel());
		this.defaultHeaderEditor = defaultHeaderEditor;
		this.defaultHeaderRenderer = defaultHeaderRenderer;
		init(table.getColumnModel());
	}

	/**
	 * Create the header from a TableColumnModel.
	 */
	public EditableHeader(TableColumnModel columnModel) {
		super(columnModel);
		init(columnModel);
	}


	/** Recreates the TableColumnModel. */
	protected void init(TableColumnModel columnModel)	{
		recreateTableColumn(columnModel);
	}

	/** Loop through the ColumnModel and replace all TableColumns with adequate EditableTableHeaderColumns. */
	protected void recreateTableColumn(TableColumnModel columnModel) {
		int n = columnModel.getColumnCount();
		EditableHeaderTableColumn[] newCols = new EditableHeaderTableColumn[n];
		TableColumn[] oldCols = new TableColumn[n];
		for (int i=0;i<n;i++) {
			oldCols[i] = columnModel.getColumn(i);
			newCols[i] = new EditableHeaderTableColumn(defaultHeaderEditor);
			newCols[i].copyValues(oldCols[i]);
		}
		for (int i=0;i<n;i++) {
			columnModel.removeColumn(oldCols[i]);
		}
		for (int i=0;i<n;i++) {
			if (defaultHeaderRenderer != null)
				newCols[i].setHeaderRenderer(defaultHeaderRenderer);
			columnModel.addColumn(newCols[i]);
		}
	}

	/** Overridden to add a mouse listener to table, so editing can be stopped when table gets focus. */
	public void setTable(JTable table)	{
		//System.err.println("setting table in tableheader: "+table);
			
		if (this.table != null)	{
			this.table.removeMouseListener(mouseLsnr);
		}

		super.setTable(table);

		if (this.table != null)	{
			this.table.addMouseListener(mouseLsnr);
		}
	}

	/** Overridden to set a editable UI to header. */
	public void updateUI(){
		super.updateUI();
		setUI(new EditableHeaderUI());
	}
	

	/*public String getToolTipText()	{
		return "Click To Edit Header";
	}*/


	/** Called by UI when double clicked header */
	public boolean editCellAt(int index, EventObject e){
		if (cellEditor != null && !cellEditor.stopCellEditing()) { 
			return false;
		}
		if (!isCellEditable(index)) {
			return false;
		}    

		TableCellEditor editor = getCellEditor(index);

		if (editor != null && editor.isCellEditable(e)) {
		  editorComp = prepareEditor(editor, index);
		  editorComp.setBounds(getHeaderRect(index));
		  add(editorComp);
		  editorComp.validate();
		  setCellEditor(editor);
		  editingColumn = index;
		  editor.addCellEditorListener(this);

		  return true;
		}    
		return false;
	}

	/** Returns true if the EditableHeaderTableColumn at passed (visual) index returns true. */
	private boolean isCellEditable(int index) {
		int columnIndex = index;	//columnModel.getColumn(index).getModelIndex();
		EditableHeaderTableColumn col = (EditableHeaderTableColumn)columnModel.getColumn(columnIndex);
		return col.isHeaderEditable();
	}

	/** Returns the editor of column at (visual) index. */
	private TableCellEditor getCellEditor(int index) {
		int columnIndex = index;	//columnModel.getColumn(index).getModelIndex();
		EditableHeaderTableColumn col = (EditableHeaderTableColumn)columnModel.getColumn(columnIndex);
		return col.getHeaderEditor();
	}

	/** Changes to the new editor and removes and adds CellEditorListener. */
	private void setCellEditor(TableCellEditor newEditor) {
		TableCellEditor oldEditor = cellEditor;
		cellEditor = newEditor;

		// firePropertyChange

		if (oldEditor != null && oldEditor instanceof TableCellEditor) {
			oldEditor.removeCellEditorListener(this);
		}
		if (newEditor != null && newEditor instanceof TableCellEditor) {
			newEditor.addCellEditorListener(this);
		}
	}

	/** Calls editor.getTableCellEditorComponent() with header value at index. */
	private Component prepareEditor(TableCellEditor editor, int index) {
		Object value = columnModel.getColumn(index).getHeaderValue();
		JTable table = getTable();
		Component comp = editor.getTableCellEditorComponent(table, value, true, HEADER_ROW, index);
		if (comp instanceof JComponent)	{
			((JComponent)comp).setNextFocusableComponent(this);             
		}
		return comp;
	}

	/** Returns the currently set editor. This is called by the UI when canceling editor. */
	public TableCellEditor getCellEditor() {
		return cellEditor;
	}

	/** Called by the EditableHeaderUI to retrieve the focus Component for editing. */
	public Component getEditorComponent() {
		return editorComp;
	}

	/** Why is this public, Tame? */
	private void removeEditor() {
		TableCellEditor editor = getCellEditor();
		if (editor != null) {
			editor.removeCellEditorListener(this);

			requestFocus();
			remove(editorComp);

			Rectangle cellRect = getHeaderRect(editingColumn);

			setCellEditor(null);
			editingColumn = -1;
			editorComp = null;

			repaint(cellRect);
		}
	}

	/** Returns true if the editor is running. */
	public boolean isEditing() {
		return (getCellEditor() == null) ? false : true;
	}


	/**
	 * Implements CellEditorListener.
	 * Removes the editor and sets the new value to table column model.
	 * <CODE>headerChanged(...)</CODE> is called.
	 */
	public void editingStopped(ChangeEvent e) {
		TableCellEditor editor = getCellEditor();
		
		if (editor != null) {
			// retrieve new value from editor
			Object value = editor.getCellEditorValue();

			// set new value to table header column
			columnModel.getColumn(editingColumn).setHeaderValue(value);
			
			// call some subclasses implementation for change event
			headerChanged(value, editingColumn);

			// end editing visually
			removeEditor();
		}
	}
	
	/** Implements CellEditorListener. Just removes the editor. */
	public void editingCanceled(ChangeEvent e) {
		removeEditor();
	}
	

	/** Overridden to cancel editing if column model is resized. */
    public void resizeAndRepaint() {
		if (isEditing())
			editingCanceled(null);
		super.resizeAndRepaint();
	}


	/**
	 * Do call this method only from CellEditors.
	 * It is public as a implementation side effect.
	 * A CellEditor will need to call this method when the filter- or sort-button was pressed.
	 */
	void callHeaderChanged(Object value, int columnIndex)	{
		headerChanged(value, columnIndex);
	}

	
	/**
	 * Override this to receive changes from the header, even when it was not closed.
	 * Called everytime the header editing ends (by pressing enter or leaving header).
	 * Override this to process changed text.
	 * Default implementation does nothing.
	*/
	protected void headerChanged(Object value, int columnIndex)	{
		//System.err.println("received headerChanged(value="+value+", columnIndex="+columnIndex+")");
	}

	

	/** Test Main */
	public static void main(String[] args) {
		JTable table = new JTable(7, 5);
		
		//try	{ UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); } catch (Exception e) {}
			
		Icon asc  = new ImageIcon(EditableHeader.class.getResource("images/down.gif"));
		Icon desc = new ImageIcon(EditableHeader.class.getResource("images/up.gif"));

		FilterSortExpandListener lsnr = new FilterSortExpandListener()	{
			public void headerChanged(HeaderValueStruct v, int columnIndex)	{
				Thread.dumpStack();
				System.err.println("headerChanged (unchanged=0, sort=1, filter=2, expand=3): "+v.getChanged()+", column "+columnIndex+", filter "+v.getFilter());
			}
		};

		FilterSortExpandHeaderEditor.setTableHeader(
				table, lsnr, 7 /* FILTER|SORT|EXPAND */, asc, desc);
		JFrame frame = new JFrame("FILTER|SORT|EXPAND");
		JScrollPane pane = new JScrollPane(table);
		frame.getContentPane().add(pane);
		frame.setBounds(10, 10,  500, 300 );
		frame.setVisible(true);

		//////////////////////////////////////////////////////////////
		frame = new JFrame("FILTER|SORT");
		table = new JTable(7, 5);
		FilterSortExpandHeaderEditor.setTableHeader(
				table, lsnr, 3 /* FILTER|SORT */, asc, desc);
		pane = new JScrollPane(table);
		frame.getContentPane().add(pane);
		frame.setBounds(30, 30, 500, 300 );
		frame.setVisible(true);

		//////////////////////////////////////////////////////////////
		frame = new JFrame("FILTER|EXPAND");
		table = new JTable(7, 5);
		FilterSortExpandHeaderEditor.setTableHeader(
				table, lsnr, 5 /* FILTER|EXPAND */, asc, desc);
		pane = new JScrollPane(table);
		frame.getContentPane().add(pane);
		frame.setBounds(50, 50, 500, 300 );
		frame.setVisible(true);

		//////////////////////////////////////////////////////////////
		frame = new JFrame("FILTER");
		table = new JTable(7, 5);
		FilterSortExpandHeaderEditor.setTableHeader(
				table, lsnr, 1 /* FILTER */, asc, desc);
		pane = new JScrollPane(table);
		frame.getContentPane().add(pane);
		frame.setBounds(70, 70, 500, 300 );
		frame.setVisible(true);

		//////////////////////////////////////////////////////////////
		frame = new JFrame("SORT|EXPAND");
		table = new JTable(7, 5);
		FilterSortExpandHeaderEditor.setTableHeader(
				table, lsnr, 6 /* SORT|EXPAND */, asc, desc);
		pane = new JScrollPane(table);
		frame.getContentPane().add(pane);
		frame.setBounds(90, 90, 500, 300 );
		frame.setVisible(true);

		//////////////////////////////////////////////////////////////
		frame = new JFrame("SORT");
		table = new JTable(7, 5);
		FilterSortExpandHeaderEditor.setTableHeader(
				table, lsnr, 2 /* SORT */, asc, desc);
		pane = new JScrollPane(table);
		frame.getContentPane().add(pane);
		frame.setBounds(110, 110, 500, 300 );
		frame.setVisible(true);

		//////////////////////////////////////////////////////////////
		frame = new JFrame("EXPAND");
		table = new JTable(7, 5);
		FilterSortExpandHeaderEditor.setTableHeader(
				table, lsnr, 4 /* EXPAND */, asc, desc);
		pane = new JScrollPane(table);
		frame.getContentPane().add(pane);
		frame.setBounds(130, 130, 500, 300 );
		frame.setVisible(true);
	}

}
