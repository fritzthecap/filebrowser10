package fri.gui.swing.mailbrowser.send;

import java.io.File;
import java.util.Vector;
import java.awt.*;
import java.awt.event.WindowListener;
import javax.swing.*;
import javax.mail.*;
import fri.util.os.OS;
import fri.util.mail.*;
import fri.util.error.Err;
import fri.gui.mvc.view.swing.PopupMouseListener;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.mailbrowser.Language;
import fri.gui.swing.mailbrowser.attachment.AttachmentPanel;
import fri.gui.swing.text.ClipableJTextField;
import fri.gui.swing.combo.history.*;

/**
	Send panel, building all GUI elements.
*/
public class SendPanel extends JPanel
{
	private SendController controller;
	private HistCombo to, cc, remail;
	private ClipableJTextField subject;
	private AttachmentPanel attachments;
	
	/**
		Open a send panel. Optionally there can be passed a reply-to- or forward-message.
		If this is non-null, sendFolders must be given.
		@param reply true for reply, false for forward, null for none
		@param reply true for reply, false for forward, null for none
	*/
	public SendPanel(
		WindowListener frameCloser,
		SendProperties props,
		SendFolderSet sendFolders,
		Message msg,
		Boolean reply)
	{
		super(new BorderLayout());
		
		JPanel pLeft = new JPanel(new GridLayout(4, 1));
		pLeft.add(new JLabel(Language.get("To")));
		pLeft.add(new JLabel(Language.get("Cc")));
		pLeft.add(new JLabel(Language.get("ReMail")));
		pLeft.add(new JLabel(Language.get("Subject")));
		
		JPanel pRight = new JPanel(new GridLayout(4, 1));
		pRight.add(to = new RecipientCombo());
		to.clear();
		pRight.add(cc = new CcRecipientCombo());
		cc.clear();
		pRight.add(remail = new RemailCombo());
		remail.clear();
		pRight.add(subject = new ClipableJTextField());
		subject.setToolTipText(Language.get("Send_Subject"));
		
		JPanel header = new JPanel(new BorderLayout());
		header.add(pLeft, BorderLayout.WEST);
		header.add(pRight, BorderLayout.CENTER);

		attachments = new SendAttachmentPanel();
		attachments.setBorder(BorderFactory.createTitledBorder(Language.get("Attachments")));

		controller = new SendController(props, sendFolders, to, cc, remail, subject, attachments, frameCloser, msg);
		MessageEditor editor = new MessageEditor(controller);	// needs controller for inserting actions

		// check if reply or forward message
		if (msg != null)	{
			try	{
				setMessage(msg, sendFolders, editor, attachments, reply);

				if (reply != null)	{
					if (reply.equals(Boolean.TRUE))	{	// set the "To:" field if reply is true
						subject.setText("Re: "+msg.getSubject());
						to.setText(makeReplyAddress(msg));
						ComponentUtil.requestFocus(editor.getSensorComponent());
					}
					else	{	// forward
						subject.setText("Fw: "+msg.getSubject());
						ComponentUtil.requestFocus(to.getTextEditor());
					}
				}
				else	{
					subject.setText(msg.getSubject());
					to.setText(MessageUtil.addressesToString(msg.getRecipients(Message.RecipientType.TO)));
					cc.setText(MessageUtil.addressesToString(msg.getRecipients(Message.RecipientType.CC)));
					if (to.getText().length() <= 0)
						ComponentUtil.requestFocus(to.getTextEditor());
					else
					if (subject.getText().length() <= 0)
						ComponentUtil.requestFocus(subject);
					else
						ComponentUtil.requestFocus(editor.getSensorComponent());
				}
			}
			catch (Exception e)	{
				Err.error(e);
			}
		}
		else	{
			ComponentUtil.requestFocus(to.getTextEditor());

			if (SignatureDialog.getSignature() != null)	{
				editor.getSensorComponent().setText("\n\n\n"+SignatureDialog.getSignature());
			}
		}
		
		
		controller.setEditor(editor, msg, reply);	// needs a sensor component for undo and other listeners
		
		JToolBar tb = new JToolBar();
		if (OS.isAboveJava13) tb.setRollover(true);
		
		controller.visualizeAction(SendController.ACTION_SEND, tb);
		controller.visualizeAction(SendController.ACTION_CONFIGURE, tb);
		controller.visualizeAction(SendController.ACTION_SAVE, tb);
		tb.addSeparator();
		controller.visualizeAction(SendController.ACTION_ATTACH, tb);
		tb.addSeparator();
		controller.visualizeAction(SendController.ACTION_CUT, tb);
		controller.visualizeAction(SendController.ACTION_COPY, tb);
		controller.visualizeAction(SendController.ACTION_PASTE, tb);
		tb.addSeparator();
		controller.visualizeAction(SendController.ACTION_UNDO, tb);
		controller.visualizeAction(SendController.ACTION_REDO, tb);
		tb.addSeparator();
		controller.visualizeAction(SendController.ACTION_SIGNATURE, tb);
		tb.addSeparator();
		controller.visualizeAction(SendController.ACTION_CRYPT, tb);
		
		JPopupMenu popup = new JPopupMenu();
		controller.visualizeAction(SendController.ACTION_ATTACH, popup, false);
		attachments.addMouseListener(new PopupMouseListener(popup));
		
		JPanel north = new JPanel(new BorderLayout());
		north.add(tb, BorderLayout.NORTH);
		north.add(header, BorderLayout.CENTER);
		//north.add(attachments, BorderLayout.SOUTH);

		add(north, BorderLayout.NORTH);
		add(new JScrollPane(editor.getSensorComponent()), BorderLayout.CENTER);
		add(attachments, BorderLayout.SOUTH);
		
		new AttachmentDndReceiver(attachments, controller);
		new AttachmentDndReceiver(editor.getSensorComponent(), controller);
	}


