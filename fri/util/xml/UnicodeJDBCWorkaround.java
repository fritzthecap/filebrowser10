package fri.util.xml;

/**
	Convert some platform specific characters.
*/

public abstract class UnicodeJDBCWorkaround
{
	/**
		Convert characters between 0x80 und 0x9F for ISO8859-1.
	*/
	public static String filterChars(String s, String encoding)	{	// Zeichen unter 32 ausser 9 und 10 wegfiltern
		if (s == null)
			return null;

		StringBuffer buf = new StringBuffer();
		String errorChars = "";
		
		// workaround for WinWord characters
		boolean iso8859_1 = encoding == null || encoding.equals("ISO-8859-1");
		
		for (int i = 0 ; i < s.length(); i++)	{	// indicator state
			int c = (int)s.charAt(i), c1;
			if ((c1 = filterNonASCIIChars(c, iso8859_1)) != 0)	{
				if (c1 != 13)	{	// carr return.
					buf.append((char)c1);
					//System.err.println(">>"+(char)c+"<<  dez "+(int)c+" hex "+Integer.toHexString(c));
				}
			}
			else	{
				// FRi Workaround JDBC Unicode Bug
				// Unicode above 255 will be cutten by JDBC, so do not allow them here
				errorChars = errorChars+" "+(char)c+" (hex "+Integer.toHexString(c)+")";
				//System.err.println(errorChars);
			}
		}		

		if (errorChars.length() > 0)	{
			new Exception("unknown platform character(s): "+errorChars).printStackTrace();
		}

		return buf.toString();
	}


	private static int filterNonASCIIChars(int c, boolean iso8859_1)	{
		if (c < 32)	// control
			if (c == 9 ||	// tabulator
					c == 10 ||	// newline, APP_LINEBREAK
					c == 13)	// carr.return, no error
				return c;
			else
				return 0;
			
		/** Hartcodierte Spezialfaelle */
		
		
		if (c >= 0x201c && c <= 0x201f)	{	// quotes
			return '"';
		}
		if (c >= 0x2018 && c <= 0x201b)	{	// quotes
			return '\'';
		}

		if (c >= 0x2010 && c <= 0x2015)	{	// minus dashes
			return '-';
		}
		
		/** In ISO-8859-1 unbekannte Zeichen */
		
		if (iso8859_1)	{
			if (c >= 0x80 && c <= 0x9F)	{	// control
				return 0;
			}
		}
		
		return c;
		
		/*	
		if (c == 0x80)
			return 0;	// control
		if (c == 0x81)
			return 0;	// control
		if (c == 0x82)
			return ' ';	// break permitted here
		if (c == 0x83)
			return ' ';	// no break here
		if (c == 0x84)
			return 0;	// index
		if (c == 0x85)
			return '\n';	// next line
		if (c == 0x86)
			return 0;	// start selected area
		if (c == 0x87)
			return 0;	// end selected area
		if (c == 0x88)
			return 0;	// char tab set
		if (c == 0x89)
			return 0;	// char tab justification
		if (c == 0x8A)
			return 0;	// line tab
		if (c == 0x8B)
			return 0;	// partial line down
		if (c == 0x8C)
			return 0;	// partial line up
		if (c == 0x8D)
			return 0;	// reverse line feed
		if (c == 0x8E)
			return 0;	// single shift two
		if (c == 0x8F)
			return 0;	// single shift three
		if (c == 0x90)
			return 0;	// device control
		if (c == 0x91)
			return 0;	// private use one
		if (c == 0x92)
			return 0;	// private use two
		if (c == 0x93)>
			return 0;	// set transmit state
		if (c == 0x94)
			return 0;	// cancel char
		if (c == 0x95)
			return 0;	// message waiting
		if (c == 0x96)
			return 0;	// start guarded area
		if (c == 0x97)
			return 0;	// end guarded area
		if (c == 0x98)
			return 0;	// start string
		if (c == 0x99)
			return 0;	// control
		if (c == 0x9A)
			return 0;	// single char intro
		if (c == 0x9B)
			return 0;	// control sequence intro
		if (c == 0x9C)
			return 0;	// string terminator
		if (c == 0x9D)
			return 0;	// OS command
		if (c == 0x9E)
			return 0;	// privacy message
		if (c == 0x9F)
			return 0;	// application program command
		*/
	}

}
