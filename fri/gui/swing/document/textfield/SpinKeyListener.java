package fri.gui.swing.document.textfield;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.text.JTextComponent;

/**
	A KeyListener for MaskingDocument that increments and decrements
	numbers and string lists in MaskingElements. This works only if
	the textfields Document is instanceof MaskingDocument.
*/

public class SpinKeyListener extends KeyAdapter implements
	SpinListener
{
	private boolean shiftInProgress;

	public SpinKeyListener()	{
	}

	// begin interface KeyListener
	
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_DOWN)	{
			decrement((JTextComponent)e.getSource());
		}
		else
		if (e.getKeyCode() == KeyEvent.VK_UP)	{
			increment((JTextComponent)e.getSource());
		}
	}
	
	// end interface KeyListener

	// begin interface SpinListener

	public void decrement(JTextComponent textfield)	{
		//System.err.println("cursorDown at caret position "+caretPosition);
		shift(textfield, false);
	}

	public void increment(JTextComponent textfield)	{
		//System.err.println("cursorUp at caret position "+caretPosition);
		shift(textfield, true);
	}

	// end interface SpinListener


	protected void shift(JTextComponent textfield, boolean up)	{
		if (shiftInProgress)	{
			return;
		}
			
		shiftInProgress = true;
		
		try	{
			int changeOffset = textfield.getCaretPosition();
			MaskingDocument document = (MaskingDocument)textfield.getDocument();
		
			// try to find a good element for cursor spin
			MaskingElement me = document.elementForOffset(changeOffset, false);	// try right
			if (me.isFixed() && changeOffset > 0)	{
				me = document.elementForOffset(changeOffset - 1, false);	// try left
			}
			
			if (me.isFixed())
				return;
			
			// found a mutable element
			String oldText = me.getText();
			int relativeOffset = Math.max(changeOffset - document.startOffsetForElement(me), 0);
			String newText = up ? me.cursorUp(relativeOffset) : me.cursorDown(relativeOffset);
			
			// if text changed, update document
			if (newText != null && newText.equals(oldText) == false)	{
				String s = document.refresh();	// refresh textfield
				// restore cursor position
				int dot = document.startOffsetForElement(me) + Math.min(relativeOffset, newText.length());
				textfield.setCaretPosition(Math.min(dot, s.length()));
			}
		}
		finally	{
			shiftInProgress = false;
		}
	}

	// end interface SpinListener


	// test main
	/*
	public static void main(String [] args)	{
		javax.swing.JTextField tf = MaskingDocument.createMaskingTextField(
				new fri.gui.swing.document.textfield.mask.SignedIntegerMask());
		
		tf.addKeyListener(new SpinKeyListener());
		
		javax.swing.JFrame f = new javax.swing.JFrame();
		f.getContentPane().setLayout(new java.awt.FlowLayout());
		f.getContentPane().add(tf);
		f.pack();
		f.setVisible(true);
	}
	*/
}