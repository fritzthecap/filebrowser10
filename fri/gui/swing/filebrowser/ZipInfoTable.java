package fri.gui.swing.filebrowser;

import java.io.File;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import fri.util.NumberUtil;
import fri.util.os.OS;
import fri.gui.CursorUtil;
import fri.util.FileUtil;

/**
	Target:
		<br>a scrollpane that delegates to a JTable.<br>
	Responsibilites:<br>
		Render contents of an archive file, let extract.
*/

public class ZipInfoTable extends InfoTable implements
	MouseListener,
	ActionListener
{
	private JPopupMenu popup;
	private JMenuItem extract, extractAll, open;
	private ZipInfoData zipdata;
	private ZipInfoTableDndListener dndLsnr;
	private Point currentPoint;

	/**
		Create a table to render contents of passed file or folder.
		@param filename folder or file to render.
	*/
	public ZipInfoTable(
		InfoRenderer frame,
		TreeEditController tc,
		NetNode node,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
	{
		super(frame, tc, new NetNode [] { node }, filter, include, showfiles, showhidden);
		zipdata = (ZipInfoData)data;
	}
	

	protected FileTableData initData(
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
	{
		return new ZipInfoData(frame, nodes[0], filter, include, showfiles, showhidden);
	}

	/** Return a model where no column is editable. */
	protected DefaultTableModel initModel(FileTableData data)	{
		return new ZipInfoTableModel(data);
	}


	
	/** Init popup menu and add listeners. */
	protected void init()	{
		table.addMouseListener(this);
		popup = new JPopupMenu();
		popup.add(extract = new JMenuItem("Extract"));
		extract.addActionListener(this);
		popup.add(extractAll = new JMenuItem("Extract All"));
		extractAll.addActionListener(this);
		popup.addSeparator();
		popup.add(open = new JMenuItem("Open"));
		open.addActionListener(this);
		// drag and drop to filesystem
		dndLsnr = new ZipInfoTableDndListener(table, sorter, data);
	}


	/** Closing file handle of opened zip file */
	public void close()	{
		zipdata.close();
		table.close();
	}

	
	/** Calculate all folders size.
			This gets called from background thread  */
	public Hashtable calculateZipFolderSizes()	{
		System.err.println("calculateZipFolderSizes");
		int anz = getModel().getRowCount();
		Hashtable sizeHash = new Hashtable();
		FileTableData v = (FileTableData)getModel().getDataVector();
		
		for (int i = 0; i < anz; i++)	{
			String type = (String)getModel().getValueAt(i, v.getTypeColumn());
			
			if (type.toLowerCase().indexOf("folder") >= 0)	{
				String n = (String)getModel().getValueAt(i, v.getNameColumn());
				long size = zipdata.calculateZipFolderSize(i);
				sizeHash.put(n, Long.valueOf(size));
			}
		}
		return sizeHash;
	}

	/** Notification that zip folders should be shown with their recursive size.
			This gets called from event thread */
	public boolean setZipFolderSizes(Hashtable hash, JLabel files, JLabel folders)	{
		//System.err.println("setZipFolderSizes: ");
		int folderCnt = 0, fileCnt = 0;
		FileTableData v = (FileTableData)getModel().getDataVector();
		
		for (int i = 0; i < getModel().getRowCount(); i++)	{
			String type = (String)getModel().getValueAt(i, v.getTypeColumn());
			
			if (type.toLowerCase().indexOf("folder") >= 0)	{
				folderCnt++;
				String n = (String)getModel().getValueAt(i, v.getNameColumn());
				Long l = (Long)hash.get(n);
				
				if (l != null && l.longValue() > 0)	{
					getModel().setValueAt(l, i, v.getSizeColumn());
				}
			}
			else
				fileCnt++;
		}
		//System.err.println("  Files: "+fileCnt+", Folders: "+folderCnt);
		files.setText(NumberUtil.printNumber(fileCnt));
		folders.setText(NumberUtil.printNumber(folderCnt));
		return true;
	}
	
		
	// interface MouseListener
	
	public void mousePressed (MouseEvent e)	{
		//int i = table.rowAtPoint(e.getPoint());
		//table.setRowSelectionInterval(i, i);
		if (e.isPopupTrigger())	{
			doPopup(e);
		}
	}
	public void mouseEntered (MouseEvent e)	{
	}
	public void mouseExited (MouseEvent e)	{
	}
	public void mouseClicked (MouseEvent e)	{
		if (e.isPopupTrigger() == false && e.getClickCount() >= 2)	{
			openSelectedNodes(e.getPoint());
		}
	}
	public void mouseReleased (MouseEvent e)	{
		if (e.isPopupTrigger())	{
			doPopup(e);
		}
	}

	private void doPopup(MouseEvent e)	{
		currentPoint = e.getPoint();
		extract.setEnabled(table.getSelectedRowCount() > 0);
		popup.show(e.getComponent(), e.getX(), e.getY());
	}



	private void openSelectedNodes(Point p)	{
		CursorUtil.setWaitCursor(frame);
		
		try	{
			Vector v = dndLsnr.extractSelectedToTempPath();
			if (v == null)	{
				return;
			}
			
			NetNode [] narr = new NetNode[v.size()];
	
			for (int i = 0; i < v.size(); i++)	{
				File f = (File)v.get(i);
				// BufferedTreeNode node = BufferedTreeNode.fileToBufferedTreeNode(f, tc.getRoot());
				// narr[i] = (NetNode)node.getUserObject();	// immer wieder NullPointerExceptions!
	
				narr[i] = NodeLocate.fileToNetNode(
						(NetNode)tc.getRoot().getUserObject(),
						FileUtil.getPathComponents(f, OS.isWindows));
			}
			
			new OpenLauncher(frame, tc, narr, tc.getOpenCommands(), p, table);
		}
		finally	{
			CursorUtil.resetWaitCursor(frame);
		}
	}



	// interface ActionListener, Popup-Menu
	
	public void actionPerformed(ActionEvent e)	{
		CursorUtil.setWaitCursor(frame);
		try	{
			if (e.getSource() == open)	{
				openSelectedNodes(currentPoint);
			}
			else
			if (e.getSource() == extract || e.getSource() == extractAll)	{
				// estimate suggested extract target folder
				NetNode current = tc.getSelectedContainerNode();
				if (current == null)
					current = nodes[0].getParent();
					
				// show target folder dialog
				File [] files = FileChooser.showDirectoryDialog(
						"Extract",
						frame,
						tc.getRootNetNode(),
						(File)current.getObject(),
						true);
						
				// if not canceled, extract
				if (files != null) {
					String extractPath = files[0].getPath();			
					System.err.println("extracting to "+extractPath);
					
					zipdata.error = null;
					
					if (e.getSource() == extract)	{	// extract selected
						int [] iarr = dndLsnr.getSelectedConvertedRowIndexes();
	
						if (iarr != null)
							zipdata.extractEntries(iarr, extractPath);
					}
					else
					if (e.getSource() == extractAll)	{	// extract all
						zipdata.extractAll(extractPath);
					}
	
					if (zipdata.error != null)	{
						JOptionPane.showMessageDialog(
								frame,
								zipdata.error,
								"Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(frame);
		}
	}


	public void setSelectedListLine()	{
	}
	
}