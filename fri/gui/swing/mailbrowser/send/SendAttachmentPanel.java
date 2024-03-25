package fri.gui.swing.mailbrowser.send;

import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.mail.*;
import fri.gui.swing.mailbrowser.attachment.AttachmentPanel;
import fri.gui.swing.mailbrowser.attachment.AttachmentButton;

/**
	Manage a list of AttachmentButtons, to remove attachment BodyPart when a button is removed.
	
	@author Fritz Ritzberger, 2003
*/

public class SendAttachmentPanel extends AttachmentPanel implements
	ContainerListener	// listen for attachment button removal
{
	private Vector buttons;
	
	public SendAttachmentPanel()	{
		addContainerListener(this);
	}

	public AttachmentButton addAttachment(Part part)
		throws MessagingException
	{
		if (buttons == null)
			buttons = new Vector();

		AttachmentButton btn = super.addAttachment(part);
		
		buttons.add(btn);	// for recognition when removed
		return btn;
	}

	protected void createController(AttachmentButton btn)	{
		PartOpenRemoveController.installOpenRemovePopup(btn);
	}

	/** Implements ContainerListener to remove attachment when its button is removed. */
	public void componentRemoved(ContainerEvent e)	{
		Component c = e.getChild();
		for (int i = 0; i < buttons.size(); i++)	{
			if (buttons.get(i) == c)	{
				attachments.remove(i);
				buttons.remove(i);
				return;
			}
		}
		throw new RuntimeException("Button not found: "+c);
	}

	public void componentAdded(ContainerEvent e)	{
	}

}
