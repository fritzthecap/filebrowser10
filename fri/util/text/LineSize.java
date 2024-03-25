package fri.util.text;

import java.util.StringTokenizer;

/**
 * Line count from a collection of lines, and maximum line length.
 */
public abstract class LineSize
{
	private static final String nl = System.getProperty("line.separator");
	
	/** Return number of lines of passed String, tokenized by platform <i>line.separator</i>. */
	public static int getLineCount(String s)	{
		if (s != null && s.length() > 0)	{
			StringTokenizer stok = new StringTokenizer(s, nl);
			return stok.countTokens();
		}
		return 0;
	}

	/** Return maximum line length of passed String, tokenized by platform <i>line.separator</i>. */
	public static int getMaximumLineLength(String s)	{
		int max = 0;
		
		if (s != null && s.length() > 0)	{
			StringTokenizer stok = new StringTokenizer(s, nl);
			
			while (stok.hasMoreTokens())	{
				String t = stok.nextToken();
				if (t.length() > max)
					max = t.length();
			}
		}
		
		return max;
	}


	private LineSize()	{}	// do not instantiate

}
