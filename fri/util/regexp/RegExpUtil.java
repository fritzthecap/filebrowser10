package fri.util.regexp;

import java.util.*;
import java.io.*;
import gnu.regexp.*;
import fri.util.props.ClassProperties;

/**
	<UL>
	<LI>Ziel:
		Implementierung einfacher Shell-Wildcards "*?[^]".
		Filtern einer Liste von Objekten nach einem uebergenenen
		Suchbegriff, der Shell-Wildcards enthalten kann.
	<LI>Verhalten von getFiltered() und getFilteredExtended():
		Ist das include-Flag gesetzt werden nur Objekte zurueckgeliefert,
		deren toString() Methode Strings liefert, die dem Suchbegriff
		entsprechen. Ist es auf false gesetzt, wird die Komplementaermenge
		geliefert.
	<LI>Fehler:
		Nicht synchronisiert (nicht thread-safe)!
	</UL>
*/

public abstract class RegExpUtil
{
	/** Zeichenfolge, die alternierende Muster voneinander trennt: "|" */
	public static final String ALTERNATE_SEPARATOR;
	/** Zeichen, das alternierende Muster voneinander trennt: '|' */
	private static final char ALTERNATE_SEPARATOR_CHAR;
	/** Zeichen, die Wortgrenzen sind, als "Character-Class". */
	private static final String WORD_BOUNDS;
	
	static	{
		String s;
		s = ClassProperties.get(RegExpUtil.class, "regexpWordBoundChars");
		if (s == null)
			WORD_BOUNDS = "[^\\w_]";	// "[^0-9A-Za-z_�������]"
		else
			WORD_BOUNDS = s;

		s = ClassProperties.get(RegExpUtil.class, "alternatingSeparator");
		if (s == null)
			ALTERNATE_SEPARATOR = "|";
		else
			ALTERNATE_SEPARATOR = s;
			
		ALTERNATE_SEPARATOR_CHAR = ALTERNATE_SEPARATOR.charAt(0);
	}


	/** Returns true if one of "?*+[^]" is contained. Respects backslash as masking character. */
	public static boolean containsDefaultWildcards(String s)	{
		boolean valid = true;
		
		for (int i = 0; i < s.length(); i++)	{
			char c = s.charAt(i);
			
			switch (c)	{
				case '\\':
					valid = !valid;
					break;
				case '?':
				case '*':
				case '+':
				case '[':
				case ']':
				case '^':
					if (valid)
						return true;
					valid = true;
					break;
				default:
					valid = true;
					break;
			}
		}
		return false;
	}
	
		
	
	/**
		Prueft ob unmaskierte Alternativ-Separatoren "|" im Filtertext sind.
	*/
	public static boolean isMoreThanOnePattern(String filtertext)	{
		if (filtertext == null)
			return false;
			
		// pattern contains alternating separator, look for escaped ones
		boolean escape = false;

		for (int i = 0; i < filtertext.length(); i++)	{
			char c = filtertext.charAt(i);

			if (c == '\\')	{
				escape = !escape;
			}
			else
			if (c == ALTERNATE_SEPARATOR_CHAR)	{
				if (!escape)
					return true;
				escape = false;
			}
			else	{
				escape = false;
			}
		}
		
		return false;
	}
	
	/**
		Returns a String array made of all alternating patterns in pattern.
		From "a|b|c" you get [ "a", "b", "c" ].
	*/
	public static String [] parseAlternation(String pattern)	{
		if (pattern == null)
			return new String[0];
			
		Vector v = new Vector();
		boolean escape = false;
		StringBuffer sb = new StringBuffer();
			
		for (int i = 0; i < pattern.length(); i++)	{
			char c = pattern.charAt(i);

			if (c == '\\')	{
				escape = !escape;
				sb.append(c);
			}
			else
			if (c == ALTERNATE_SEPARATOR_CHAR)	{
				if (!escape)	{
					v.add(sb.toString());
					sb = new StringBuffer();
				}
				else	{
					sb.append(c);
				}
				escape = false;
			}
			else	{
				escape = false;
				sb.append(c);
			}
		}

		if (sb.length() > 0)
			v.add(sb.toString());
		
		String [] result = new String [v.size()];
		v.copyInto(result);
		
		return result;
	}
	
