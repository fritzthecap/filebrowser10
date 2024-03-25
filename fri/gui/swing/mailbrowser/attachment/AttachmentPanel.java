package fri.gui.swing.mailbrowser.attachment;

import java.io.*;
import java.util.Vector;
import javax.swing.*;
import javax.mail.*;
import javax.mail.internet.MimeUtility;
import fri.util.mail.MessageUtil;
import fri.gui.swing.mailbrowser.Language;

/**
	Panel that holds action buttons for all attachments. It serves as attachment viewer
	and attachment editor when writing a new message. The managed attachment list
	contains BodyPart objects.
	
	@author Fritz Ritzberger, 2003
*/

public class AttachmentPanel extends JPanel
{
	protected Vector attachments;

	public AttachmentPanel()	{
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	}

	/** Returns the number of contained attachments. */
	public int getAttachmentCount()	{
		return attachments == null ? 0 : attachments.size();
	}
	
	/** Returns the list of contained Part attachments. */
	public Vector getAttachments()	{
		return attachments;
	}
	
	/** Add a Part attachments and show a button that represents it. */
	public AttachmentButton addAttachment(Part part)
		throws MessagingException
	{
		// add to attachment list
		
		if (attachments == null)	{
			attachments = new Vector();
		}
		attachments.add(part);
		
		// add button
		
		String label = makeButtonLabel(part);
		if (label != null && label.length() > 40)
			label = label.substring(0, 40)+" ...";
		String tooltip = Language.get("Attachment_Type")+": "+MessageUtil.baseContentType(part);

		AttachmentButton btn = new AttachmentButton(part, label, tooltip);
		createController(btn);

		add(btn);
		
		return btn;
	}

	protected void createController(AttachmentButton btn)	{
		PartOpenSaveController.installOpenSavePopup(btn);
	}


	/** Create a button-label from a message Part: filename or description or content-type. */
	public static String makeButtonLabel(Part part)
		throws MessagingException
	{
		String label = part.getFileName();
		if (label == null)
			label = part.getDescription();
		else
			label = new File(label).getName();
			
		if (label == null)	{
			label = MessageUtil.baseContentType(part);
		}
		else	{
			try	{
				label = MimeUtility.decodeText(label);
			}
			catch (UnsupportedEncodingException e)	{
			}
		}
		
		return label;
	}

}
