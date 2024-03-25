package fri.gui.swing.table;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import fri.util.props.*;
import fri.util.text.TextUtil;

/**
	Make persistent the column widths of a JTable.
*/

public abstract class PersistentColumnsTable
{
	private PersistentColumnsTable()	{}
	
	
	/**
		Load column widths from memory or file if this properties has never been read.
		The properties file is read from table class package.
		@param table table which columns are to size
	*/
	public static boolean load(JTable table)	{
		return load(table, table.getClass());
	}
	
	/**
		Load column widths from memory or file if this properties has never been read.
		@param table table which columns are to size
		@param res resource class for reading the properties file if never loaded before.
	*/
	public static boolean load(JTable table, Class res)	{
		Properties props = ClassProperties.getProperties(res);
		if (props.size() <= 0)
			return false;
			
		TableColumnModel cm = table.getColumnModel();
		for (int i = 0; i < cm.getColumnCount(); i++)	{
			String id = getColumnKey(cm, i);
			String width = props.getProperty(id);
			int w;

			try	{
				w = Integer.valueOf(width).intValue();
				//System.err.println("load column width for: "+id+" = "+w);
			}
			catch (NumberFormatException e)	{
//				// be compatible with old implementation that worked with numbers
//				System.err.println("could not load column width for: "+id+", trying numeric key ...");
//				id = "column"+make2CharsWide(i);
//				width = props.getProperty(id);
//				try	{
//					w = Integer.valueOf(width).intValue();
//				}
//				catch (NumberFormatException e2)	{

					//e.printStackTrace();
					System.err.println("WARNING: could not load column width for: "+id);
					continue;
//				}
			}

			TableColumn column = cm.getColumn(i);
			column.setPreferredWidth(w);
		}
		return true;
	}

	
	/**
		Store to memory a JTable's column widths.
	*/
	public static void remember(JTable table)	{
		remember(table, table.getClass());
	}
	
	/**
		Store to memory a JTable's column widths from another class than that of the table.
	*/
	public static void remember(JTable table, Class res)	{		
		TableColumnModel cm = table.getColumnModel();
		for (int i = 0; i < cm.getColumnCount(); i++)	{
			int width = cm.getColumn(i).getWidth();
			String col = getColumnKey(cm, i);
			ClassProperties.put(res, col, Integer.toString(width));
		}
	}


	public static String getColumnKey(TableColumnModel cm, int i)	{
		Object o = cm.getColumn(i).getHeaderValue();
		String id = "";
		if (o != null)	{
			id = TextUtil.makeIdentifier(o.toString());
		}
		if (id.length() <= 0)	{
			id = make2CharsWide(i);
		}
		return "column"+id;
	}

	private static String make2CharsWide(int i)	{
		String idx = Integer.toString(i);
		if (idx.length() <= 9)	// for JTable maximum 99 columns is OK
			idx = "0"+idx;
		return idx;
	}


	/**
		Store to disk file the column properties for passed table and use table
		package for location properties file.
		@param table table which columns have to get stored to disk
	*/
	public static void store(JTable table)	{
		store(table, table.getClass());
	}

	/**
		Store to disk file the column properties for passed table and use table
		package for location properties file.
		@param table table which columns have to get stored to disk
		@param res class to locate the properties file.
	*/
	public static void store(JTable table, Class res)	{
		remember(table, res);
		ClassProperties.store(res);
	}



	// test main
	
	public static final void main(String [] args)	{
		TableModel model = new AbstractTableModel() {
			public int getColumnCount() { return 10; }
			public int getRowCount() { return 10;}
			public Object getValueAt(int row, int col) { return new Integer(row*col); }
			public boolean isCellEditable(int row, int col)	{ return true; }
		};
		final JTable table = new JTable(model);
		PersistentColumnsTable.load(table);
		
		JFrame f = new JFrame();
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
		f.addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				PersistentColumnsTable.store(table);
				System.exit(0);
			}
		});
		f.pack();
		f.show();
	}
	
}