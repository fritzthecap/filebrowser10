package fri.gui.swing.table.header;

import javax.swing.*;
import javax.swing.table.*;

/**
 * Wrapper for TableColumn to hold the header editor and a
 * property that tells wheter the header is editable.
 * It creates a textfield as the default header editor.
 */

public class EditableHeaderTableColumn extends TableColumn
{
	protected TableCellEditor headerEditor;
	protected boolean isHeaderEditable;

	public EditableHeaderTableColumn() {
		this(null);
	}

	public EditableHeaderTableColumn(TableCellEditor headerEditor) {
		setHeaderEditor(headerEditor == null ? createDefaultHeaderEditor() : headerEditor);
		isHeaderEditable = true;
	}

	public void setHeaderEditor(TableCellEditor headerEditor) {
		this.headerEditor = headerEditor;
	}

	public TableCellEditor getHeaderEditor() {
		return headerEditor;
	}

	public void setHeaderEditable(boolean isEditable) {
		isHeaderEditable = isEditable;
	}

	public boolean isHeaderEditable() {
		return isHeaderEditable;
	}

	public void copyValues(TableColumn base) {    
		modelIndex     = base.getModelIndex();
		identifier     = base.getIdentifier();
		width          = base.getWidth();
		minWidth       = base.getMinWidth();
		setPreferredWidth(base.getPreferredWidth());
		maxWidth       = base.getMaxWidth();
		headerRenderer = base.getHeaderRenderer();
		headerValue    = base.getHeaderValue();
		cellRenderer   = base.getCellRenderer();
		cellEditor     = base.getCellEditor();
		isResizable    = base.getResizable();
	}
	
	protected TableCellEditor createDefaultHeaderEditor() {
		return new DefaultCellEditor(new JTextField());
	}
  
}

