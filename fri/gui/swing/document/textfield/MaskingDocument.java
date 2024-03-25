package fri.gui.swing.document.textfield;

import java.util.Vector;
import java.awt.Toolkit;
import javax.swing.*;
import javax.swing.text.*;

/**
	A Document for textfields that holds more than one data fields and
	validates all field contents with their different rules.
	To achieve this it delegates <i>insertString()</i> and <i>remove()</i>
	calls to the contained MaskingElements at current cursor position.
	After collecting all substrings from its elements the MaskingDocument
	rewrites the whole Document.<br>
	As a result of this strategy the content of the Document is always
	removed and rewritten newly, so a DocumentListener needs only to
	implement <i>insertUpdate()</i> to catch changes.
	<p>
	The MaskingDocument needs the textfield to control cursor
	movements and positions.
	<p>
	If elements are not passed to constructor but added with
	<i>addMaskingElement()</i>, the document needs a final call
	<i>textfield.setText("")</i> after creation to show placeholders and
	contents of its elements.
	<p>
	Usage:
	<pre>
		JTextField textfield = new JTextField();
		textfield.setDocument(new MaskingDocument(textfield, new UnsignedIntegerMask()));
	</pre>
	If you want to use some mask container:
	<pre>
		JTextField textfield = new JTextField();
		textfield.setDocument(
			new MaskingDocument(
					textfield,
					new DateTimeContainer().getMaskingElements()));
	</pre>
*/

public class MaskingDocument extends PlainDocument
{
	private AttributeSet attrSet = null;
	protected JTextComponent textfield;
	protected Vector elements = new Vector();
	private int firstErrorPosition;
	private boolean reachedMaximalLength;
	private boolean cursorJumpOverFixedElements = true;

	
	/** Create an empty document in passed textfield. */
	public MaskingDocument(JTextComponent textfield)	{
		this(textfield, (MaskingElement[])null);
	}

	/**  */
	public MaskingDocument(JTextComponent textfield, MaskingElement element)	{
		this(textfield, new MaskingElement [] { element });
	}

	/**  */
	public MaskingDocument(JTextComponent textfield, MaskingElement [] elements)	{
		super();
		
		this.textfield = textfield;

		textfield.setDocument(this);
		
		if (elements != null && elements.length > 0)	{
			for (int i = 0; i < elements.length; i++)	{
				addMaskingElement(elements[i]);
			}
			textfield.setText("");	// show element contents
		}
	}


	public void addMaskingElement(MaskingElement e)	{
		elements.add(e);
	}
	
	public void removeMaskingElement(MaskingElement e)	{
		elements.remove(e);
	}
	
	public MaskingElement [] getMaskingElements()	{
		MaskingElement [] elems = new MaskingElement[elements.size()];
		elements.copyInto(elems);
		return elems;
	}
	
	
	public String refresh()	{
		try	{
			return updateTextFieldText(0, -1, null);	// refresh textfield
		}
		catch (BadLocationException ex)	{
			ex.printStackTrace();
		}
		return null;
	}


	/**
		Set the property that cursor jumps over fixed elements when
		moving by insertion or removal. Default is true.
	*/
	public void setCursorJumpOverFixedElements(boolean cursorJumpOverFixedElements)	{
		this.cursorJumpOverFixedElements = cursorJumpOverFixedElements;
	}

	/**
		Returns true if cursor jumps over fixed elements when
		moving by insertion or removal.
	*/
	public boolean isCursorJumpOverFixedElements()	{
		return cursorJumpOverFixedElements;
	}

	
	// Overriding PlainDocument
	
