package fri.gui.swing.filebrowser;

import java.io.File;
import java.awt.Color;
import javax.swing.*;
import javax.swing.border.TitledBorder;

import fri.gui.CursorUtil;
import fri.gui.swing.editor.*;

/**
	TextArea that renders found lines from one file.
	It provides methods to open the file, and start
	a search window in the editor or viewer.
*/

public class SearchResult extends JTextArea
{
	private File file;
	private int foundLocations;
	private String pattern;
	private String syntax;
	private boolean ignoreCase, wordMatch;
	
	
	public SearchResult(
		File file,
		int found,
		String lines,
		String pattern,
		String syntax,
		boolean ignoreCase,
		boolean wordMatch)
	{
		this.file = file;
		this.pattern = pattern;
		this.syntax = syntax;
		this.ignoreCase = ignoreCase;
		this.wordMatch = wordMatch;
		this.foundLocations = found;
		
		String title = file.getName()+" - \""+pattern+"\"  matched "+found+" location(s).  "+file.getParent();
		setBorder(BorderFactory.createTitledBorder(title));
		((TitledBorder)getBorder()).setTitleColor(Color.gray);
		((TitledBorder)getBorder()).setBorder(BorderFactory.createLineBorder(Color.gray));

		setTabSize(2);
		setEditable(false);
		setText(lines);
		setToolTipText("Double Click Or Open Popup To Search In File");
	}

	/** Internal view the selected File and search patterns. */
	void view()	{
		CursorUtil.setWaitCursor(this);
		try	{
			new FileViewer(file, pattern, syntax, ignoreCase, wordMatch);
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}

	/** Internal edit the selected File and search patterns. */
	void edit()	{
		CursorUtil.setWaitCursor(this);
		try	{
			EditorFrame ed = TreeEditController.getEditor(file);
			ed.find(pattern, syntax, ignoreCase, wordMatch);
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}
	
	EditorFrame editNoFind()	{
		return TreeEditController.getEditor(file);
	}
	
	String getPattern()	{
		return pattern;
	}

	String getSyntax()	{
		return syntax;
	}

	boolean getIgnoreCase()	{
		return ignoreCase;
	}

	boolean getWordMatch()	{
		return wordMatch;
	}

	File getFile()	{
		return file;
	}

	int getFoundLocations()	{
		return foundLocations;
	}

}