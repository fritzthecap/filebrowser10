package fri.gui.swing.resourcemanager;

import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
	ActionListener that brings up the component choice dialog on <i>actionPerformed()</i>.
	This ActionListener only works in conjunction with <i>JResourceManagingEventQueue</i>.
	If this is not wanted, use <i>fri.gui.awt.resourcemanager.ResourceDialogActionListener</i>.
*/

public class JResourceDialogActionListener implements ActionListener
{
	private Window parent;
	
	/** Constructs an ActionListener for a Window that will be requested from ResourceManagingEventQueue on actionPerformed. */
	public JResourceDialogActionListener(Window parent)	{
		this.parent = parent;
	}
	
	/** Opens the dialog by means of ResourceManagingEventQueue. */
	public void actionPerformed(ActionEvent e)	{
		JResourceManagingEventQueue.showDialog(parent);
	}

}