	public void insertString(int offs, String inserted, AttributeSet a) 
		throws BadLocationException
	{
		if (attrSet == null)
			attrSet = a;

		MaskingElement me = elementForOffset(offs, true);
		int relativeOffset = Math.max(offs - startOffsetForElement(me), 0) + inserted.length();
		//System.err.println("insertString, offset "+offs+", relativeOffset "+relativeOffset+", element >"+me.getText()+"<");

		String s = updateTextFieldText(offs, -1, inserted);
		
		if (firstErrorPosition >= 0)	{
			//System.err.println("Some error happened at "+firstErrorPosition);
			textfield.setCaretPosition(Math.min(Math.max(offs, firstErrorPosition), s.length()));
		}
		else	{
			// check if before a fixed element, skip if so
			int newDot = startOffsetForElement(me) + relativeOffset;
			//System.err.println("New dot will be at "+newDot+", element >"+me.getText()+"<");
			if (reachedMaximalLength)	{
				newDot = jumpOverFixedElement(newDot, true);
			}
			newDot = Math.min(newDot, s.length());
			//System.err.println("Setting dot at "+newDot+", start offset "+startOffsetForElement(me));
			textfield.setCaretPosition(newDot);
		}
	}


	public void remove(int offs, int length)
		throws BadLocationException
	{
		int dot = textfield.getCaret().getDot();
		int mark = textfield.getCaret().getMark();
		boolean selection = (mark != dot);
		boolean isBackSpace = (selection == false && offs != dot);

		MaskingElement me = elementForOffset(offs, false);
		int relativeOffset = Math.max(offs - startOffsetForElement(me), 0);

		updateTextFieldText(offs, length, null);

		int newDot = startOffsetForElement(me) + relativeOffset;
		//System.err.println("remove, offset "+offs+", relativeOffset "+relativeOffset+", element >"+me.getText()+"<");
		if (isBackSpace)	{
			newDot = jumpOverFixedElement(newDot, false);
		}
		textfield.setCaretPosition(Math.min(Math.max(0, newDot), getLength()));
	}

	
	protected String updateTextFieldText(int offset, int deleteLen, String inserted)
		throws BadLocationException
	{
		//System.err.println("MaskingDocument.updateTextFieldText offset "+offset+", deleteLen "+deleteLen+", inserted >"+inserted+"<, text length "+getLength());
		String s = loopElements(offset, deleteLen, inserted);

		super.remove(0, getLength());
		super.insertString(0, s, attrSet);
		
		return s;
	}
	
	
	protected String loopElements(int offset, int deleteLen, String inserted)	{
		firstErrorPosition = -1;
		int pos = 0;
		boolean compoundInsertion = false;
		
		// edit elements
		for (int i = 0; i < elements.size(); i++)	{
			MaskingElement me = (MaskingElement)elements.get(i);
			boolean isDelete = deleteLen > 0;
			boolean isInsert = inserted != null && inserted.length() > 0;
			int len = getMatchLength(me, isInsert);
			
			if ((isInsert || isDelete) && offsetMatchesElement(len, offset, pos))	{
				int relativeOffset = offset - pos;
				
				if (isDelete)	{
					int toDelete = Math.min(me.length() - relativeOffset, deleteLen);

					//System.err.println("delete happens at "+me.getClass()+", offset "+offset);
					me.textRemoval(relativeOffset, toDelete);

					if (toDelete < deleteLen)	{
						offset += me.length() - relativeOffset;
					}
					deleteLen -= toDelete;
				}
				else
				if (isInsert)	{
					int max = me.getMaximalLength() - relativeOffset;
					String toInsert = inserted;

					if (max < inserted.length())	{
						toInsert = inserted.substring(0, max);	// delegate rest to next element
					}
					
					//System.err.println("insert happens at "+me.getClass()+", offset "+offset+" with >"+toInsert+"<");
					me.textInsertion(relativeOffset, toInsert);
					
					int done = Math.max(me.trueLength() - relativeOffset, 0);
					if (done < inserted.length())	{	// partial insert
						inserted = inserted.substring(done);
						offset += done;
						compoundInsertion = true;	// ignore errors when compound insertion
						//System.err.println("  continue insert with >"+inserted+"<");
					}
					else	{	// jump over fixed fields when typing
						reachedMaximalLength = me.reachedMaximalLength();
					}
				}
				
				if (me.getError() >= 0 && !compoundInsertion)	{	// error happened
					firstErrorPosition = pos + me.getError();
					Toolkit.getDefaultToolkit().beep();
				}
			}
			
			pos += me.length();
		}

		// collect result at end, as elements could update each other
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < elements.size(); i++)	{
			MaskingElement me = (MaskingElement)elements.get(i);
			sb.append(me.getText());
		}
		
