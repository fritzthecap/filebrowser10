package fri.util.text;

public abstract class Indent
{
	/**
		Returns a less indented copy of the passed text.
		@param s text to ex-dent
		@param wasNewline true if a newline preceeds the text
	*/
	public static String exdent(String s, boolean wasNewline)	{
		StringBuffer t = new StringBuffer();
		int len = s.length();
		
		for (int i = 0; i < len; i++)	{
			char c = s.charAt(i);
			
			if (c == '\n')	{
				wasNewline = true;
				t.append(c);
			}
			else
			if (wasNewline && (c == '\t' || c == ' '))	{
				wasNewline = false;
			}
			else	{
				t.append(c);
				wasNewline = false;
			}
		}

		return t.toString();
	}


	/**
		Returns a more indented copy of the passed text.
		@param s text to indent
		@param wasNewline true if a newline preceeds the text
	*/
	public static String indent(String s, boolean wasNewline)	{
		StringBuffer t = new StringBuffer();
		int len = s.length();
		
		for (int i = 0; i < len; i++)	{
			char c = s.charAt(i);
			
			if (c == '\n')	{
				wasNewline = true;
				t.append(c);
				
				if (i < len - 1)
					t.append('\t');
			}
			else
			if (i == 0 && wasNewline)	{
				t.append('\t');
				t.append(c);
			}
			else	{
				t.append(c);
			}
		}
		return t.toString();
	}

	private Indent()	{}	// do not instantiate
}

