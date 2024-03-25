package fri.gui.swing.document.textfield;

import java.awt.event.*;
import javax.swing.text.*;

/**
	A AdjustmentListener for MaskingDocument that increments and decrements
	numbers and string lists in MaskingElements. It needs the textfield,
	as the AdjustmentEvent source is NOT the textfield.
*/

public class SpinAdjustmentListener extends SpinKeyListener implements
	AdjustmentListener
{
	protected JTextComponent textfield;
	
	public SpinAdjustmentListener(JTextComponent textfield)	{
		super();
		this.textfield = textfield;
	}

	// begin interface AdjustmentListener
	
	public void adjustmentValueChanged(AdjustmentEvent e)	{
		if (e.getValue() < 0)	{
			increment(textfield);
		}
		else
		if (e.getValue() > 0)	{
			decrement(textfield);
		}
	}
	
	// end interface AdjustmentListener

	// test main
	/*
	public static void main(String [] args)	{
		MaskingElement [] elements = new MaskingElement []	{
			new fri.gui.swing.document.textfield.mask.UnsignedIntegerMask();
		};
		
		javax.swing.JTextField tf = MaskingDocument.createMaskingTextField(elements);

		fri.gui.swing.spinner.InfiniteSpinner sp = new fri.gui.swing.spinner.InfiniteSpinner(tf);
		
		sp.addAdjustmentListener(new SpinAdjustmentListener(tf));
		
		javax.swing.JFrame f = new javax.swing.JFrame();
		f.getContentPane().setLayout(new java.awt.FlowLayout());
		f.getContentPane().add(sp);
		f.pack();
		f.setVisible(true);
	}
	*/
	
}