package fri.util.text.lines;

import java.awt.Point;
import java.util.Vector;

/**
	Verwaltung von Zeilen und Suchergebnissen eines Textes.

	@author Fritz Ritzberger
*/

public class TextlineList extends Vector
{
	private Vector lineCoords;
	private String text;
	private int baseLineNr;
	private Vector notWanted = null;
	private boolean lineNumbers = true;

	/**
		Anlegen einer Zeilen-Verwaltung aus einem String.
		@param text String, der als Zeilen-Liste verwaltet werden soll.
	*/
	public TextlineList(String text)	{
		this(text, 0);
	}

	/**
		Anlegen einer Zeilen-Verwaltung aus einem String mit einer
		vorgegebenen Start-Zeilennummer.
		@param text String, der als Zeilen-Liste verwaltet werden soll.
		@param baseLineNr erste anzuzeigene Zeilennummer. Default ist 0.
	*/
	public TextlineList(String text, int baseLineNr)	{
		this.text = text;
		this.baseLineNr = baseLineNr;
		int i = 0, prev = 0;
		
		for (; i < text.length(); i++)	{
			char c = text.charAt(i);
			
			if (c == '\n')	{
				if (lineCoords == null)
					lineCoords = new Vector(128, 128);
					
				lineCoords.addElement(new Point(prev, i));
				prev = i + 1;

				if (prev - 1 != i && i == text.length() - 1)	{	// not empty line, newline at end
					lineCoords.addElement(new Point(prev, prev));
				}
			}
		}
		
		if (prev < i)	{	// there was no newline at end
			if (lineCoords == null)
				lineCoords = new Vector(1);

			lineCoords.addElement(new Point(prev, i));
		}
	}


	/** Wird false uebergeben, werden keine Zeilennummern angezeigt. Default ist true. */
	public void setUseLineNumbers(boolean lineNumbers)	{
		this.lineNumbers = lineNumbers;
	}
	
	
	/**
		Einfuegen eines Such-Treffers mit einem Start- und End-Offset,
		gerechnet vom Textbeginn an.<br>
		Diese Methode kann NICHT mit deleteMatch() gemischt werden!
		@param start Offset des Match-Beginnes vom Textbeginn an gerechnet
		@param end Offset des Match-Endes vom Textbeginn an gerechnet
	*/
	public void insertMatch(int start, int end)	{
		if (notWanted != null)
			throw new IllegalStateException("TextlineList can not accept insertMatch() if deleteMatch() was called once!");
		insertMatch(start, end, true);
	}
	
	/**
		Loeschen eines Such-Treffers mit einem Start- und End-Offset,
		gerechnet vom Textbeginn an. Dieser Treffer soll NICHT im Ergebnis sein.
		Wird benoetigt, wenn man alle Zeilen finden will, die ein Pattern NICHT enthalten.<br>
		Diese Methode kann NICHT mit insertMatch() gemischt werden!
		@param start Offset des Match-Beginnes vom Textbeginn an gerechnet
		@param end Offset des Match-Endes vom Textbeginn an gerechnet
	*/
	public void deleteMatch(int start, int end)	{
		if (size() > 0)
			throw new IllegalStateException("TextlineList can not accept deleteMatch() if insertMatch() was called once!");
		insertMatch(start, end, false);
	}
	
	
	private void insertMatch(int start, int end, boolean positive)	{
		int first = -1;

		if (positive == false && notWanted == null)
			notWanted = new Vector();
		
		for (int i = 0; i < lineCoords.size(); i++)	{
			Point p = (Point)lineCoords.elementAt(i);
			
			// single- and multi-line match
			if (p.x <= start && p.y >= start)	{	// start ist innerhalb der Zeile
				first = i;
			}

			if (first >= 0 && p.x <= end && p.y >= end)	{	// end ist innerhalb der Zeile
				if (positive)	{
					Point pFirst = (Point)lineCoords.elementAt(first);
					Textline tl = new Textline(text.substring(pFirst.x, p.y), first, i, start, end);
					addElement(tl);
				}
				else	{
					for (int j = first; j <= i; j++)
						notWanted.addElement(lineCoords.elementAt(j));
				}
				return;
			}
		}
		
		Point p = (Point)lineCoords.elementAt(lineCoords.size() - 1);
		throw new IllegalArgumentException("Match position not found: start "+start+", end "+end+", having maximum Point "+p);
	}


	/**
		Den i-ten Match aus Liste der gefundenen Textzeilen als Point,
		in dem Start- und End-Position gespeichert ist, zurueckliefern.
		@param i Index des Suchbegriff-Treffers in Liste aller Matches
	*/
	public Point getRangeByMatchIndex(int i)	{
		if (i < size())	{
			Textline tl = (Textline)elementAt(i);
			return tl.getMatchStartEnd();
		}
		return null;
	}

