package fri.gui.swing.datechooser;

import java.util.Calendar;

/**
	This listener catches the mouse press on a day button
	in CalendarCombo (which closes the popup).
	
	@author Fritz Ritzberger
*/

public interface DaySelectionListener
{
	/** Implementers receive this when user clicked on any day. */
	public void daySelectionChanged(Calendar selected);
}