package fri.util.regexp;

import gnu.regexp.*;

/**
	Provide Syntax Strings and their RESyntax objects
*/

public abstract class Syntaxes
{
	private final static String [] syntaxes =	{
		"No Regular Expressions",
		"Wildcards *?[^]+",
		"EMACS",
		"AWK",
		"SED",
		"ED",
		"EGREP",
		"GREP",
		"PERL5",
		"PERL5_S",
		"POSIX_AWK",
		"POSIX_BASIC",
		"POSIX_EGREP",
		"POSIX_EXTENDED",
		"POSIX_MINIMAL_BASIC",
		"POSIX_MINIMAL_EXTENDED",
	};


	private Syntaxes()	{}
	

	/** get a choice of syntaxes */
	public static String [] getSyntaxes()	{
		return syntaxes;
	}


	/** decide if the chosen item means: no regexp, but wildcards */
	public static boolean doWildcards(String name)	{
		return name.toLowerCase().startsWith("wildcards");
	}

	/** decide if the chosen item means: no regexp, no wildcards */
	public static boolean doNoRegExp(String name)	{
		return name.toLowerCase().startsWith("no reg");
	}
	
	/** Returns true if the passed syntax can do altrnations, expessed by "|" like "a|b|c". */
	public static boolean canAlternate(String name)	{
		if (name.equals( "ED") ||
				name.equals( "EMACS") ||
				name.equals( "SED") ||
				name.equals( "GREP") ||
				name.equals( "POSIX_BASIC") ||
				name.equals( "POSIX_MINIMAL_BASIC"))
			return false;
		return true;
	}
	
	/** recognize a chosen item and @return the syntax for "name" */
	public static RESyntax getSyntax(String name)	{
		RESyntax syntax = null;	// returned at "wildcards" or "no regular expressions"
			
		name = name.toUpperCase();
		
		if (name.equals( "AWK"))
			syntax = RESyntax.RE_SYNTAX_AWK;
		else
		if (name.equals( "ED"))
			syntax = RESyntax.RE_SYNTAX_ED;
		else
		if (name.equals( "EGREP"))
			syntax = RESyntax.RE_SYNTAX_EGREP;
		else
		if (name.equals( "EMACS"))
			syntax = RESyntax.RE_SYNTAX_EMACS;
		else
		if (name.equals( "GREP"))
			syntax = RESyntax.RE_SYNTAX_GREP;
		else
		if (name.equals( "PERL5"))
			syntax = RESyntax.RE_SYNTAX_PERL5;
		else
		if (name.equals( "PERL5_S"))
			syntax = RESyntax.RE_SYNTAX_PERL5_S;
		else
		if (name.equals( "POSIX_AWK"))
			syntax = RESyntax.RE_SYNTAX_POSIX_AWK;
		else
		if (name.equals( "POSIX_BASIC"))
			syntax = RESyntax.RE_SYNTAX_POSIX_BASIC;
		else
		if (name.equals( "POSIX_EGREP"))
			syntax = RESyntax.RE_SYNTAX_POSIX_EGREP;
		else
		if (name.equals( "POSIX_EXTENDED"))
			syntax = RESyntax.RE_SYNTAX_POSIX_EXTENDED;
		else
		if (name.equals( "POSIX_MINIMAL_BASIC"))
			syntax = RESyntax.RE_SYNTAX_POSIX_MINIMAL_BASIC;
		else
		if (name.equals( "POSIX_MINIMAL_EXTENDED"))
			syntax = RESyntax.RE_SYNTAX_POSIX_MINIMAL_EXTENDED;
		else
		if (name.equals( "SED"))
			syntax = RESyntax.RE_SYNTAX_SED;
			
		return syntax;
	}

}