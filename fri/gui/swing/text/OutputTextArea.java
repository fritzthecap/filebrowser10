package fri.gui.swing.text;

import javax.swing.text.Document;
import fri.gui.text.*;

/**
	Implements TextRenderer und refreshes tabsize after setDocument().
*/

public class OutputTextArea extends ClipableJTextArea implements
	TextRenderer
{
	public OutputTextArea()	{
	}
	
	public OutputTextArea(int rows, int columns)	{
		super(rows, columns);
	}
	
	/** Overridden to set tabsize again after loading Document. */	
	public void setDocument(Document doc)	{
		int tabSize = getTabSize();
		super.setDocument(doc);
		super.setTabSize(tabSize);
	}
	
	/** Overridden to do a refresh by toggling lineWrap two times. */	
	public void setTabSize(int size)	{
		super.setTabSize(size);
		
		// need some refresh
		boolean b = getLineWrap();
		super.setLineWrap(!b);
		super.setLineWrap(b);
	}
}
