package fri.gui.swing.propdialog;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import fri.util.sort.quick.*;
import fri.gui.swing.text.ClipableJTextField;

/**
	Properties Dialog, that lets edit names and values of
	a properties map.
	By default this is a modal dialog.
	A popup menu gives the possibility to add and delete rows.
*/

public class PropEditDialog extends PropViewDialog implements
	ActionListener,
	MouseListener,
	KeyListener
{
	protected JButton ok, can;
	private boolean canceled = true;
	private JPopupMenu popup;
	private JMenuItem delete, insert;


	/**
		Dialog that shows the passed Properties map.
		@param f parent frame
		@param props properties map
	*/
	public PropEditDialog(JFrame f, Properties props)
	{
		super(f, true, props);
		init();
	}
	
	/**
		Dialog that shows the passed Properties map.
		@param f parent frame
		@param props properties map
	*/
	public PropEditDialog(JFrame f, Properties props, String title)
	{
		super(f, true, props, title);
		init();
	}

	/**
		Dialog that shows the passed Properties map.
		@param d parent dialog
		@param props properties map
	*/
	public PropEditDialog(JDialog d, Properties props, String title)
	{
		super(d, true, props, title);
		init();
	}

	/**
		Dialog that shows the passed Properties map.
		@param f parent frame
		@param modal dialog modality
		@param props properties map
	*/
	public PropEditDialog(JFrame f, boolean modal, Properties props, String title)
	{
		super(f, modal, props, title);
		init();
	}

	private void init()	{
		// build popup to insert/delete rows
		popup = new JPopupMenu();
		popup.add(insert = new JMenuItem("Insert"));
		insert.addActionListener(this);
		popup.add(delete = new JMenuItem("Delete"));
		delete.addActionListener(this);		
		// watch for popup-trigger
		table.addMouseListener(this);
		table.getParent().addMouseListener(this);
		table.addKeyListener(this);
		addWindowListener(new WindowAdapter()	{
			public void windowOpened(WindowEvent e)	{
				ok.requestFocus();
			}
		});
	}


	/** do nothing to avoid uneditable textfield from PropViewDialog */
	protected void setUneditableEditor()	{
	}

	protected Container buildGUI()	{
		Container c = super.buildGUI();

		table.setDefaultEditor(String.class, new DefaultCellEditor((JTextField)new ClipableJTextField()));

// FRi: delete/insert key cannot be caught when first click starts editing
//		DefaultCellEditor ded = (DefaultCellEditor)table.getDefaultEditor(String.class);
//		ded.setClickCountToStart(1);
		
		c.add(buildButtonPanel(), BorderLayout.SOUTH);
	
		if (model.getRowCount() <= 0)
			insertRowAt("", "", 0);

		return c;
	}

	protected JPanel buildButtonPanel()	{
		JPanel panel = new JPanel();
		panel.add(ok = new JButton("Ok"));
		ok.addActionListener(this);
		panel.add(can = new JButton("Cancel"));
		can.addActionListener(this);
		return panel;
	}
			

//	public void setProperties(Properties props)	{
//		this.props = props;
//		Vector names = buildNames(props);
//		this.values = buildModelValues(props, names);
//		this.model = new DefaultTableModel(values, columns);
//		this.sorter.setModel(model);
//	}
	
	public Properties getProperties()	{
		storeToProperties();
		return this.props;
	}
	
	public boolean isCanceled()	{
		return canceled;
	}


	// interface ActionListener

	public void actionPerformed(ActionEvent e)	{
		//System.err.println("PropEditDialog.actionPerformed "+e.getActionCommand());
		if (e.getSource() == ok)	{
			canceled = false;
			commitTable();				
			storeToProperties();
			close();
			//dispose();
		}
		else
		if (e.getSource() == can)	{
			close();
			//dispose();
		}
		else
		if (e.getSource() == insert)	{
			insertRowAtSelections();
		}
		else
		if (e.getSource() == delete)	{
			removeSelectedRows();
		}
	}

	protected void commitTable()	{
		DefaultCellEditor edi = (DefaultCellEditor)table.getCellEditor();
		if (edi != null)
			edi.stopCellEditing();
		else
			table.editingStopped(null);
	}
	
	
	public void storeToProperties()	{
		props.clear();

		for (int i = 0; i < values.size(); i++)	{
			Vector v = (Vector)values.elementAt(i);
			String name = ((String)v.elementAt(0)).trim();
			String value = ((String)v.elementAt(1)).trim();

			if (name.length() > 0)	{
				//System.err.println("position "+i+", name >"+name+"<  value >"+value+"<");
				props.put(name, value);
			}
		}
	}

	// popup callbacks
	private void insertRowAtSelections()	{
		Integer [] Iarr = getSelectedAsSortedArray();
		if (Iarr != null)	{
			table.getSelectionModel().clearSelection();
			// clone and insert all selected rows
			for (int i = Iarr.length - 1; i >= 0; i--)	{
				//System.err.println("inserting at "+Iarr[i].intValue());
				insertRowAt(Iarr[i].intValue());
			}
			// if exactly one row inserted, edit it
			if (Iarr.length == 1)
				table.editCellAt(Iarr[0].intValue() + 1, 0);
		}
		else	{	// insert row at end
			insertRowAt("", "", model.getRowCount() - 1);
		}
	}

	protected void insertRowAt(int row)	{
		int newrow = row + 1;
		insertRowAt(
				new String((String)model.getValueAt(row, 0)),
				new String((String)model.getValueAt(row, 1)),
				newrow);
		DefaultListSelectionModel lm = (DefaultListSelectionModel)table.getSelectionModel();
		lm.addSelectionInterval(newrow, newrow);
	}

	protected void insertRowAt(Object name, Object value, int index)	{
		System.err.println("insertRowAt "+index+": "+name+" "+value);
		Vector row = new Vector(2);
		row.addElement(name);
		row.addElement(value);
		((DefaultTableModel)model).insertRow(Math.max(index, 0), row);
	}
	
	private void removeSelectedRows()	{
		Integer [] Iarr = getSelectedAsSortedArray();
		if (Iarr != null)	{
			for (int i = Iarr.length - 1; i >= 0; i--)
				((DefaultTableModel)model).removeRow(Iarr[i].intValue());
		}
		if (model.getRowCount() <= 0)
			insertRowAt("", "", 0);
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


	// interface MouseListener
	
	public void mousePressed(MouseEvent e)	{
		if (e.isPopupTrigger())	{
			showPopup(e);
		}
	}
	public void mouseEntered(MouseEvent e)	{
	}
	public void mouseExited(MouseEvent e)	{
	}
	public void mouseClicked(MouseEvent e)	{
	}
	public void mouseReleased(MouseEvent e)	{
		if (e.isPopupTrigger())	{
			showPopup(e);
		}
	}

	private void showPopup(MouseEvent e)	{
		if (table.getSelectedRowCount() <= 0)	{	// set selection if not set
			int row = table.rowAtPoint(e.getPoint());
			DefaultListSelectionModel lm = (DefaultListSelectionModel)table.getSelectionModel();
			lm.setSelectionInterval(row, row);
		}
		popup.show(e.getComponent(), e.getX(), e.getY());
	}
	

	// interface KeyListener
	
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_DELETE)	{
			removeSelectedRows();
		}
		else
		if (e.getKeyCode() == KeyEvent.VK_INSERT)	{
			insertRowAtSelections();
		}
	}
	public void keyReleased(KeyEvent e)	{
	}
	public void keyTyped(KeyEvent e)	{
	}




	// test main

	public static void main(String [] args)	{
		Properties props = new Properties();
		props.put("aaa", "AAA");
		props.put("ddd", "DDD");
		props.put("eee", "EEE");
		props.put("bbb", "BBB");
		props.put("ccc", "CCC");
		PropEditDialog dialog = new PropEditDialog(new JFrame(), props);
		dialog.show();
	}
	
}