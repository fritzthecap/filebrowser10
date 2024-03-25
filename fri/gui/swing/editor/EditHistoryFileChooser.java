package fri.gui.swing.editor;

import java.util.*;
import java.io.File;
import java.text.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import fri.util.props.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.dnd.*;
import fri.gui.swing.mdi.SwapableMdiPane;
import fri.gui.swing.table.sorter.TableSorter;

/**
 * Holds a list of recently edited files and lets them load into editor.
 * Persists edit history using the class of the MdiPane passed in constructor.
 * <p />
 * Lifecycle: allocated on editor construction, loads list from persistence,
 * offers it sorted by time or path or name. Opens a modeless dialog
 * on show() method, supports drag & drop. Lets add loaded files from
 * controller by <i>fileLoaded()</i>. Saves modifications on close().
 * Opens editor windows by calling <i>SwapableMdiPane.createMdiFrame()</i>
 * with file as argument.
 * <p />
 * System property "editor.history.size" controls the maximum editor history size, default is 128.
 * 
 * @author Fritz Ritzberger
 * Created on 11.03.2006
 */
public class EditHistoryFileChooser
{
	private static final int MAX_HISTORY = PropertyUtil.getSystemInteger("editor.history.size", 128);
	
	private static final int NAME_COLUMN = 0;
	private static final int PATH_COLUMN = 1;
	private static final int DATE_COLUMN = 2;
	private static final String [] attributeNames = new String []	{
			"name",
			"path",
			"date",
	};
	private static final String entityType = "filehistory";
	
	private static final Vector columnNames = new Vector(3);
	
	static	{
		columnNames.add("Name");
		columnNames.add("Path");
		columnNames.add("Date");
	}
	
	private static DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	private SwapableMdiPane mdiPane;
	private TableSorter sortedModel;
	private JTable table;
	private JDialog dialog;
	private AbstractButton load;
	private JPopupMenu popup;
	private AbstractButton removeAll, removeNonExisting, removeSelected;
	private boolean changed;
	
	
	/** Creates an edit history that cooperates with passed MdiPane. Does not show dialog yet. */
	public EditHistoryFileChooser(SwapableMdiPane mdiPane)	{
		this.mdiPane = mdiPane;
		Properties fileTable = ClassProperties.getProperties(mdiPane.getClass());
		Vector list2D = TableProperties.convert(fileTable, entityType, attributeNames);
		DefaultTableModel m = new DefaultTableModel(list2D, columnNames)	{
			/** do not let edit any cell */
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		this.sortedModel = new TableSorter(m);
		sortByDate();
	}
	
	/** Controller adds files by calling this method. */
	public void fileLoaded(File file)	{
		if (file == null)
			return;
		
		changed = true;
		
		Vector rowData = new Vector();
		rowData.add(file.getName());	// ordered: name
		rowData.add(new File(file.getParent()).getAbsolutePath());	// ordered: path
		rowData.add(dateFmt.format(new Date()));	// ordered: date
		
		// try to remove already contained equal file
		DefaultTableModel m = (DefaultTableModel) sortedModel.getModel();
		Vector list2D = m.getDataVector();
		for (int i = 0; i < list2D.size(); i++)	{
			Vector row = (Vector) list2D.get(i);
			if (row.get(NAME_COLUMN).equals(rowData.get(NAME_COLUMN)) && row.get(PATH_COLUMN).equals(rowData.get(PATH_COLUMN)))
				m.removeRow(i);
		}
		
		// add new file
		m.insertRow(0, rowData);
		
		// resort by currently selected sort column
		sortedModel.resort();
	}
	
	/** Action "History" has been called. */
	public void show()	{
		if (dialog == null)	{
			dialog = new JDialog(ComponentUtil.getFrame(mdiPane), "Editor History", false);
			dialog.getContentPane().add(buildTable(), BorderLayout.CENTER);
			dialog.getContentPane().add(buildActions(), BorderLayout.SOUTH);
			dialog.addWindowListener(new WindowAdapter()	{
				public void windowClosing(WindowEvent e) {
					save();
				}
			});
			
			KeyListener escapeListener = new KeyAdapter()	{
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
						close();
				}
			};
			dialog.addKeyListener(escapeListener);
			table.addKeyListener(escapeListener);
			load.addKeyListener(escapeListener);
		}
		new GeometryManager(dialog).show();
	}
	
	/** Editor closes, dialog gets closed, history gets saved. */
	public void close()	{
		save();
		if (dialog != null)	{	// close dialog
			dialog.setVisible(false);
			try	{ dialog.dispose(); }	catch (Exception ex)	{ ex.printStackTrace(); };
			dialog = null;
			table = null;
			popup = null;
			load = removeAll = removeNonExisting = removeSelected = null;
		}
	}
	
	private void save()	{
		if (changed)	{
			changed = false;

			// shorten list to maximum
			sortByDate();	// resorts data vector physically by date
			Vector list2D = ((DefaultTableModel) sortedModel.getModel()).getDataVector();
			for (int i = list2D.size() - 1; i >= 0; i--)	// loop from back to remove
				if (i >= MAX_HISTORY)
					list2D.remove(i);
	
			// save to persistence
			ClassProperties.setProperties(
					mdiPane.getClass(),
					TableProperties.convert(list2D, entityType, attributeNames));
			ClassProperties.store(mdiPane.getClass());
		}
	}
	
