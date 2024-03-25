package fri.gui.swing.diff;

import java.awt.Component;

/**
	Bring up a dialog and load chosen file into a given view.
*/

public interface FileObjectOpenDialog extends FileObjectLoader
{
	/** Called by open action from one of the two diff views. */
	public void openFile(Component c);

}