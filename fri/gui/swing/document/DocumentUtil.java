package fri.gui.swing.document;

import java.util.Vector;
import java.util.List;
import java.awt.Point;
import javax.swing.text.*;

public abstract class DocumentUtil
{
	/** Returns list of lines of the document. */
	public static List documentToLineList(Document doc)	{
		return documentToLineList(doc, false);
	}
	
	/** Returns list of lines of the document. */
	public static List documentToLineList(Document doc, boolean withoutNewlines)	{
		Element elem = doc.getDefaultRootElement();
		Vector v = new Vector(elem.getElementCount());
		
		for (int i = 0; i < elem.getElementCount(); i++)	{
			Element e = elem.getElement(i);
			int start = e.getStartOffset();
			int end = e.getEndOffset();
			
			try	{
				String s = e.getDocument().getText(start, end - start);
				
				if (withoutNewlines && s.endsWith("\n"))
					s = s.substring(0, s.length() - 1);

				v.add(s);
			}
			catch (BadLocationException ex)	{
				ex.printStackTrace();
				//System.err.println("  error offset was: "+ex.offsetRequested());
			}
		}
		return v;
	}

	/** Returns list of lines of the document. */
	public static String [] documentToStringArray(Document doc)	{
		Vector v = (Vector)documentToLineList(doc);
		String [] sarr = new String [v.size()];
		v.copyInto(sarr);
		return sarr;
	}

	/**
		Returns a Point with x = character position and y = line number
		for the passed caret position within passed Document.
	*/
	public static Point caretToPoint(int dot, Document doc)	{
		Element map = doc.getDefaultRootElement();
		int currLine = map.getElementIndex(dot);
		Element lineElement = map.getElement(currLine);
		int start = lineElement.getStartOffset();
		int line = currLine + 1;
		int col = dot - start;
		return new Point(col, line);
	}

	
	private DocumentUtil()	{}
}