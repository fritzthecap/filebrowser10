package fri.gui.swing.filebrowser;

import java.io.File;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.table.*;
import fri.gui.swing.table.sorter.*;
import fri.gui.swing.table.PersistentColumnsTable;

/**
	JTable, die Datei-Information anzeigen und sortieren kann.
*/

public class InfoDataTable extends JTable implements
	NodeRenamer,
	NodeSelecter
{
	private TableSorter sorter;
	private DefaultTableModel model;
	private Component parent;
	private InfoTableCellEditor cellEditor;
	private InfoTableTimeCellEditor timeCellEditor;


	public InfoDataTable(
		DefaultTableModel model,
		Component parent)
	{
		super(model);
		this.parent = parent;
		setSortModel(model);
		
		InfoTableCellRenderer r = new InfoTableCellRenderer(
				(FileTableData)model.getDataVector(),
				this.sorter);
				
		setDefaultRenderer(Long.class, r);
		setDefaultRenderer(String.class, r);

		initColumnWidth();

		int cnt = Math.min(model.getRowCount() + 1, 20);
		int h = cnt * getRowHeight();
		setPreferredScrollableViewportSize(new Dimension(getPreferredSize().width, h));

		cellEditor = new InfoTableCellEditor(null);
		setDefaultEditor(String.class, cellEditor);
		//cellEditor.setClickCountToStart(1);	// disables right mouse button popup !!!
		
		// set editor for file time
		timeCellEditor = new InfoTableTimeCellEditor(null);
		setDefaultEditor(Long.class, timeCellEditor);
	}
	

	public void setDndListener(InfoTableDndListener lsnr)	{
		cellEditor.setDndListener(lsnr);
		timeCellEditor.setDndListener(lsnr);
	}
	

	
	public TableSorter getSorter()	{
		return sorter;
	}


	public void setSortModel(TableModel model)	{
		this.model = (DefaultTableModel)model;
		sorter = new TableSorter(model, parent);
		super.setModel(sorter);
		sorter.addMouseListenerToHeaderInTable(this);
	}


	// interface NodeRenamer
	
	public void beginRename()	{
		int i = getSelectedRow();
		if (i >= 0)	{
			FileTableData v = (FileTableData)model.getDataVector();
			editCellAt(i, convertColumnIndexToView(v.getNameColumn()));
		}
	}

	public void close()	{
		PersistentColumnsTable.store(this);
	}


	private void initColumnWidth()	{
		if (PersistentColumnsTable.load(this) == false)	{
			System.err.println("setting initial columns width in InfoDataTable ...");
			TableColumn column;
			FileTableData v = (FileTableData)model.getDataVector();
			column = this.getColumnModel().getColumn(v.getTypeColumn());
			column.setPreferredWidth(25);
			column.setMaxWidth(80);
			column = this.getColumnModel().getColumn(v.getNameColumn());
			column.setPreferredWidth(100);
			if (v.getPathColumn() >= 0)	{
				column = this.getColumnModel().getColumn(v.getPathColumn());
				column.setPreferredWidth(120);
			}
			column = this.getColumnModel().getColumn(v.getExtensionColumn());
			column.setPreferredWidth(30);
			column = this.getColumnModel().getColumn(v.getTimeColumn());
			column.setPreferredWidth(100);
			column.setMaxWidth(120);
			column = this.getColumnModel().getColumn(v.getSizeColumn());
			column.setPreferredWidth(60);
			column.setMaxWidth(80);
			column = this.getColumnModel().getColumn(v.getAccessColumn());
			column.setPreferredWidth(60);
			column.setMaxWidth(80);
		}
	}


	// interface NodeSelecter
	
	// public void selectAll()	// is implemented by JTable itself!
	
	public void printAsText()	{
		FileTableData v = (FileTableData)model.getDataVector();
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < getRowCount(); i++)	{
			int row = getSorter().convertRowToModel(i);
			File f = v.getFileAt(row);
			sb.append(f.getPath());
			
//			String name = getSorter().getValueAt(row, v.getNameColumn()).toString();
//			String path = getSorter().getValueAt(row, v.getPathColumn()).toString();
//			sb.append(path);
//			if (path.endsWith(File.separator) == false)
//				sb.append(File.separator);
//			sb.append(name);

			sb.append('\n');
		}
		
		new FileViewer(sb.toString());
	}

}