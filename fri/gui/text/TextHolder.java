package fri.gui.text;

import java.awt.Component;

/**
	Target: Common access to AWT- and Swing-TextAreas.
*/

public interface TextHolder extends TextGetSet
{
	/** Sets the current cursor position in the textarea. */
	public void setCaretPosition(int pos);

	/** Returns the current cursor position of the textarea. */
	public int getCaretPosition();

	/** Returns the start offset of selected text in textarea. */
	public int getSelectionStart();

	/** Returns the end offset of selected text in textarea. */
	public int getSelectionEnd();

	/** Selects a location within textarea. If start is equal to end, clears selection. Implicitely sets the caret position. */
	public void select(int start, int end);

	/** Returns the currently selected text. This is for setting initial search pattern. */
	public String getSelectedText();

	/** Sets the input focus to textarea. */
	public void requestFocus();

	/** Returns true if the TextHolder is NOT readonly. */
	public boolean isEditable();

	/** Returns the Component the text lies within. This is for adding the editor to a panel, or for seeking parent window. */
	public Component getTextComponent();
}