	/**
		Filtere die uebergebene Liste, wobei der Suchbegriff mehrere Alternativen
		enthalten kann. Die Alternativen sind mittels ALTERNATE_SEPARATOR "|" zu
		trennen ("*.java|*.class").
		@param filtertext Suchbegriff(e)
		@param names Liste der Objekte, die mit ihrer toString() Methode zu filtern ist
		@param include wenn true wird die dem Suchbegriff entsprechende Menge geliefert, sonst die nicht entsprechende
		@return gefilterte, neu allokierte Liste (die uebergebene bleibt unveraendert)
	*/
	public static Vector getFilteredAlternation(
		String filtertext,
		Vector names,
		boolean include,
		boolean caseSensitive)
	{
		if (!isMoreThanOnePattern(filtertext))
			return getFiltered(filtertext, names, include, caseSensitive);

		String [] stok = parseAlternation(filtertext);
		Vector [] varr = new Vector [stok.length];
		int anz = 0;
		int min = 0;
		
		int ii = 0;
		for (int i = 0; i < stok.length; i++)	{
			Vector v = getFiltered(stok[i], names, include, caseSensitive);
			
			if (v != null)	{
				varr[ii] = v;
				ii++;
				anz += v.size();
				min = min == 0 ? anz : anz < min ? anz : min;
			}
		}
		
		// get results together
		Vector erg = new Vector(include ? anz : min);
		
		if (include)	{	// vereinige Mengen eindeutig
			for (int j = 0; j < varr.length && varr[j] != null; j++)	{
				for (int i = 0; i < varr[j].size(); i++)	{
					Object o = varr[j].elementAt(i);
					if (erg.contains(o) == false)
						erg.addElement(o);
				}
			}
		}
		else	{	// exclude, Durchschnitt aller Mengen
			for (int j = 0; j < varr.length && varr[j] != null; j++)	{
				for (int i = 0; i < varr[j].size(); i++)	{
					Object o = varr[j].elementAt(i);
					
					boolean found = true;	// contained in all lists?
					for (int k = 0; found && k < varr.length && varr[k] != null; k++)
						if (varr[k].contains(o) == false)
							found = false;
							
					if (found && erg.contains(o) == false)
						erg.addElement(o);
				}
			}
		}
		return erg;
	}
	

	/**
		Filtere die uebergebene Liste nach dem uebergenenen Suchbegriff.
		@param filtertext Suchbegriff
		@param names Liste der Objekte, die mit ihrer toString() Methode zu filtern ist
		@return gefilterte, neu allokierte Liste (die uebergebene bleibt unveraendert)
	*/
	public static Vector getFiltered(
		String filtertext,
		Vector names)
	{
		return getFiltered(filtertext, names, true);
	}

	/**
		Filtere die uebergebene Liste nach dem uebergenenen Suchbegriff.
		@param filtertext Suchbegriff
		@param names Liste der Objekte, die mit ihrer toString() Methode zu filtern ist
		@param names Liste der Objekte, die mit ihrer toString() Methode zu filtern ist
		@return gefilterte, neu allokierte Liste (die uebergebene bleibt unveraendert)
	*/
	public static Vector getFiltered(
		String filtertext,
		Vector names,
		boolean include)
	{
		return getFiltered(filtertext, names, true, true);
	}

	/**
		Filtere die uebergebene Liste nach dem uebergenenen Suchbegriff.
		Es kann angegeben werden, ob der Suchbegriff ausschliessend oder
		einschliessend wirkt.
		Ist filtertext null, "" oder "*" wird die uebergebene Liste
		zurueckgeliefert, sonst eine neu allokierte.
		@param filtertext Suchbegriff
		@param names Liste der Objekte, die mit ihrer toString() Methode zu filtern ist
		@param include Die dem Suchbegriff entsprechende Menge oder ihr Komplement
		@return gefilterte, neu allokierte Liste (die uebergebene bleibt unveraendert)
	*/
	public static Vector getFiltered(
		String filtertext,
		Vector names,
		boolean include,
		boolean caseSensitive)
	{
		//System.err.println("RegExpUtil.getFiltered getting: "+names);
		if (names == null)
			return null;

		if (filtertext == null || filtertext.equals("") || filtertext.equals("*"))
			if (include)
				return names;
			else
				return null;

		filtertext = setDefaultWildcards(filtertext);

		RE expr = caseSensitive ? getExpression(filtertext) : getExpressionIgnoreCase(filtertext);
		if (expr == null)
			return names;

		Vector v = new Vector(names.size());
		for (int i = 0; i < names.size(); i++)	{
			Object o = names.elementAt(i);

			String name;
			if (o instanceof MatchStringProducer)
				name = ((MatchStringProducer)o).getMatchString();
			else
				name = o.toString();

			boolean ismatch = expr.isMatch(name);
			if (ismatch && include || !ismatch && !include)
				v.addElement(o);
		}
		//System.err.println("RegExpUtil.getFiltered matches: "+v);
		return v;
	}


