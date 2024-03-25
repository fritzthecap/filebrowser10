package fri.gui.swing.propdialog;

import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/**
	Properties Dialog, that lets edit names and boolean values of
	a properties map.
	By default this is a modal dialog.
*/

public class BoolPropEditDialog extends PropEditDialog
{
	/**
		Dialog that shows the passed Properties map.
		@param f parent frame
		@param props properties map
	*/
	public BoolPropEditDialog(JFrame f, Properties props)
	{
		this(f, true, props, "Boolean Properties");
	}
	
	/**
		Dialog that shows the passed Properties map.
		@param f parent frame
		@param props properties map
	*/
	public BoolPropEditDialog(JFrame f, Properties props, String title)
	{
		this(f, true, props, title);
	}

	/**
		Dialog that shows the passed Properties map.
		@param f parent frame
		@param modal dialog modality
		@param props properties map
	*/
	public BoolPropEditDialog(JFrame f, boolean modal, Properties props, String title)
	{
		super(f, modal, props, title);
	}


	protected void insertRowAt(int row)	{
		int newrow = row + 1;
		insertRowAt(
				new String((String)model.getValueAt(row, 0)),
				new Boolean(((Boolean)model.getValueAt(row, 1)).booleanValue()),
				newrow);
		DefaultListSelectionModel lm = (DefaultListSelectionModel)table.getSelectionModel();
		lm.addSelectionInterval(newrow, newrow);
	}


	protected Vector buildModelValues(Properties props, Vector names)	{
		return this.values = BoolPropViewDialog.buildTableModelVector(props, names);
	}
	
	/** Create a table model for others than String (Boolean) */
	protected TableModel createTableModel(Vector values, Vector columns)	{
		TableModel tm = new DefaultTableModel(values, columns)	{
			public Class getColumnClass(int c) {
				return getValueAt(0, c).getClass();
			}
		};
		return tm;
	}
	
	/** Overridde to store Boolean values */
	public void storeToProperties()	{
		props.clear();

		for (int i = 0; i < values.size(); i++)	{
			Vector v = (Vector)values.elementAt(i);
			String name = ((String)v.elementAt(0)).trim();
			Boolean value = (Boolean)v.elementAt(1);

			if (name.length() > 0)	{
				//System.err.println("position "+i+", name >"+name+"<  value >"+value+"<");
				props.put(name, value.booleanValue() ? "true" : "false");
			}
		}
	}
	

	// test main

	public static void main(String [] args)	{
		Properties props = new Properties();
		props.put("aaa", "true");
		props.put("ddd", "false");
		props.put("eee", "true");
		props.put("bbb", "false");
		props.put("ccc", "true");
		BoolPropEditDialog dialog = new BoolPropEditDialog(new JFrame(), props);
		dialog.show();
		props.list(System.err);
	}
	
}