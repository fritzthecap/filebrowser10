package fri.gui.swing.document.textfield;

/**
	Responsibilities of an element of an input mask, e.g. the day part of a date field.
	<ul>
		<li>Fixed text or not (e.g. the ":" separator for a time field is fixed).
			</li>
		<li>Placeholder String.
			</li>
		<li>Maximal character length.
			</li>
		<li>Current character length.
			</li>
		<li>Current text.
			</li>
		<li>Check of inserted characters.
			</li>
		<li>Dispatch text insertion and removal, with relative offset.
			</li>
	</ul>
	Subclasses must implement constructor and checkCharacter() method.
	<p>
	The method <i>getText()</i> returns the text of this element,
	but instead of <i>setText()</i> one must use <i>textInsertion()</i>
	to write text to textfield.
*/

public abstract class MaskingElement
{
	protected boolean fixed;
	protected int maximalLength = Integer.MAX_VALUE;
	protected int minimalLength = 0;	// derived from placeHolder
	protected String placeHolder;
	protected char minimalPlaceHolder = ' ';
	protected String mask;
	private String text;
	protected int error = -1;
	protected boolean overwrite = false;
	


	/** Create a MaskingElement with no fixed text. */
	public MaskingElement()	{
		this(false);
	}

	/** Create a MaskingElement with a placeholder. Minimal and maximal length are affected. */
	public MaskingElement(String placeHolder)	{
		this(placeHolder, false);
	}

	/** Create a MaskingElement with a placeholder. optionally fixed. */
	public MaskingElement(boolean fixed)	{
		this(null, fixed);
	}
	
	/**
		Create a MaskingElement with a placeholder. optionally fixed.
		@param placeHolder String that is to be used as background for input text.
			May be null, but not zero length.
		@param fixed true if text should be immutable.
	*/
	public MaskingElement(String placeHolder, boolean fixed)	{
		if (placeHolder != null && placeHolder.length() <= 0)
			throw new IllegalArgumentException("placeHolder cannot be zero length!");
		
		this.fixed = fixed;
		init(placeHolder);
	}
	
	
	/** Sets the passed text or minimalPlaceHolder when null. */
	protected void init(String placeHolder)	{
		setText(this.placeHolder = placeHolder);
	}
	
	
	/**
		Returns true if the passed character is valid on passed position.
		To be implemented by subclasses.
	*/
	public abstract boolean checkCharacter(char c, int i);


	/**
		The minimal place a element takes is one character.
		This method sets a new minimal placeholder character.
		It will show next time the textfield changes.
	*/
	public void setMinimalPlaceHolder(char minimalPlaceHolder)	{
		String s = getTrueText();
		this.minimalPlaceHolder = minimalPlaceHolder;
		setText(s);
	}
	
	/**
		Returns the minimal placeholder for this element.
	*/
	public char getMinimalPlaceHolder()	{
		return minimalPlaceHolder;
	}


	/**
		Sets a new placeHolder if no text was inserted up to this call.
		@placeHolder new background text fr this element.
		@exception IllegalStateException if text was already inserted, as it can
			not be separated from old placeHolder.
	*/
	public void setPlaceHolder(String placeHolder)	{
		String old = getTrueText();
		if (old != null && old.length() > 0 && placeHolder != null && placeHolder.equals(old) == false)	{
			throw new IllegalStateException("Can not change placeHolder when text was already inserted!");
		}
		
		this.placeHolder = placeHolder;
		
		if (old != null && old.length() > 0)
			textInsertion(0, old);
	}
	
	/**
		Returns the current placeHolder.
	*/
	public String getPlaceHolder()	{
		return placeHolder;
	}
	
	
	/**
		Overwrite is always true when placeHolder was defined.
		Else it depends on property <i>overwrite</i>, default this is false.
	*/
	public boolean isOverwrite()	{
		return overwrite;
	}
	
	/**
		Set the <i>overwrite</i> property, that controls input when
		placeHolder was not set.
	*/
	public void setOverwrite(boolean overwrite)	{
		this.overwrite = overwrite;
	}

	/**
		Returns the error state, -1 for no error, else error position.
	*/
	public int getError()	{
		return error;
	}