	private JComponent buildTable()	{
		this.table = new JTable(sortedModel);
		
		// handle double click and popup events
		table.addMouseListener(new MouseAdapter()	{
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2)
					openSelectedFiles();
			}
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					popupTrigger(e);
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					popupTrigger(e);
			}
			private void popupTrigger(MouseEvent e)	{
				popup.show(table, e.getX(), e.getY());
			}
		});
		
		// set a cell renderer that shows non-existing files disabled
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()	{
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				File f = getFileForVisibleRow(row);
				c.setEnabled(f != null && f.exists());
				return c;
			}
		});
		
		// enable load button when files are selected
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()	{
			public void valueChanged(ListSelectionEvent e) {
			    if (table != null)	{
			        int [] selectedRows = table.getSelectedRows();
			        load.setEnabled(selectedRows != null && selectedRows.length > 0);
			    }
			}
		});
		
		// enable sorting by mouse click
		sortedModel.addMouseListenerToHeaderInTable(table);
		
		// enable drag&drop from table into editor
		new EditHistoryDndSender(table);
		
		return new JScrollPane(table);
	}
	
	private JComponent buildActions()	{
		JPanel p = new JPanel();
		load = new JButton("Load");
		load.setEnabled(false);
		p.add(load);

		popup = new JPopupMenu();
		popup.add(removeNonExisting = new JMenuItem("Remove Non-Existing Files"));
		popup.add(removeSelected =  new JMenuItem("Remove Selected"));
		popup.add(removeAll = new JMenuItem("Remove All"));
		// must not diable popup items as popup would not show on empty table

		ActionListener al = new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				if (e.getSource() == load)
					openSelectedFiles();
				else
				if (e.getSource() == removeSelected)
					removeSelected();
				else
				if (e.getSource() == removeNonExisting)
					removeNonExisting();
				else
				if (e.getSource() == removeAll)
					removeAll();
			}
		};
		// listen to actions
		load.addActionListener(al);
		removeSelected.addActionListener(al);
		removeNonExisting.addActionListener(al);
		removeAll.addActionListener(al);

		return p;
	}
	
	private void sortByDate()	{
		sortedModel.sortByColumn(DATE_COLUMN, false);
	}
	
	// returns all selected and existing files from list, fills passed list with non-existing files
	private List getSelectedFiles()	{
		int [] rows = table.getSelectedRows();
		List result = new ArrayList(rows != null ? rows.length : 0);

		for (int i = 0; rows != null && i < rows.length; i++)	{
			File file = getFileForVisibleRow(rows[i]);
			result.add(file);
		}
		
		return result;
	}
	
	private File getFileForVisibleRow(int row)	{
		row = sortedModel.convertRowToModel(row);
		DefaultTableModel m = (DefaultTableModel) sortedModel.getModel();
		if (m.getRowCount() > row)	{
			Vector v = (Vector) ((DefaultTableModel) sortedModel.getModel()).getDataVector().get(row);
			String name = (String) v.get(NAME_COLUMN);
			String path = (String) v.get(PATH_COLUMN);
			return new File(path, name);
		}
		return null;
	}
	
	// creates editor windows with all selected and existing files
	private void openSelectedFiles()	{
		List selected = getSelectedFiles();
		for (int i = 0; i < selected.size(); i++)	{
			File file = (File) selected.get(i);
			mdiPane.createMdiFrame(file);
		}
		
		if (selected.size() > 0)
			close();
	}

	private void removeSelected()	{
		DefaultTableModel m = (DefaultTableModel) sortedModel.getModel();
	    int [] rows = table.getSelectedRows();
	    if (rows == null)
	        return;
	    Arrays.sort(rows);
		for (int i = rows.length - 1; i >= 0; i--)	{
			int modelRow = sortedModel.convertRowToModel(rows[i]);
			m.removeRow(modelRow);
		}
	}
	
	private void removeNonExisting()	{
		DefaultTableModel m = (DefaultTableModel) sortedModel.getModel();
		for (int i = m.getRowCount() - 1; i >= 0; i--)	{	// loop from back to remove
			String name = (String) m.getValueAt(i, NAME_COLUMN);
			String path = (String) m.getValueAt(i, PATH_COLUMN);
			File file = new File(path, name);
			if (file.exists() == false || i >= MAX_HISTORY)	{
				m.removeRow(i);
				changed = true;
			}
		}
	}

	private void removeAll()	{
		DefaultTableModel m = (DefaultTableModel) sortedModel.getModel();
		m.setRowCount(0);
		changed = true;
	}
	
	
	
	// sends selected files per drag & drop
	private class EditHistoryDndSender implements DndPerformer
	{
		public EditHistoryDndSender(JTable table) {
			new DndListener(this, table);
		}

		public Transferable sendTransferable() {
			List files = getSelectedFiles();
			return files.size() > 0 ? new JavaFileList(files) : null;
		}

		public boolean receiveTransferable(Object data, int action, Point p) {
			return false;
		}

		public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors) {
			return DataFlavor.javaFileListFlavor;
		}

		public void dataMoved() {}
		public void dataCopied() {}
		public void actionCanceled() {}

		public boolean dragOver(Point p) {
			return false;
		}

		public void startAutoscrolling() {}
		public void stopAutoscrolling() {}
	}

	
	
	/**
	 * Service method for open dialogs and menus. Returns the list of recently edited still existing files.
	 * @param max maximum count of files in returned array.
	public static File [] getHistory(Class persistenceClazz, int max)	{
		Properties fileTable = ClassProperties.getProperties(persistenceClazz);
		Vector list2D = TableProperties.convert(fileTable, entityType, attributeNames);
		List list = new ArrayList(MAX_HISTORY);
		for (int i = 0; i < max && i < list2D.size(); i++)	{
			Vector v = (Vector) list2D.get(i);
			String name = (String) v.get(NAME_COLUMN);
			String path = (String) v.get(PATH_COLUMN);
			File file = new File(path, name);
			if (file.exists())
				list.add(file);
		}
		return (File []) list.toArray(new File[list.size()]);
	}
	 */
}
