package fri.gui.swing.filebrowser;

import java.util.Vector;
import javax.swing.*;
import java.io.File;
import fri.gui.awt.resourcemanager.ResourceIgnoringContainer;

/**
	HistoryCombo that adds text only to history if insertText()
	is called with that item. So only folders and no leafs get
	added to selection- and expand-history.
*/

public class PathHistoryCombo extends JComboBox implements
	ResourceIgnoringContainer
{
	public static int MAXHISTORY = 30;
	private DefaultComboBoxModel model;
	private Vector myHist = new Vector();
	private PathHistoryTextField editor;
	private String suggestedFileName = null;

	
	public PathHistoryCombo(TreeExpander expander)	{
		setModel(model = new DefaultComboBoxModel(myHist));
		setEditor(editor = new PathHistoryTextField(expander));
		setEditable(true);
		//Component c = getEditor().getEditorComponent();
	}

	/**
		Set the passed text as display string to the text field of combo. Called by valueChanged().
	*/
	public void setText(String s)	{
		if (suggestedFileName != null && s.endsWith(File.separator))	{
			String currPath = getText();
			
			if (currPath.endsWith(File.separator) == false)	{
				int last = currPath.lastIndexOf(File.separator);
				if (last > 0)	{
					String currFile = currPath.substring(last + 1);
					suggestedFileName = currFile;
				}
			}
			s = s+suggestedFileName;
		}

		editor.setItem(s);
	}

	/** Get the text from the text field of combo. */
	public String getText()	{
		return editor.getText();
	}

	/** Insert the passed text at second position in history. Called by treeExpanded(). */
	public void insertText(String s)	{
		if (s.equals(""))
			return;
			
		int idx = model.getIndexOf(s);
		if (idx > 0)	// is contained, not at first position, remove it
			model.removeElementAt(idx);

		if (idx != 0)	// when not contained or was removed
			model.insertElementAt(s, 0);
			
		setText(s);

		int cnt = model.getSize();
		if (cnt > MAXHISTORY)	{
			model.removeElementAt(cnt - 1);
		}
	}
	
	
	/**
		Set a (suggested) filename to path field, that should
		be kept even when path changes.
	*/
	public void setKeepFilename(String suggestedFileName)	{
		this.suggestedFileName = suggestedFileName;
		
		// replace current suggested name with new one
		String currPath = getText();
		int last;
		if ((last = currPath.lastIndexOf(File.separator)) >= 0 && currPath.endsWith(File.separator) == false)	{
			currPath = currPath.substring(0, last + 1);
		}
		editor.setItem(currPath+suggestedFileName);
	}

}