	/**
		Returns the placeholder, if no text was inserted, else the current
		text (that can contain placeholeder characters). If no placeholder
		was defined and no text was inserted, this returns "" (empty string).
		Never returns null.
	*/
	public String getText()	{
		return text != null ? text : getPlaceHolder() != null ? getPlaceHolder() : "";
	}

	/**
		Directly setting text without using textInsertion. This overrides
		any character check and placeholder substitution of masks.
	*/
	protected void setText(String text)	{
		if (text == null)
			this.text = ""+minimalPlaceHolder;
		else
			this.text = text;
	}


	/**
		Returns the length of getText().
	*/
	public int length()	{
		return getText().length();
	}

	/**
		Returns the text length, zero if text is only minimalPlaceHolder.
		Needed to calculate consumed length when inserting.
	*/
	public int trueLength()	{
		if (isFixed() == false && getPlaceHolder() == null && text != null && text.equals(""+minimalPlaceHolder))
			return 0;
		return length();
	}

	/**
		Returns empty string, if contents are minimal placeholder, else contained text.
	*/
	protected String getTrueText()	{
		String s = getText();
		if (s != null && s.equals(""+minimalPlaceHolder))	// to prevent un-locateable and invisible field
			s = "";
		return s;
	}
	
	/**
		Returns true if text reached some maximal length from a placeHolder.
		To be overridden by subclasses.
	*/
	public boolean reachedMaximalLength()	{
		return length() == getMaximalLength();
	}

	/**
		Returns true if this mask was constructed with a fixed=true argument.
	*/
	public boolean isFixed()	{
		return fixed;
	}
	
	/**
		Returns text length for fixed text, placeholder length if placeholder was defined,
		else the maximal length that defaults to Integer.MAX_VALUE.
	*/
	public int getMaximalLength()	{
		return isFixed() ? getText().length() : getPlaceHolder() != null ? getPlaceHolder().length() : maximalLength;
	}

	/**
		Returns text length for fixed text, placeholder length if placeholder was defined,
		else the minimal length that defaults to 0.
	*/
	protected int getMinimalLength()	{
		return isFixed() ? getText().length() : getPlaceHolder() != null ? getPlaceHolder().length() : minimalLength;
	}
	
	
	/** Returns the whole new text of this mask after insertion. */
	public String textInsertion(int offset, String inserted)	{
		//System.err.println("element insert, offset "+offset+", inserted >"+inserted+"<");
		if (isFixed())	{
			return getText();
		}
		
		error = -1;
		
		StringBuffer sb = new StringBuffer(getTrueText());

		for (int i = 0; error == -1 && i < inserted.length(); i++)	{
			char c = inserted.charAt(i);
			int idx = Math.min(offset + i, sb.length());
			
			if (checkCharacter(c, offset + i) == false)	{
				//System.err.println("-> detected error at relative offset "+i+": "+sb.toString());
				error = idx;
			}
			else
			if (getPlaceHolder() == null && isOverwrite() == false)	{
				sb.insert(idx, c);
			}
			else	{	// overwrite placeholder
				if (sb.length() > idx)
					sb.setCharAt(idx, c);
				else
					sb.append(c);
			}
		}
		
		String s = sb.substring(0, Math.min(getMaximalLength(), sb.length()));
		setText(s.length() <= 0 ? null : s);

		//System.err.println("element insert, text >"+text+"<");
		return getText();
	}

	
	/** Returns the whole new text of this mask after removal. */
	public String textRemoval(int offset, int length)	{
		//System.err.println("element remove, offset "+offset+", length "+length);
		if (isFixed())	{
			return getText();
		}
		
		error = -1;
		
		StringBuffer sb = new StringBuffer(getText());
		sb.delete(offset, offset + length);
		
		if (sb.length() < getMinimalLength() && getPlaceHolder() != null)	{
			String ph = getPlaceHolder().substring(offset, offset + length);
			sb.insert(offset, ph);
		}
		
		String s = sb.toString();
		setText(s.length() <= 0 ? null : s);

		//System.err.println("element remove, text >"+text+"<");
		return getText();
	}
	
	
	
	/** Default implementation, returns null. */
	public String cursorUp(int offset)	{
		return null;
	}

	/** Default implementation, returns null. */
	public String cursorDown(int offset)	{
		return null;
	}
	
}