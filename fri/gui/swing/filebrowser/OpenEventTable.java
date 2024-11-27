package fri.gui.swing.filebrowser;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import fri.gui.swing.commandmonitor.EnvDialog;
import fri.gui.swing.table.sorter.*;
import fri.util.sort.quick.*;
import fri.util.FileUtil;
import fri.util.os.OS;
import fri.gui.swing.text.*;

/**
	Target: a scrollpane that delegates to a JTable.
	<p />
	Responsibilites: all basic operations on table rows, set initial size of columns,
		set cell editor combo-box, listen to mouse- and key-events.
*/
public class OpenEventTable extends JScrollPane implements
	ListSelectionListener,
	ActionListener,
	MouseListener,
	KeyListener
{
	private JTable table;
	private OpenEventTableModel model;
	private JPopupMenu popup;
	private JMenuItem delete, insert, browse, environment;
	private int selected = 0;
	private OpenCommandList commands;
	private JFrame frame;
	private TableSorter sorter;
	

	/**
		Create a table on the passed frame with the command list.
		@param frame parent that adds this scroll pane
		@param commands list of commands for open events.
	*/
	public OpenEventTable(JFrame frame, OpenCommandList commands) {
		this.frame = frame;
		this.commands = commands;
		
		model = new OpenEventTableModel(commands);
		sorter = new TableSorter(model, frame);
		table = new JTable(sorter);

		sorter.addMouseListenerToHeaderInTable(table);

		ListSelectionModel lm = table.getSelectionModel();
		lm.addListSelectionListener(this);
		lm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		new OpenEventTableDndListener(table, this);

		popup = new JPopupMenu();
		popup.add(insert = new JMenuItem("Insert"));
		insert.addActionListener(this);
		popup.add(delete = new JMenuItem("Delete"));
		delete.addActionListener(this);
		popup.addSeparator();
		popup.add(environment = new JMenuItem("Edit Environment(s)"));
		environment.addActionListener(this);
		popup.addSeparator();
		popup.add(browse = new JMenuItem("Browse For Command"));
		browse.addActionListener(this);

		table.addMouseListener(this);	// for showing popup
		table.addKeyListener(this);	// insert, delete
			
		table.setDefaultEditor(String.class, new DefaultCellEditor((JTextField)new ClipableJTextField()));

		DefaultCellEditor ded;
		ded = (DefaultCellEditor)table.getDefaultEditor(Boolean.class);
		ded.setClickCountToStart(1);

		TableColumn column;
		column = table.getColumnModel().getColumn(OpenCommandList.SHORTNAME_COLUMN);
		column.setPreferredWidth(80);
		column = table.getColumnModel().getColumn(OpenCommandList.PATTERN_COLUMN);
		column.setPreferredWidth(80);
		column = table.getColumnModel().getColumn(OpenCommandList.COMMAND_COLUMN);
		column.setPreferredWidth(240);
		column = table.getColumnModel().getColumn(OpenCommandList.PATH_COLUMN);
		column.setPreferredWidth(160);
		column = table.getColumnModel().getColumn(OpenCommandList.MONITOR_COLUMN);
		column.setPreferredWidth(50);
		column.setMaxWidth(50);
		column = table.getColumnModel().getColumn(OpenCommandList.LOOP_COLUMN);
		column.setPreferredWidth(50);
		column.setMaxWidth(50);
		column = table.getColumnModel().getColumn(OpenCommandList.TYPE_COLUMN);
		column.setPreferredWidth(50);
		column.setMaxWidth(50);
		JComboBox comboBox = new JComboBox();
		comboBox.addItem("Files");
		comboBox.addItem("Folders");
		comboBox.addItem("All");
		ded = new DefaultCellEditor(comboBox);
		ded.setClickCountToStart(1);
		column.setCellEditor(ded);
		column = table.getColumnModel().getColumn(OpenCommandList.INVARIANT_COLUMN);
		column.setPreferredWidth(50);
		column.setMaxWidth(50);

		table.setPreferredScrollableViewportSize(new Dimension(700, 200));
		//table.setBackground(SystemColor.window);
		
		setViewportView(table);
	}


	/**
		Create a empty line with the passed pattern in second column.
		If there is an empty first line, delete this before.
		Select the new line.
	*/
	public void setNewPattern(String patt, boolean isLeaf)	{
		int i = 0;
		if (commands.isEmpty())	{	// delete the one blank row
			if (model.getRowCount() > 0)
				model.removeRow(0);
			addRow(patt, isLeaf, 0);
		}
		else	{
			if ((i = hasPattern(patt)) < 0)	{
				i = model.getRowCount();
				addRow(patt, isLeaf, i);
			}
		}
		table.getSelectionModel().setSelectionInterval(i, i);
	}


	private void addRow(String patt, boolean isLeaf, int i)	{
		model.addRow(model.buildRow(patt, isLeaf));
		String lpatt = patt.toLowerCase();
		String [] pattAndCmd;
		
		if (isLeaf)	{
			// Definition of default actions by extension
			
			if ((pattAndCmd = FileExtensions.isJavaClass(patt)) != null)	{
				model.setValueAt(pattAndCmd[1], i, OpenCommandList.COMMAND_COLUMN);
				model.setValueAt(pattAndCmd[0], i, OpenCommandList.PATTERN_COLUMN);
				model.setValueAt(Boolean.TRUE, i, OpenCommandList.LOOP_COLUMN);
				model.setValueAt(Boolean.FALSE, i, OpenCommandList.MONITOR_COLUMN);
				model.setValueAt("Internal Launch", i, OpenCommandList.SHORTNAME_COLUMN);
			}
			else
			if ((pattAndCmd = FileExtensions.isArchive(patt)) != null)	{
				model.setValueAt(pattAndCmd[1], i, OpenCommandList.COMMAND_COLUMN);
				model.setValueAt(pattAndCmd[0], i, OpenCommandList.PATTERN_COLUMN);
				model.setValueAt(Boolean.TRUE, i, OpenCommandList.LOOP_COLUMN);
				model.setValueAt(Boolean.FALSE, i, OpenCommandList.MONITOR_COLUMN);
				model.setValueAt("View Archive", i, OpenCommandList.SHORTNAME_COLUMN);
			}
			else
			if ((pattAndCmd = FileExtensions.isHTML(lpatt)) != null)
			{
				model.setValueAt(pattAndCmd[1], i, OpenCommandList.COMMAND_COLUMN);
				model.setValueAt(pattAndCmd[0], i, OpenCommandList.PATTERN_COLUMN);
				model.setValueAt(Boolean.TRUE, i, OpenCommandList.LOOP_COLUMN);
				model.setValueAt(Boolean.FALSE, i, OpenCommandList.MONITOR_COLUMN);
				model.setValueAt("View Document", i, OpenCommandList.SHORTNAME_COLUMN);
			}
			else
			if ((pattAndCmd = FileExtensions.isXML(lpatt)) != null)
			{
				model.setValueAt(pattAndCmd[1], i, OpenCommandList.COMMAND_COLUMN);
				model.setValueAt(pattAndCmd[0], i, OpenCommandList.PATTERN_COLUMN);
				model.setValueAt(Boolean.FALSE, i, OpenCommandList.LOOP_COLUMN);
				model.setValueAt(Boolean.FALSE, i, OpenCommandList.MONITOR_COLUMN);
				model.setValueAt("Edit Document", i, OpenCommandList.SHORTNAME_COLUMN);
			}
			else
			if ((pattAndCmd = FileExtensions.isImage(lpatt)) != null)
			{
				model.setValueAt(pattAndCmd[1], i, OpenCommandList.COMMAND_COLUMN);
				model.setValueAt(pattAndCmd[0], i, OpenCommandList.PATTERN_COLUMN);
				model.setValueAt(Boolean.FALSE, i, OpenCommandList.LOOP_COLUMN);
				model.setValueAt(Boolean.FALSE, i, OpenCommandList.MONITOR_COLUMN);
				model.setValueAt("View Image", i, OpenCommandList.SHORTNAME_COLUMN);
			}
//			else
//			if (lpatt.endsWith(".java"))	{
//				model.setValueAt("JAVA com.sun.tools.javac.Main -cp $CLASSPATH $FILE", i, OpenCommandList.COMMAND_COLUMN);
//				model.setValueAt("*.java", i, OpenCommandList.PATTERN_COLUMN);
//				model.setValueAt(Boolean.FALSE, i, OpenCommandList.LOOP_COLUMN);
//				model.setValueAt(Boolean.FALSE, i, OpenCommandList.MONITOR_COLUMN);
//				model.setValueAt("Compile (Javac)", i, OpenCommandList.SHORTNAME_COLUMN);
//			}
			else
			if (OS.isUnix && lpatt.endsWith(".sh") ||
					OS.isUnix && lpatt.endsWith(".ksh") ||
					OS.isUnix && lpatt.endsWith(".csh") ||
					OS.isUnix && lpatt.endsWith(".pl") ||
					OS.isUnix && lpatt.endsWith(".py") ||
					OS.isUnix && lpatt.endsWith(".tcl") ||
					OS.isWindows && lpatt.endsWith(".exe") ||
					OS.isWindows && lpatt.endsWith(".bat"))
			{
				model.setValueAt("*."+FileUtil.getExtension(patt), i, OpenCommandList.PATTERN_COLUMN);
				model.setValueAt("$FILE", i, OpenCommandList.COMMAND_COLUMN);
				model.setValueAt(Boolean.TRUE, i, OpenCommandList.LOOP_COLUMN);
				model.setValueAt(Boolean.TRUE, i, OpenCommandList.MONITOR_COLUMN);
				model.setValueAt("Execute", i, OpenCommandList.SHORTNAME_COLUMN);
			}
			else	{
				// default: view as plain text
				String s = FileUtil.getExtension(patt);
				s = (s == null || s.length() <= 0) ? patt : "*."+s;
				model.setValueAt(s, i, OpenCommandList.PATTERN_COLUMN);
				//model.setValueAt("VIEW $FILE", i, OpenCommandList.COMMAND_COLUMN);// cannot do that as line would be valid then
				model.setValueAt(Boolean.TRUE, i, OpenCommandList.LOOP_COLUMN);
				model.setValueAt(Boolean.FALSE, i, OpenCommandList.MONITOR_COLUMN);
				model.setValueAt("Open File", i, OpenCommandList.SHORTNAME_COLUMN);
			}
		}
		else	{	// is folder
			//model.setValueAt("mount $FILE", i, OpenCommandList.COMMAND_COLUMN);// cannot do that as line would be valid then
			model.setValueAt(Boolean.TRUE, i, OpenCommandList.LOOP_COLUMN);
			model.setValueAt(Boolean.FALSE, i, OpenCommandList.MONITOR_COLUMN);
			model.setValueAt(Boolean.TRUE, i, OpenCommandList.INVARIANT_COLUMN);
			model.setValueAt("Mount Device", i, OpenCommandList.SHORTNAME_COLUMN);
		}
	}
	
	
	
	private int hasPattern(String name)	{
		for (int i = 0; i < model.getRowCount(); i++)	{
			String patt = (String)model.getValueAt(i, OpenCommandList.PATTERN_COLUMN);
			if (commands.match(name, patt))
				return i;
		}
		return -1;
	}

	
	/** @return table of editor */
	public JTable getTable()	{
		return table;
	}

	
	/** stop editing saving current content */
	public void commit()	{
		//System.err.println("stop cell editing");
		//DefaultCellEditor edi = (DefaultCellEditor)table.getCellEditor();
		//edi.stopCellEditing();
		table.editingStopped(null);
		
		for (int i = model.getRowCount() - 1; i >= 0; i--)	{
			// check if line is valid, if not, remove it
			String cmd = (String)model.getValueAt(i, OpenCommandList.COMMAND_COLUMN);
			if (cmd != null && !cmd.equals(""))
				cmd = (String)model.getValueAt(i, OpenCommandList.PATTERN_COLUMN);
				
			if (cmd == null || cmd.equals(""))	{
				model.removeRow(i);
			}
			else	{	// trim strings
				for (int j = 0; j < OpenCommandList.COLUMNS; j++)	{
					Object o = model.getValueAt(i, j);
					if (o instanceof String)	{
						String s = (String)o;
						if (s.indexOf(" ") >= 0)
							model.setValueAt(s.trim(), i, j);
					}
				}
			}
		}
	}
	
	private void insertRowAtSelections()	{
		Integer [] Iarr = getSelectedAsSortedArray();
		if (Iarr != null)	{
			table.getSelectionModel().clearSelection();
			for (int i = Iarr.length - 1; i >= 0; i--)
				insertRowAt(Iarr[i].intValue());
		}
	}

	private void insertRowAt(int row)	{
		if (row >= model.getRowCount())
			row = model.getRowCount() - 1;
		int newrow = row + 1;
		insertRowAt(
				new String((String)model.getValueAt(row, OpenCommandList.PATTERN_COLUMN)),
				new String((String)model.getValueAt(row, OpenCommandList.TYPE_COLUMN)),
				newrow);
		DefaultListSelectionModel lm = (DefaultListSelectionModel)table.getSelectionModel();
		lm.addSelectionInterval(newrow, newrow);
	}

	private void insertRowAt(String pattern, String type, int index)	{
		Vector row = model.buildRow(pattern, type);
		model.insertRow(index, row);
	}

	private void removeRow(int i)	{
		if (i >= 0 && i < model.getRowCount())
			model.removeRow(i);
	}

	private void removeSelectedRows()	{
		Integer [] Iarr = getSelectedAsSortedArray();
		if (Iarr != null)	{
			for (int i = Iarr.length - 1; i >= 0; i--)
				removeRow(Iarr[i].intValue());
		}
		if (model.getRowCount() <= 0)
			insertRowAt("*.java", "Files", 0);
	}

	private Integer [] getSelectedAsSortedArray()	{
		// removing rows from end to start of list: order of model!
		int [] iarr = table.getSelectedRows();
		Integer [] Iarr = null;
		if (iarr != null)	{
			Iarr = new Integer [iarr.length];
			for (int i = 0; i < iarr.length; i++)
				Iarr[i] = Integer.valueOf(sorter.convertRowToModel(iarr[i]));
			QSort sorter = new QSort();
			sorter.sort(Iarr);
		}
		return Iarr;
	}

	private void chooseFile() {
		String exePath = System.getProperty("java.library.path");
		int first = exePath.indexOf(System.getProperty("path.separator"));
		if (first > 0)	{
			exePath = exePath.substring(0, first);
		}
		else	{
			exePath = System.getProperty("user.home");
		}

		File [] files = FileChooser.showFileDialog(
				"Set Command",
				frame,
				TreePanel.getGlobalRootNode(),
				null,	// no suggested file name
				new File(exePath),
				OS.isWindows ? "*.exe" : null,	// filter
				true);	// single select
				
		if (files != null)	{
			String currPath = files[0].getPath();			
			int [] iarr = table.getSelectedRows();
			for (int j = 0; j < iarr.length; j++)	{
				setPathInto(currPath, iarr[j]);
			}
		}
	}

	/**
		Set a chosen path into command-column of the table, respecting
		the current arguments of the command-line. Default the path column
		is set to path of the application.
		
		@param newPath path string to insert
		@param idx index of row
	*/
	public void setPathInto(String newPath, int idx)	{
		int oldidx = idx;
		idx = sorter.convertRowToModel(idx);
		String cmd = (String)model.getValueAt(idx, OpenCommandList.COMMAND_COLUMN);
		String path = new String(newPath);
		int i = cmd.indexOf(" ");	// exchange only first word of command
		if (i > 0)	{
			path = path+cmd.substring(i);
		}
		else	{
			path = path+" $FILE";
		}
		model.setValueAt(path, idx, OpenCommandList.COMMAND_COLUMN);
		table.getSelectionModel().setSelectionInterval(oldidx, oldidx);

		//model.setValueAt(FileUtil.separatePath(newPath), i, OpenCommandList.PATH_COLUMN)
	}


	private void editEnv()	{
		int [] iarr = table.getSelectedRows();
		Vector v = null;
		String [] env = null;
		for (int i = 0; i < iarr.length; i++)	{
			int idx = sorter.convertRowToModel(iarr[i]);
			String [] earr = model.getEnvironment(idx);
			if (earr != null)	{
				if (v == null)
					v = new Vector();
				for (int j = 0; j < earr.length; j++)
					if (v.contains(earr[j]) == false)
						v.addElement(earr[j]);
			}
		}
		if (v != null)	{
			env = new String [v.size()];
			v.copyInto(env);
		}
		
		EnvDialog ed = new EnvDialog(frame, env);
		
		if (ed.getOK())	{
			env = ed.getEnv();
			for (int i = 0; i < iarr.length; i++)	{
				model.putEnvironment(iarr[i], env);
			}
		}
	}
		
	
	
	
	// interface ActionListener, Popup-Menu
	
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == insert)	{
			insertRowAtSelections();
		}
		else
		if (e.getSource() == delete)	{
			removeSelectedRows();
		}
		else
		if (e.getSource() == browse)	{
			chooseFile();
		}
		else
		if (e.getSource() == environment)	{
			editEnv();
		}
	}


	// interface ListSelectionListener
	
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;

		ListSelectionModel lm = table.getSelectionModel();
		selected = lm.getLeadSelectionIndex();
	}



	// interface MouseListener
	
	public void mousePressed (MouseEvent e)	{
		if (e.isPopupTrigger())	{
			doPopup(e);
		}
	}
	public void mouseEntered (MouseEvent e)	{
	}
	public void mouseExited (MouseEvent e)	{
	}
	public void mouseClicked (MouseEvent e)	{
	}
	public void mouseReleased (MouseEvent e)	{
		if (e.isPopupTrigger())	{
			doPopup(e);
		}
	}

	private void doPopup(MouseEvent e)	{
		if (table.getSelectedRowCount() <= 0)	{	// set selection if not set
			int row = table.rowAtPoint(e.getPoint());
			DefaultListSelectionModel lm = (DefaultListSelectionModel)table.getSelectionModel();
			lm.setSelectionInterval(row, row);
		}
		popup.show(e.getComponent(), e.getX(), e.getY());
	}
	

	// interface KeyListener
	
	public void keyPressed(KeyEvent e)	{
		//System.err.println("keyPressed "+e);
		if (e.getKeyCode() == KeyEvent.VK_DELETE)	{
			removeSelectedRows();
		}
		else
		if (e.getKeyCode() == KeyEvent.VK_INSERT)	{
			insertRowAtSelections();
		}
		else
		if (e.getKeyCode() == KeyEvent.VK_ENTER)	{
			//System.err.println("ENTER pressed on "+selected);
			chooseFile();
		}
		else
		if (e.getKeyCode() == KeyEvent.VK_F2)	{
			table.editCellAt(selected, 0);
		}
		else	{
			super.processKeyEvent(e);	// else the installed Escape keypress would not work
		}
	}
	public void keyReleased(KeyEvent e)	{
	}
	public void keyTyped(KeyEvent e)	{
	}
	
}