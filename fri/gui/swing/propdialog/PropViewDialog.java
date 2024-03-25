package fri.gui.swing.propdialog;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import fri.gui.swing.table.sorter.*;
import fri.util.sort.quick.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.table.*;

/**
	Properties Dialog, that lets view and copy names and values of
	a properties map. It contains a sortable JTable.
	Default this is a modeless dialog.
*/

public class PropViewDialog extends JDialog
{
	private static final String title = "System Properties";
	/** properties to render */
	protected Properties props;
 	/** table model 2-dimensional list made of properties */
	protected Vector values;
	protected Frame frame;
	private boolean systemProps = false;
	protected TableSorter sorter;
	protected JTable table;
	protected TableModel model;
	protected Vector columns;

	/**
		Modeless Dialog that shows Java System-Properties.
		@param d parent dialog
	*/
	public PropViewDialog(JDialog d)	{
		super(d, title, false);
		this.props = System.getProperties();
		systemProps = true;
		init();
	}
	
	/**
		Modeless Dialog that shows Java System-Properties.
		@param f parent frame
	*/
	public PropViewDialog(JFrame f)	{
		this(f, false);
	}
	
	/**
		Dialog that shows Java System-Properties.
		@param modal if this dialog should block program execution
		@param rest see above
	*/
	public PropViewDialog(JFrame f, boolean modal)	{
		super(f, title, modal);
		this.props = System.getProperties();
		this.frame = f;
		systemProps = true;
		init();
	}

	/**
		Modeless Dialog that shows the passed Properties map.
		@param f parent frame
		@param props properties map
	*/
	public PropViewDialog(JFrame f, Properties props)	{
		this(f, false, props);
	}
	
	/**
		Dialog that shows the passed Properties map.
		@param modal if this dialog should block program execution
		@param rest see above
	*/
	public PropViewDialog(JFrame f, boolean modal, Properties props)	{
		this(f, modal, props, "Properties");
	}
	
	/**
		Dialog that shows the passed Properties map.
		@param title string to be shown in title bar of dialog
		@param rest see above
	*/
	public PropViewDialog(JFrame f, boolean modal, Properties props, String title)	{
		super(f, title, modal);
		this.props = props;
		this.frame = f;
		init();
	}

	/**
		Dialog that shows the passed Properties map.
		@param d dialog parent
		@param title string to be shown in title bar of dialog
		@param rest see above
	*/
	public PropViewDialog(JDialog d, boolean modal, Properties props, String title)	{
		super(d, title, modal);
		this.props = props;
		//this.frame = f;
		init();
	}


	/** build the GUI and add close callback. */	
	private void init()	{
		buildGUI();

		if (frame == null)	{
			Object o = this;
			do	{
				o = ((Component)o).getParent();
			}
			while (o instanceof JDialog);
			frame = (Frame)o;
		}

		new GeometryManager(this).pack();

		addWindowListener (new WindowAdapter () {
			public void windowClosing(WindowEvent ev) {
				close();
			}
		});
	}

	/** called when closing window by system close */
	protected void close()	{
		//System.err.println("close in PropViewDialog");
		PersistentColumnsTable.store(table, getClass());
		dispose();
	}

	/** editor should allow clipboard copy, but dey changes in text */
	protected void setUneditableEditor()	{
		DefaultCellEditor ded = (DefaultCellEditor)table.getDefaultEditor(String.class);
		JTextField edi = (JTextField)ded.getComponent();
		edi.setEditable(false);
	}

	/** add a JScrollPane holding a JTable to center of dialog */
	protected Container buildGUI()	{
		JScrollPane sp = buildPanel();
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(sp, BorderLayout.CENTER);
		return c;
	}

	/** build the JScrollPane that holds the JTable */	
	protected JScrollPane buildPanel()	{
		Vector names = buildNames(props);
		this.values = buildModelValues(props, names);

		columns = new Vector(2);
		columns.addElement(getColumn1Name());
		columns.addElement(getColumn2Name());

		model = createTableModel(values, columns);
		sorter = new TableSorter(model, this);
		table = createTable(sorter);
		sorter.addMouseListenerToHeaderInTable(table);

		setUneditableEditor();	// allow copy, deny editing

		int cnt = Math.min(model.getRowCount() + 1, 30);
		int h = cnt * table.getRowHeight();
		table.setPreferredScrollableViewportSize(new Dimension(/*table.getPreferredSize().width*/ 400, h));

		PersistentColumnsTable.load(table, getClass());
		JScrollPane sp = new JScrollPane(table);
		return sp;
	}


	protected JTable createTable(TableModel tm)	{
		return new JTable(tm);
	}
	
	protected String getColumn1Name()	{
		return "Name";
	}
	protected String getColumn2Name()	{
		return "Value";
	}

	protected TableModel createTableModel(Vector values, Vector columns)	{
		return new DefaultTableModel(values, columns);
	}

	
	/** make the list of names from properties, sort it. */
	protected Vector buildNames(Properties props)	{
		Vector names = new Vector(props.size() > 0 ? props.size() : 1);
		for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			names.addElement(name);
		}
		//System.err.println("names = "+names);
		// sort the names
		QSort sort = new QSort();
		return sort.sort(names);
	}

	/** make the 2-dimensional list of names and values for a TableModel from properties. */
	protected Vector buildModelValues(Properties props, Vector names)	{
		Vector values = new Vector(props.size() > 0 ? props.size() : 1);
		for (Enumeration e = names.elements(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			Vector v = new Vector(2);
			v.addElement(name);
			if (systemProps && name.equals("line.separator"))	{
				v.addElement(getHexString(props.getProperty(name)));
			}
			else	{
				v.addElement(props.getProperty(name));
			}
			values.addElement(v);
		}
		return this.values = values;
	}
	
	
	/**
		Convert a String to its hexadecimal representation.
		@param name input string
		@return name converted to hex-number sequence
	*/
	public static String getHexString(String name)	{
		StringBuffer buf = new StringBuffer(name);
		String hexa = "";
		for (int i = 0; i < buf.length(); i++)	{
			String zahl = Integer.toHexString(buf.charAt(i)).toUpperCase();
			while (zahl.length() < 2)	{
				zahl = "0"+zahl;
			}
			zahl = "0x"+zahl;
			if (hexa.equals(""))
				hexa = zahl;
			else
				hexa = hexa + ", " + zahl;
		}
		return hexa;
	}




	// test main

	public static void main(String [] args)	{
		PropViewDialog pd = new PropViewDialog(new JFrame());
		pd.show();
	}
}