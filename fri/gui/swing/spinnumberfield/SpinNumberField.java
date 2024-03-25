package fri.gui.swing.spinnumberfield;

import java.awt.event.*;
import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import fri.gui.swing.document.textfield.SpinAdjustmentListener;
import fri.gui.swing.spinner.InfiniteSpinner;

/**
	A number textfield with spin buttons to the right and
	an optional label to the left, allowing positive numbers
	within a settable range.
*/

public class SpinNumberField extends InfiniteSpinner
{
	/**
		Empty Spinner TextField.
	*/
	public SpinNumberField()	{
		this(-1L);
	}

	/**
		Spinner TextField starting with passed number.
		@param value initial number for textfield
	*/
	public SpinNumberField(long value)	{
		this(value, 0L, Long.MAX_VALUE);
	}

	/**
		Spinner TextField with passed minimal and maximal values.
		@param min minimal number for textfield
		@param max maximal number for textfield
	*/
	public SpinNumberField (long min, long max)	{
		this(-1L, min, max);
	}

	/**
		Spinner TextField starting with passed number and minimal and maximal values.
		@param value initial number for textfield
		@param min minimal number for textfield
		@param max maximal number for textfield
	*/
	public SpinNumberField (long value, long min, long max)	{
		this(value, min, max, (short)4);
	}

	/**
		Spinner TextField starting with passed number and minimal and maximal values
		and a column count.
		@param value initial number for textfield
		@param min minimal number for textfield
		@param max maximal number for textfield
		@param charWidth column width of textfield
	*/
	public SpinNumberField (long value, long min, long max, short charWidth)	{
		super(new WholeNumberField(charWidth));
		setValueAndRange(value, min, max);
		addAdjustmentListener(new SpinAdjustmentListener((JTextComponent)getEditor()));
		((JTextComponent)getEditor()).setCaretPosition(((JTextComponent)getEditor()).getText().length());
	}

	/**
		Empty Spinner TextField with a left label.
		@param leftLabel label to be shown to the left of numbertextfield.
	*/
	public SpinNumberField(String leftLabel)	{
		this();
		add(new JLabel(leftLabel), BorderLayout.WEST);
	}


	public void clear()	{
		setValue(-1L);
	}
	

	/** Returns the spinner component (scrollbar). */
	public JScrollBar getSpinner()	{
		return sb;
	}


	/**
		Set value, minimum and maximum.
		@param value initial number for textfield
		@param min minimal number for textfield
		@param max maximal number for textfield
	*/
	public void setValueAndRange(long value, long min, long max)	{
		getNumberEditor().setRange(min, max);
		getNumberEditor().setValue(value);
	}


	/**
		Setzen des Minimums und Maximums des
		Bereiches, in dem sich die Zahl bewegen soll.
		@param min Minimum fuer die Zahl. Mit -1 kann man den Bereich auf "undefiniert" setzen!
		@param max Maximum fuer die Zahl.
	*/
	public void setRange(long min, long max)	{
		getNumberEditor().setRange(min, max);
	}


	/** Propagates a numeric value to the TextField. */
	public void setValue(long value)	{
		getNumberEditor().setValue(value);
	}

	/** Returns a numeric value from TextField */
	public long getValue()	{
		return getNumberEditor().getValue();
	}


	/** Liefert das WholeNumberField oder den von aussen gesetzten NumberEditor zurueck. */
	public NumberEditor getNumberEditor()	{
		return (NumberEditor)getEditor();
	}


	public void setToolTipText(String t)	{
		super.setToolTipText(t);
		getEditor().setToolTipText(t);
	}

	
	/**
		Anmelden eines ActionListener, indem an WholeNumberField delegiert wird,
		wenn der NumberEditor ein JTextField ist.
	*/
	public void addActionListener(ActionListener lsnr)	{
		getNumberEditor().addActionListener(lsnr);
	}

	/**
		Abmelden eines ActionListener, indem an WholeNumberField delegiert wird,
		wenn der NumberEditor ein JTextField ist.
	*/
	public void removeActionListener(ActionListener lsnr)	{
		getNumberEditor().removeActionListener(lsnr);
	}



	/*
	public static void main(String [] args)	{
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new java.awt.FlowLayout());
		final SpinNumberField tf = new SpinNumberField(17);
		tf.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				System.err.println("actionPerformed: "+tf.getValue());
			}
		});
		tf.getNumberEditor().addNumberEditorListener(new NumberEditorListener()	{
			public void numericValueChanged(long newValue)	{
				System.err.println("numericValueChanged: "+newValue);
			}
		});
		tf.setRange(5, 999);
		frame.getContentPane().add(tf);
		frame.pack();
		frame.show();
	}
	*/
	
}