package fri.gui.swing.propdialog;

import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/**
	Properties Dialog, that lets view boolean names and values of
	a properties map. It contains a sortable JTable.
	Default this is a modeless dialog.
*/

public class BoolPropViewDialog extends PropViewDialog
{
	/**
		Modeless Dialog that shows the passed Properties map.
		@param f parent frame
		@param props properties map
	*/
	public BoolPropViewDialog(JFrame f, Properties props)	{
		this(f, false, props);
	}
	
	/**
		Dialog that shows the passed Properties map.
		@param modal if this dialog should block program execution
		@param rest see above
	*/
	public BoolPropViewDialog(JFrame f, boolean modal, Properties props)	{
		this(f, modal, props, "Boolean Properties");
	}
	
	/**
		Dialog that shows the passed Properties map.
		@param title string to be shown in title bar of dialog
		@param rest see above
	*/
	public BoolPropViewDialog(JFrame f, boolean modal, Properties props, String title)	{
		super(f, modal, props, title);
	}


	/** make the 2-dimensional list of names and values for a TableModel from properties. */
	protected Vector buildModelValues(Properties props, Vector names)	{
		return this.values = buildTableModelVector(props, values);
	}
	
	/** Create a table model for others than String (Boolean) */
	protected TableModel createTableModel(Vector values, Vector columns)	{
		TableModel tm = new DefaultTableModel(values, columns)	{
			public Class getColumnClass(int c) {
				return getValueAt(0, c).getClass();
			}
			public boolean isCellEditable(int row, int col)	{
				if (col == 1)
					return false;
				return true;
			}
		};
		return tm;
	}
	
	/** do static reusable work for a table model */
	public static Vector buildTableModelVector(Properties props, Vector names)	{
		Vector values = new Vector(props.size() > 0 ? props.size() : 1);
		
		for (Enumeration e = names.elements(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			String value = props.getProperty(name).toLowerCase();
			
			if (value.equals("true") || value.equals("false"))	{
				Vector v = new Vector(2);
				v.addElement(name);
				v.addElement(new Boolean(value.equals("true") ? true : false));
				values.addElement(v);
			}
		}
		return values;
	}




	// test main

	public static void main(String [] args)	{
		Properties props = new Properties();
		props.put("Hallo", "true");
		props.put("Welt", "false");
		BoolPropViewDialog pd = new BoolPropViewDialog(new JFrame(), props);
		pd.show();
	}
}