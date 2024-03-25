package fri.gui.swing.spintextfield;

import java.util.Vector;
import java.awt.ItemSelectable;
import java.awt.event.*;
import javax.swing.*;
import fri.gui.swing.spinner.InfiniteSpinner;
import fri.gui.swing.document.textfield.MaskingDocument;
import fri.gui.swing.document.textfield.SpinAdjustmentListener;
import fri.gui.swing.document.textfield.SpinKeyListener;
import fri.gui.swing.document.textfield.mask.StringListMask;

/**
	Multiple choice for a list of Strings.
	Adds a "Spinner" to a JTextField, which is made of a scrollbar
	forced to textfield height. This is like a ComboBox without a popup.
*/

public class SpinTextField extends InfiniteSpinner implements
	ItemSelectable
{
	private Vector lsnrs = new Vector(1);
	private StringListMask mask;
	private String oldValue = null;
	

	/** Create a SpinTextField from a Vector of Strings. */
	public SpinTextField(Vector strings)	{
		this(strings.toArray());
	}

	/** Create a SpinTextField from a String array. */
	public SpinTextField(Object [] strings)	{
		super(new JTextField());
		build((String[])strings);
	}


	/** Build the GUI elements and add listeners. */
	protected void build(String [] strings)	{
		mask = new StringListMask(strings[0], strings)	{
			protected String cursor(boolean up)	{
				String s = super.cursor(up);
				fireItemStateChanged();
				return s;
			}
		};

		JTextField tf = (JTextField)getEditor();
		tf.setColumns(mask.getMaximalLength());
		
		tf.setDocument(new MaskingDocument(tf, mask));
		
		tf.addKeyListener(new SpinKeyListener());
		addAdjustmentListener(new SpinAdjustmentListener(tf));
	}



	/** Sets the passed text to textfield or adjusts the spinner if item is in list. */
	public void setText(String item)	{
		mask.setStringValue(item);
		((MaskingDocument)getEditor().getDocument()).refresh();
	}

	/** Returns the current text from textfield. */
	public String getText()	{
		return mask.getStringValue();
	}


	/** Sets the spinner to the passed value. */
	public void setSelectedIndex(int i)	{
		mask.setStringValueIndex(i);
		((MaskingDocument)getEditor().getDocument()).refresh();
	}
	
	/** Returns the index of the curent text in list, or -1 if not in list. */
	public int getSelectedIndex()	{
		return mask.getStringValueIndex();
	}
	

	/** Implements ItemSelectable: returns the one selected item. */
	public Object[] getSelectedObjects()	{
		String s = mask.getStringValue();
		return new String [] { s };
	}
	
	
	/** Returns the spinner scrollbar. */
	public JScrollBar getSpinner()	{
		return sb;
	}
	

	/** Fires the ItemEvent with selected item, ITEM_STATE_CHANGED and SELECTED. */
	private void fireItemStateChanged()	{
		String s = mask.getStringValue();
		if (oldValue != null && s.equals(oldValue))
			return;
			
		oldValue = s;
		
		ItemEvent evt = null;
		for (int i = 0; i < lsnrs.size(); i++)	{
			if (evt == null)	{
				evt = new ItemEvent(
						this,
						ItemEvent.ITEM_STATE_CHANGED,
						s,
						ItemEvent.SELECTED);
			}
			((ItemListener)lsnrs.elementAt(i)).itemStateChanged(evt);
		}
	}

 	
	/** Adds an ActionListener to textfield. */
	public void addActionListener(ActionListener lsnr)	{
		((JTextField)getEditor()).addActionListener(lsnr);
	}

	/** Removes an ActionListener from textfield. */
	public void removeActionListener(ActionListener lsnr)	{
		((JTextField)getEditor()).removeActionListener(lsnr);
	}


	/** Implements ItemSelectable: adds an ItemListener */
	public void addItemListener(ItemListener lsnr)	{
		lsnrs.add(lsnr);
	}

	/** Implements ItemSelectable: removes an ItemListener */
	public void removeItemListener(ItemListener lsnr)	{
		lsnrs.remove(lsnr);
	}



	// test main

	public static void main(String [] args)	{
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new java.awt.FlowLayout());
		
		String [] sarr = new String [] { "Hallo", "Welt", "!", };
		final SpinTextField tf = new SpinTextField(sarr);
		tf.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				System.err.println("actionPerformed: "+e);
			}
		});
		tf.addItemListener(new ItemListener()	{
			public void itemStateChanged(ItemEvent e)	{
				System.err.println("itemStateChanged: "+e);
			}
		});

		frame.getContentPane().add(tf);
		frame.pack();
		frame.show();
	}
	
}