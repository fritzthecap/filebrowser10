package fri.gui.swing.filebrowser;

import java.io.*;
import java.awt.Point;
import java.util.Vector;	
import gnu.regexp.*;
import fri.util.regexp.*;
import fri.util.io.NewlineDetectingInputStreamReader;

/**
	Matches a File content against a pattern.
	The pattern can be single- or multiline.
	Sends numbered result lines to a
	SearchResultDispatcher if requested.
*/

public class SearchContentPattern implements SearchPattern
{
	private RE [] expr;
	private int defCompFlags = RE.REG_MULTILINE;
	private int compFlags;
	private RESyntax syntax;
	private boolean showFoundLines;
	private SearchResultDispatcher dlg;
	private String origPattern;
	private String origSyntaxString;
	private boolean ignoreCase, wordMatch, positive;
	private NewlineDetectingInputStreamReader newlineAwareReader;
	private InputStream in;
	
	
	public SearchContentPattern(
		String pattern,
		boolean ignoreCase,
		String regExpSyntax,
		boolean positive,
		boolean wordMatch,
		boolean showFoundLines,
		SearchResultDispatcher dlg)
	throws
		REException,
		ArrayIndexOutOfBoundsException
	{
		this.showFoundLines = showFoundLines;
		this.dlg = dlg;
		this.origPattern = pattern;
		this.wordMatch = wordMatch;
		this.ignoreCase = ignoreCase;
		this.origSyntaxString = regExpSyntax;
		this.positive = positive;
		
		if (Syntaxes.doWildcards(regExpSyntax))	{
			pattern = RegExpUtil.setDefaultWildcards(pattern);
			syntax = Syntaxes.getSyntax("PERL5");
		}
		else
		if (Syntaxes.doNoRegExp(regExpSyntax))	{
			pattern = RegExpUtil.setNoWildcards(pattern);
			syntax = Syntaxes.getSyntax("PERL5");
		}
		else	{
			syntax = Syntaxes.getSyntax(regExpSyntax);
		}

		compFlags = defCompFlags | (ignoreCase ? RE.REG_ICASE : 0);

		if (RegExpUtil.isMoreThanOnePattern(pattern) && !Syntaxes.canAlternate(regExpSyntax))	{
			String [] stok = RegExpUtil.parseAlternation(pattern);
			expr = new RE [stok.length];
			
			for (int i = 0; i < stok.length; i++)	{
				expr[i] = getRE(stok[i], compFlags, syntax, wordMatch);
			}
		}
		else	{
			expr = new RE [1];
			expr[0] = getRE(pattern, compFlags, syntax, wordMatch);
		}
	}
	
	
	private RE getRE(String patt, int compFlags, RESyntax syntax, boolean wordMatch) throws
		REException,
		ArrayIndexOutOfBoundsException
	{
		if (wordMatch)
			patt = RegExpUtil.getWordBoundsPattern(patt);
		//Thread.dumpStack();
		System.err.println("search pattern is >"+patt+"<");
		return new RE(patt, compFlags, syntax);
	}
	
	

	// interface SearchPattern

	public boolean match(SearchFile f)	{
		if (f.isDirectory())
			return false;
			
		if (dlg.canceled())
			return false;
			
		dlg.progress(1L);
		
		// look for Java-Newlines in search pattern
		if (origPattern.indexOf("\n") >= 0)	{	// algorithm MUST read all lines
			//System.err.println("matching multiline, positive "+positive);
			return matchMultiline(f);
		}
		else	{	// algorithm might not read all lines
			//System.err.println("matching singleline, positive "+positive);
			return matchSingleline(f);
		}
	}
	
	
	private BufferedReader createBufferedReader(SearchFile f)
		throws IOException
	{
		in = f.getInputStream(dlg);
		newlineAwareReader = new NewlineDetectingInputStreamReader(in);
		BufferedReader br = new BufferedReader(newlineAwareReader);
		return br;
	}


