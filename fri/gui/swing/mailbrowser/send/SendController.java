package fri.gui.swing.mailbrowser.send;

import java.io.File;
import java.util.*;
import java.awt.event.*;
import javax.swing.text.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import fri.util.error.Err;
import fri.util.mail.*;
import fri.gui.swing.undo.*;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.progressdialog.*;
import fri.gui.swing.filechooser.*;
import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.mailbrowser.Language;
import fri.gui.swing.mailbrowser.CryptDialog;
import fri.gui.swing.mailbrowser.ConfigureDialog;
import fri.gui.swing.mailbrowser.attachment.AttachmentPanel;

/**
	Send controller.
*/

public class SendController extends ActionConnector implements
	DocumentListener,
	CaretListener,
	ContainerListener
{
	public static final String ACTION_SEND = "Send";
	public static final String ACTION_SAVE = "Save";
	public static final String ACTION_ATTACH = "Attach";
	public static final String ACTION_CUT = "Cut";
	public static final String ACTION_COPY = "Copy";
	public static final String ACTION_PASTE = "Paste";
	public static final String ACTION_UNDO = "Undo";
	public static final String ACTION_REDO = "Redo";
	public static final String ACTION_CONFIGURE = "Configure";
	public static final String ACTION_SIGNATURE = "Signature";
	public static final String ACTION_CRYPT = "Cryptography";
	
	private SendProperties props;
	private SendFolderSet sendFolders;
	private DoAction redo, undo;
	private HistCombo to, cc, remail;
	private JTextComponent subject;
	private MessageEditor editor;
	private AttachmentPanel attachments;
	private Message oldMessage;
	private WindowListener frameCloser;
	private Message messageToDelete;
	
	
	/**
		Allocate a controller for all actions in the send message window.
		@param props SendProperties, host and from-address and optional SMTP authentication user/password
		@param sendFolders drafts, outbox, sent-mail folders for saving and sending the message, can be null
		@param to textfield for recipient(s)
		@param cc textfield for "Cc" recipient(s)
		@param subject textfield for mail subject
		@param attachments panel holding all attachments if any
		@param frameCloser frame that provides <i>windowClosing(null)</i> for closing the window after sending, can be null
	*/
	public SendController(
		SendProperties props,
		SendFolderSet sendFolders,
		HistCombo to,
		HistCombo cc,
		HistCombo remail,
		JTextComponent subject,
		AttachmentPanel attachments,
		WindowListener frameCloser,
		Message msg)
	{
		super(null, null, null);
		
		this.props = props;
		this.sendFolders = sendFolders;
		this.to = to;
		this.cc = cc;
		this.remail = remail;
		this.subject = subject;
		this.attachments = attachments;
		this.frameCloser = frameCloser;
		this.oldMessage = msg;
		
		registerAction(ACTION_SEND, Icons.get(Icons.sendMail), "Send Message", KeyEvent.VK_S, InputEvent.ALT_MASK);
		registerAction(ACTION_SAVE, Icons.get(Icons.save), "Save Draft", KeyEvent.VK_S, InputEvent.CTRL_MASK);
		registerAction(ACTION_ATTACH, Icons.get(Icons.pin), "Attach File");
		registerAction(ACTION_CONFIGURE, Icons.get(Icons.configure), "Configure Send Connection");
		registerAction(ACTION_SIGNATURE, Icons.get(Icons.signature), "Edit Mail Signature");
		registerAction(ACTION_CRYPT, Icons.get(Icons.key), "Cryptography");

		Action cut = new DefaultEditorKit.CutAction();
		cut.putValue(Action.NAME, ACTION_CUT);
		registerAction(cut, Icons.get(Icons.cut), "Cut Selection", KeyEvent.VK_X, InputEvent.CTRL_MASK);

		Action copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, ACTION_COPY);
		registerAction(copy, Icons.get(Icons.copy), "Copy Selection", KeyEvent.VK_C, InputEvent.CTRL_MASK);

		Action paste = new DefaultEditorKit.PasteAction();
		paste.putValue(Action.NAME, ACTION_PASTE);
		registerAction(paste, Icons.get(Icons.paste), "Paste Clipboard", KeyEvent.VK_V, InputEvent.CTRL_MASK);
		
		undo = new DoAction(DoAction.UNDO);
		undo.putValue(Action.NAME, ACTION_UNDO);
		registerAction(undo, Icons.get(Icons.undo), "Undo Previous Action", KeyEvent.VK_Z, InputEvent.CTRL_MASK);
		
		redo = new DoAction(DoAction.REDO);
		redo.putValue(Action.NAME, ACTION_REDO);
		registerAction(redo, Icons.get(Icons.redo), "Redo Undone Action", KeyEvent.VK_Y, InputEvent.CTRL_MASK);
		
		setEnabled(ACTION_SAVE, false);
		setEnabled(ACTION_CUT, false);
		setEnabled(ACTION_COPY, false);
		setEnabled(ACTION_PASTE, true);
		
		//setEnabled(ACTION_UNDO, false);	// undo and redo action are delegates and can not set enabled their owners!
		//setEnabled(ACTION_REDO, false);

		// set save enabled when subject gets entered
		subject.getDocument().addDocumentListener(this);
	}


	/** Do internationalization for action labels. */
	protected String language(String label)	{
		return Language.get(label);
	}


	public void setEditor(MessageEditor editor, Message msg, Boolean reply)	{
		JTextComponent sensor = editor.getSensorComponent();
		DoListener doListener = new DoListener(undo, redo);
		sensor.getDocument().addUndoableEditListener(doListener);

		changeAllKeyboardSensors(sensor);

		sensor.getDocument().addDocumentListener(this);
		sensor.addCaretListener(this);
		
		this.editor = editor;
		
		attachments.addContainerListener(this);	// listen for changes in attachment panel
		
		if (reply == null && msg != null)	{	// must be a draft or outbox, about to be sent
			this.messageToDelete = msg;
		}
	}


	/** Implements ContainerListener to set save button enabled. */
	public void componentRemoved(ContainerEvent e)	{
		setChanged(true);
	}

	public void componentAdded(ContainerEvent e)	{
		setChanged(true);
	}


	/** Implements CaretListener to set enabled Cut and Copy actions. */
	public void caretUpdate(CaretEvent e) {
		boolean selection = (e.getDot() != e.getMark());
		boolean canEdit = selection && editor.getSensorComponent().isEditable();
		setEnabled(ACTION_CUT, canEdit);
		setEnabled(ACTION_COPY, canEdit);
	}

	/** implements DocumentListener */	
	public void changedUpdate(DocumentEvent e)	{
		setChanged(true);
	}
	/** implements DocumentListener */	
	public void insertUpdate(DocumentEvent e)	{
		setChanged(true);
	}
	/** implements DocumentListener */	
	public void removeUpdate(DocumentEvent e)	{
		setChanged(true);
	}

	private void setChanged(boolean changed)	{
		setEnabled(ACTION_SAVE, changed && sendFolders != null);
	}



	private Vector getToAddresses(TextLineHolder textField)
		throws Exception
	{
		String addresses = textField.getText();
		addresses = addresses != null ? addresses.replace(';', ',') : null;
		InternetAddress [] recipients = InternetAddress.parse(addresses);
		if (recipients == null || recipients.length <= 0)
			return null;
		
		Vector v = new Vector();
		for (int i = 0; i < recipients.length; i++)
			v.add(recipients[i].toString());
		
		return v;
	}


	private SendMail buildSendMail(boolean sendPending)	{
		try	{
			String from = remail.getText();
			if (from.trim().length() <= 0)	{
				from = props.getFrom();
				if (from == null)
					from = "";
				else
				if (props.getPersonal() != null)
					from = "\""+props.getPersonal()+"\" <"+from+">";
			}
			
			Vector recipientList = getToAddresses(to);
			if (recipientList == null && sendPending)	{
				Err.warning(Language.get("Please_Enter_Recipients"));
				to.getTextEditor().requestFocus();
				return null;
			}
			
			Vector ccList = getToAddresses(cc);
			
			String subjectText = subject.getText().trim();
			if (subjectText.length() <= 0 && sendPending)	{
				int ret = JOptionPane.showConfirmDialog(
						ComponentUtil.getWindowForComponent(defaultKeySensor),
						Language.get("Send_Message_Without_Subject"),
						Language.get("Warning"),
						JOptionPane.YES_NO_CANCEL_OPTION);
				
				if (ret != JOptionPane.YES_OPTION)	{
					subject.requestFocus();
					return null;	// was canceled
				}
			}

			String mailText = editor.getText();
			
			Vector attachmentList = attachments.getAttachments();
			
			SendMail sendMail = new SendMail(
					from,
					recipientList,
					ccList,
					subjectText,
					mailText,
					attachmentList,
					props,
					props.getPassword());
			
			return sendMail;
		}
		catch (Exception e)	{
			Err.error(e);
			return null;
		}
	}



	public void cb_Send(Object selection)	{
		// ensure there are enough send parameters
		if (props.isValid() == false)	{
			ConfigureDialog dlg;
			do	{
				dlg = new ConfigureDialog(defaultKeySensor, props);
				
				if (dlg.wasCanceled() == false)
					Err.warning(Language.get("Invalid_Send_Properties"));
			}
			while (props.isValid() == false && dlg.wasCanceled() == false);
			
			if (props.isValid() == false)
				return;
		}
		
		// send the message
		final SendMail sendMail = buildSendMail(true);
		if (sendMail == null)
			return;
		
		setEnabled(ACTION_SEND, false);

		final CancelProgressDialog progressDlg = new CancelProgressDialog(
				editor.getSensorComponent(),
				Language.get("Sending_Message")+" ...",
				4);	// 1: save to outbox, 2: send, 3: delete from outbox, 4: save to sent-mail
		progressDlg.setIsByteSize(false);
		
		Runnable todo = new Runnable()	{
			public void run()	{
				try	{
					Message msg = sendMail.getMessage();
					msg.addHeader("X-Mailer", "Fri-Mail (c) Ritzberger Fritz 2003");
					Message [] sendingMessage = new Message [] { msg };
					
					if (sendFolders != null)	{
						sendFolders.outbox.append(sendingMessage);

						if (messageToDelete != null)	{	// if it is a draft or resent from outbox, the original message can be deleted
							sendFolders.current.delete(new Message [] { messageToDelete });
							messageToDelete = null;
						}
					}
					progressDlg.progress(1);
					System.err.println("Appended message to outbox folder, sending it now ...");
					
					sendMail.send();	// really send the mail
					progressDlg.progress(1);
					System.err.println("Sent message to receiver ...");
					
					if (sendFolders != null)	{
						sendFolders.sent.append(new Message [] { msg });
						System.err.println("Appended message to sent-mail folder ...");
						progressDlg.progress(1);

						sendFolders.outbox.delete(sendingMessage);
						System.err.println("Deleted message from outbox folder ...");
					}
					progressDlg.progress(1);
				}
				catch (Exception e)	{
					if (e.toString().indexOf("Unknown SMTP host") >= 0)
						Err.warning(Language.get("Not_Connected_To_Internet")+"\n\n"+e.toString());
					else
						Err.error(e);
				}
				finally	{
					progressDlg.endDialog();	// running finish
				}
			}
		};
		
		Runnable finish = new Runnable()	{
			public void run()	{
				setEnabled(ACTION_SEND, true);

				if (progressDlg.canceled() == false)	{
					setEnabled(ACTION_SAVE, false);	// to not ask for save changes when closing, mail is now in sent folder

					if (frameCloser != null)
						frameCloser.windowClosing(null);
				}
			}
		};

		progressDlg.start(todo, finish);
		progressDlg.getDialog();	// shows on screen
	}
	



	public void cb_Save(Object selection)	{
		if (save())
			setEnabled(ACTION_SAVE, false);
	}
	
	private boolean save()	{
		SendMail sendMail = buildSendMail(false);
		if (sendMail != null)	{
			Message msg = sendMail.getMessage();
			
			ObservableReceiveMail drafts = sendFolders.drafts;
			try	{
				if (oldMessage != null)
					drafts.delete(new Message [] { oldMessage });
	
				Message [] arg = new Message [] { msg };
				drafts.append(arg);
				oldMessage = arg[0];	// take newly created message

				return true;
			}
			catch (Exception e)	{
				Err.error(e);
			}
		}
		return false;
	}
	
	
	public void cb_Attach(Object selection)	{
		// open file chooser and add to attachment panel
		File [] files = null;
		try	{
			files = DefaultFileChooser.openDialog(editor.getSensorComponent(), SendController.class);
		}
		catch (CancelException e)	{
			return;
		}
		
		if (files != null && files.length > 0)
			attachFiles(files);
	}
	
	public void attachFiles(File [] files)	{
		try	{
			for (int i = 0; i < files.length; i++)	{
				MimeBodyPart messageBodyPart = new MimeBodyPart();
				String filename = files[i].getPath();
				DataSource source = new FileDataSource(filename);
				messageBodyPart.setFileName(files[i].getName());
				messageBodyPart.setDataHandler(new DataHandler(source));
				attachments.addAttachment(messageBodyPart);
			}
			attachments.revalidate();
			attachments.repaint();
		}
		catch (MessagingException e)	{
			Err.error(e);
		}
	}
	
	
	public void cb_Configure(Object selection)	{
		new ConfigureDialog(defaultKeySensor, props);
	}

	public void cb_Signature(Object selection)	{
		new SignatureDialog(defaultKeySensor);
	}

	public void cb_Cryptography(Object selection)	{
		CryptDialog dlg = new CryptDialog(defaultKeySensor, editor.getText());
		
		if (dlg.getResult() != null)	{
			editor.setText(dlg.getResult());
			
			if (dlg.wasEncrypt())	{	// encrypt
				editor.getSensorComponent().setEditable(false);
				setEnabled(ACTION_PASTE, false);
				setEnabled(ACTION_UNDO, false);
				setEnabled(ACTION_REDO, false);
			}
			else	{	// decrypt
				editor.getSensorComponent().setEditable(true);
				setEnabled(ACTION_PASTE, true);
				setEnabled(ACTION_UNDO, true);
				setEnabled(ACTION_REDO, true);
			}
		}
	}


	
	private int confirmSave()	{
		if (getEnabled(ACTION_SAVE) == false)
			return JOptionPane.NO_OPTION;	// no changes

		if (editor.getSensorComponent().getDocument().getLength() <= 0 &&
				attachments.getAttachmentCount() <= 0 &&
				subject.getText().length() <= 0)
			return JOptionPane.NO_OPTION;
		
		return JOptionPane.showConfirmDialog(
				ComponentUtil.getWindowForComponent(defaultKeySensor),
				Language.get("Save_Changes"),
				Language.get("Warning"),
				JOptionPane.YES_NO_CANCEL_OPTION);
	}


	public boolean close()	{
		// check for unsaved changes
		int ret = confirmSave();
		if (ret == JOptionPane.YES_OPTION)	{
			if (save() == false)
				return false;
		}
		else
		if (ret != JOptionPane.NO_OPTION)	{
			return false;	// canceled
		}
		return true;
	}

}