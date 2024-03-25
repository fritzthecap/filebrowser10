package fri.util.text;

public abstract class Trim
{
	/**
		Replace all whitespace amounts and tab by one blank.
		Remove trailing spaces.
		@param s String to trim, must *NOT* be null
	*/
	public static String removeSpaceAmounts(String s)	{
		return removeSpaceAmounts(s, false);
	}
	
	/**
		Replace all whitespace amounts and tab by one blank.
		Remove trailing spaces, remove leading when exceptLeadingSpaces is false.
		Nevertheless, if exceptLeadingSpaces is true, a line that contains only
		spaces will result in an empty string.
		@param s String to trim, must *NOT* be null
		@param exceptLeadingSpaces do not remove leading spaces (Phyton!)
			except when line is empty
	*/
	public static String removeSpaceAmounts(String s, boolean exceptLeadingSpaces)	{
		StringBuffer sb = new StringBuffer(s.length());
		boolean wasSpace = true;	// true: remove leading spaces
		boolean exceptLeading = exceptLeadingSpaces;
		
		for (int i = 0; i < s.length(); i++)	{
			char c = s.charAt(i);
			
			if (c == '\r')	// ignore Bill Gates
				continue;
			
			if (!exceptLeading && c == '\t')
				c = ' ';
				
			boolean isSpace = Character.isWhitespace(c);
			
			if (isSpace == false)
				wasSpace = exceptLeading = false;
				
			if (exceptLeading || isSpace == false)	{
				sb.append(c);
			}
			else
			if (isSpace)	{
				if (!wasSpace)
					sb.append(c);
				
				wasSpace = true;
			}
		}
		
		// delete trailing space, can be just one
		if (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1)))
			sb.deleteCharAt(sb.length() - 1);

		// when NOT ignoring leading spaces, it could be just spaces, then ignore it
		// check last character to be whitespace
		if (sb.length() > 0 && exceptLeadingSpaces && Character.isWhitespace(sb.charAt(sb.length() - 1)))
			sb.setLength(0);
		
		return sb.toString();
	}


	/** Removes trailing digits from passed String. @return new String without trailing digits, "" when null. */
	public static String removeTrailingDigits(String s)	{
		if (s == null)
			return "";
		int i = s.length() - 1;
		for (; i >= 0 && Character.isDigit(s.charAt(i)); i--)
			;
		return s.substring(0, i + 1);
	}

	/** Removes trailing spaces (ASCII 32 only) from passed String. @return new String without trailing spaces, null when null. */
	public static String removeTrailingSpaces(String s)	{
		if (s == null)
			return s;
		int i = s.length() - 1;
		for (; i >= 0 && s.charAt(i) == ' '; i--)
			;
		return s.substring(0, i + 1);
	}



	private Trim()	{}	// do not instantiate


	public static void main(String [] args)	{
		System.err.println(">"+removeSpaceAmounts("  Hallo  Welt   !	Wie gehts    ?\r\n", true)+"<");
		System.err.println(">"+removeSpaceAmounts("  \n", true)+"<");
		System.err.println(">"+removeTrailingDigits("button123")+"<");
		System.err.println(">"+removeTrailingDigits("button")+"<");
		System.err.println(">"+removeTrailingDigits("")+"<");
	}
}