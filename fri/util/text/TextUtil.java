package fri.util.text;

import java.util.Vector;

public abstract class TextUtil
{
	/**
		Makes a Java identifier from passed string, replacing invalid chars by '_'.
		The returned string does not contain non-Java-identifier characters anymore.
	*/
	public static String makeIdentifier(String src)	{
		if (src == null)
			return null;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < src.length(); i++)	{
			char c = src.charAt(i);
			if (i == 0 && (c == '$' || !Character.isJavaIdentifierStart(c)) ||
					i != 0 && (c == '$' || !Character.isJavaIdentifierPart(c)))
			{
				c = '_';
			}
			sb.append(c);
		}
		return sb.toString();
	}


	/**
		Parses the passed string into tokens separated by spaces, respecting doublequotes.
		The string 'a "b c" d' is converted into ['a', 'b c', 'd'].
	*/
	public static String [] tokenizeDoubleQuote(String text)	{
		Vector v = tokenizeBySeparatorRespectQuotes(text, null, '"');
		String [] sarr = new String[v.size()];
		v.copyInto(sarr);
		return sarr;
	}
	
	/**
		Parses the passed string into tokens separated by any of the given separators, respecting doublequotes.
		Once a separator character was identified, no other will be recognized as separator (the first separator in
		string to parse will be the only one).
	*/
	public static Vector tokenizeBySeparatorRespectQuotes(String text, String separators, char quote)	{
		boolean separatorSpace = false;
		if (separators == null)
			separatorSpace = true;
			
		boolean wasQuote = false;
		String firstFound = null;
		StringBuffer sb = new StringBuffer();
		Vector v = new Vector();
		
		for (int i = 0; i < text.length(); i++)	{
			char c = text.charAt(i);
			
			if (c == quote)	{
				wasQuote = !wasQuote;
			}
			else
			if ((separatorSpace && Character.isWhitespace(c) || !separatorSpace && inChars(c, separators)) && wasQuote == false)	{
				if (!separatorSpace && firstFound == null)
					separators = firstFound = ""+c;	// found a separator character, now shrink to this one
				
				if (false == separatorSpace || sb.length() > 0)	{
					v.addElement(sb.toString());
					sb.setLength(0);
				}
			}
			else	{
				sb.append(c);
			}
		}
		
		if (false == separatorSpace || sb.length() > 0)
			v.addElement(sb.toString());
		
		return v;
	}


	private static boolean inChars(char c, String separators)	{
		for (int i = 0; i < separators.length(); i++)
			if (separators.charAt(i) == c)
				return true;
		return false;
	}


	/*
	/** Return true if the passed String contains a "\r" or "\n". *
	public static boolean hasAnyNewline(String s)	{
		return s != null && (s.indexOf("\n") >= 0 || s.indexOf("\r") >= 0);
	}
	
	/**
		Returns the line the offset lies within.
		Textbuffer is single- or multiline, the result can be single- or multiline.
	*
	public static String getLineFromOffset(int offset, String textbuffer)	{
		return getLineFromOffsets(offset, offset, textbuffer);
	}

	/**
		Returns the line the start and end offsets ly within.
		Textbuffer is single- or multiline, the result can be single- or multiline.
	*
	public static String getLineFromOffsets(int start, int end, String textbuffer)	{
		// search line begin
		if (start > end)
			throw new IllegalArgumentException("Start offset can not be bigger than end offset: "+start+", "+end);

		int lineStart = -1;
		for (int k = start; lineStart < 0 && k > 0; k--)	{
			char c = textbuffer.charAt(k);
			if (c == '\n' || c == '\r')
				lineStart = k + 1;
		}
		if (lineStart < 0)
			lineStart = 0;

		// search line end
		int lineEnd = -1;
		for (int k = end; lineEnd < 0 && k < textbuffer.length(); k++)	{
			char c = textbuffer.charAt(k);
			if (c == '\n' || c == '\r')
				lineEnd = k - 1;
		}
		if (lineEnd < 0)
			lineEnd = textbuffer.length();

		return textbuffer.substring(lineStart, lineEnd);
	}
	*/

	private TextUtil()	{}	// do not instantiate
}