	private boolean matchSingleline(SearchFile f)	{
		BufferedReader br = null;
		boolean matched = (positive == false);
		
		try {
			br = createBufferedReader(f);

			int matchCount = 0;
			String line;
			StringBuffer sb = new StringBuffer();
			
			// read all lines and look for matches
			
			for (int lineNr = 1; dlg.canceled() == false && (line = br.readLine()) != null; lineNr++) {
				boolean found = false;
				Vector matchesList = null;
				
				// loop through alternative patterns
				for (int i = 0; i < expr.length; i++)	{
					if (showFoundLines || dlg.isReplacing())	{	// get all matches
						REMatch [] matches = expr[i].getAllMatches(line);
						boolean contained = (matches != null && matches.length > 0);
						
						if (contained && !positive)
							return false;	// found, but looking for files not containing pattern

						if (contained && positive)
							matched = found = true;	// found, looking for files that contain the pattern
						else
						if (!contained && !positive)
							found = true;	// not found, looking for files not containing pattern
						
						if (contained)	{
							matchCount = matchCount + matches.length;
							matchesList = collectMatches(matchesList, matches);
						}
					}
					else	{	// we need only first match
						REMatch match = expr[i].getMatch(line);
	
						if (match != null)
							return positive;	// finally will close file
					}
				}
				
				if (found && showFoundLines)	{
					sb.append(lineNr);
					sb.append(displayBlanks(lineNr));
					sb.append(line);
					sb.append("\n");	// newlines are NOT contained in readLine() return!
				}

				// consider text replacement of result dispatcher

				handleReplacement(lineNr == 1, f, line, matchesList, false);

			}	// end for all lines

			// display lines if matches have been found
			
			if (matched && showFoundLines && dlg.canceled() == false)	{
				sb.deleteCharAt(sb.length() - 1);	// delete newline at end
				
				try	{
					File file = f.getFile();
					dlg.showGrepResult(file, matchCount, sb.toString(), origPattern, origSyntaxString, ignoreCase, wordMatch);
				}
				catch (IOException e)	{	// do not interrupt search because an archive is invalid
					e.printStackTrace();
				}
			}

		}
		catch (IOException e)	{
			e.printStackTrace();
			return matched;
		}
		finally	{
			try { br.close(); } catch (Exception e) {}
			
			if (dlg.isReplacing())
				dlg.closeReplacement(f);
		}
		
		return matched;
	}


	
	private boolean matchMultiline(SearchFile f)	{
		BufferedReader br = null;
		boolean matched = (positive == false);
		
		try {
			// open the inputstream
			br = createBufferedReader(f);
			
			// read the whole file text while replacing platform newlines with \n
			// and creating a list of line start and end offsets

			String line;
			StringBuffer sb = new StringBuffer(in.available());
			Vector lineCoords = new Vector(128, 128);
			int start = 0, end;
			
			while (dlg.canceled() == false && (line = br.readLine()) != null)	{
				sb.append(line);
				sb.append("\n");	// newlines are NOT contained in readLine() return!
				
				end = start + line.length();
				Point p = new Point(start, end);
				lineCoords.addElement(p);
				
				start = end + 1;	// after newline
			}
			
			String text = sb.toString();

			if (text.length() <= 0)
				return positive;
			
			// now match at least one expression against text
			// if lines must be shown, match all expressions

			Vector foundLines = positive ? new Vector() : lineCoords;	// all lines are good when searching negative
			int matchCount = 0;
			Vector matchesList = null;
			
			for (int i = 0; dlg.canceled() == false && i < expr.length; i++)	{
				if (showFoundLines || dlg.isReplacing())	{	// get all matches
					REMatch [] matches = expr[i].getAllMatches(text);
					boolean contained = (matches != null && matches.length > 0);

					if (contained && !positive)
						return false;
						
					if (contained && positive)
						collectFoundLines(matches, lineCoords, foundLines);
					
					if (contained)	{
						matchCount = matchCount + matches.length;
						matchesList = collectMatches(matchesList, matches);
					}
				}
				else	{	// we need only first match
					REMatch match = expr[i].getMatch(text);

					if (match != null)
						return positive;	// finally will close file
				}
			}
			
			// now build display result and dispatch it if matches have been found
			
			matched = foundLines.size() > 0;

			if (matched && showFoundLines && dlg.canceled() == false)	{
				// make display text from found lines
				sb.setLength(0);
				
				for (int i = 0; i < foundLines.size(); i++)	{
					Point p = (Point)foundLines.elementAt(i);
					String s = text.substring(p.x, p.y);
					appendLineWithoutNewline(sb, lineCoords.indexOf(p) + 1, s);
					if (i < foundLines.size() - 1)
						sb.append("\n");
				}
				
				try	{
					File file = f.getFile();
					dlg.showGrepResult(file, matchCount, sb.toString(), origPattern, origSyntaxString, ignoreCase, wordMatch);
				}
				catch (IOException e)	{
					e.printStackTrace();
				}
			}

			// consider text replacement of result dispatcher

			handleReplacement(true, f, text, matchesList, true);

		}
		catch (IOException e)	{
			e.printStackTrace();
			return matched;
		}
		finally	{
			try { br.close(); } catch (Exception e) {}
			
			if (dlg.isReplacing())
				dlg.closeReplacement(f);
		}
		
		return matched;
	}



