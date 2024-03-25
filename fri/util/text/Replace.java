package fri.util.text;

public abstract class Replace
{
	/** Count the occurences of a character in a string */
	public static int getOccurrences(String s, String inStr)	{
		int cnt = 0, i = s.indexOf(inStr, 0);
		while (i >= 0)	{
			cnt++;
			i = s.indexOf(inStr, i + 1);
		}
		return cnt;
	}

	/**
	 * Replaces a part(s) of the source string by new contents The method replaces all occurences of searchFor in original
	 * string. It means that after first replacing new text (replaceWith) will not be searched for searchFor, and it's
	 * possible to make replacement of "%%tag%%" by "%%tag%%%%tag%%" without getting of infinitive cycle.
	 * @param source       a source string
	 * @param searchFor    a string which should be replaced
	 * @param replaceWith  a string which replaces searchFor
	 */
	public static String replace(String source, String searchFor, String replaceWith) {
		if (searchFor == null || searchFor.length() == 0) {
			return source;
		}
		
		int idx = 0;
		StringBuffer resultBuff = null; // buffer to store replaced string
		for (int foundIdx = -1; (foundIdx = source.indexOf(searchFor, idx)) >= 0; idx = foundIdx + searchFor.length()) {
			if (resultBuff == null) {
			    resultBuff = new StringBuffer(); // lazy initialization
			}
			resultBuff.append(source.substring(idx, foundIdx));
			resultBuff.append(replaceWith);
		}
		
		if (idx > 0) {
			resultBuff.append(source.substring(idx)); // put rest of source string to the buffer
			return resultBuff.toString();
		}
		else {
			return source; // no replacement made, return source string
		}
	}

	/** Fast replace method for many occurences of a short string. */
	public static String replaceMany(String text, String patt, String repl)	{
		StringBuffer buf = new StringBuffer(text.length());

		for (int i = 0 ; i < text.length(); i++)	{	// indicator state
			char c = text.charAt(i);

			if (c == patt.charAt(0))	{	// recognition state
				boolean found = i + patt.length() <= text.length();	// could be within text
				
				// check pattern matches
				for (int j = i + 1, p = 1; found && p < patt.length() && j < text.length(); j++, p++)	{
					if (text.charAt(j) != patt.charAt(p))	{
						found = false;
					}
				}

				if (found)	{
					i += patt.length() - 1;
					buf.append(repl);
				}
				else	{
					buf.append(c);
				}
			}
			else	{	// not recognition state
				buf.append(c);
			}
		}
		
		return buf.toString();
	}


	private Replace()	{}
}
