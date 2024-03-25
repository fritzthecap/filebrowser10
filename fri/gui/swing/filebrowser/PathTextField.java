package fri.gui.swing.filebrowser;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.event.*;
import java.io.File;
import fri.util.FileUtil;
import fri.util.os.OS;
import fri.gui.swing.text.ClipableJTextField;

/**
	Target: Expand and collapse pathes written in this text field.
	Behaviour: Listens for File-Separators inserted or deleted.
*/

class PathTextField extends ClipableJTextField implements
	DocumentListener,
	ActionListener,
	FocusListener
{
	private TreeExpander texp;
	private String text; 
	protected boolean hasFocus = false;
	private boolean noSetText = false;
	
	
	PathTextField(TreeExpander texp)	{
		super();
		this.texp = texp;
		getDocument().addDocumentListener(this);
		addActionListener(this);
		addFocusListener(this);
	}

	
	// do expand a path
	private void expand()	{
		callTree(true);
	}

	// do collapse a path
	private void collapse()	{
		callTree(false);
	}

	private void callTree(final boolean expand)	{
		final String [] path = FileUtil.getPathComponents(new File(text), OS.isWindows, false);
		// ... open non-canonical path, as Windows makes specifics ...
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				if (hasFocus == false)
					return;
					
				noSetText = !expand;	// avoid setText() when collapsing, as file-separator gets appended
				hasFocus = false;
				
				if (expand)
					texp.explorePath(path);
				else
					texp.collapsePath(path);
					
				noSetText = false;
				hasFocus = true;
			}
		});
	}


	public void setText(String s)	{
		if (noSetText == false && s != null)	{
			super.setText(s);
		}
	}
	

	// interface DocumentListener
		
	public void insertUpdate(DocumentEvent e)	{
		if (!hasFocus)
			return;

		text = getAllText();

		if (text == null || text.trim().equals(""))
			return;

		if (text.endsWith(File.separator))	{
			expand();
		}
		else	{
			File f = new File(text);
			if (f.exists())
				expand();
		}
	}
	
	public void removeUpdate(DocumentEvent e)	{
		if (!hasFocus)
			return;

		String oldtext = text;
		text = getAllText();

		if (text == null || text.trim().equals(""))
			return;

		if (!text.endsWith(File.separator) && oldtext.endsWith(File.separator))	{
			collapse();
		}
	}
	
	public void changedUpdate(DocumentEvent e)	{
	}


	private String getAllText()	{
		Document doc = getDocument();
		try	{
			text = doc.getText(0, doc.getLength());
		}
		catch (BadLocationException ex)	{
			System.err.println("FEHLER: "+ex);
		}
		return text;
	}


	// interface ActionListener
	
	/** Implemented to expand the selected history item. */
	public void actionPerformed(ActionEvent e)	{
		//System.err.println("PathTextField actionPerformed");
		text = getAllText();
		if (text == null || text.trim().equals(""))
			return;
		expand();
	}
	
	// interface FocusListener

	public void focusGained(FocusEvent e)	{
		hasFocus = true;
		text = getAllText();
		//selectAll();
	}
	public void focusLost(FocusEvent e)	{
		hasFocus = false;
	}

}