	/**
		Den naehesten Index des Match nach rechts/unten aus der uebergebenen Cursorposition bestimmen. 
		@param caret die Cursorposition: textholder.getCaretPosition()
	*/
	public int getMatchIndexByCaret(int caret)	{
		for (int i = 0; i < size(); i++)	{
			Textline tl = (Textline)elementAt(i);
			Point p = tl.getMatchStartEnd();
			
			if (p.x >= caret)
				return i;
		}
		return size() > 0 ? 0 : -1;
	}


	/**
		Ersetzt den naechsten Match nach index durch den uebergebenen String.<br>
		Achtung: Durch diese Methode entspricht der Text dieser TextlineList
		nicht mehr dem zurueckgegebenen Text!
		@replace String, der den Match ersetzten soll.
		@param index position, ab der nach einem Match gesucht werden soll.
		@return substituierten Text.
	*/
	public String replace(String replace, int index)	{
		int i = getMatchIndexByCaret(index);
		
		if (i >= 0)	{
			Textline tl = (Textline)elementAt(i);
			Point p = tl.getMatchStartEnd();
			StringBuffer sb = new StringBuffer(text.length() + replace.length());
			sb.append(text.substring(0, p.x));
			sb.append(replace);
			sb.append(text.substring(p.y, text.length()));
			return sb.toString();
		}
		
		throw new IllegalArgumentException("Index nicht enthalten im Suchergebnis: "+index);
	}

	
	/**
		Ersetzt alle Matches durch den uebergebenen String.<br>
		Achtung: Durch diese Methode entspricht der Text dieser TextlineList
		nicht mehr dem zurueckgegebenen Text!
		@replace String, der jeden Match ersetzten soll.
		@return substituierten Text.
	*/
	public String replaceAll(String replace)	{
		StringBuffer sb = new StringBuffer(text.length() + replace.length() * size());
		int curr = 0;
		
		for (int i = 0; i < size(); i++)	{
			Textline tl = (Textline)elementAt(i);
			Point p = tl.getMatchStartEnd();

			sb.append(text.substring(curr, p.x));
			sb.append(replace);
			curr = p.y;
		}
		
		if (curr < text.length())
			sb.append(text.substring(curr, text.length()));
		
		return sb.toString();
	}


	/**
		Returns the filtered text after insertMatch() was called.
		Duplicate lines (containing more matches) are ignored.
	*/
	public String toString()	{
		StringBuffer sb = new StringBuffer();
		
		if (notWanted != null)	{
			for (int i = 0; i < lineCoords.size(); i++)	{
				Point p = (Point)lineCoords.elementAt(i);
				
				if (notWanted.indexOf(p) < 0)	{
					Textline tl = new Textline(text.substring(p.x, p.y), i, i, p.x, p.y);
					sb.append(tl.toString()+"\n");
				}
			}
		}
		else	{
			String lastLine = null;
	
			for (int i = 0; i < size(); i++)	{
				Textline tl = (Textline)elementAt(i);
				String line = tl.toString();
				
				if (lastLine == null || line.equals(lastLine) == false)
					sb.append(line+"\n");
					
				lastLine = line;
			}
		}
		
		return sb.toString();
	}
	



	protected class Textline
	// Jede Textline enthaelt exakt ein Match, auch wenn mehrere Matches in einer Zeile liegen.
	{
		private String displayLine;
		private Point matchStartEnd;
	

		public Textline(String line, int lineNo1, int lineNo2, int start, int end)	{
			lineNo1 += baseLineNr + 1;
			lineNo2 += baseLineNr + 1;
			
			if (TextlineList.this.lineNumbers)	{
				if (lineNo1 == lineNo2)
					displayLine = ""+lineNo1;
				else
					displayLine = ""+lineNo1+"-"+lineNo2;
					
				displayLine = displayLine+displayBlanks(lineNo1)+line;
			}
			else	{
				displayLine = line;
			}
			
			matchStartEnd = new Point(start, end);
		}

		private String displayBlanks(int lineNo)	{
			int len = Integer.toString(lineNo).length();
			if (len == 1) return "     ";
			if (len == 2) return "    ";
			if (len == 3) return "   ";
			if (len == 4) return "  ";
			if (len == 5) return " ";
			return "";
		}
		
		public Point getMatchStartEnd()	{
			return matchStartEnd;
		}
	

		public String toString()	{
			return displayLine;
		}
		
	}
	
}
