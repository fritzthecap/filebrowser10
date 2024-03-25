package fri.util.xml.xml4j;

import java.util.Hashtable;
import com.ibm.xml.parser.ErrorListener;
import org.xml.sax.*;
import org.w3c.dom.Node;
import fri.util.error.Err;

/**
	XML4J dependent class.<br>
	Collect all errors into one big String and output it when close() is called.
	Merges SAX and TX-DOM error handling.
*/

class ParserErrorListener implements
	ErrorListener,
	ErrorHandler
{
	private static final String ignore = "is not declared in the DTD";
	public static final String newline = System.getProperty("line.separator");
	static final Hashtable errorNodes = new Hashtable();
	private StringBuffer errors = new StringBuffer();
	private Node fallbackNode;

	
	/** Implements TX-Parser ErrorListener: collect errors into StringBuffer. */
	public int error(
		String file,
		int lineNo,
		int charOffset,
		Object key,
		String msg)
	{
		if (msg.indexOf(ignore) < 0)	{
			String s = file+": line "+lineNo+", column "+charOffset+": "+msg;
			errors.append(newline + s);

			if (ParserNodeFactory.currentNode != null)
				errorNodes.put(ParserNodeFactory.currentNode, s);

			return 1;
		}
		return 0;
	}

	/** Close and output errors to Err. */
	public void close()	{
		if (errors.length() > 0)	{
			Err.error(new Exception(errors.toString()));

			if (errorNodes.size() <= 0 && fallbackNode != null)	{	// SAX parser was used
				errorNodes.put(fallbackNode, errors.toString());
			}
		}
	}


	public void setFallbackNode(Node fallbackNode)	{
		this.fallbackNode = fallbackNode;
	}


	/** Implements SAX-Parser ErrorHandler: collect errors into StringBuffer. */
	public void error(SAXParseException e)	{
		appendSAXException(e);
	}

	/** Implements SAX-Parser ErrorHandler: collect errors into StringBuffer. */
	public void fatalError(SAXParseException e)	{
		appendSAXException(e);
	}

	/** Implements SAX-Parser ErrorHandler: collect errors into StringBuffer. */
	public void warning(SAXParseException e)	{
		appendSAXException(e);
	}

	private void appendSAXException(SAXParseException e)	{
		errors.append(
			newline+
			"line "+e.getLineNumber()+
			", column "+e.getColumnNumber()+
			": "+e.getMessage());
	}

}