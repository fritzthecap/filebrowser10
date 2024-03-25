package fri.gui.swing.filebrowser;

import java.io.*;
import java.util.*;
import gnu.regexp.REMatch;

/**
	Handles replacement in files. It receives either a whole file text
	or each line of the file. Each file replacement is wrapped in two
	calls: openReplacement() and closeReplacement().
	In between there is one call to replaceText() or 1-n calls to replaceLine().
	<p>
	This class is responsible for writing a substituted text to the file when
	any matches were found. If no matches were found, do nothing.
*/

public class SearchReplaceWriter
{
	private Vector lines = new Vector();
	private boolean changed;
	private boolean isTemporary;
	private int start, end;
	private String newline;
	
	
	public void openReplacement(SearchFile comingFile)	{
		init();
		isTemporary = comingFile.isTemporaryFile();
	}


	public void replaceLine(String line, String newline, REMatch [] matches, String replace, boolean wordMatch)	{
		if (isTemporary)
			return;
		
		this.newline = newline;	// store current newline sequence
		
		String replacedLine = line;
		
		if (matches != null && matches.length> 0)	{
			changed = true;	// set changed flag for saving the file after at closeReplacement()
			
			StringBuffer sb = new StringBuffer(line.length() + matches.length * replace.length());
			int prev = 0;
			
			for (int i = 0; i < matches.length; i++)	{
				start = matches[i].getStartIndex();
				end   = matches[i].getEndIndex();
				adjustWordMatch(wordMatch, line);	// adjusts start and end
				
				sb.append(line.substring(prev, start));
				sb.append(replace);
				
				prev = end;	// skip passed text
				start = line.length();	// for last part
			}
			
			if (prev < start)	// if something remained
				sb.append(line.substring(prev, start));	// do last part
			
			replacedLine = sb.toString();
		}
		
		lines.addElement(replacedLine);
	}


	public void replaceText(String text, String newline, REMatch [] matches, String replace, boolean wordMatch)	{
		replaceLine(text, newline, matches, replace, wordMatch);	// result is now in lines list
		
		if (changed)	{	// split text into lines for close
			text = (String)lines.elementAt(0);
			
			lines.removeAllElements();	// there will be lines instead of whole text

			int i = 0, prev = 0;

			for (; i < text.length(); i++)	{
				char c = text.charAt(i);
				
				if (c == '\n')	{
					lines.addElement(text.substring(prev, i));
					
					if (prev != i && i == text.length() - 1)	{	// not empty line, newline at end
						lines.addElement("");
					}

					prev = i + 1;
				}
			}
			
			if (prev < i)	{	// there was no newline at end
				lines.addElement(text.substring(prev, i));
			}
		}
	}


	public void closeReplacement(SearchFile passedFile)	{
		// avoid multipe calls, do not write if not changed or if temporary file
		if (changed && !isTemporary)	{
			System.err.println("writing substituted file: "+passedFile.getName());
			BufferedWriter bw = null;
		
			try	{
				// the file was closed by caller
				bw = new BufferedWriter(new FileWriter(passedFile.getFile()));
				
				for (int i = 0; i < lines.size(); i++)	{
					String line = (String)lines.elementAt(i);
					bw.write(line, 0, line.length());
					
					if (i < lines.size() - 1)
						bw.write(newline, 0, newline.length());
				}
			}
			catch (IOException e)	{
				e.printStackTrace();
			}
			finally	{
				try	{ bw.close(); } catch (Exception e)	{}
			}
		}
		
		init();
	}


	private void init()	{
		changed = false;
		lines.removeAllElements();
	}


	// adjust wordMatch offsets as separators have been integrated
	private void adjustWordMatch(boolean wordMatch, String text)	{
		if (wordMatch)	{
			if (start > 0)
				start++;

			if (end < text.length())
				end--;
		}
	}

}