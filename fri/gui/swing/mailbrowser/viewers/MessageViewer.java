package fri.gui.swing.mailbrowser.viewers;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.mail.*;
import javax.activation.*;
import fri.util.error.Err;
import fri.gui.swing.mailbrowser.Language;
import fri.gui.swing.mailbrowser.attachment.AttachmentPanel;

public class MessageViewer extends JPanel implements
	CommandObject
{
	public MessageViewer()	{
		super(new BorderLayout());
	}

	/** Implementing CommandObject: show the message. */
	public void setCommandContext(String verb, DataHandler dh)
		throws IOException
	{
		try	{
			removeAll();

			Object content = dh.getContent();
			System.err.println("MessageViewer, content class is: "+content.getClass());

			if (content instanceof Message)
				setMessage((Message)content);
			else
				setUnknownMessageType(content, dh.getContentType());
		}
		catch (Exception e)	{
			Err.error(e);
		}
	}

	/** Public for MessageController to render a selected message here. */
	public void setMessage(Message msg)
		throws Exception
	{
		add(new HeaderRenderer(msg), BorderLayout.NORTH);

		Component c = ViewerFactory.getViewer(msg);
		if (c != null)	{	// could be a launcher that provides no Component
			add(c, BorderLayout.CENTER);
		}
	}

	private void setUnknownMessageType(Object content, String contentType)
		throws IOException
	{
		String text = "";
		if (content instanceof InputStream)	{
			BufferedInputStream bis = new BufferedInputStream((InputStream)content);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try	{
				byte [] b = new byte[1024];
				int cnt;
				while((cnt = bis.read(b)) != -1)	{
					os.write(b, 0, cnt);
				}
				os.close();
				text = os.toString();
			}
			finally	{
				try	{ bis.close(); } catch (Exception e)	{}
			}
		}
		else	{
			text = Language.get("Having_No_Viewer_For_Message_Type"+": "+contentType+"\n"+content.getClass());
		}
		
		JTextArea ta = new JTextArea(text);
		ta.setEditable(false);
		ta.setLineWrap(true);
		
		add(new JScrollPane(ta));
	}



	// this is workaround as all viewer are dynamiclly loaded and no static attachment panel is possible.
	

	private AttachmentPanel attachmentPanel;
	
	/** Loop all Sub-Components for AttachmentPanels and add their attachments to this one. */
	public void organizeAttachmentPanels()	{
		attachmentPanel = null;
		organizeAttachmentPanels(this);
	}

	private void organizeAttachmentPanels(Container toLoop)	{
		// search for attachment panel
		for (int i = toLoop.getComponentCount() - 1; i >= 0; i--)	{
			Component c = toLoop.getComponent(i);
			
			if (c instanceof AttachmentPanel)	{
				if (attachmentPanel == null)	{
					attachmentPanel = (AttachmentPanel)c;
				}
				else	{
					Container container = (Container)c;
					for (int j = container.getComponentCount() - 1; j >= 0; j--)	{
						Component btn = container.getComponent(j);
						((JComponent)container).remove(btn);
						attachmentPanel.add(btn);
					}
					((JComponent)container.getParent()).remove(container);
				}
				break;	// not more than one attachment panel in this container
			}
		}
		
		// loop all subcomponents for attachment panels
		for (int i = toLoop.getComponentCount() - 1; i >= 0; i--)	{
			Component c = toLoop.getComponent(i);
			if (c instanceof Container)
				organizeAttachmentPanels((Container)c);
		}
	}

}