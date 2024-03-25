package fri.gui.swing.mailbrowser.viewers;

import java.io.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.text.*;
import javax.activation.*;
import javax.mail.internet.*;
import fri.util.error.Err;
import fri.gui.swing.text.ComfortTextArea;
import fri.gui.swing.mailbrowser.Language;
import fri.gui.swing.mailbrowser.CryptDialog;

public class TextViewer extends JScrollPane implements
	CommandObject,
	PartView
{
	protected JTextComponent textArea;
	private JPanel panel;
	private DataHandler dh;
	

	/** Implementing CommandObject: show the plain text. */
	public void setCommandContext(String verb, DataHandler dh)
		throws IOException
	{
		setDataHandler(dh);
		
		String charset = detectCharset(dh);
		InputStream is = dh.getInputStream();
		InputStreamReader isr;
		try	{
			isr = charset != null ? new InputStreamReader(is, charset) : new InputStreamReader(is);
		}
		catch (Exception e)	{
			Err.error(e);
			isr = new InputStreamReader(is);
		}
		BufferedReader reader = new BufferedReader(isr);
		CharArrayWriter writer = new CharArrayWriter();

		try	{
			int cnt;
			char [] chars = new char[1024];
			while((cnt = reader.read(chars)) != -1)	{
				writer.write(chars, 0, cnt);
			}
			writer.close();

			textArea.setText(writer.toString());
			textArea.setCaretPosition(0);
		}
		finally	{
			try	{ reader.close(); }	catch (Exception e)	{}
		}
	}


	/** Store the received data handler. */
	protected void setDataHandler(DataHandler dh)	{
		this.dh = dh;
		
		textArea = createTextArea();
		textArea.setEditable(false);
		setViewportView(textArea);
		
		createController();
	}


	/** Adds the PartViewController action "Save As" to textarea popup. */
	protected void createController()	{
		PartSaveController controller = new PartSaveController(this);
		JPopupMenu popup = ((ComfortTextArea)textArea).getPopupMenu();
		controller.visualizeAction(PartSaveController.ACTION_SAVE, popup, false, 0);
		popup.insert(new JSeparator(), 1);
		
		Action a = new AbstractAction(Language.get("Cryptography"))	{
			public void actionPerformed(ActionEvent e)	{
				String text = textArea.getText();
				CryptDialog dlg = new CryptDialog(textArea, text, false);
				if (dlg.getResult() != null)
					textArea.setText(dlg.getResult());
			}
		};
		popup.insert(a, 2);
		popup.insert(new JSeparator(), 3);
	}

	
	/** Create the appropriate txt area for that viewer. */
	protected JTextComponent createTextArea()	{
		JTextArea textArea = new ComfortTextArea()	{
			protected String findLabel()	{
				return Language.get("Find_Text");
			}
			protected String goLineLabel()	{
				return Language.get("Go_To_Line");
			}
			protected String tabWidthLabel()	{
				return Language.get("Tab_Width");
			}
			protected String wrapLinesLabel()	{
				return Language.get("Wrap_Lines");
			}
		};
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		setViewportView(panel);	// again set the viewport view
		return textArea;
	}

	/** Detect the appropriate charset and translate to java charset */
	protected String detectCharset(DataHandler dh)	{
		String charset = null;
		try	{
			ContentType type = new ContentType(dh.getContentType());
			String cs = type.getParameter("charset");
			if (cs != null)	{
				charset = MimeUtility.javaCharset(cs);
				System.err.println("MIME content type "+type+" is Java charset "+charset);
			}
		}
		catch (ParseException e)	{
		}
		return charset;
	}

	/** Add one or more inline CommandObjects to bottom of textarea. */
	public void addInlineComponent(Component c)	{
		if (panel == null)	{
			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setBackground(textArea.getBackground());	// simulate continuity of textarea
			panel.add(textArea);
		}
		
		panel.add(c);	// add the new Component at bottom
		setViewportView(panel);	// again set the viewport view
	}


	/** Implements PartView. */
	public DataHandler getDataHandler()	{
		return dh;
	}

	/** Implements PartView. */
	public JComponent getSensorComponent()	{
		return textArea;
	}

}