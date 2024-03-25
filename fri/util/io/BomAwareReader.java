package fri.util.io;

import java.io.*;

/**
 * Reads away UNICODE Byte Order Mark on construction. See
 * http://www.unicode.org/unicode/faq/utf_bom.html
 * <p />
 * These BOMs are supported:
 * <pre>
 *  00 00 FE FF    = UTF-32, big-endian
 *  FF FE 00 00    = UTF-32, little-endian
 *  FE FF          = UTF-16, big-endian
 *  FF FE          = UTF-16, little-endian
 *  EF BB BF       = UTF-8
 *  added 2008-09-29:
 *  F7 64 4C       = UTF-1
 *  DD 73 66 73    = UTF-EBCDIC
 * </pre>
 * 
 * This class uses a BufferedReader as delegate!
 * 
 * @author Fritz Ritzberger 2007
 */
public class BomAwareReader extends Reader
{
	private static final BOM [] byteOrderMarks = new BOM []	{
		new BOM("UTF-8",    new byte [] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }),
		new BOM("UTF-16BE", new byte [] { (byte) 0xFE, (byte) 0xFF }),
		new BOM("UTF-16LE", new byte [] { (byte) 0xFF, (byte) 0xFE }),
		new BOM("UTF-32BE", new byte [] { (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF }),
		new BOM("UTF-32LE", new byte [] { (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00 }),
		new BOM("UTF-1",     new byte [] { (byte) 0xF7, (byte) 0x64, (byte) 0x4C }),
		new BOM("UTF-EBCDIC",new byte [] { (byte) 0xDD, (byte) 0x73, (byte) 0x66, (byte) 0x73 }),
	};
	
	private static final int MAXIMUM_BOM_LENGTH;
	
	static	{
		int max = 0;
		for (int i = 0; i < byteOrderMarks.length; i++)
			if (byteOrderMarks[i].bom.length > max)
				max = byteOrderMarks[i].bom.length;
		
		MAXIMUM_BOM_LENGTH = max;
	}
	
	public static final byte [] getByteOrderMark(String name)	{
		for (int i = 0; i < byteOrderMarks.length; i++)
			if (name.equalsIgnoreCase(byteOrderMarks[i].name))
				return byteOrderMarks[i].bom;
		System.err.println("WARNING: found no byte order mark named >"+name+"<");
		return null;
	}

	private Reader delegate;
	private String encoding;
	private String detectedEncoding;
	private byte [] bom;

	public BomAwareReader(InputStream in) throws IOException {
		init(in, null);
	}

	public BomAwareReader(InputStream in, String defaultEncoding) throws IOException {
		init(in, defaultEncoding);
	}

	/**
	 * Returns the encoding that was read from byte order mark if there was one,
	 * else the passed default encoding. This method can be called after constructing this object.
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Returns the BOM that was detected in the input stream if there was one, else null.
	 */
	public byte [] getByteOrderMark() {
		return bom;
	}

	/** Overridden to use a delegate reader with the correct encoding. */
	public int read(char[] cbuf, int off, int len) throws IOException {
		return delegate.read(cbuf, off, len);
	}

	/** Overridden to close the delegate reader. */
	public void close() throws IOException {
		delegate.close();
	}


	/** Subclasses can set any detected encoding through this method. */
	protected void setDetectedEncoding(String detectedEncoding)	{
		System.err.println("Setting detected encoding to >"+encoding+"<");
		setEncoding(this.detectedEncoding = detectedEncoding);
	}

	/** If an encoding has been detected from BOM or any other (XML, HTML), this returns the detected encoding. */
	public String getDetectedEncoding()	{
		return detectedEncoding;
	}

	private void setEncoding(String encoding)	{
		System.err.println("Setting encoding to >"+encoding+"<");
		this.encoding = encoding;
	}

	/** Override to read forward more than 4 bytes on construction. */
	protected int getReadingForwardByteCount()	{
		return MAXIMUM_BOM_LENGTH;
	}
	
	/** Override to handle other than the byte order marks done by this Reader. This does nothing. */
	protected void noByteOrderMarkFound(PushbackInputStream in, byte [] bom, int havingRead)	{
	}
	

	/**
	 * Read-ahead four bytes and check for BOM marks. Extra bytes are unread back
	 * to the stream, only BOM bytes are skipped.
	 */
	private void init(InputStream inputStream, String defaultEncoding) throws IOException {
		byte [] bom = new byte[getReadingForwardByteCount()];
		PushbackInputStream detectStream = new PushbackInputStream(inputStream, bom.length);
		int havingRead = readAhead(detectStream, bom, 0, bom.length);
		
		int unread = havingRead;
		boolean found = false;
		
		for (int i = 0; found == false && i < byteOrderMarks.length; i++)	{
			byte [] byteOrderMark = byteOrderMarks[i].bom;
			boolean matches = (byteOrderMark.length > 0);	// avoid empty BOMs
			for (int j = 0; matches && j < byteOrderMark.length; j++)
				matches = (bom[j] == byteOrderMark[j]);
			
			if (matches)	{
				found = true;
				unread = havingRead - byteOrderMark.length;
				setDetectedEncoding(byteOrderMarks[i].name);
				this.bom = new byte[byteOrderMark.length];
				System.arraycopy(byteOrderMark, 0, this.bom, 0, byteOrderMark.length);
			}
		}
		
		if (found == false)	{
			setEncoding(defaultEncoding);
			noByteOrderMarkFound(detectStream, bom, havingRead);	// let subclasses check for other (XML header)
		}

		if (unread > 0)
			detectStream.unread(bom, havingRead - unread, unread);

		if (getEncoding() == null)
			delegate = new BufferedReader(new InputStreamReader(detectStream));
		else
			delegate = new BufferedReader(new InputStreamReader(detectStream, getEncoding()));
	}
	
	private int readAhead(InputStream detectStream, byte [] buffer, int offset, int length)
		throws IOException
	{
		int havingRead = 0;
		int portion;
		do	{
			portion = detectStream.read(buffer, offset + havingRead, length - havingRead);
			if (portion != -1)
				havingRead += portion;
		}
		while (portion != -1 && havingRead < length);
		return havingRead;
	}

	
	private static class BOM
	{
		final String name;
		final byte [] bom;
		
		BOM(String name, byte [] bom)	{
			this.bom = bom;
			this.name = name;
		}
	}
	

	
	public static void main(String [] args)
		throws IOException
	{
		byte [] bytes = new byte[6];
	
		/* UTF-8 byte order mark
		 */
		bytes[0] = (byte) 0xEF;
		bytes[1] = (byte) 0xBB;
		bytes[2] = (byte) 0xBF;
		bytes[3] = (byte) 'A';
		
		/* UTF-16BE byte order mark
		bytes[0] = (byte) 0xFE;
		bytes[1] = (byte) 0xFF;
		bytes[2] = (byte) 0;
		bytes[3] = (byte) 'A';
		 */
	
		/* UTF-32BE: jre 1.4 has no converter, this would cause an UnsupportedEncodingException!
		bytes[0] = (byte) 0x0;
		bytes[1] = (byte) 0x0;
		bytes[2] = (byte) 0xFE;
		bytes[3] = (byte) 0xFF;
		bytes[4] = (byte) 0;
		bytes[5] = (byte) 'A';
		 */
		
		BomAwareReader reader = new BomAwareReader(new ByteArrayInputStream(bytes));
		System.err.println("Detected encoding: "+reader.getEncoding());
		int c = reader.read();
		System.err.println("First character: "+(c > -1 ? ""+(char) c : "EOF"));
		reader.close();
	}

}
