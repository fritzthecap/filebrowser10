package fri.gui.swing.propertiestextfield;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import fri.util.props.PropertiesList;
import fri.gui.swing.ComponentUtil;

/**
	Editor for JTable that lets set name-value pairs.
*/

public class PropertiesTableCellEditor extends DefaultCellEditor implements
	KeyListener
{
	protected Object value = null;
	protected PropertiesTextField tf;
	private boolean listening = false;
	

	/** Call this() with a new PropertiesTextField. */
	public PropertiesTableCellEditor()	{
		this(new PropertiesTextField());
	}

	/** Call super() with passed PropertiesTextField derivate. */
	public PropertiesTableCellEditor(PropertiesTextField propertiesTextField)	{
		super(propertiesTextField);
		
		tf = (PropertiesTextField)editorComponent;
		tf.removeActionListener(delegate);

		setClickCountToStart(1);
		
		delegate = new EditorDelegate() {
			public void setValue(Object value) {
				// alle anderen Datentypen sollen hier auffliegen
				tf.setText((PropertiesList)(this.value = value));
				
				tf.getEditor().selectAll();
				
				if (listening == false && tf.getTextEditor() != null)	{
					listening = true;
					tf.getTextEditor().addKeyListener(PropertiesTableCellEditor.this);
				}

				ComponentUtil.requestFocus(tf.getTextEditor());
			}
			public boolean shouldSelectCell(EventObject anEvent) { 
				if (anEvent instanceof MouseEvent) { 
					MouseEvent e = (MouseEvent)anEvent;
					return e.getID() != MouseEvent.MOUSE_DRAGGED;
				}
				return true;
			}
		};
		tf.addActionListener(delegate);
	}


	/** Overridden to set textfield and combo background color. */
	public Component getTableCellEditorComponent(
		JTable table,
		Object value,
		boolean selected,
		int row,
		int column)
	{
		Component c = super.getTableCellEditorComponent(table, value, selected, row, column);
		tf.setBackground(table.getBackground());
		tf.setForeground(table.getForeground());
		tf.setFont(table.getFont());
		return c;
	}

	
	/** Overridden to save text from textfield. Calls super.stopCellEditing() */
	public boolean stopCellEditing() {
		value = tf.getText();
		return super.stopCellEditing(); 
	}


	/** Overridden to return value from textfield. */
	public Object getCellEditorValue() {
		return value;
	}


	// implements KeyListener

	/** implements KeyListener to end GUI at escape or enter. */
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
			cancelCellEditing();
		}
	}
	public void keyReleased(KeyEvent e)	{}
	public void keyTyped(KeyEvent e)	{}



	/* test main */
	public static final void main(String [] args)	{
		Properties props = new Properties();
		props.setProperty("Hallo", "Welt");
		props.setProperty("Hello", "World");
		final PropertiesList propList1 = new PropertiesList(props);
		props = new Properties();
		props.setProperty("Welt", "Hallo");
		props.setProperty("World", "Hello");
		final PropertiesList propList2 = new PropertiesList(props);
		
		Object [][] oarr = new Object [1][1];
		oarr[0] = new Object [] { propList1, propList2 };
		String [] cols = new String [] { "eins", "zwei" };
		TableModel model = new DefaultTableModel(oarr, cols) {
			public Class getColumnClass(int col) { return PropertiesList.class; }
		};

		JTable table = new JTable();
		table.setModel(model);
		table.setDefaultEditor(PropertiesList.class, new PropertiesTableCellEditor());

		JFrame f = new JFrame("PropertiesTableCellEditor");
		f.addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				System.err.println(propList1.toString());
				System.err.println(propList2.toString());
				System.exit(1);
			}
		});
		//f.getContentPane().setLayout(new FlowLayout());
		f.getContentPane().add(table);
		f.setSize(new Dimension(200, 200));
		f.show();
	}

}