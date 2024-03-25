package fri.gui.swing.spinnumberfield;

import java.awt.Color;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*; 
import fri.gui.swing.document.textfield.MaskingDocument; 
import fri.gui.swing.document.textfield.SpinKeyListener;
import fri.gui.swing.document.textfield.mask.UnsignedLongMask; 

/**
	JTextField that allows input of positive numbers, including null
	values. Default it is right aligned. Input can be observed by
	adding a NumberEditorListener. If text represents a number below
	a positive minimum, a red border is set to textfield.
*/

public class WholeNumberField extends JTextField implements
	NumberEditor,
	DocumentListener
{
	private Vector lsnrs = null;
	private UnsignedLongMask mask;
	private long oldValue = -2L;
	private Border okBorder = null;
	private Border errorBorder = BorderFactory.createLineBorder(Color.red);


	/** Create a textfield with 4 columns. */
	public WholeNumberField() {
		this((short)4);
	}
	
	/** Create a textfield with passed column count. */
	public WholeNumberField(short columns) {
		super(columns);

		setHorizontalAlignment(RIGHT);
		
		mask = new UnsignedLongMask();
		setDocument(new MaskingDocument(this, mask));
		
		getDocument().addDocumentListener(this);
		
		addKeyListener(new SpinKeyListener());
	}


	// interface NumberEditor
	
	/** Implements NumberEditor: Returns the current number or -1 if empty (null). */
	public long getValue() {
		Long l = mask.getLongValue();
		if (l == null)
			return -1L;
		return l.longValue();
	}

	/** Implements NumberEditor: Sets the current number or empty if value is -1. */
	public void setValue(long value) {
		long [] minMax = mask.getMinMax();
		long min = minMax[0];
		long max = minMax[1];
		if (value != -1L && (value > max || value < min))
			throw new IllegalArgumentException("invalid value "+value+", not within min/max: "+min+"/"+max);

		//System.err.println("WholeNumberField.setValue("+value+")");
		mask.setLongValue(value == -1L ? null : new Long(value));
		((MaskingDocument)getDocument()).refresh();
	}
	

	/**
		Implements NumberEditor: Set the allowed range for displayed number.
		@param min miminal value for textfield
		@param max maxinal value for textfield
	*/
	public void setRange(long min, long max)	{
		if (min < 0)
			throw new IllegalArgumentException("minimum "+min+" must be bigger or equal 0");

		if (min > max)
			throw new IllegalArgumentException("minimum  "+min+" is smaller than maximum "+max);
		
		mask.setMinMax(min, max);
	}
	
		
	/** Implements NumberEditor: add a value listener. */
	public void addNumberEditorListener(NumberEditorListener lsnr)	{
		if (lsnrs == null)
			lsnrs = new Vector();
		lsnrs.add(lsnr);
	}

	/** Implements NumberEditor: remove a value listener. */
	public void removeNumberEditorListener(NumberEditorListener lsnr)	{
		if (lsnrs == null)
			return;
		lsnrs.remove(lsnr);
	}

	
	private void fireNumberChanged()	{
		if (lsnrs == null)
			return;

		long value = getValue();
		if (value == oldValue)	// avoid repeated calls
			return;	// remove and insert was sent
		
		oldValue = value;

		manageBorder(value);
				
		for (Enumeration e = lsnrs.elements(); e.hasMoreElements(); )	{
			NumberEditorListener lsnr = (NumberEditorListener)e.nextElement();
			lsnr.numericValueChanged(value);
		}
	}

	private void manageBorder(long value)	{	
		if (okBorder == null)	{
			okBorder = getBorder();
		}

		long [] minMax = mask.getMinMax();
		long min = minMax[0];
		long max = minMax[1];
		
		// check if allowed value
		if (value <= max && value >= min || value == -1L)
			setBorder(okBorder);
		else
			setBorder(errorBorder);
	}
	
	// end interface NumberEditor


	// begin interface DocumentListener

	public void insertUpdate(DocumentEvent e)	{
		fireNumberChanged();
	}
	
	public void removeUpdate(DocumentEvent e)	{
		fireNumberChanged();
	}

	public void changedUpdate(DocumentEvent e)	{
	}
	
	// end interface DocumentListener


	// test main
	/*
	public static void main(String [] args)	{
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
		//frame.getContentPane().setLayout(new FlowLayout());
		WholeNumberField tf = new WholeNumberField();
		tf.setRange(9, 99);
		frame.getContentPane().add(tf);
		tf.addNumberEditorListener(new NumberEditorListener()	{
			public void numericValueChanged(long newValue)	{
				System.err.println("numericValueChanged: "+newValue);
			}
		});
		frame.pack();
		frame.show();
	}
	*/
}