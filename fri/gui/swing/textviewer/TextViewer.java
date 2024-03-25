package fri.gui.swing.textviewer;

import javax.swing.*;
import java.awt.*;
import fri.gui.swing.IconUtil;
import fri.gui.swing.text.*;
import fri.gui.swing.application.*;

/**
	A singleton JFrame that renders a text and sets a topic to title bar.
	@see fri.gui.swing.text.ComfortTextArea
*/

public class TextViewer extends GuiApplication
{
	protected static TextViewer singleton = null;
	protected ComfortTextArea ta;
	protected String topic = null;
	

	/** Load the text in the one and only viewer, get it to foreground  */
	public static TextViewer singleton(String text)	{
		return singleton(null, text);
	}
	
	/**
		Load the text in the one and only viewer, get it to foreground.
		@param text text to render
		@param topic theme to show in titlebar
	*/
	public static TextViewer singleton(String topic, String text)	{
		if (singleton == null)	{
			singleton = new TextViewer(topic, text);
		}
		else	{
			singleton.setText(text);
			if (topic != null)
				singleton.setTitle(topic);
		}
		singleton.setVisible(true);
		return singleton;
	}



	/** Create a new instance of text viewer for the passed text */
	public TextViewer(String text)	{
		this(null, text);
	}
	
	/**
		Create a new instance of text viewer for the passed text.
		@param text text to render
		@param topic theme to show in titlebar
	*/
	public TextViewer(String topic, String text)	{
		this.topic = topic;
		build();
		setText(text);
	}

		
	protected void build()	{
		setTitle(topic == null ? "Text Viewer" : topic);
		
		Container c = getContentPane();
		
		JPanel panel = new JPanel(new BorderLayout());
		ta = new ComfortTextArea();
		ta.setEditable(false);
		JScrollPane sp = new JScrollPane(ta);
		panel.add(sp);
		c.add(panel);
		
		JPopupMenu popup = ta.getPopupMenu();
		popup.addSeparator();
		JMenuItem customize = new JMenuItem("Customize GUI");
		popup.add(customize);
		
		if (IconUtil.lastIcon != null)	{
			setIconImage(IconUtil.lastIcon);
		}
		
		init(customize, new Object [] { popup });
	}



	/** Set a new text to text viewer */
	public void setText(String text)	{
		ta.setText(text);
		ta.setCaretPosition(0);
	}

	
	public void find(
		String pattern,
		String syntax,
		boolean ignoreCase,
		boolean matchWord)
	{
		ta.find(pattern, syntax, ignoreCase, matchWord);
	}


	public void find()	{
		ta.find();
	}

}