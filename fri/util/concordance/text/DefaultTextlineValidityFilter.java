package fri.util.concordance.text;

import fri.util.concordance.ValidityFilter;

/**
	A concordance text filter that checks if a letter or digit is within text.
*/

public class DefaultTextlineValidityFilter implements ValidityFilter
{
	/** Returns trimmed line when letter or digit was found, else null. */
	public Object isValid(Object line)	{
		String s = line.toString().trim();
		
		// check if anything semantic is within line
		for (int i = 0; i < s.length(); i++)	{
			char c = s.charAt(i);
			if (Character.isLetterOrDigit(c))
				return s;
		}
		
		return null;
	}

}

