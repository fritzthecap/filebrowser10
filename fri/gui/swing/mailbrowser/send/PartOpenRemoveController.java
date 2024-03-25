package fri.gui.swing.mailbrowser.send;

import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import fri.gui.swing.mailbrowser.attachment.AttachmentButton;
import fri.gui.swing.mailbrowser.viewers.AbstractPartOpenController;

/**
	Controller that contains the "Remove" action to remove a PartView from its AttachmentPanel.
*/

public class PartOpenRemoveController extends AbstractPartOpenController
{
	public static final String ACTION_REMOVE = "Remove";
	
	/** Create a "Delete" cntroller for passed PartView. */
	public PartOpenRemoveController(AttachmentButton opener)	{
		super(opener);
		registerAction(ACTION_REMOVE, (String)null, (String)null, KeyEvent.VK_DELETE, 0);
	}


	/** Callback for "Save As" action. */
	public void cb_Remove(Object selection)	{
		JComponent btn = (JComponent)partView;	// MUST be attachment button!
		JComponent parent = (JComponent)btn.getParent();
		parent.remove(btn);
		
		parent.revalidate();
		parent.repaint();
	}


	
	/** Install a popup with "Open" and "Remove" items on the passed PartView. */
	public static void installOpenRemovePopup(AttachmentButton partOpener)	{
		installOpenPopup(new PartOpenRemoveController(partOpener), partOpener, new String [] { ACTION_OPEN, ACTION_REMOVE });
	}

}
