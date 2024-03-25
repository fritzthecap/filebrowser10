package fri.gui.swing.document.textfield.mask;

import java.text.*;
import fri.gui.swing.document.textfield.MaskingElement;

/**
	Basic class for all number masks. Hold methods to avoid
	formatting by FormattingFocusListener and a method to
	retrieve plain number text ("1234" instead of "1.234",
	when string content is formatted).
*/

public abstract class NumberMask extends MaskingElement implements
	NumberFormatHolder
{
	protected char minus = new DecimalFormatSymbols().getMinusSign();
	protected char groupingSeparator = new DecimalFormatSymbols().getGroupingSeparator();
	protected NumberFormat format;
	
	
	public NumberMask()	{
		this(null);
	}

	public NumberMask(String placeHolder)	{
		super(placeHolder);
		this.format = createNumberFormat();
	}


	/**
		Directly setting text without using textInsertion. This overrides
		any character check and placeholder substitution of masks.
	*/
	public void setDisplayText(String text)	{
		setText(text);
	}

	/**
		Directly getting text from document.
	*/
	public String getDisplayText()	{
		return getText();
	}


	/**
		Enable and disable formatting with thousands separators.
		This should not be allowed in a fraction part of a decimal number.
	*/
	protected NumberFormat createNumberFormat()	{
		return NumberFormat.getInstance();
	}

	public NumberFormat getNumberFormat()	{
		return placeHolder != null ? null : format;
	}

	public void setNumberFormat(NumberFormat format)	{
		this.format = format;
	}


	/** Implements NumberFormatHolder for FormattingFocusListener. */
	public NumberFormat getNumberFormat(boolean focusLost)	{
		return getNumberFormat();
	}

	
	/**
		Returns plain number text instead of probably formatted contents.
		Thousands separators (grouping) could be confused with decimal separators!
		This method removes all grouping separators (not decimal separator!)
		from the number text.
	*/
	public String getNumberText()	{
		String s = getText().trim();
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < s.length(); i++)	{
			char c = s.charAt(i);
			
			if (c != groupingSeparator)	{
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	
}