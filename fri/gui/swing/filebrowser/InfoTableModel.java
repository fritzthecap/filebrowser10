package fri.gui.swing.filebrowser;

import javax.swing.table.*;
import java.util.Vector;
import java.awt.Toolkit;
import fri.util.FileUtil;

/**
	Responsibilities:
		enable column class rendering,
		get columns from data,
		decide which column is editable.
		Listen for changes in filesystem.
*/

public class InfoTableModel extends DefaultTableModel
{
	public static final int NO_PATH = 0;	// for InfoFrame with exactly one parent node
	public static final int RELATIVE_PATH = 1;	// for InfoFrame with different parents
	public static final int ABSOLUTE_PATH = 2;	// for SearchFrame
	private int whichPath = NO_PATH;
	private String parentPath = null;
	private FileTableData data = null;
	protected Vector lsnrs = null;
	private InfoRenderer renderer;
	private TreeEditController tc;

	
	public InfoTableModel(FileTableData data, InfoRenderer renderer)	{
		super(data, data.getColumns());
		this.renderer = renderer;
		this.data = data;
		for (int i = 0; i < data.size(); i++)	{
			NetNode n = (NetNode)data.getObjectAt(i);
			addTableNodeListenerAt(i, n);
		}
	}
	
	
	public void setTreeEditController(TreeEditController tc)	{
		this.tc = tc;
	}
	
	
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}


	public boolean isCellEditable(int row, int col)	{
		FileTableData v = (FileTableData)getDataVector();
		
		if (col == v.getNameColumn())	//InfoData.NAME_COLUMN)
			return true;
		if (col == v.getTimeColumn())	//InfoData.TIME_COLUMN)
			return true;
		return false;
	}

	
	/** rename a node */
	public void setValueAt(Object o, int row, int col)	{
		FileTableData v = (FileTableData)getDataVector();
		
		if (col == v.getNameColumn() && tc != null)	{
			//System.err.println("InfoTableModel.setValue "+o);
			String newname = FileUtil.separateFile((String)o);
			String oldname = FileUtil.separateFile((String)getValueAt(row, col));
			NetNode n = (NetNode)data.getObjectAt(row);
			
			if ((n = tc.finishRename(n, newname, oldname)) != null)	{
				super.setValueAt(o, row, col);
				data.setObjectAt(row, n);
			}
			else	{
				Toolkit.getDefaultToolkit().beep();
			}
		}
		else
		if (col == v.getTimeColumn() && tc != null)	{
			String newtime = (String)o;
			NetNode n = (NetNode)data.getObjectAt(row);
			Long time = tc.setTime(n, newtime);
			
			if (time != null)	{
				super.setValueAt(time, row, col);
			}
			else	{
				Toolkit.getDefaultToolkit().beep();
			}
		}
		else	{
			super.setValueAt(o, row, col);
		}
	}
	
	
	public void setWhichPath(int whichPath)	{
		this.whichPath = whichPath;
	}
	public void setWhichPath(int whichPath, String parentPath)	{
		setWhichPath(whichPath);
		this.parentPath = parentPath;
	}
	
	
	public FileTableData getData()	{
		return data;
	}
	
	public void close()	{
		clear();
	}


	private String pathRenderString(NetNode n)	{
		return pathRenderString(n, whichPath, parentPath);
	}
	
	public static String pathRenderString(NetNode n, int whichPath, String parentPath)	{
		String s = "";
		switch (whichPath)	{
			case NO_PATH:
				s = n.getLabel();
				break;
			case RELATIVE_PATH:
				s = FileUtil.makeRelativePath(parentPath, n.getFullText());
				break;
			case ABSOLUTE_PATH:
				s = n.getFullText();
				break;
		}
		//System.err.println("pathRenderString "+s);
		return s;
	}
	
	
	public void addRow(NetNode n)	{
		Vector row = InfoTableModel.createRow(n, data, whichPath, parentPath);
		addTableNodeListenerAt(-1, n);	// add to end
		super.addRow(row);
		//System.err.println("addRow, listeners: "+lsnrs.size()+", data size: "+data.size());
	}


	public static Vector createRow(NetNode n, FileTableData data, int whichPath, String parentPath)	{
		String file = InfoTableModel.pathRenderString(n, whichPath, parentPath);
		String name = whichPath == InfoTableModel.NO_PATH ? file : n.isLeaf() ? data.getName(file) : "";
		String path = whichPath == InfoTableModel.NO_PATH ? null : n.isLeaf() ? data.getPath(file) : file;
		
		Vector row = data.buildRow(
			n,
			n.isLeaf() == false,
			name,
			n.getType(),
			path,
			new Long(n.getSize()),
			new Long(n.getModified()),
			n.getReadWriteAccess());
			
		return row;
	}


	public void removeRow(int i)	{
		super.removeRow(i);
		removeTableNodeListenerAt(i);
		data.removeRow(i);
	}

	public void clear()	{
		for (int i = data.size() - 1; i >= 0; i--)	{
			removeRow(i);
		}
		data.init();
	}

	public void addTableNodeListenerAt(int i, NetNode n)	{
		if (lsnrs == null)
			lsnrs = new Vector();	
		TableNodeListener tl = new TableNodeListener(n);
		n.addNetNodeListener(tl);
		if (i < 0 || lsnrs.size() <= i)
			lsnrs.add(tl);
		else
			lsnrs.insertElementAt(tl, i);
	}

	public void removeTableNodeListenerAt(int i)	{
		if (lsnrs == null)
			return;
		NetNode n = (NetNode)data.getObjectAt(i);
		TableNodeListener tl = (TableNodeListener)lsnrs.elementAt(i);
		n.removeNetNodeListener(tl);
		lsnrs.remove(i);
		//System.err.println("table node listeners: "+lsnrs.size()+", data size: "+data.size());
	}

	
	
	
	class TableNodeListener implements NetNodeListener
	{
		private NetNode node;
		
		
		public TableNodeListener(NetNode n)	{
			this.node = n;
		}
		
		public void childrenRefreshed(Vector list)	{
		}
			
		public void nodeRenamed()	{
			String newName = pathRenderString(node);
			int i = data.getIndexOf(node);
			TreeEditController t = tc;
			tc = null;
			FileTableData v = (FileTableData)getDataVector();
			setValueAt(newName, i, v.getNameColumn());
			tc = t;
		}
	
		public void nodeDeleted()	{
			int i = data.getIndexOf(node);
			if (i >= 0)
				removeRow(i);
			else
				System.err.println("deleted node not found: "+node);
		}
		
		public void nodeInserted(Object child)	{
		}
		
		public void movePending()	{
			renderer.getTable().revalidate();
			renderer.getTable().repaint();
		}	
	}

}