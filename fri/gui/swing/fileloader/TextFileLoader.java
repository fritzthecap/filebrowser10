package fri.gui.swing.fileloader;

import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import fri.util.io.*;

/**
	Loading a text file in a background thread.
	Manages optional document- and undo-listeners.
	Optionally converts platform newlines to Java newlines.
*/
public class TextFileLoader extends GuiFileLoader
{
	private final Document doc;
	private final DocumentListener docListener;
	private final UndoableEditListener undoListener;
	private boolean convertNewline = true;
	private final String encoding;
	private Reader reader;
	private String detectedEncoding;
	private byte [] bom;
	private final boolean detectEncodingFromByteOrderMark;
	private final boolean detectEncodingFromXmlHtmlHeader;

	/**
		Create a background FileLoader inserting into a Document.
		@param file file to load from disk
		@param doc document to fill with file contents
		@param docListener listener for document changes to add when finished, nullable
		@param undoListener listener for undoable edits to add when finished, nullable
		@param panel panel with BorderLayout where to add progressbar at SOUTH, nullable
		@param loadObserver listener which is interested in status of loading.
			At start setLoading(true) is called, at end setLoading(false). Can be null.
		@param waiter object to notify() when finished loading, nullable
		@param encoding assumed encoding of file, can be null for System-Default
	*/
	public TextFileLoader(
		File file,
		Document doc,
		DocumentListener docListener,
		UndoableEditListener undoListener,
		JComponent panel,
		LoadObserver loadObserver,
		Object waiter,
		String encoding,
		boolean detectEncodingFromByteOrderMark,
		boolean detectEncodingFromXmlHtmlHeader)
	{
		super(file, panel, loadObserver, waiter);
		this.doc = doc;
		this.docListener = docListener;
		this.undoListener = undoListener;
		this.encoding = encoding;
		this.detectEncodingFromByteOrderMark = detectEncodingFromByteOrderMark;
		this.detectEncodingFromXmlHtmlHeader = detectEncodingFromXmlHtmlHeader;
		
		if (detectEncodingFromByteOrderMark || detectEncodingFromXmlHtmlHeader)	{
			try	{
				ensureReader();
			}
			catch (IOException e)	{	// do not report such error now, let work() do that
				reader = null;
			}
		}
	}

	/** Constructor with null DocumentListener and null UndoableEditListener. */
	public TextFileLoader(
		File file,
		Document doc,
		JComponent panel,
		LoadObserver loadObserver,
		Object waiter,
		String encoding,
		boolean detectEncodingFromBom,
		boolean detectEndocingFromXmlHtmlHeader)
	{
		this(
			file,
			doc,
			null,
			null,
			panel,
			loadObserver,
			waiter,
			encoding,
			detectEncodingFromBom,
			detectEndocingFromXmlHtmlHeader);
	}
	
	/** Constructor with null DocumentListener and null UndoableEditListener, optional without newline conversion. */
	public TextFileLoader(
		File file,
		Document doc,
		JComponent panel,
		LoadObserver loadObserver,
		Object waiter,
		boolean convertNewline)
	{
		this(
			file,
			doc,
			panel,
			loadObserver,
			waiter,
			null,
			false,
			false);
		this.convertNewline = convertNewline;
	}
	
	
	/**
	 * Returns the encoding if one has been detected from Byte-Order-Mark or XML-encoding,
	 * else null. Does *NOT* return the passed encoding! This method can be called after
	 * construction.
	 */
	public String detectedEncoding()	{
		return detectedEncoding;
	}
	
	/**
	 * Returns the byte-order-mark if one has been detected from the file, else null.
	 */
	public byte [] detectedByteOrderMark()	{
		return bom;
	}
	

	protected void beforeWork()	{
		super.beforeWork();

		if (undoListener != null)
			doc.removeUndoableEditListener(undoListener);
			
		if (docListener != null)
			doc.removeDocumentListener(docListener);
	}

	protected void afterWork()	{
		super.afterWork();

		if (interrupted == false)	{	// window was NOT closed
			if (docListener != null)
				doc.addDocumentListener(docListener);
				
			if (undoListener != null)
				doc.addUndoableEditListener(undoListener);
		}
	}


	/**
	 * Uses a FileReader to load the file (converts characters to UNICODE!).
	 * Continually inserts into passed Document.
	 * @param len the length of the file
	 */
	protected void work(int len)
		throws Exception
	{
		if (len > 0)	{
			ensureReader();

			try	{
				char [] buffer = new char[len > 8192 ? 8192 : len];
				int havingRead;
				boolean wasCarriageReturn = false;
				
				while (interrupted == false && (havingRead = reader.read(buffer, 0, buffer.length)) != -1) {
					String fromFile;
					
					if (convertNewline)	{
						char [] convertedBuffer = buffer;	// converted buffer gets allocated on demand only
						int convertedBufferLength = havingRead;
						
						// replace any \r by \n, and when a \n follows, remove this  
						for (int i = 0, convertedBufferIndex = 0; i < havingRead; i++)	{
							if (buffer[i] == '\r')	{
								convertedBuffer[convertedBufferIndex] = '\n';
								convertedBufferIndex++;
								wasCarriageReturn = true;
							}
							else	{
								if (buffer[i] == '\n' && wasCarriageReturn)	{	// now must delete \n
									convertedBufferLength--;
									if (convertedBuffer == buffer)	{	// lazily allocate new buffer
										convertedBuffer = new char[convertedBufferLength];
										if (i > 0)
											System.arraycopy(buffer, 0, convertedBuffer, 0, i);
									}
									// delete by *not* incrementing ci
								}
								else	{
									if (convertedBuffer != buffer)
										convertedBuffer[convertedBufferIndex] = buffer[i];
									convertedBufferIndex++;
								}
								wasCarriageReturn = false;
							}
						}
						
						fromFile = new String(convertedBuffer, 0, convertedBufferLength);
					}
					else	{	// not converting newlines
						fromFile = new String(buffer, 0, havingRead);
					}
					
					reportProgressAndError(fromFile, havingRead, null);	// called in event thread
				}
			}
			finally	{
				try	{ reader.close(); }	catch (Exception e)	{}
				reader = null;
			}
		}
	}


	private void ensureReader()
		throws IOException
	{
		if (reader == null)	{
			if (detectEncodingFromXmlHtmlHeader || detectEncodingFromByteOrderMark)	{
				BomAwareReader encodingDetectingReader = detectEncodingFromXmlHtmlHeader
					? new BomXmlHtmlAwareReader(new FileInputStream(file), encoding)
					: new BomAwareReader(new FileInputStream(file), encoding);
				detectedEncoding = encodingDetectingReader.getDetectedEncoding();
				bom = encodingDetectingReader.getByteOrderMark();
				reader = encodingDetectingReader;
			}
			else	{
				reader = (encoding != null)
					? new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding))
					: new BufferedReader(new FileReader(file));
			}
		}
	}

	
	protected void insertTextProgress(Object data, int bytesRead)	{
		try	{
			doc.insertString(doc.getLength(), (String)data, null);
		}
		catch (Exception e)	{
			e.printStackTrace();
		}

		super.insertTextProgress(data, bytesRead);
	}
		
}
