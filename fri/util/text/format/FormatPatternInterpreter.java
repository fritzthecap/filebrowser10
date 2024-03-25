package fri.util.text.format;

/**
	Parses format patterns like <code>new DecimalFormat().toPattern()</code>
	into its meaningful masks and its separator sections.
	For documentation of pattern see <code>java.text.DecimalFormat</code> and
	<code>java.text.SimpleDateFormat</code>. A mask consists of a sequence
	of one or more characters that are all the same character. A separator
	consists of any sequence of characters. Quotes are escaped by quotes
	(character stuffing): ''
	
  @author Ritzberger Fritz
*/

public class FormatPatternInterpreter
{
	/**
		Parse passed pattern and call back to passed semantic.
	*/
	public FormatPatternInterpreter(String pattern, FormatPatternSemantic consumer)	{
		char state = (char)0;
		StringBuffer sep = new StringBuffer();
		StringBuffer data = new StringBuffer();
		boolean openSeparator = false;
		
		// interpret pattern and call semantic
		for (int i = 0; i < pattern.length(); i++)	{
			char c = pattern.charAt(i);
			
			if (!openSeparator && consumer.isMaskCharacter(c))	{
				finishToken(consumer, sep, false);

				if (state != (char)0 && state != c)
					finishToken(consumer, data, true);

				data.append(c);
				state = c;
			}
			else	{	// in separator
				if (state != (char)0)
					finishToken(consumer, data, true);

				if (c == '\'')	{	// open separator string
					if (!openSeparator)	{
						openSeparator = true;
					}
					else	// is open separator
					if (i < pattern.length() - 1 && pattern.charAt(i + 1) == '\'')	{
						sep.append(c);
						i++;	// skip stuffed character
					}
					else	{	// is open separator
						openSeparator = false;
					}
				}
				else	{	// normal separator character
					sep.append(c);
				}

				state = (char)0;
			}
		}
		
		finishToken(consumer, sep, false);
		finishToken(consumer, data, true);
	}


	private void finishToken(FormatPatternSemantic consumer, StringBuffer s, boolean isMask)	{
		if (s.length() > 0)	{
			if (isMask)
				consumer.finishMask(s);
			else
				consumer.finishSeparator(s);
			
			s.setLength(0);
		}
	}

}