		return sb.toString();
	}


	/** Returns a MaskingElement for an insertion or removal offset. */
	protected MaskingElement elementForOffset(int offset, boolean isInsert)	{
		int pos = 0;
		MaskingElement me = null;
		
		for (int i = 0; i < elements.size(); i++)	{
			me = (MaskingElement)elements.get(i);
			int len = getMatchLength(me, isInsert);

			if (offsetMatchesElement(len, offset, pos))	{
				return me;
			}
			pos += me.length();
		}
		return me;
	}

	/** Returns a offset for a MaskingElement. */
	protected int startOffsetForElement(MaskingElement me)	{
		int pos = 0;
		for (int i = 0; elements != null && i < elements.size(); i++)	{
			MaskingElement me1 = (MaskingElement)elements.get(i);

			if (me.equals(me1))	{
				//System.err.println("startOffsetForElement >"+me.getText()+"< is: "+pos);
				return pos;
			}
			pos += me1.length();
		}
		return -1;
	}

	private boolean offsetMatchesElement(int elementLen, int offset, int soFar)	{
		return offset >= soFar && offset < soFar + elementLen;
	}

	private int getMatchLength(MaskingElement me, boolean isInsert)	{
		return (!isInsert || me.reachedMaximalLength()) ? me.length() : me.length() + 1;
	}

	private int jumpOverFixedElement(int newDot, boolean isInsert)	{
		if (isCursorJumpOverFixedElements() == false)
			return newDot;
		
		int maskIndex = isInsert ? newDot : Math.max(0, newDot - 1);
		MaskingElement me = elementForOffset(maskIndex, false);
		
		if (me.isFixed())	{
			//System.err.println("jumping over fixed element >"+me.getText()+"< with dot "+newDot);
			int leftLen = newDot - startOffsetForElement(me);
			int i = isInsert ?
					newDot + (me.length() - leftLen) :
					newDot - leftLen;
					
			if (i != newDot)
				return jumpOverFixedElement(i, isInsert);
		}
		
		return newDot;
	}
		
	

	/**
		Convenience method to create a JTextField on a document with passed element.
	*/
	public static JTextField createMaskingTextField(MaskingElement element)	{
		return createMaskingTextField(new MaskingElement [] { element });
	}

	/**
		Convenience method to create a JTextField on a document with passed elements.
		This method tries to set the best column count to textfield.
	*/
	public static JTextField createMaskingTextField(MaskingElement [] elements)	{
		JTextField tf = new JTextField();
		new MaskingDocument(tf, elements);
		
		int len = 0;
		for (int i = 0; i < elements.length; i++)	{
			int cur = elements[i].length();
			int max = elements[i].getMaximalLength();
			len += max < 40 ? max : cur;
		}

		len = len > 0 ? len : 10;
		tf.setColumns(len);
		System.err.println("setting "+len+" columns to textfield");
		
		return tf;
	}



	/* test main
	public static void main(String [] args)	{
		fri.gui.swing.document.textfield.mask.DayNameMask w =
				new fri.gui.swing.document.textfield.mask.DayNameMask(false);
		fri.gui.swing.document.textfield.mask.MonthNumberMask m =
				new fri.gui.swing.document.textfield.mask.MonthNumberMask("00");
		fri.gui.swing.document.textfield.mask.YearMask y =
				new fri.gui.swing.document.textfield.mask.YearMask();
		fri.gui.swing.document.textfield.mask.DayNameSettingDayNumberMask d =
				new fri.gui.swing.document.textfield.mask.DayNameSettingDayNumberMask(y, m, w, "00");
		
		MaskingElement [] elements = new MaskingElement []	{
			w,
			new fri.gui.swing.document.textfield.mask.FixedMask(", "),
			y,
			new fri.gui.swing.document.textfield.mask.FixedMask("-"),
			m,
			new fri.gui.swing.document.textfield.mask.FixedMask("-"),
			d,
		};
		
		javax.swing.JTextField tf = MaskingDocument.createMaskingTextField(elements);
		
		tf.addKeyListener(new SpinKeyListener());
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