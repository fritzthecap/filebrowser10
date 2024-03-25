package fri.gui.swing.document.textfield.mask;

import java.util.Vector;
import fri.gui.swing.document.textfield.MaskingElement;

/**
	Mask that holds a list of Strings and is intended to be the superclass
	for all String list masks. No cursor control and no <i>setStringValue()</i>
	method is implemented, but <i>checkCharacter()</i>.
*/

public class DefaultStringListMask extends MaskingElement
{
	protected Vector strings;
	protected int selected = -1;
	protected boolean first = true;
	
	
	public DefaultStringListMask(String [] sarr)	{
		this(null, sarr);
	}

	public DefaultStringListMask(String initial, String [] sarr)	{
		init(initial, sarr);
	}


	protected void  init(String initial, String [] sarr)	{
		strings = new Vector();
		
		maximalLength = 0;
		minimalLength = Integer.MAX_VALUE;

		for (int i = 0; i < sarr.length; i++)	{
			strings.add(sarr[i]);
			
			if (sarr[i].length() > maximalLength)
				maximalLength = sarr[i].length();

			if (sarr[i].length() < minimalLength)
				minimalLength = sarr[i].length();
				
			if (selected == -1 && initial != null && sarr[i].equals(initial))
				selected = i;
		}
		
		if (minimalLength <= 0)	{
			minimalLength = 1;	// else unlocateable
		}
		
		setStringValue(initial);
	}


	/**
		Perform primitive character/position check.
	*/
	public boolean checkCharacter(char c, int i)	{
		for (int j = 0; j < strings.size(); j++)	{
			String s = strings.get(j).toString();
			
			if (s.length() > i)	{
				char c1 = s.charAt(i);
				
				if (c1 == c)	{
					return true;
				}
			}
		}
		return false;
	}


	/**
		Perform compound check in string list. This does not ensure that the
		current text is a valid string contained in stringlist, as deleting
		and writing must be possible.
	*/
	public String textInsertion(int offset, String inserted)	{
		String old = getText();	// save old text
		String s = super.textInsertion(offset, inserted);
		
		boolean ok = false;
		if (error == -1)	{	// if character check succeeded
			for (int i = 0; i < strings.size(); i++)	{
				String s1 = strings.get(i).toString();
				if (s1.startsWith(s))	{
					ok = true;
				}
			}
			
			if (ok == false)	{
				error = offset;
				setText(old);
			}
		}
		
		return getText();
	}


	public boolean reachedMaximalLength()	{
		if (placeHolder == null)
			return strings.indexOf(getText()) >= 0;
		else
			return super.reachedMaximalLength();
	}


	/** Returns the current text from this field or null if not defined. */
	public String getStringValue()	{
		String s = getTrueText();
		if (s == null || s.length() <= 0 || strings.indexOf(s) < 0)	{
			return null;
		}
		return s;
	}

	/** Sets the current text for this field or null if not defined. */
	public void setStringValue(String s)	{
		textRemoval(0, length());
		selected = -1;
		
		if (s != null)	{
			textInsertion(0, s);
			selected = strings.indexOf(s);
		}
	}


	/** Returns the index in list of current text. */
	public int getStringValueIndex()	{
		String s = getStringValue();
		return s == null ? -1 : strings.indexOf(s);
	}

	/** Sets the current text for this field or null if not defined. */
	public void setStringValueIndex(int i)	{
		String s = (i >= 0 && i < strings.size()) ? strings.get(i).toString() : null;
		setStringValue(s);
	}

}