	private Vector collectMatches(Vector matchesList, REMatch [] matches)	{
		for (int i = 0; dlg.isReplacing() && i < matches.length; i++)	{
			if (matchesList == null)
				matchesList = new Vector(matches.length);
			matchesList.addElement(matches[i]);
		}
		return matchesList;
	}
	

	private void handleReplacement(
		boolean isFirst,
		SearchFile f,
		String text,
		Vector matchesList,
		boolean isWholeText)
	{
		if (dlg.isReplacing())	{
			if (isFirst)
				dlg.openReplacement(f);
			
			REMatch [] matches = (matchesList != null && matchesList.size() > 0) ?
					(REMatch[])matchesList.toArray(new REMatch[matchesList.size()]) :
					null;
			
			if (isWholeText)
				dlg.replaceText(text, newlineAwareReader.getNewline(), matches, wordMatch);
			else
				dlg.replaceLine(text, newlineAwareReader.getNewline(), matches, wordMatch);
		}
	}
	


	private void collectFoundLines(
		REMatch [] matches,	// matched text positions
		Vector lineCoords,	// contains all line coordinates
		Vector foundLines)	// will contain found line coordinates after call
	{
		for (int mi = 0; mi < matches.length; mi++)	{
			int start = matches[mi].getStartIndex();
			int end = matches[mi].getEndIndex();

			// search the match in line list
			boolean again = true;
			for (int pi = 0, begin = -1; again && pi < lineCoords.size(); pi++)	{
				Point p = (Point)lineCoords.elementAt(pi);

				if (p.x <= start && p.y >= start)	{	// start innerhalb der Zeile
					begin = pi;
				}
				
				if (begin >= 0)	{	// in match
					if (foundLines.indexOf(p) < 0)
						foundLines.addElement(p);
					
					if (p.x <= end && p.y >= end)	{	// end innerhalb der Zeile
						again = false;
					}
				}	// end if in match
			}	// end for all lines
		}	// end for all matches
	}


	private void appendLineWithoutNewline(StringBuffer sb, int lineNr, String line)	{
		sb.append(lineNr);
		sb.append(displayBlanks(lineNr));
		sb.append(line);
	}
	
	
	private String displayBlanks(int lineNo)	{
		int len = Integer.toString(lineNo).length();
		if (len == 1) return "    ";
		if (len == 2) return "   ";
		if (len == 3) return "  ";
		if (len == 4) return " ";
		return "";
	}
	
}