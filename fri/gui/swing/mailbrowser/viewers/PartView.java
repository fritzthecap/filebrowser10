package fri.gui.swing.mailbrowser.viewers;

import javax.swing.JComponent;
import javax.activation.DataHandler;

/**
	PartView implementers have a sensor component for installing
	a popup, and provide a Part for showing its contents and saving it to filesystem.
*/

public interface PartView
{
	/** Returns the component on which a popup would get installed, or to be a dialog parent. */
	public JComponent getSensorComponent();
	
	/** Returns the DataHandler from the obtained attachment Part. */
	public DataHandler getDataHandler();
}