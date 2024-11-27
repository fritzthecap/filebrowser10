package fri.gui.swing.filebrowser;

import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.Vector;
import java.io.File;
import java.util.Hashtable;
import fri.gui.swing.table.sorter.*;

/**
	Target:
		<br>a scrollpane that delegates to a JTable.<br>
	Responsibilites:<br>
		Render contents of a folder.
*/

public class InfoTable extends JScrollPane implements
	ListSelectionListener,
	NodeInserter
{
	protected InfoRenderer renderer;
	protected InfoDataTable table;
	protected DefaultTableModel model;
	protected FileTableData data;
	protected NetNode [] nodes;
	protected final JFrame frame;
	protected TableSorter sorter;
	private BufferedTreeNode root;
	private BufferedTreeNode folder = null;
	private JTree tree;
	protected TreeEditController tc;
	private InfoDataTableMouseListener ml;
	private int whichPath = InfoTableModel.RELATIVE_PATH;
	private String parentPath;
	

	/**
		Create a table to render contents of passed file or folder.
		@param filename folder or file to render.
	*/
	public InfoTable(
		InfoRenderer renderer,
		TreeEditController tc,
		NetNode [] nodes,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
	{
		//System.err.println("InfoTable constructor");
		this.renderer = renderer;
		this.frame = renderer.getFrame();
		this.tc = tc;
		this.tree = tc.getTree();
		this.root = tc.getRoot();
		this.nodes = nodes;
		if (nodes.length == 1)	{
			folder = BufferedTreeNode.fileToBufferedTreeNode((File)nodes[0].getObject(), root);
			//System.err.println("got folder!");
		}
		
		whichPath = nodes.length == 1 || havingSameParent(nodes) ?
				InfoTableModel.NO_PATH : InfoTableModel.RELATIVE_PATH;
				
		parentPath = whichPath == InfoTableModel.RELATIVE_PATH ?
				InfoFrame.getCommonPath(nodes).getFullText() : null;
				
		//System.err.println("InfoTable whichPath = "+(whichPath == InfoTableModel.NO_PATH ? "NO_PATH" : "RELATIVE_PATH")+", common path "+parentPath);

		this.data = initData(filter, include, showfiles, showhidden);
		this.model = initModel(data);
		// set path rendering method
		this.table = new InfoDataTable(model, frame);
		this.sorter = table.getSorter();
		
		table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				
		setViewportView(table);
		
		init();
	}
	
	
	private boolean havingSameParent(NetNode [] nodes)	{
		NetNode pnt = null;
		for (int i = 0; i < nodes.length; i++)	{
			NetNode thisPnt = nodes[i].getParent();
			if (pnt == null)
				pnt = thisPnt;
			else
			if (thisPnt != null && thisPnt.equals(pnt) == false)
				return false;
		}
		return true;
	}


	/** @return data for table */
	protected FileTableData initData(
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
	{
		InfoData data = new InfoData(nodes, filter, include, showfiles, showhidden, whichPath);
		
		Vector v = data.getData();
		
		for (int i = 0; i < v.size(); i++)	{
			NetNode n = (NetNode)v.elementAt(i);
			Vector row = InfoTableModel.createRow(n, data, whichPath, parentPath);
			data.addElement(row);
		}
		return data;
	}



	/** @return model for table */
	protected DefaultTableModel initModel(FileTableData data)	{
		InfoTableModel model = new InfoTableModel(data, renderer);
		model.setTreeEditController(tc);	// for rename
		model.setWhichPath(whichPath, parentPath);
		return model;
	}


	/** initialize table, add selection listener  */
	protected void init()	{
		table.getSelectionModel().addListSelectionListener(this);
		ml = new InfoDataTableMouseListener(tc, table);
		table.addMouseListener(ml);
		table.addMouseMotionListener(ml);
		table.getParent().addMouseListener(ml);
		table.getParent().addMouseMotionListener(ml);

		addKeyboardActions(table, tc);

		addKeyboardAction(table, "File", KeyEvent.VK_INSERT, 0, tc);
		addKeyboardAction(table, "Folder", KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK, tc);
		addKeyboardAction((JComponent)table.getParent(), "File", KeyEvent.VK_INSERT, 0, tc);
		addKeyboardAction((JComponent)table.getParent(), "Folder", KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK, tc);
	}



	public void close()	{
		removeKeyboardActions(table);

		removeKeyboardAction(table, "File", KeyEvent.VK_INSERT, 0);
		removeKeyboardAction(table, "Folder", KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK);
		removeKeyboardAction((JComponent)table.getParent(), "File", KeyEvent.VK_INSERT, 0);
		removeKeyboardAction((JComponent)table.getParent(), "Folder", KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK);

		((InfoTableModel)model).close();

		table.close();
	}


	public BufferedTreeNode getFolder()	{
		return folder;
	}
	
	public JTable getJTable()	{
		return table;
	}
	
	public DefaultTableModel getModel()	{
		return model;
	}
	
	
	// interface ListSelectionListener
	public void valueChanged(ListSelectionEvent e)	{
		if (e.getValueIsAdjusting())
			return;
		setSelectedListLine();
	}


	public void setSelectedListLine()	{
//		frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		setSelectedListLine(
				frame,
				table,
				sorter,
				data,
				tree,
				root,
				tc,
				folder != null ? this : null,
				folder != null ? folder : null);
//		frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}


	/** Connect to treeview controller for performing actions. */
	public static void setSelectedListLine(
		JFrame frame,
		InfoDataTable table,
		TableSorter sorter,
		FileTableData data,
		JTree tree,
		BufferedTreeNode root,
		TreeEditController tc,
		NodeInserter inserter,
		BufferedTreeNode folder)
	{
		int [] iarr = table.getSelectedRows();	
		Vector v = new Vector();
		
		for (int i = 0; iarr != null && i < iarr.length; i++)	{
			File f = data.getFileAt(sorter.convertRowToModel(iarr[i]));
	 		BufferedTreeNode d = BufferedTreeNode.fileToBufferedTreeNode(f, root);
	 		if (d == null)	{
	 			System.err.println("WARNING: node not found in tree view: "+f);
	 			continue;
	 		}
	 		v.add(d);
		}
		
		if (v.size() > 0)	{	// set selection to controller
			DefaultMutableTreeNode [] nodes = new DefaultMutableTreeNode[v.size()];
			v.copyInto(nodes);
	 		tc.setDelegateSelection(frame, nodes, table, inserter, table, folder);
		}
		else	{	// set empty selection to controller
			tc.setDelegateSelection(frame, null, null, inserter, table, folder);
		}
	}


	public File [] getSelectedFileArray()	{
		return getSelectedFileArray(table, sorter, data);
	}

	public static File [] getSelectedFileArray(JTable table, TableSorter sorter, FileTableData data)	{
		int [] iarr = table.getSelectedRows();	
		File [] files = null;
		for (int i = 0; iarr != null && i < iarr.length; i++)	{
			File f = data.getFileAt(sorter.convertRowToModel(iarr[i]));
			if (files == null)
				files = new File [iarr.length];
			files[i] = f;
		}
		return files;
	}

	
	/** Calculate all folders size of zip archive.
			This gets called from background thread  */
	public Hashtable calculateZipFolderSizes()	{
		return null;
	}

	/** Notification that zip folders should be shown with their recursive size.
			This gets called from event thread */
	public boolean setZipFolderSizes(Hashtable hash, JLabel files, JLabel folders)	{
		return false;
	}
	


	/** set the ready calculated size of a folder */	
	public void setSubFolderSize(NetNode node, long size)	{
		int i = data.getIndexOf(node);
		if (i >= 0)	{
			FileTableData v = (FileTableData)getModel().getDataVector();
			model.setValueAt(Long.valueOf(size), i, v.getSizeColumn());
		}
		else
			System.err.println("setSubFolderSize, node not found: "+node);
	}



	// interface NodeInserter: insert into folder node of this window
	
	public void insertContainer()	{
		insertObject(true);
	}
	
	public void insertNode()	{
		insertObject(false);
	}

	private void insertObject(boolean isFolder)	{
		if (folder != null)
			tc.setDelegateSelection(
					frame,
					new DefaultMutableTreeNode [] { folder },
					table,
					this,
					table,
					folder);
	
		tc.insertObject(isFolder);
	
		if (folder != null)
		 	setSelectedListLine();
	}

	
	// register keystrokes

	private static void addKeyboardAction(JComponent c, String actionName, int key, int mod, ActionListener al)	{
		c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(key, mod), actionName);
		c.getActionMap().put(actionName, new ActionListenerDelegate(al, actionName));
	}

	private static void removeKeyboardAction(JComponent c, String actionName, int key, int mod)	{
		c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(
				KeyStroke.getKeyStroke(key, mod));
		c.getActionMap().remove(actionName);
	}
	
	public static void addKeyboardActions(JComponent c, ActionListener al)	{
		addKeyboardAction(c, "Clear", KeyEvent.VK_ESCAPE, 0, al);
		addKeyboardAction(c, "Popup", KeyEvent.VK_F4, 0, al);
		addKeyboardAction(c, "Popup", KeyEvent.VK_F10, InputEvent.SHIFT_MASK, al);
		addKeyboardAction(c, "Open", KeyEvent.VK_ENTER, 0, al);
		addKeyboardAction(c, "Info", KeyEvent.VK_ENTER, InputEvent.ALT_MASK, al);
		addKeyboardAction(c, "Find", KeyEvent.VK_F, InputEvent.CTRL_MASK, al);
		addKeyboardAction(c, "Find", KeyEvent.VK_F3, 0, al);
		addKeyboardAction(c, "Rename", KeyEvent.VK_F2, 0, al);
		addKeyboardAction(c, "Remove", KeyEvent.VK_DELETE, 0, al);
		addKeyboardAction(c, "Empty", KeyEvent.VK_DELETE, InputEvent.ALT_MASK, al);
		addKeyboardAction(c, "Delete", KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK, al);
		addKeyboardAction(c, "Cut", KeyEvent.VK_X, InputEvent.CTRL_MASK, al);
		addKeyboardAction(c, "Copy", KeyEvent.VK_C, InputEvent.CTRL_MASK, al);
		addKeyboardAction(c, "Paste", KeyEvent.VK_V, InputEvent.CTRL_MASK, al);
		addKeyboardAction(c, "Undo", KeyEvent.VK_Z, InputEvent.CTRL_MASK, al);
		addKeyboardAction(c, "Redo", KeyEvent.VK_Y, InputEvent.CTRL_MASK, al);
		addKeyboardAction(c, "Select All", KeyEvent.VK_A, InputEvent.CTRL_MASK, al);
	}
	
	public static void removeKeyboardActions(JComponent c)	{
		removeKeyboardAction(c, "Clear", KeyEvent.VK_ESCAPE, 0);
		removeKeyboardAction(c, "Popup", KeyEvent.VK_F4, 0);
		removeKeyboardAction(c, "Popup", KeyEvent.VK_F10, InputEvent.SHIFT_MASK);
		removeKeyboardAction(c, "Open", KeyEvent.VK_ENTER, 0);
		removeKeyboardAction(c, "Info", KeyEvent.VK_ENTER, InputEvent.ALT_MASK);
		removeKeyboardAction(c, "Find", KeyEvent.VK_F, InputEvent.CTRL_MASK);
		removeKeyboardAction(c, "Find", KeyEvent.VK_F3, 0);
		removeKeyboardAction(c, "Rename", KeyEvent.VK_F2, 0);
		removeKeyboardAction(c, "Remove", KeyEvent.VK_DELETE, 0);
		removeKeyboardAction(c, "Empty", KeyEvent.VK_DELETE, InputEvent.ALT_MASK);
		removeKeyboardAction(c, "Delete", KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK);
		removeKeyboardAction(c, "Cut", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		removeKeyboardAction(c, "Copy", KeyEvent.VK_C, InputEvent.CTRL_MASK);
		removeKeyboardAction(c, "Paste", KeyEvent.VK_V, InputEvent.CTRL_MASK);
		removeKeyboardAction(c, "Undo", KeyEvent.VK_Z, InputEvent.CTRL_MASK);
		removeKeyboardAction(c, "Redo", KeyEvent.VK_Y, InputEvent.CTRL_MASK);
		removeKeyboardAction(c, "Select All", KeyEvent.VK_A, InputEvent.CTRL_MASK);
	}

}