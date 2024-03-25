package fri.gui.swing.mailbrowser.viewers;

import java.awt.event.*;
import fri.gui.mvc.view.swing.DoubleClickListener;
import fri.gui.swing.mailbrowser.attachment.AttachmentButton;

/**
	Abstract controller that contains the "Open" action for attachments, with its callback.
*/

public abstract class AbstractPartOpenController extends AbstractPartController
{
	public static final String ACTION_OPEN = "Open";
	
	/** Create a "Open" cntroller for passed PartView. */
	public AbstractPartOpenController(AttachmentButton opener)	{
		super(opener);
		registerAction(ACTION_OPEN, (String)null, (String)null, KeyEvent.VK_ENTER, 0);
	}

	public void cb_Open(Object selection)	{
		((AttachmentButton)partView).open();
	}


	/** Install a popup and attaches a DoubleClickMouseListener to it. */
	public static void installOpenPopup(final AbstractPartOpenController controller, AttachmentButton partOpener, String [] actionNames)	{
		installPopup(controller, partOpener, actionNames);
		
		DoubleClickListener.install(partOpener, new ActionListener()	{	// controller can not be action listener!
			public void actionPerformed(ActionEvent e)	{
				controller.cb_Open(null);
			}
		});
	}

}