	/**
		Compare a name with a pattern.
		@param pattern regular expression using "?*[^]"
		@param name name to match
		@return true if name is a specialization of pattern
	*/
	public static boolean match(String name, String pattern)	{
		RE expr = getDefaultExpression(pattern);
		if (expr == null)
			return false;
		//System.err.println("matching name \""+name+"\" to pattern \""+pattern+"\"");
		return expr.isMatch(name);
	}

	/**
		Compare a name with a pattern.
		@param pattern regular expression using "?*[^]"
		@param name name to match
		@return true if name is a specialization of pattern
	*/
	public static boolean matchIgnoreCase(String name, String pattern)	{
		RE expr = getDefaultExpressionIgnoreCase(pattern);
		if (expr == null)
			return false;
		//System.err.println("matching name "+name+" to pattern "+pattern);
		return expr.isMatch(name);
	}


	/**
		Compare a name with a pattern. Pattern can contain more than one pattern,
		separated by " " (spaces).
		@param pattern regular expression using "?*[^]", more than one possible
		@param name name to match
		@return true if name is a specialization of pattern
	*/
	public static boolean matchAlternation(String name, String pattern)	{
		if (!isMoreThanOnePattern(pattern))
			return match(name, pattern);
			
		String [] stok = parseAlternation(pattern);
		for (int i = 0; i < stok.length; i++)	{
			if (match(name, stok[i]))
				return true;
		}
		return false;
	}



	public static RE getExpression(String filtertext)	{
		return getExpr(filtertext, 0);
	}
	
	public static RE getExpressionIgnoreCase(String filtertext)	{
		return getExpr(filtertext, RE.REG_ICASE);
	}
	
	private static RE getExpr(String filtertext, int compFlag)	{
		RE expr = null;
		try	{
			expr = new RE(filtertext, compFlag);
		}
		catch (gnu.regexp.REException e)	{
			e.printStackTrace();
			System.err.println("FEHLER: "+filtertext);
		}
		return expr;
	}
	
	
	/**
		A Cient that buffers expressions uses this to retrieve the expression.
	*/
	public static RE getDefaultExpression(String pattern)	{
		return getExpression(setDefaultWildcards(pattern));
	}
	
	
	/**
		A Cient that buffers expressions uses this to retrieve the expression.
	*/
	public static RE getDefaultExpressionIgnoreCase(String pattern)	{
		return getExpressionIgnoreCase(setDefaultWildcards(pattern));
	}
	

	/**
		A Client that buffers expressions can use this match method.
		@param expression must be RE expression
	*/
	public static boolean matchExpression(Object expression, String name)	{
		if (expression == null)
			return false;
		RE expr = (RE)expression;
		return expr.isMatch(name);
	}
	

	/**
		Als wildcards werden behandelt "*?[^]"
		Diese Methode arbeitet fuer PERL% Syntax!
		@return String der als Pattern verwendet werden kann.
	*/
	public static String setDefaultWildcards(String such)	{
		// Definition der Wildcards
		such = escape(such);
		such = subMetaChar(such, "*", ".*");
		such = subMetaChar(such, "?", ".");
		//System.err.println("RegExpUtl.setDefaultWildcards >"+such+"<");
		return such;
	}

	private static String escape(String such)	{
		// Maskiere in Default-Wildcards nicht verwendete Meta-Zeichen: "\{}().$^"
		such = escape(such, '\\');	// muss erster sein
		such = escape(such, '{');
		such = escape(such, '}');
		such = escape(such, '(');
		such = escape(such, ')');
		such = escape(such, '.');
		such = escape(such, '$');
		//such = escape(such, '|');	// Verwehrt alternative Patterns bei "No Regular Expressions"
		//System.err.println("RegExpUtl.escape returns: "+such);
		return such;
	}

