package fri.gui.swing.filebrowser;

import gnu.regexp.*;
import fri.util.regexp.*;

/**
	Matches a File name against a pattern. The only used wildcards
	are "*?[^]+" (no syntax chooser is provided in file pattern panel).
*/

public class SearchFilePattern implements SearchPattern
{
	private RE [] expr;
	private int defCompFlags = RE.REG_MULTILINE;
	private int compFlags;
	private RESyntax syntax = Syntaxes.getSyntax("PERL5");
	private boolean include, alwaysMatches = false;

	
	public SearchFilePattern(
		String pattern,
		boolean ignoreCase,
		boolean include)
	throws
		gnu.regexp.REException,
		ArrayIndexOutOfBoundsException
	{
		this.include = include;
		
		if (pattern.equals("") || pattern.equals("*"))	{
			alwaysMatches = true;
		}
		else	{
			compFlags = defCompFlags | (ignoreCase ? RE.REG_ICASE : 0);
			
			if (RegExpUtil.isMoreThanOnePattern(pattern))	{
				String [] stok = RegExpUtil.parseAlternation(pattern);
				expr = new RE [stok.length];
				
				for (int i = 0; i < stok.length; i++)	{
					expr[i] = getRE(stok[i], compFlags, syntax);
				}
			}
			else	{
				expr = new RE [1];
				expr[0] = getRE(pattern, compFlags, syntax);
			}
			//System.err.println("setting match pattern "+pattern);
		}
	}


	private RE getRE(String patt, int compFlags, RESyntax syntax)
	throws
		gnu.regexp.REException,
		ArrayIndexOutOfBoundsException
	{
		return new RE(RegExpUtil.setDefaultWildcards(patt), compFlags, syntax);
	}
		
	
	// interface SearchPattern

	public boolean match(SearchFile f)	{
		if (alwaysMatches)
			return include;
			
		String name = f.getName();
		//System.err.println("matching "+f+", include "+include);
		
		for (int i = 0; i < expr.length; i++)	{
			//System.err.println(" ... against "+expr[i]);
			boolean match = expr[i].isMatch(name);
			
			if (match)
				return include;
		}
		
		//System.err.println("not matched");
		return !include;
	}
	
}