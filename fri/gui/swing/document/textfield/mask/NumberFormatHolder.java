package fri.gui.swing.document.textfield.mask;

import java.text.NumberFormat;

/**
	Interface for all number masks that need formatting by
	FormattingFocusListener.
*/

public interface NumberFormatHolder
{
	public NumberFormat getNumberFormat(boolean focusLost);
	public String getDisplayText();
	public void setDisplayText(String s);
}