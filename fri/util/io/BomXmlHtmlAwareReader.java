package fri.util.io;

import java.io.*;

/**
 * <p>
 * Reads away UNICODE Byte Order Mark on construction.
 * When no BOM was found, tries to detect an XML header and parses it for encoding.
 * Pushes back any XML after that.
 * </p><p>
 * Specification of XML header can be found on http://www.w3.org/TR/REC-xml
 * </p><p>
 * Strategy of this scanner:
 * Read ahead HTML_HEADER_BUFFER bytes (HTML pages having charset directive behind this will not succeed).
 * Scan case insensitive, read away comments and attribute quotes.
 * When found "&lt;/head", stop.
 * When found "&lt;meta", search for attribute "http-equiv='content-type'"
 * When this was found, it is the right tag, search for "content" attribute, get its value.
 * Read over optional quote, scan within attribute value for "charset",
 * scan along for spaces and "=", then extract all "a-zA-Z0-9._\-".
 * When this non-empty, it is the encoding.
 * 
 * @author Fritz Ritzberger 2007
 */
public class BomXmlHtmlAwareReader extends BomXmlAwareReader
{
	// the maximum length of HTML header - difficult to say!
	private static final int HTML_HEADER_BUFFER = 1024;

	private static final String HTML_TAG = "<html";
	private static final String HEAD_OPEN_TAG = "<head";
	private static final String HEAD_CLOSE_TAG = "</head";
	private static final String OPEN_COMMENT = "<!--";
	private static final String CLOSE_COMMENT = "-->";
	private static final String META_TAG = "<meta";
	private static final String HTTP_EQUIV_ATTRIBUTE = "http-equiv";
	private static final String HTTP_EQUIV_VALUE = "content-type";
	private static final String CONTENT_ATTRIBUTE = "content";
	private static final String CHARSET_NAME = "charset";

	public BomXmlHtmlAwareReader(InputStream in) throws IOException {
		super(in);
	}

	public BomXmlHtmlAwareReader(InputStream in, String defaultEnc) throws IOException {
		super(in, defaultEnc);
	}

	/** Overridden to request a quite big buffer: 1024. */
	protected int getReadingForwardByteCount()	{
		return HTML_HEADER_BUFFER;
	}
	
	/** Overridden to check for HTML when XML failed. */
	protected void noByteOrderMarkFound(PushbackInputStream in, byte [] bytes, int havingRead)	{
		if (havingRead <= 0 || getEncoding() != null)
			return;	// empty, or having found XML encoding declaration
		
		String text = new String(bytes, 0, havingRead);
		//System.err.println("Text length: "+text.length()+"\n"+text);

		int i = 0;
		if ((i = scanCommentsAndQuotes(text, i, HTML_TAG)) < 0)
			return;
		
		if ((i = scanCommentsAndQuotes(text, i, HEAD_OPEN_TAG)) < 0)
			return;

		// we can be quite sure it is HTML now
		while ((i = scanCommentsAndQuotes(text, i, META_TAG)) >= 0)	{	// found "<meta"
			int j = scanForCharset(text, i);	// no HTML comments are within a tag
			
			if (j > i && j < text.length())	{	// standing either at end of tag or at start of encoding string
				char c = text.charAt(j);	// check current char
				
				if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z')	{	// start of valid encoding name
					int k = j + 1;
					boolean valid;
					do	{
						c = text.charAt(k);
						valid = (c  >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_' || c == '.');
						if (valid)
							k++;
					}
					while (valid);
					String charset = text.substring(j, k);
					setDetectedEncoding(charset);
					return;
				}
				i = j;
			}
			else
				break;	// invalid tag
		}
		
		System.err.println("WARNING: gave up parsing HTML-header, encoding not found.");
	}
	
	// gets HTML text, scans for "<meta", reads away comments and quotes
	private int scanCommentsAndQuotes(String text, int i, String pattern)	{
		boolean comment = false;
		while (i < text.length())	{
			if (comment == false)	{
				if (match(text, i, OPEN_COMMENT))	{	// comment detected
					comment = true;
					i += OPEN_COMMENT.length();
				}
				else
				if (match(text, i, HEAD_CLOSE_TAG))	{	// end head detected
					return -1;
				}
				else
				if (match(text, i, pattern))	{	// meta tag found
					return i + pattern.length();
				}
				else	{
					i = readAwayQuotes(text, i);
					i++;
				}
			}
			else
			if (comment == true && match(text, i, CLOSE_COMMENT))	{	// comment is closed
				comment = false;
				i += CLOSE_COMMENT.length();
			}
			else	{
				i++;
			}
		}
		return -1;	// not found
	}
	
