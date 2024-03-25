package fri.util.text;

import java.util.*;
import java.io.*;

/**
	Parse a String by whitespace respecting "quote enclosions" and
	translating tokens \b \n \r \t \f and octals like \007,
	substitute $Variables or ${Variables} by their value from hashtable.
	This means that "a b" or 'a b' is returned in one String,
	but a b is returned in two Strings.
	Both quotes have the same preference,
	double quote " masks single quote ', and ' masks ".
	Back quote ` is not implemented.
*/

public abstract class CmdLineSubstitution
{
	private static String newline = System.getProperty("line.separator");
	private static boolean caseSensitive = true;

	/**
		Parse and substitute the command string into executable tokens.
		@param argStr String to parse
		@param env String with "name = value" lines of text for substitution
		@return array of Strings that is substituted vith values from env
	*/
	public static String[] parse(String argStr, String env)	{
		return parse(argStr, textToProps(env));
	}

	/**
		Parse and substitute the command string into executable tokens.
		@param argStr String to parse
		@param env String array with "name=value" for substitution
		@return array of Strings that is substituted vith values from env
	*/
	public static String[] parse(String argStr, String [] env)	{
		return parse(argStr, arrayToProps(env));
	}

	/**
		Parse and substitute the command string into executable tokens.
		@param argStr String to parse
		@param subHash names for $variables and their values
		@return array of Strings that is substituted vith values from hash
	*/
	public static String[] parse(String argStr, Hashtable hash)	{
		if (argStr == null || argStr.length() == 0)
			return new String[0];

		String[] args = parseArgumentString(argStr);
		return argSubstitution(args, hash);
	}


	/**
		Method to scan properties from a array where no escapes have been made
		@param string array containing a name=value pair in each string
		@return properties containing the environment
	*/
	public static Properties arrayToProps(String [] env)	{
		return textToProps(arrayToString(env));
	}

	/**
		Method to scan properties from a text where no escapes have been made.
		@param text containing a name=value pair on each line
		@return properties containing the environment
	*/
	public static Properties textToProps(String text)	{
		Properties props = new Properties();
		if (text == null || text.length() <= 0)
			return props;
			
		text = escapeBackslash(text);
		InputStream bis = new BufferedInputStream(new ByteArrayInputStream(text.getBytes()), text.length());
		try	{
			props.load(bis);
		}
		catch (IOException e)	{
		}

		return props;
	}

