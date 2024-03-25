package fri.util.io;

import java.io.*;

/**
 * Reads away UNICODE Byte Order Mark on construction.
 * When no BOM was found, tries to detect an XML header and parses it for encoding.
 * Pushes back any XML after that.
 * <p />
 * Specification of XML header can be found on http://www.w3.org/TR/REC-xml
 * 
 * @author Fritz Ritzberger 2007
 */
public class BomXmlAwareReader extends BomAwareReader
{
	// the maximum length of <?xml version = "1.0" encoding = "ISO-8859-1" standalone = "yes" ?>
	private static final int XML_HEADER_BUFFER = 128;

	public BomXmlAwareReader(InputStream in) throws IOException {
		super(in);
	}

	public BomXmlAwareReader(InputStream in, String defaultEnc) throws IOException {
		super(in, defaultEnc);
	}

	protected int getReadingForwardByteCount()	{
		return XML_HEADER_BUFFER;
	}
	
	protected void noByteOrderMarkFound(PushbackInputStream in, byte [] bytes, int havingRead)	{
		if (havingRead <= 0)
			return;	// empty
		
		String text = new String(bytes, 0, havingRead);
		if (text.startsWith("<?xml") == false)
			return;	// is not XML header
		
		int i = text.indexOf("encoding", "<?xml".length());
		if (i < 0)
			return;	// no encoding attribute in header
		
		// read away XML spaces and '='
		i += "encoding".length();
		boolean isSpace;
		boolean wasEqual = false;
		do	{
			char c = text.charAt(i);
			boolean isEqual = (c == '=');
			if (isEqual)
				wasEqual = true;
			isSpace = (isEqual || c == 0x20 || c == 0x9 ||c == 0xD || c == 0xA);
			i++;
		}
		while (i < text.length() && isSpace);
		
		if (wasEqual && i < text.length() - 3)	{	// - 3: we need minimum a quoted encoding char after spaces
			i--;	// read too far
			// now we are at the quoted value of encoding attribute
			char stopChar = text.charAt(i);	// remember the kind of quote
			if (stopChar == '\'' || stopChar == '"')	{
				i++;	// skip opening quote
				int k = text.indexOf(stopChar, i);	// explore closing quote, starting from i
				if (k > i)	{
					setDetectedEncoding(text.substring(i, k));
					return;
				}
			}
		}
		System.err.println("WARNING: gave up parsing XML-header after "+i+" bytes, encoding not found.");
	}

	
	public static void main(String [] args)
		throws IOException
	{
		String xmlHeader = "<?xml version = '1.0' encoding \t\n = 'ISO-8859-1' standalone = 'yes'?>";
		BomXmlAwareReader reader = new BomXmlAwareReader(new ByteArrayInputStream(xmlHeader.getBytes()));
		System.err.println("Detected encoding: "+reader.getEncoding());
		int c = reader.read();
		System.err.println("First character: "+(c > -1 ? ""+(char) c : "EOF"));
		reader.close();
	}

}