	/**
		Alle regular expression Metazeichen ausfluchten: woertliche Suche.
		Diese Methode arbeitet fuer PERL% Syntax!
		@return String der als woertlicher Suchbegriff verwendet werden kann.
	*/
	public static String setNoWildcards(String such)	{
		such = escape(such);
		// Maskiere in No-Regular-Expressions nicht verwendete Meta-Zeichen: "^*?[]+$"
		such = escape(such, '*');
		such = escape(such, '?');
		such = escape(such, '[');
		such = escape(such, ']');
		such = escape(such, '^');
		such = escape(such, '$');
		such = escape(such, '+');
		return such;
	}

	private static String subMetaChar(String text, String zeichen, String ersatz)	{
		RE expr = null;
		try	{
			expr = new RE("\\"+zeichen);	// fuer Erkennung alle Zeichen maskieren
		}
		catch (gnu.regexp.REException e)	{
			System.err.println("RegExpUtil.subMetaChar, REException: "+e.toString());
			return text;
		}
		return expr.substituteAll(text, ersatz);
	}
	
	// escape passed character, but respect already masked characters
	private static String escape(String text, char zeichen)	{
		StringBuffer sb = new StringBuffer();
		boolean mask = false;
		
		for (int i = 0; i < text.length(); i++)	{
			char c = text.charAt(i);
			
			switch (c)	{
				case '\\':
					if (zeichen != '\\')
						mask = !mask;
					else
						sb.append('\\');
					break;
				default:
					if (mask == false && c == zeichen)
						sb.append('\\');
					mask = false;
					break;
			}
			sb.append(c);
		}
		
		return sb.toString();
	}


	/**
		Fertigt aus einem uebergebene Pattern eines mit "Wortgrenzen" an.
		<pre>
		String bounds = "[^0-9A-Za-z_]";
		patt = bounds+patt+bounds+"|"+bounds+patt+"$|^"+patt+bounds+"|^"+patt+"$";
		</pre>
	*/
	public static String getWordBoundsPattern(String pattern)	{
		// pattern contains alternating separator, look for escaped ones
		String [] patterns = parseAlternation(pattern);
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < patterns.length; i++)	{
			String s = patterns[i];

			sb.append(WORD_BOUNDS);
			sb.append(s);
			sb.append(WORD_BOUNDS);
			sb.append("|");

			sb.append(WORD_BOUNDS);
			sb.append(s);
			sb.append("$");
			sb.append("|");

			sb.append("^");
			sb.append(s);
			sb.append(WORD_BOUNDS);
			sb.append("|");

			sb.append("^");
			sb.append(s);
			sb.append("$");

			if (i < patterns.length - 1)
				sb.append("|");
		}
		
