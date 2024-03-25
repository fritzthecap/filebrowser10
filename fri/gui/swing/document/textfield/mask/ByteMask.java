package fri.gui.swing.document.textfield.mask;

import java.awt.Toolkit;
import fri.gui.swing.document.textfield.MaskingElement;

/**
	ByteMask lets input the value of a byte, 0 - 255.
	Several numberbases can be used with this mask: 0 (character), and 2 - 32.
	For character base only the input of characters 32 - 255 is possible.
*/

public class ByteMask extends MaskingElement
{
	private int base;
	private int delta;
	private int limitLower;
	private int limitUpper;
	
	/** Create a ByteMask that lets input a byte value using the the passed number base. */
	public ByteMask(int base)	{
		setBase(base);
	}
	
	/** Set a base: 0 (character), 2 - 32 are possible number roots. */
	public void setBase(int base)	{
		this.base = base;
		delta = base - 10;
		limitLower = 'A' + delta;
		limitUpper = 'a' + delta;

		maximalLength = (base < Character.MIN_RADIX) ? 1
			: (base <= 2) ? 8
			: (base <= 3) ? 6
			: (base <= 6) ? 4
			: (base <= 15) ? 3
			: 2;
	}
	
	/** Checking a single inserted character. */
	public boolean checkCharacter(char c, int i)	{
		if (base < Character.MIN_RADIX)
			return c >= ' ' && c <= 255;

		if (base <= 10)
			return c >= '0' && c < (char)('0' + base);

		return
			c >= '0' && c <= '9' ||
			c >= 'A' && c < limitUpper || c >= 'a' && c < limitLower;
	}

	/** Checking the text after insertion. Set old text when value is bigger than 255. */
	public String textInsertion(int offset, String inserted)	{
		//System.err.println("textInsertion at "+offset+", inserted "+inserted);
		String oldText = getText();	// save old text
		
		super.textInsertion(offset, inserted);	// evaluates text

		if (error == -1)	{	// no error happened during insertion
			try	{
				if (base < Character.MIN_RADIX)	{
					if (inserted.length() > 1)
						throw new Exception("String too long: "+inserted);
				}
				else	{
					String s = getText();
					int i = Integer.parseInt(s, base);
					if (i > 255)
						throw new Exception("Maximum value exceeded: "+s);
				}
			}
			catch (Exception e)	{
				Toolkit.getDefaultToolkit().beep();
				System.err.println("text insertion error was: "+e.getMessage());
				setText(oldText);
			}
		}
		
		return getText();
	}


	protected void setText(String text)	{
		if (text == null)
			text = "";
		super.setText(text);
	}

}