	private String makeReplyAddress(Message msg)	{
		String toAddresses = "";
		try	{
			toAddresses = MessageUtil.addressesToString(msg.getReplyTo());
		}
		catch (MessagingException e)	{
			Err.error(e);
		}
		return toAddresses;
	}
	
	
	private void setMessage(
		final Message msg,
		SendFolderSet sendFolders,
		MessageEditor editor,
		final AttachmentPanel attachments,
		final Boolean reply)
		throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		
		ReceiveMail.MessagePartVisitor visitor = new ReceiveMail.MessagePartVisitor()	{
			private boolean first = true;
			
			public void finalPart(int absolutePartNumber, int treeLevel, Part part) throws Exception	{
				System.err.println("SendPanel.setMessage, reply is "+reply+", checking content type "+part.getContentType());
				if (part.getContentType().toLowerCase().startsWith("text/plain"))	{	// is plain text
					String text = part.getContent().toString();
						
					if (reply != null)	{	// add header line about sender, mark written text
						if (first)	{
							first = false;
							sb.append("\n\n");
							sb.append(reply.equals(Boolean.TRUE) ? Language.get("You_Wrote") : makeReplyAddress(msg)+" "+Language.get("Person_Wrote"));
							sb.append("\n\n");
						}
						
						// mark written text
						String marker = "> ";
						for (int i = 0; i < treeLevel; i++)
							marker = marker + "> ";

						text = MessageUtil.replyText(text, marker);
					}
						
					sb.append(text);
				}
				else	{	// add attachment if draft or forward-message or some rich text part
					if (reply == null || reply.equals(Boolean.FALSE) /*|| part.getContentType().startsWith("text/")*/ )	{
						attachments.addAttachment(part);
					}
				}
			}
			
			public void multiPart(int absolutePartNumber, int treeLevel, Part part) throws Exception {
			}
		};
		
		sendFolders.current.messageParts(msg, visitor);
		editor.setText(sb.toString());
	}

	
	/** Set the recipients, needed by AddressController. */
	public void setTo(Address [] addresses)	{
		to.setText(MessageUtil.addressesToString(addresses));
	}
	
	
	public boolean close()	{
		to.save();
		cc.save();
		remail.save();
		return controller.close();
	}

}


class RecipientCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();
	private static File globalFile = null;

	public RecipientCombo()	{
		super();
		setToolTipText(Language.get("Enter_Mail_Recipients"));
		manageTypedHistory(this, new File(HistConfig.dir()+"MailRecipientCombo.list"));
	}

	// interface TypedHistoryHolder
	public void setTypedHistoryData(Vector v, File f)	{
		globalHist = v;
		globalFile = f;
	}
	public Vector getTypedHistory()	{
		return globalHist;
	}
	public File getHistoryFile()	{
		return globalFile;
	}
}

class CcRecipientCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();
	private static File globalFile = null;

	public CcRecipientCombo()	{
		super();
		setToolTipText(Language.get("Enter_Other_Mail_Recipients"));
		manageTypedHistory(this, new File(HistConfig.dir()+"MailCcRecipientCombo.list"));
	}

	// interface TypedHistoryHolder
	public void setTypedHistoryData(Vector v, File f)	{
		globalHist = v;
		globalFile = f;
	}
	public Vector getTypedHistory()	{
		return globalHist;
	}
	public File getHistoryFile()	{
		return globalFile;
	}
}

class RemailCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();
	private static File globalFile = null;
	
	public RemailCombo()	{
		super();
		setToolTipText(Language.get("Enter_Re_Mail_Adress"));
		manageTypedHistory(this, new File(HistConfig.dir()+"MailRemailCombo.list"));
	}
	
	// interface TypedHistoryHolder
	public void setTypedHistoryData(Vector v, File f)	{
		globalHist = v;
		globalFile = f;
	}
	public Vector getTypedHistory()	{
		return globalHist;
	}
	public File getHistoryFile()	{
		return globalFile;
	}
}