		return sb.toString();
	}


	/**
		Returns true if the pattern would match everything, according to passed syntax.
		This returns true if pattern.equals(""),
		true if syntax == null and pattern.equals("*"),
		false if syntax == null and pattern.equals("*") == false.
	*/
	public static boolean alwaysMatches(String pattern, String syntax)	{
		if (pattern.equals(""))	// everything matches a "no-pattern"
			return true;

		if (syntax == null)	// no syntax was chooseable
			return pattern.equals("*");

		if (Syntaxes.doNoRegExp(syntax))
			return false;

		if (Syntaxes.doWildcards(syntax) && pattern.equals("*"))
			return true;

		if (pattern.equals(".*"))	// standard regular expression that matches everything
			return true;

		return false;
	}
	
	

	/**
		Liefert einen (mehrzeiligen) Help-Text zu Regular Expressions zurueck.
		Dies ist der Inhalt der Datei RegExp.txt.
	*/
	public static String getHelpText()	{
		try	{
			BufferedReader in = new BufferedReader(new InputStreamReader(RegExpUtil.class.getResourceAsStream("RegExp.txt")));
			StringBuffer sb = new StringBuffer(3300);
			char [] carr = new char[3300];
			int cnt;
			while ((cnt = in.read(carr)) != -1)	{
				sb.append(carr, 0, cnt);
			}
			return sb.toString();
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
		return null;
	}
	
	



	// test main
	/*
	public static void main(String [] args)	{
//		for (int i = 0; i < 256; i++)	{
//			System.err.println(""+i+", okt "+Integer.toOctalString(i)+", hex "+Integer.toHexString(i)+" = "+(char)i);
//		}
		
//		try	{
//			String patt = "[\141-\172\344]+";
//			RE expr = new RE(patt);
//			String test = ".a�b:";
//			REMatch match = expr.getMatch(test);
//			System.err.println("match for \""+patt+"\" in \""+test+"\" is: \""+match+"\"");
//		}
//		catch (gnu.regexp.REException e)	{
//			e.printStackTrace();
//		}

//		try	{
//			String patt = "[^\\w_]ab[^\\w_]";
//			RE expr = new RE(patt, RE.REG_MULTILINE, RESyntax.RE_SYNTAX_POSIX_MINIMAL_BASIC);
//			String test = ".ab:";
//			REMatch match = expr.getMatch(test);
//			System.err.println("match for \""+patt+"\" in \""+test+"\" is: \""+match+"\"");
//		}
//		catch (gnu.regexp.REException e)	{
//			e.printStackTrace();
//		}

//		String [] syntaxes = {
//			"EMACS",
//			"AWK",
//			"SED",
//			"ED",
//			"EGREP",
//			"GREP",
//			"PERL5",
//			"PERL5_S",
//			"POSIX_AWK",
//			"POSIX_BASIC",
//			"POSIX_EGREP",
//			"POSIX_EXTENDED",
//			"POSIX_MINIMAL_BASIC",
//			"POSIX_MINIMAL_EXTENDED"
//		};
//		
//		for (int i = 0; i < syntaxes.length; i++)	{
//			try	{
//				System.err.println("TESTING SYNTAX: "+syntaxes[i]);
//				RESyntax syntax = Syntaxes.getSyntax(syntaxes[i]);
//				String patt = "a|b";
//				RE expr = new RE(patt, RE.REG_MULTILINE, syntax);
//				String test = "xxxaxxx";
//				REMatch match = expr.getMatch(test);
//				System.err.println("match for \""+patt+"\" in \""+test+"\" is: \""+match+"\"");
//			}
//			catch (gnu.regexp.REException e)	{
//				e.printStackTrace();
//			}
//		}

//		try	{
//			RESyntax syntax = Syntaxes.getSyntax("PERL5");
//			String patt = getWordBoundsPattern("H�[l]*lo?");
//			RE expr = new RE(patt, RE.REG_MULTILINE, syntax);
//			System.err.println("num subs is: "+expr.getNumSubs());
//		}
//		catch (REException e)	{
//			e.printStackTrace();
//		}
//	}

//		String patt = "new*ile";
//		String s = "newfile and another newfile";
//		System.err.println("match ("+s+", "+patt+") "+match(s, patt));

//		try	{
//			RESyntax syntax = Syntaxes.getSyntax("PERL5");//EGREP");
//			String patt = getWordBoundsPattern("H�[l]*lo");
//			RE expr = new RE(patt, RE.REG_MULTILINE, syntax);
//			System.err.println("pattern is: "+patt);
//			
//			String [] test = new String [] {
//					"H�llo",
//					" H�llo ",
//					"H�lloWelt",
//					"A H�llllo� B", 
//					"C H�lo D",
//					"H� lo X",
//					"X H�lXXX XXXlo",
//					"AH�l lloZ" };
//					
//			for (int i = 0; i < test.length; i++)	{
//				REMatch match = expr.getMatch(test[i]);
//				System.err.println("match for \""+test[i]+"\" is: \""+match+"\"");
//			}
//		}
//		catch (gnu.regexp.REException e)	{
//			e.printStackTrace();
//		}

//		System.err.println(getHelpText());

//		String patt = "*foo(/)*";
//		String s = "private void foo(/)	{";
//		System.err.println("match (\""+s+"\", \""+patt+"\"); result is: "+match(s, patt));

//		try	{
//			RESyntax syntax = Syntaxes.getSyntax("PERL5");
//			String patt = "a/b/c";
//			RE expr = new RE(patt, RE.REG_MULTILINE, syntax);
//			String test = "/a/b/c/d";
//			REMatch match = expr.getMatch(test);
//			System.err.println("match for \""+patt+"\" in \""+test+"\" is: \""+match+"\"");
//		}
//		catch (gnu.regexp.REException e)	{
//			e.printStackTrace();
//		}

//		String s = "Hallo|Halo\\|Welt\\";
//		String [] stok = parseAlternation(s);
//		for (int i = 0; i < stok.length; i++)
//			System.err.println("next token is >"+stok[i]+"<");

//		System.err.println("token is >"+escape("C:\\newFolder1\\")+"<");
	}
	*/

}