package fri.gui.swing.foldermonitor;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/** Rendering folders and files as icons, setting colors on change field. */

class FolderMonitorTableCellRenderer extends DefaultTableCellRenderer
{
	private int column;
	private Color created = new Color(0xBBFFBB), deleted = new Color(0xFFDADA), modified = new Color(0xCCFFFF);
	private boolean selected;
	private JTable table;
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column)	{
		this.column = table.convertColumnIndexToModel(column);
		this.selected = selected;
		this.table = table;

		return super.getTableCellRendererComponent(table, value, selected, focus, row, column);
	}
	
	protected void setValue(Object value) {
		setIcon(null);
		setBackground(selected ? table.getSelectionBackground() : table.getBackground());

		if (column == Constants.columns.indexOf(Constants.FILETYPE))	{
			setText("");

			if (((String)value).equals(Constants.TYPE_FOLDER))
				setIcon(UIManager.getIcon("Tree.closedIcon"));
			else
			if (((String)value).equals(Constants.TYPE_FILE))
				setIcon(UIManager.getIcon("Tree.leafIcon"));
			else
				super.setValue(value);
		}
		else
		if (column == Constants.columns.indexOf(Constants.CHANGE))	{
			if (((String)value).equals(Constants.EVENT_CREATED))
				setBackground(created);
			else
			if (((String)value).equals(Constants.EVENT_DELETED))
				setBackground(deleted);
			else
			if (((String)value).equals(Constants.EVENT_MODIFIED))
				setBackground(modified);

			super.setValue(value);
		}
		else	{
			super.setValue(value);
		}
	}

}