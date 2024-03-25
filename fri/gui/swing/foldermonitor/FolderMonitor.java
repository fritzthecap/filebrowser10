package fri.gui.swing.foldermonitor;

import java.io.File;
import java.util.*;
import java.text.DateFormat;
import java.awt.EventQueue;
import javax.swing.*;
import javax.swing.table.*;
import fri.util.application.Closeable;
import fri.gui.swing.table.*;
import fri.util.NumberUtil;

/**
	Watch changes in folders.
	<p>
	TODO: Actions: Open - Drag&Drop - Suspend - Resume - Delete all created - Delete selected
	 Service: Show size sum - View - Edit
*/

public class FolderMonitor extends JScrollPane implements
	EventRenderer,
	Closeable
{
	private static DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
	private JTable table;
	private WatchThread thread;


	public FolderMonitor()	{
		this((File)null);
	}

	public FolderMonitor(File root)	{
		this(root == null ? null : new File [] { root });
	}

	public FolderMonitor(File [] roots)	{
		table = new JTable(new Vector(), Constants.columns);
		initColumnWidth();

		setViewportView(table);

		setRoots(roots);
	}


	/** Stops thread. Clears table. Starts thread with new roots to watch. */
	public void setRoots(File [] roots)	{
		setRoots(roots, 2000);
	}
	
	JTable getTable()	{	// for installing key listeners
		return table;
	}
	
	public void setRoots(File [] roots, int pauseMillis)	{
		if (roots != null && roots.length > 0)	{
			if (thread != null)
				thread.setStopped();

			clear();

			thread = new WatchThread(roots, this, pauseMillis);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
	}

	/** Implements Closeable: stops thread. */
	public boolean close()	{
		if (thread != null)	{
			thread.setStopped();
			thread = null;
		}
		PersistentColumnsTable.store(table, FolderMonitor.class);
		return true;
	}

	/** Suspends execution of watcher thread. */
	public void setSuspended(boolean suspended)	{
		if (thread != null)
			thread.setSuspended(suspended);
	}

	/** Clears table (does not suspend or stop thread). */
	public void clear()	{
		PersistentColumnsTable.remember(table, FolderMonitor.class);
		TableModel model = new UneditableTableModel(new Vector(), Constants.columns);
		table.setModel(model);
		table.setDefaultRenderer(String.class, new FolderMonitorTableCellRenderer());
		initColumnWidth();
		table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}

	/** Implements EventRenderer: adds a row to table (in Swing event thread). */
	public void event(Date time, String change, String name, String path, String type, long size)	{
		final Object [] row = new Object [Constants.columns.size()];
		row[Constants.columns.indexOf(Constants.TIME)] = formatter.format(time);
		row[Constants.columns.indexOf(Constants.FILETYPE)] = type;
		row[Constants.columns.indexOf(Constants.CHANGE)] = change;
		row[Constants.columns.indexOf(Constants.NAME)] = name;
		row[Constants.columns.indexOf(Constants.PATH)] = path;
		row[Constants.columns.indexOf(Constants.SIZE)] = NumberUtil.getFileSizeString(size);
		
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				((DefaultTableModel)table.getModel()).addRow(row);
			}
		});
	}
	

	private void initColumnWidth()	{
		if (PersistentColumnsTable.load(table, FolderMonitor.class) == false)	{
			TableColumn column;
			column = table.getColumnModel().getColumn(Constants.columns.indexOf(Constants.TIME));
			column.setPreferredWidth(120);
			column = table.getColumnModel().getColumn(Constants.columns.indexOf(Constants.CHANGE));
			column.setPreferredWidth(50);
			column = table.getColumnModel().getColumn(Constants.columns.indexOf(Constants.FILETYPE));
			column.setPreferredWidth(20);
			column.setMaxWidth(20);
			column = table.getColumnModel().getColumn(Constants.columns.indexOf(Constants.NAME));
			column.setPreferredWidth(140);
			column = table.getColumnModel().getColumn(Constants.columns.indexOf(Constants.PATH));
			column.setPreferredWidth(370);
			column = table.getColumnModel().getColumn(Constants.columns.indexOf(Constants.SIZE));
			column.setPreferredWidth(50);
		}
	}



	public static void main(String [] args)	{
		File file = args.length > 0 ? new File(args[0]) : new File(System.getProperty("user.dir"));
		JFrame f = new JFrame("Folder Monitor - "+file);
		final FolderMonitor p = new FolderMonitor();
		f.addWindowListener(new java.awt.event.WindowAdapter()	{
			public void windowClosing(java.awt.event.WindowEvent e)	{
				p.close();
			}
		});
		f.getContentPane().add(p);
		f.setSize(650, 500);
		f.setVisible(true);
		p.setRoots(new File [] { file });	// do this now as columns would have no width else
	}

}