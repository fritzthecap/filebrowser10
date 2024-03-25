package fri.gui.swing.document.textfield.mask;

/**
	A MaskingDocument element that lets input unsigned Longs.
	This mask can be used for finite numbers.
	Optionally a minumum and maximum can be passed, within 0
	and Long.MAX_VALUE.
*/

public class UnsignedLongMask extends UnsignedIntegerMask
{
	public UnsignedLongMask()	{
		this((Long)null);
	}

	public UnsignedLongMask(Long initial)	{
		this(initial, 0L, Long.MAX_VALUE);
	}

	public UnsignedLongMask(long min, long max)	{
		this((Long)null, min, max);
	}

	public UnsignedLongMask(Long initial, long min, long max)	{
		init(min, max, initial);
	}

	public UnsignedLongMask(String placeHolder, long min, long max)	{
		this(placeHolder, null, min, max);
	}

	public UnsignedLongMask(String placeHolder, Long initial, long min, long max)	{
		this.placeHolder = placeHolder;
		init(min, max, initial);
	}



//	protected void checkValue(String s)
//		throws NumberFormatException
//	{
//		long i = convertStringToNumber(s);
//		if (i > max)
//			throw new NumberFormatException("above maximum "+max);
//	}


	/** Returns the Long value from this mask or null if undefined. */
	public Long getLongValue()	{
		return getLongValueInternal();
	}
	

	/** Sets the integer value for this mask. */
	public void setLongValue(Long l)	{
		if (l == null)
			textRemoval(0, length());
		else
			setLongValueInternal(l.longValue());
	}

}