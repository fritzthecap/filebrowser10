package fri.gui.swing.editor;

import java.io.File;
import java.awt.Point;
import javax.swing.JComponent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditListener;
import fri.gui.text.TextHolder;

/**
	This interface models everything that the desktop pane
	needs from the textarea that edits a file.
*/

public interface EditorTextHolder extends TextHolder
{
	public interface ChangeListener
	{
		/**
			Text has changed or was saved. This gets called at every text change
			with parameter true, and when saved with paramter false.
			Any object that is interested in every text change can register
			as ChnageListener (like a search window that needs to refresh
			its found indexes).
			@param dirty false, if text was saved, true when text was changed.
		*/
		public void changed(boolean dirty);
	}

	/** Load data that were passed in constructor. */
	public void load();
	
	/** Reload data that were passed in constructor. */
	public void reload();

	/** Save data. @return true if ok  */
	public boolean save();

	/** Save data under an filename that must be chosen. @return true if ok  */
	public boolean saveAs();
	
	/** Interrupt loading as the frame gets closed. */
	public void interrupt();

	/** Return true if data were changed in editor */
	public boolean getChanged();

	/** Set the borderlayouted container to show a progress bar during load. */
	public void setProgressContainer(JComponent panel);

	/** Set a undo handler */
	public void setUndoListener(UndoableEditListener undoListener);
	public void unsetUndoListener(UndoableEditListener undoListener);

	/** Set a caret listener for showing line number and character position. */
	public void setCaretListener(CaretListener cl);
	public void unsetCaretListener(CaretListener cl);
	
	/** Convert the current caret position to x=character/y=line point. To be called from CaretListener. */
	public Point caretToPoint(int caretPos);

	/** Signalize changes in document */
	public void setChangeListener(ChangeListener cl);
	public void unsetChangeListener(ChangeListener cl);

	/** Return the file that is loaded in this textarea */
	public File getFile();
	
	/** Set the textarea to warn or not if file was changed from background. */
	public void setWarnDirty(boolean warnDirty);

	/** Return documents of textholder, for removing its undoable edits */
	public Object getUndoableEditIdentifier();

	/** Return true if file is currenty loading. The wait object is the EditorTextHolder itself. */
	public boolean isLoading();

}