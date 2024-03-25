package fri.gui.swing.mailbrowser.viewers;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.activation.*;
import javax.mail.*;
import fri.util.error.Err;
import fri.gui.swing.mailbrowser.attachment.AttachmentPanel;

public class MultipartViewer extends JPanel implements
	CommandObject
{
	public MultipartViewer()	{
		super(new BorderLayout());
	}

	/** Implementing CommandObject: show the message. */
	public void setCommandContext(String verb, DataHandler dh)
		throws IOException
	{
		//System.err.println("setting command context for multipart viewer, content type: "+dh.getContentType());
		Multipart multiPart = (Multipart)dh.getContent();
		Component mainComponent = null;

		try {
			BodyPart mainBody = multiPart.getBodyPart(0);
			mainComponent = ViewerFactory.getViewer(mainBody);
			add(mainComponent, BorderLayout.CENTER);
		}
		catch (Exception e) {
			Err.error(e);
		}

		AttachmentPanel attachments = null;

		try {
			for (int i = 1; i < multiPart.getCount(); i++)	{
				try {
					BodyPart part = multiPart.getBodyPart(i);
					String disposition = part.getDisposition();
					Component c;
					
					if (disposition != null &&
							disposition.equalsIgnoreCase(Part.INLINE) &&
							mainComponent instanceof TextViewer &&
							ViewerFactory.canViewInline(part) &&
							(c = getViewer(part)) != null)
					{
						// add to mainComponent bottom
						TextViewer tv = (TextViewer)mainComponent;
						tv.addInlineComponent(c);

						if (c instanceof JComponent)
							((JComponent)c).setToolTipText(AttachmentPanel.makeButtonLabel(part));
					}
					else	{
						if (attachments == null)
							attachments = new AttachmentPanel();
						
						attachments.addAttachment(part);
					}
				}
				catch (Exception e)	{
					Err.error(e);
				}
			}
		}
		catch (MessagingException e)	{
			Err.error(e);
		}
		
		if (attachments != null && attachments.getAttachmentCount() > 0)	{
			JScrollPane sp = new JScrollPane(
					attachments,
					JScrollPane.VERTICAL_SCROLLBAR_NEVER,
					attachments.getComponentCount() > 3 ?
							JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
							: JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			add(sp, BorderLayout.SOUTH);
		}
	}

	
	private Component getViewer(BodyPart part)	{
		try	{
			return ViewerFactory.getViewer(part);
		}
		catch (Throwable e)	{
			Err.error(e);
		}
		return null;
	}

}
