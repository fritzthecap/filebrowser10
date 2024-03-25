package fri.gui.swing.editor;

import java.io.File;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.text.Document;
import javax.swing.text.Element;
import fri.util.text.Indent;
import fri.util.text.Replace;
import fri.gui.swing.text.TextAreaSeparatorDefinition;

/**
	A textarea that lets set autoindent and plain/platform newline option.
	It implements a key listener do do tab indention and autoindent.
	It installs an additional TextAreaSeparatorDefinition.

	@author Ritzberger Fritz
*/

public class TextEditArea extends BasicTextEditArea implements
	KeyListener
{
	/**
		Create the textarea and load the passed file if not null.
		@param file to load.
		@param tabSize number of spaces for one tab.
	*/
	public TextEditArea(File file)	{
		super(file);

		addKeyListener(this);	// tabulator block indent
		new TextAreaSeparatorDefinition(this);
	}
	

	/** Overwritten to do plain/platform newline conversion. */	
	protected String convertWhenStoring(String text)	{
		String newline = System.getProperty("line.separator");
		String javaNewline = "\n";
		if (Config.getNewline() == null)	{	// platform newline
			if (newline.equals("\n") == false)
				text = Replace.replaceMany(text, javaNewline, newline);
		}
		else
		if (Config.getNewline().equals(Config.WINDOWS_NEWLINE))	{
			text = Replace.replaceMany(text, javaNewline, "\r\n");
		}
		else
		if (Config.getNewline().equals(Config.MAC_NEWLINE))	{
			text = Replace.replaceMany(text, javaNewline, "\r");
		}
		// else: UNIX newline is equal to Java newline, nothing to do
		return text;
	}



	// interface KeyListener
	
	/** Implements KeyListener to catch TAB (block-indent) and ENTER (auto-indent) */
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_TAB)	{
			if (getSelectionStart() >= 0 && getSelectionEnd() >= 0 && 
					getSelectionStart() < getSelectionEnd())
			{
				if (e.isShiftDown())
					doTab(false);
				else
					doTab(true);
					
				e.consume();
			}
		}
		else
		if (e.getKeyCode() == KeyEvent.VK_ENTER && Config.getAutoIndent())	{
			doAutoIndent(e);
		}
	}

	public void keyReleased(KeyEvent e)	{}
	public void keyTyped(KeyEvent e)	{}



	// text work methods
	
	private void doTab(boolean indent)	{
		String s = super.getSelectedText();

		String prev = "\n";	// if it is textbegin, there was a newline
		int start = super.getSelectionStart();

		if (start > 0)	{	// watch for newline
			try	{ prev = getText(start - 1, 1); }
			catch (Exception e)	{}
		}
		boolean wasNewline = start <= 0 || prev != null && prev.equals("\n");

		boolean willNewline = s.indexOf("\n") >= 0;
		if (willNewline == false)	{	// look closer
			String next = "\n";	// if it is textend, there will be a newline
			int len = super.getText().length();
			int end = super.getSelectionEnd();
			
			if (end < len - 1)	{
				try	{ next = super.getText(end, 1); }
				catch (Exception e)	{}
			}
			willNewline = end >= len - 1 || next != null && next.equals("\n");
		}
		
		String t;
		if (wasNewline == false || willNewline == false)			
			t = "\t";
		else
		if (indent)
			t = Indent.indent(s, wasNewline);
		else
			t = Indent.exdent(s, wasNewline);
			
		super.replaceSelection(t);
		super.select(start, start + t.length());
	}



	private void doAutoIndent(KeyEvent e)	{
		int dot = super.getCaretPosition();
		Document buffer = getDocument();
		Element map = buffer.getDefaultRootElement();
		int currLine = map.getElementIndex(dot);
		Element lineElement = map.getElement(currLine);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		int relativeDotOffset = dot - start;
		
		String line = null;
		try	{ line = super.getText(start, end - start); }
		catch (Exception ex)	{}
		//System.err.println("enter, line on dot "+dot+"is: "+line);
		
		if (line != null)	{
			String tabs = "";
			
			for (int i = 0; i < line.length(); i++)	{
				if (line.charAt(i) == '\t')	{
					if (i < relativeDotOffset)
						tabs = tabs+"\t";
				}
				else
				if (line.charAt(i) == ' ')	{
					if (i < relativeDotOffset)
						tabs = tabs+" ";
				}
				else	{
					break;
				}
			}
			
			if (tabs.length() > 0)	{
				super.insert("\n"+tabs, super.getCaretPosition());
				e.consume();
			}
		}
	}

}
