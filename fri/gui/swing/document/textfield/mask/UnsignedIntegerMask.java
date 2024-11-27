package fri.gui.swing.document.textfield.mask;

/**
	A MaskingDocument element that lets input unsigned Integers.
	This mask can be used for finite numbers.
	Optionally a minumum and maximum can be passed, within 0
	and Integer.MAX_VALUE.
*/

public class UnsignedIntegerMask extends NumberMask
{
	protected long min, max;
	private boolean leftAlign = false;
		
	
	public UnsignedIntegerMask()	{
		this((Integer)null);
	}

	public UnsignedIntegerMask(Integer initial)	{
		this(initial, 0, Integer.MAX_VALUE);
	}

	public UnsignedIntegerMask(int min, int max)	{
		this((Integer)null, min, max);
	}

	public UnsignedIntegerMask(Integer initial, int min, int max)	{
		init(min, max, initial);
	}

	public UnsignedIntegerMask(String placeHolder, int min, int max)	{
		this(placeHolder, null, min, max);
	}

	public UnsignedIntegerMask(String placeHolder, Integer initial, int min, int max)	{
		init(placeHolder);
		init(min, max, initial);
	}

	
	protected void init(long min, long max, Number initial)	{
		setMinMax(min, max);
		
		if (initial != null)	{
			setText(initial.toString());
		}
	}

	public long [] getMinMax()	{
		return new long [] { min, max };
	}
	
	public void setMinMax(long min, long max)	{
		this.min = min;
		this.max = max;

		int i = Long.toString(min).length();
		int j = Long.toString(max).length();

		minimalLength = min > 0 ? i : 0;
		maximalLength = Math.max(i, j);
	}
	

//	/** Do overwrite when maximal length is reached. */
//	public boolean isOverwrite()	{
//		System.err.println("overwrite ? length "+length()+" maximum "+getMaximalLength());
//		return length() >= getMaximalLength();
//	}
// 	Does not work for signed numbers!
	
	

	/** Returns the integer value from this mask. */
	public Integer getIntegerValue()	{
		Long l = getLongValueInternal();
		if (l == null)
			return null;
		return Integer.valueOf(l.intValue());
	}

	/** Sets the integer value for this mask. */
	public void setIntegerValue(Integer i)	{
		if (i == null)
			textRemoval(0, length());
		else
			setLongValueInternal((long)i.intValue());
	}
	
	
	protected void setLongValueInternal(long value)	{
		textRemoval(0, length());
		
		String s = Long.toString(value);
		
		// right alignment for numbers
		int offs = 0;
		if (!leftAlign && placeHolder != null && s.length() < placeHolder.length())	{
			offs = placeHolder.length() - s.length();
		}
		
		textInsertion(offs, s);
	}
	
	
	protected Long getLongValueInternal()	{
		String s = getNumberText();
		
		if (s.length() > 0)	{
			s = getNumberFromText(s);
			return s == null ? null : Long.valueOf(s);
		}
		return null;
	}


	// assumes that number is correct, just to eliminate grouping separators
	private String getNumberFromText(String s)	{
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < s.length(); i++)	{
			char c = s.charAt(i);
			if (i == 0 && c == minus || Character.isDigit(c))	{
				sb.append(c);
			}
		}
		
		s = sb.toString();
		return s.length() > 0 && s.equals(""+minus) == false ? s : null;
	}


	public boolean checkCharacter(char c, int i)	{
		return Character.isDigit(c);
	}


	public String textInsertion(int offset, String inserted)	{
		String old = getText();	// save old text
		
		super.textInsertion(offset, inserted);	// evaluates text

		if (error == -1)	{	// no error happened during insertion
			try	{	// check new text
				checkValue(getText());
			}
			catch (NumberFormatException e)	{
				System.err.println(e.getMessage());
				error = offset;
				setText(old);
			}
		}

		return getText();
	}


	protected void checkValue(String s)
		throws NumberFormatException
	{
		Long l = getLongValueInternal();
		if (l == null)
			return;
		if (l.longValue() > max)
			throw new NumberFormatException("above maximum "+max+" or below minimum "+min);
	}
	
	
	
	public String cursorUp(int offset)	{
		return cursor(true, offset);
	}

	public String cursorDown(int offset)	{
		return cursor(false, offset);
	}

	protected String cursor(boolean up, int offset)	{
		shiftNumber(!up, offset);
		return getText();
	}

	
	public void setLeftAlignmentWhenPlaceHolder(boolean leftAlign)	{
		this.leftAlign = leftAlign;
	}
	
	public boolean isLeftAlignmentWhenPlaceHolder()	{
		return leftAlign;
	}
	
	
	/** Increments and decrements number according to cursor position. */
	protected void shiftNumber(boolean decrement, int cursorPosition)	{
		Long longValue = getLongValueInternal();
		long value = longValue != null
				? longValue.longValue()
				: decrement && max != 0
					? max
					: ! decrement && min != 0
						? min
						: Math.min(max, Math.max(min, 0L));

		if (longValue == null)	{// empty field, undefined
			setLongValueInternal(value);
		}
		else	// check for min/max
		if (decrement && value > min ||	! decrement && value < max)	{
			String part = getDeltaAsString(getText(), cursorPosition, decrement);
			
			long step = Long.valueOf(part).longValue();
			//System.err.println("step is: "+step+" value is: "+value+" min "+min+" max "+max);
			value = decrement ? value - step : value + step;

			if (decrement && value >= min || !decrement && value <= max)	{
				//System.err.println("doing step: "+step+" to value: "+value);
				setLongValueInternal(value);
			}
		}
	}


	static String getDeltaAsString(String text, int cursorPosition, boolean decrement)	{
		// return increment or decrement number relative to cursor position
		text = text.trim();
		int len = text.length();
		boolean isMinus = false;
		boolean atZero = (cursorPosition == 0);
		
		// figure out if number is negative
		if (Character.isDigit(text.charAt(0)) == false)	{
			isMinus = true;

			if (atZero)
				cursorPosition++;
			else
			if (cursorPosition == 1)
				atZero = true;
		}
			
		if (cursorPosition >= len)	{	// at end
			cursorPosition = len - 1;
		}
		
		StringBuffer sb = new StringBuffer();
		int cnt = len - cursorPosition;
		for (int i = 0; i < cnt; i++)
			sb.append(i == 0 ? "1" : "0");
		
		String part = sb.toString();
		
		// avoid jumping from 100 to 0, better jump to 90
		if (atZero && part.length() > 1 &&
				decrement != isMinus &&
				text.startsWith("10", cursorPosition))
		{
			part = part.substring(0, part.length() - 1);
		}
		
		return part;
	}
	


	/* test main
	public static void main(String [] args)	{
		javax.swing.JTextField tf = fri.gui.swing.document.textfield.MaskingDocument.createMaskingTextField(
				new UnsignedIntegerMask());

		tf.addKeyListener(new fri.gui.swing.document.textfield.SpinKeyListener());
		
		javax.swing.JFrame f = new javax.swing.JFrame("UnsignedIntegerMask");
		f.getContentPane().setLayout(new java.awt.FlowLayout());
		f.getContentPane().add(tf);
		f.pack();
		f.setVisible(true);
	}
	*/

}