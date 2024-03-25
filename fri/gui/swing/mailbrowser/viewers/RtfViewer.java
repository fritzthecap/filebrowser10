package fri.gui.swing.mailbrowser.viewers;

import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.activation.*;

public class RtfViewer extends TextViewer
{
	/** Implementing CommandObject: show the RTF text. */
	public void setCommandContext(String verb, DataHandler dh)
		throws IOException
	{
		setDataHandler(dh);
		
		JEditorPane pane = (JEditorPane)textArea;
		
		// detect the appropriate charset and translate to java charset
		String charset = detectCharset(dh);
		
		// generate a document and configure it
		EditorKit kit = pane.getEditorKit();
		Document doc = kit.createDefaultDocument();
		configureDocument(doc);

		// read the input stream into document
		InputStream is = dh.getInputStream();
		InputStreamReader isr = charset == null ? new InputStreamReader(is) : new InputStreamReader(is, charset);
		BufferedReader reader = new BufferedReader(isr);
		
		try	{
			kit.read(reader, doc, 0);
			pane.setDocument(doc);
		}
		catch (BadLocationException e)	{
			e.printStackTrace();
		}
		finally	{
			reader.close();
		}

		// scroll to start of text
		pane.setCaretPosition(0);
	}


	protected JTextComponent createTextArea()	{
		JEditorPane textArea = new JEditorPane();
		configureTextArea(textArea);
		return textArea;
	}

	protected void configureTextArea(JEditorPane textArea)	{
		textArea.setContentType("text/rtf");
	}

	protected void createController()	{
		PartSaveController.installSavePopup(this);
	}

	protected void configureDocument(Document doc)	{
		doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);	// else javax.swing.text.ChangedCharSetException
	}

}