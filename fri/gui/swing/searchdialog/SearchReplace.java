package fri.gui.swing.searchdialog;

import java.awt.Point;
import javax.swing.JFrame;
import java.util.Vector;
import gnu.regexp.RE;
import fri.util.text.lines.TextlineList;
import fri.gui.text.TextHolder;

/**
	A Search and Replace implementation for TextAreas.

	@author Fritz Ritzberger
*/

public class SearchReplace extends AbstractSearchReplace
{
	/** Create a nonmodal search dialog that offers replacement only when textarea is editable. */
	public SearchReplace(JFrame f, TextHolder textarea) {
		super(f, textarea);
	}

	/** Create a nonmodal search dialog that offers replacement when mode is SEARCH_REPLACE. */
	public SearchReplace(JFrame f, TextHolder textarea, int mode) {
		super(f, textarea, mode);
	}


	/** Returns the match that is the next (ascending) from passed caret position. */
	protected int getMatchIndexByCaretImpl(int caret)	{
		Object o = getListData();
		if (o instanceof TextlineList)	{
			TextlineList textlines = (TextlineList)o;
			return textlines.getMatchIndexByCaret(caret);
		}
		return 0;
	}

	/** Returns the match range of passed index from search result. */
	protected Point getMatchRangeImpl(int index)	{
		Object o = getListData();
		if (o instanceof TextlineList)	{
			TextlineList textlines = (TextlineList)o;
			return textlines.getRangeByMatchIndex(index);
		}
		return null;
	}


	/**
		Search for given pattern in TextHolder, generate result
		list, and insert results into JList.
		Following variables are evaluated: matches (list of found locations),
		currMatchNo (the number of the match that was selected in textarea).
		@param expr the compiled regular expression to search for.
		@returns the new search result that is to be put into result list.
	*/
	protected Vector newSearch(RE expr)	{
		// Text neu holen und Liste aufbauen
		String textbuffer = textarea.getText();
		TextlineList textlines = new TextlineList(textbuffer);

		// Suchen in Text
		matches = expr.getAllMatches(textbuffer);

		boolean found = false;
		currMatchNo = 0;
		
		// Matches in die Text-Liste einfuegen
		for (int i = 0; i < matches.length; i++) {
			int [] startEnd = getRealStartEnd(matches[i], textbuffer);
			int start = startEnd[0];
			int end   = startEnd[1];
			
			textlines.insertMatch(start, end);
			
			if (!found && startSelection <= start)	{
				currMatchNo = i;
				found = true;
			}
		}
		
		return textlines;	// Ergebnis in der JList anzeigen
	}


	/** Replaces text and sets changed text to textarea. */
	protected boolean replace(boolean all)	{
		TextlineList tl = (TextlineList)getListData();
		
		if (tl != null && tl.size() > 0)	{
			int caret = textarea.getCaretPosition();	// save caret position
			String replace = getReplacementText();
			String newText;
			
			if (all)	{	// alle Vorkommen ersetzen
				startSelection = 0;
				newText = tl.replaceAll(replace);
			}
			else	{	// nur naechstes Vorkommen ersetzen
				newText = tl.replace(replace, startSelection);
			}

			setReplacedTextToTextHolder(newText);
			// try to restore caret position
			try	{ textarea.setCaretPosition(caret); }	catch (IllegalArgumentException e)	{}

			return true;
		}

		return false;
	}


	/** Override this when actions are necessary on setting replaced text. Sets replaced text to textarea. */
	protected void setReplacedTextToTextHolder(String newText)	{
		textarea.setText(newText);
	}
	
	


	// test main
	public static void main(String [] args)	{
		final JFrame frame = new JFrame();
		//final fri.gui.swing.text.OutputTextArea ta = new fri.gui.swing.text.OutputTextArea();
		final javax.swing.JTextArea ta = new javax.swing.JTextArea();
		ta.setText("Hallo Welt!\nWie gehts?\nMuss ja gehn.");
		ta.addKeyListener(new java.awt.event.KeyAdapter()	{
			SearchReplace sr;
			public void keyPressed(java.awt.event.KeyEvent e)	{
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_F && e.isControlDown())	{
					if (sr == null)	{
						sr = new SearchReplace(frame, (TextHolder)ta);
					}
					else	{
						sr.init((TextHolder)ta, true);
						sr.setVisible(true);
					}
				}
				else
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_F3)	{
					if (sr == null)
						sr = new SearchReplace(frame, (TextHolder)ta);
					else
						sr.findNext();
				}
			}
		});
		frame.getContentPane().add(new javax.swing.JScrollPane(ta));
		frame.pack();
		frame.show();
	}

}

