package fri.gui.swing.foldermonitor;

import java.util.Date;

/**
	The View interface:
	Implementers render an event from a background watchdog thread
	and indicate when the watch is isInterrupted
*/

interface EventRenderer
{
	/** Renders the passed event information. */
	public void event(Date time, String change, String name, String path, String type, long size);

}