	// gets meta tag start position, looks for for "http-equiv='content-type'",
	// when found, looks for "content=...", when found, returns start position of the encoding
	private int scanForCharset(String text, int i)	{
		boolean isHttpEquiv = false;
		int contentStart = -1;
		char contentQuote = 0;
		while ((contentStart < 0 || isHttpEquiv == false) && i < text.length())	{
			if (match(text, i, HTTP_EQUIV_ATTRIBUTE))	{
				i = readAwaySpacesAndEqual(text, i + HTTP_EQUIV_ATTRIBUTE.length());
				boolean isQuoted = (text.charAt(i) == '"' || text.charAt(i) == '\'');
				if (isQuoted)
					i++;
				
				if (match(text, i, HTTP_EQUIV_VALUE))	{
					isHttpEquiv = true;
					i += HTTP_EQUIV_VALUE.length() + (isQuoted ? 1 : 0);
				}
				else	{
					if (isQuoted)	{
						i--;
						i = readAwayQuotes(text, i);
					}
					else	{	// read away unquoted attribute value
						char c;
						while (Character.isWhitespace(c = text.charAt(i)) == false)
							if (c == '>')	// meta tag closed
								return i;
							else
								i++;
					}
				}
			}
			else
			if (match(text, i, CONTENT_ATTRIBUTE))	{
				i = readAwaySpacesAndEqual(text, i + CONTENT_ATTRIBUTE.length());
				char c = text.charAt(i);
				boolean quoted = (c == '"' || text.charAt(i) == '\'');
				contentQuote = quoted ? c : 0;
				contentStart = quoted ? i + 1 : i;
			}
			else	{
				i = readAwayQuotes(text, i);
				if (i < text.length() && text.charAt(i) == '>')
					break;
				else
					i++;
			}
		}
		
		if (contentStart > 0 && isHttpEquiv)	{
			int success = scanContent(text, contentStart, contentQuote);
			if (success > 0)
				return success;
		}
		
		return i;
	}
	
	private int scanContent(String text, int i, char quote)	{
		while (i < text.length())	{
			if (match(text, i, CHARSET_NAME))	{
				i += CHARSET_NAME.length();
				while (Character.isWhitespace(text.charAt(i)))
					i++;
				if (text.charAt(i) == '=')	{
					i = readAwaySpacesAndEqual(text, i);
					return i;
				}
			}
			else	{
				char c = text.charAt(i);
				if (quote != 0 && c == quote || quote == 0 && (Character.isWhitespace(c) || c == '>'))	{
					return -1;
				}
				else	{
					i++;
				}
			}
		}
		return -1;
	}
	
	private boolean match(String text, int i, String pattern)	{
		int j = 0;
		for (; i < text.length() && j < pattern.length(); i++, j++)
			if (Character.toLowerCase(text.charAt(i)) != pattern.charAt(j))
				return false;
		return j == pattern.length();
	}
	
	// when text[i] is quote, returns the index of the next quote, else does nothing
	private int readAwayQuotes(String text, int i)	{
		char c = text.charAt(i);
		if (c == '"' || c == '\'')	{
			do	{
				i++;
			}
			while (i < text.length() && text.charAt(i) != c);
		}
		return i;
	}
	
	// while text[i] is space or '=' read ahead
	private int readAwaySpacesAndEqual(String text, int i)	{
		char c = text.charAt(i);
		while (Character.isWhitespace(c) || c == '=')	{
			i++;
			c = text.charAt(i);
		}
		return i;
	}
	
	
	
	public static void main(String [] args)
		throws IOException
	{
		String htmlText =
			"<HTML ><HEAD >"+
			"<meta name=\"keywords\" content=\"charset='THIS-IS-A-TRAP'\" >"+
			"<meta http-equiv='content-type' content='text/charset_TRAP; charset=UTF-8' >"+
			"<style ><!--"+
			"	<meta http-equiv='content-type' content='text/html; charset=UTF-9999'>"+
			"--></style>"+
			"<script >"+
			"	<!--"+
			"	<meta http-equiv='content-type' content='text/html; charset=UTF-7777'>"+
			"	// -->"+
			"</script >"+
			"</head ><body ></body></html>";
		
		InputStream in;
		if (args.length > 0)	{
			in = new java.net.URL(args[0]).openStream();
			System.err.println("Checking URL: "+args[0]);
		}
		else	{
			in = new ByteArrayInputStream(htmlText.getBytes());
			System.err.println("Scanning:\n"+htmlText);
		}
		BomXmlHtmlAwareReader reader = new BomXmlHtmlAwareReader(in);
		System.err.println("Detected encoding: "+reader.getEncoding());
		int c = reader.read();
		System.err.println("First character: "+(c > -1 ? ""+(char) c : "EOF"));
		reader.close();
	}

}