	/**
		@param each string in the array contains a name=value pair
		@return a text with each name=value pair on a seperate line
	*/
	public static String arrayToString(String [] env)	{
		if (env == null)
			return "";
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < env.length; i++)	{
			buf.append(env[i]);
			buf.append(newline);
		}
		return new String(buf);
	}

	/**
		@param text containing a name=value pair on each line
		@return array with a name=value pair in each string of the array
	*/
	public static String [] textToArray(String text)	{
		Properties props = textToProps(text);
		return propsToArray(props);
	}

	/**
		@param props containing environment
		@return array with a name=value pair in each string of the array
	*/
	public static String [] propsToArray(Properties props)	{
		String [] env = new String [props.size()];
		int i = 0;
		for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			String value;
			if ((value = props.getProperty(name)) == null)
				value = "";
			env[i] = name+"="+value;
			//System.err.println("env["+i+"] = "+env[i]);
			i++;
		}
		return env;
	}

	/**
		All following substitutions will be done ignoring case if set to false.
		@param sensitive value for case-sensitivity
	*/
	public static void setCaseSensitive(boolean sensitive)	{
		caseSensitive = sensitive;
	}



	// Each backslash gets masked by itself (character stuffing).
	private static String escapeBackslash(String text)	{
		if (text == null || text.length() <= 0)
			return "";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < text.length(); i++)	{
			char ch = text.charAt(i);
			if (ch == '\\')
				sb.append(ch);
			sb.append(ch);
		}
		return sb.toString();
	}


	// Substitution of variables

	private static String[] argSubstitution(String[] args, Hashtable vars)	{
		String[] result = new String[ args.length ];

		for ( int aIdx = 0 ; aIdx < args.length ; ++aIdx )	{
			String argStr = args[ aIdx ];
			int index = argStr.indexOf( '$' );
			if ( index < 0 )	{
				result[ aIdx ] = argStr;
				continue;
			}
			else	{
				result[ aIdx ] =
					stringSubstitution( argStr, vars );
			}
		}
		return result;
	}


	public static String stringSubstitution(String argStr, Hashtable vars)	{
		//System.err.println("string >"+argStr+"<");
		StringBuffer argBuf = new StringBuffer();
		boolean backSlash = false;

		if (caseSensitive == false && vars != null)	{
			Hashtable newvars = new Hashtable();
			for (Enumeration e = vars.keys(); e.hasMoreElements(); )	{
				String key = (String)e.nextElement();
				newvars.put(key.toLowerCase(), vars.get(key));
			}
			vars = newvars;
		}

		for ( int cIdx = 0 ; cIdx < argStr.length() ; )	{
			char ch = argStr.charAt( cIdx );

			switch ( ch )	{
				case '\\':
					if (backSlash)	{
						argBuf.append(ch);
					}
					backSlash = !backSlash;
					cIdx++;
					break;

				case '$':
					if (!backSlash)	{
						boolean subDone = false, wasDelim = false;
						StringBuffer nameBuf = new StringBuffer();
						cIdx++;

						for (int start = cIdx; cIdx < argStr.length() ; ++cIdx )	{
							ch = argStr.charAt( cIdx );

							if (cIdx == start && ch == '{')	{
								wasDelim = true;
							}
							else
							if (cIdx == start && Character.isJavaIdentifierStart(ch) ||
									cIdx > start && isVariableCharacter(ch))	{
								nameBuf.append( ch );
							}
							else	{
								break;
							}
						}

						if (vars != null && nameBuf.length() > 0)	{
							String s = nameBuf.toString();
							if (caseSensitive == false)
								s = s.toLowerCase();
							String value = (String)vars.get(s);
							if (value != null)	{
								argBuf.append(value);
								subDone = true;
							}
						}

						if (!subDone)	{
							if (wasDelim)
								argBuf.append("${"+nameBuf+"}");
							else
								argBuf.append("$"+nameBuf);
						}

						if (ch == '}')	// read away delimiter
							cIdx++;
					}
					else	{
						argBuf.append(ch);
						cIdx++;
					}
					backSlash = false;
					break;
				
				default:
					if (backSlash)
						argBuf.append('\\');
					backSlash = false;
					argBuf.append(ch);
					++cIdx;
					break;
			}
		}

		return argBuf.toString();
	}


	private static boolean isVariableCharacter(char ch)	{
		return
				ch == '_' ||
				ch == '-' ||
				ch == '.' ||
				Character.isJavaIdentifierPart(ch);
	}
	
	 
	private static String[] parseArgumentString(String argStr)	{
		String[] result = null;
		Vector vector = parseArgumentVector(argStr);

		if (vector != null && vector.size() > 0)	{
			result = new String[vector.size()];
			vector.copyInto(result);
		}
		
		return result;
	}


	private static Vector parseArgumentVector(String argStr)	{
		Vector			result = new Vector();
		StringBuffer	argBuf = new StringBuffer();

		boolean backSlash = false;
		boolean matchSglQuote = false;
		boolean matchDblQuote = false;

		for ( int cIdx = 0 ; cIdx < argStr.length() ; ++cIdx )	{
			char ch = argStr.charAt( cIdx );

			switch ( ch )
			{
				case ' ':
				case '\t':
				case '\n':
				case '\r':
					if ( backSlash ) {
						argBuf.append( ch );
						backSlash = false; 
					}
					else if ( matchSglQuote || matchDblQuote ) {
						argBuf.append( ch );
					}
					else if ( argBuf.length() > 0 ) {
						result.addElement( argBuf.toString() );
						argBuf.setLength( 0 );
					}
					break;

				case '\\':
					if ( backSlash || cIdx > 0 && argStr.charAt(cIdx - 1) == '\\')	{
					//if ( backSlash )
						argBuf.append( ch );
						//System.err.println("      addin backslash at "+cIdx);
					}
					backSlash = true;
					//backSlash = ! backSlash;
					break;

				case '\'':
					if ( backSlash ) {
						argBuf.append( ch );
						backSlash = false; 
					}
					else if ( matchSglQuote ) {
						result.addElement( argBuf.toString() );
						argBuf.setLength( 0 );
						matchSglQuote = false;
					}
					else if ( ! matchDblQuote ) {
						matchSglQuote = true;
					}
					else	{
						argBuf.append(ch);
					}
					break;

				case '"':
					if (backSlash) {
						argBuf.append( ch );
						backSlash = false; 
					}
					else if (matchDblQuote) {
						result.addElement( argBuf.toString() );
						argBuf.setLength( 0 );
						matchDblQuote = false;
					}
					else if ( ! matchSglQuote) {
						matchDblQuote = true;
					}
					else	{
						argBuf.append(ch);
					}
					break;

				default:
					if (backSlash) {
						argBuf.append( '\\' );
						argBuf.append( ch );
						backSlash = false;
					}
					else {
						argBuf.append( ch );
					}
					break;
			}
		}

		if ( argBuf.length() > 0 || backSlash) {
			if (backSlash) {
				argBuf.append('\\');
			}
		
			result.addElement( argBuf.toString() );
		}

		return result;
	}




	// test main

	private static void dump(String s, String [] envarr)	{
		System.err.println(">"+s+"<");
		dump(parse(s, envarr));
	}
	private static void dump(String s, String env)	{
		System.err.println(">"+s+"<");
		dump(parse(s, env));
	}
	private static void dump(String s, Hashtable hash)	{
		System.err.println(">"+s+"<");
		dump(parse(s, hash));
	}
	private static void dump(String [] sarr)	{
		for (int i = 0; i < sarr.length; i++)
			System.err.println("  "+i+" >"+sarr[i]+"<");
	}



	public static final void main(String [] args)	{
		Hashtable hash = new Hashtable();
		hash.put("fri", "fritz");
		hash.put("fra", "C:\\franz");
		hash.put("$", "Dollar");

		String erg = stringSubstitution("Hallo '$fri' an \"$fra\" \"$fra\", \\$fri", hash);
		System.err.println(erg);

		dump("brackets ${fri}_$fra ", hash);
		dump("quotes \"$fri $fra\"", hash);
		dump("masking quotes \\\"$fri $fra\\\" ", hash);
		dump("backslash E:\\\\$fri $fra \\$fri", hash);
		dump("quote preferences \"$fri $fra' \"$fri $fra'", hash);
		String env = new String("eins =EINS \\"+newline+" und EINS = "+newline+"zwei= ZWEI + ZWEI = VIER");
		dump("multiline-property-environment C:\\Projekte\\\\$eins ${zwei} \\$eins=$eins \\$zwei=$zwei\"", env);
		String [] envarr = new String [] {
			"eins = EINS \\"+newline+" und EINS",
			" zwei = ZWEI = 2"
		};
		dump("multiline-stringarray-environment $eins ${zwei}$", envarr);
		dump("no environment \\${eins}_$zwei$", (String[])null);
	}
}