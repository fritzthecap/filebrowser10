package fri.gui.swing.mailbrowser;

import java.io.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import fri.util.error.Err;
import fri.util.mail.*;
import fri.gui.CursorUtil;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.DefaultRemoveCommand;
import fri.gui.swing.table.PersistentColumnsTable;
import fri.gui.swing.util.RefreshTable;
import fri.gui.swing.progressdialog.CancelProgressDialog;
import fri.gui.swing.textviewer.TextViewer;
import fri.gui.swing.filechooser.*;
import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.mailbrowser.viewers.MessageViewer;
import fri.gui.swing.mailbrowser.send.*;
import fri.gui.swing.mailbrowser.addressbook.AddressController;
import fri.gui.swing.mailbrowser.rules.editor.RulesFrame;

/**
	The controller for the message table.
*/

public class MessageController extends ActionConnector implements
	ListSelectionListener,
	TableModelListener,
	SendWindowOpener
{
	public static final String ACTION_NEW = "New";
	public static final String ACTION_REPLY = "Reply";
	public static final String ACTION_FORWARD = "Forward";
	public static final String ACTION_SET_UNREAD = "Set Unread";
	public static final String ACTION_DELETE = "Delete";
	public static final String ACTION_CUT = "Cut";
	public static final String ACTION_COPY = "Copy";
	public static final String ACTION_SAVE = "Save As";
	public static final String ACTION_VIEW = "View Mail Code";
	public static final String ACTION_NEW_WINDOW = "New Window";
	public static final String ACTION_TO_ADDRESSBOOK = "Add To Addressbook";
	public static final String ACTION_RULES_EDITOR = "Rules Editor";
	public static final String ACTION_ABOUT = "About";

	private static RulesFrame rulesEditor;
	
	private MessageTable messageTable;
	private MessageViewer messageViewer;
	private MailClipboard clipboard;
	private AddressController addressController;


	public MessageController(MessageTable messageTable, JPanel viewerPanel)	{
		super(messageTable.getSensorComponent(), messageTable.getSelection(), null);
		
		this.messageTable = messageTable;
		clipboard = MailClipboard.getMailClipboard();
		viewerPanel.add(messageViewer = new MessageViewer());
		
		
		// do not change tooltips as they are written in i18n strings.properties!
		registerAction(ACTION_NEW, Icons.get(Icons.newDocument), "Create New Mail", KeyEvent.VK_N, InputEvent.CTRL_MASK);
		registerAction(ACTION_REPLY, Icons.get(Icons.mailReply), "Reply To Selected Mails", KeyEvent.VK_R, InputEvent.CTRL_MASK);
		registerAction(ACTION_FORWARD, Icons.get(Icons.mailForward), "Forward Selected Mails", KeyEvent.VK_F, InputEvent.CTRL_MASK);
		registerAction(ACTION_SET_UNREAD, Icons.get(Icons.bulb), "Set Selected Mails Unread");
		registerAction(ACTION_DELETE, Icons.get(Icons.remove), "Delete Selected Mails", KeyEvent.VK_DELETE, 0);
		registerAction(ACTION_CUT, Icons.get(Icons.cut), "Cut Selected Mails", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		registerAction(ACTION_COPY, Icons.get(Icons.copy), "Copy Selected Mails", KeyEvent.VK_C, InputEvent.CTRL_MASK);
		registerAction(ACTION_SAVE, Icons.get(Icons.save), "Save Selected Mails To Disk", KeyEvent.VK_S, InputEvent.CTRL_MASK);
		registerAction(ACTION_VIEW, Icons.get(Icons.eye), "View Code Of Selected Mails");
		registerAction(ACTION_NEW_WINDOW, Icons.get(Icons.newWindow), "New Mail Window");
		registerAction(ACTION_TO_ADDRESSBOOK);
		registerAction(ACTION_RULES_EDITOR, Icons.get(Icons.rules), "Message Rules Editor");
		registerAction(ACTION_ABOUT, GuiApplication.getApplicationIconURL(), "About Fri-Mail ...");
		
		messageTable.getSensorComponent().getSelectionModel().addListSelectionListener(this);
		messageTable.getSensorComponent().addKeyListener(new KeyAdapter()	{
			public void keyPressed(KeyEvent e)	{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
					MessageController.this.clipboard.clear();
					RefreshTable.refresh(MessageController.this.messageTable.getSensorComponent());
					setEnabledActions();
				}
			}
		});
		messageTable.getSensorComponent().addMouseListener(new MouseAdapter()	{
			public void mouseClicked(MouseEvent e)	{
				if (e.getClickCount() >= 2)	{
					cb_Open(getSelection().getSelectedObject());
				}
			}
		});
		messageTable.getSensorComponent().getModel().addTableModelListener(this);
		
		setEnabledActions();
	}


	/** Do internationalization for action labels. */
	protected String language(String label)	{
		return Language.get(label);
	}

	/** Set the address controller for collecting addresses. */
	public void setAddressController(AddressController addressController)	{
		this.addressController = addressController;
	}

	public FolderTreeNode getCurrentFolderNode()	{
		return messageTable.getModel().getCurrentFolder();
	}

	
	private void setEnabledActions()	{
		List sel = (List)getSelection().getSelectedObject();
		boolean selectionExists = sel != null && sel.size() > 0;
		
		setEnabled(ACTION_REPLY, selectionExists);
		setEnabled(ACTION_FORWARD, selectionExists);
		setEnabled(ACTION_SET_UNREAD, selectionExists);
		setEnabled(ACTION_SAVE, selectionExists && sel.size() == 1);
		setEnabled(ACTION_VIEW, selectionExists);
		setEnabled(ACTION_DELETE, selectionExists);
		setEnabled(ACTION_CUT, selectionExists);
		setEnabled(ACTION_COPY, selectionExists);
		setEnabled(ACTION_TO_ADDRESSBOOK, selectionExists);
	}
	

	/* Implements TableModelListener to set enabled actions when table changes. */
	public void tableChanged(TableModelEvent e)	{
		setEnabledActions();
	}
	
	
	/* Implements ListSelectionListener to set the selected message to message viewer and mark it as SEEN. */
	public void valueChanged(ListSelectionEvent e)	{
		if (e.getValueIsAdjusting())
			return;
		
		setEnabledActions();
		messageViewer.removeAll();
		messageViewer.repaint();
								
		List l = (List)getSelection().getSelectedObject();
		
		if (l != null && l.size() == 1)	{
			MessageTableRow row = (MessageTableRow)l.get(0);
			Message msg = row.getMessage();
			System.err.println("selected message class is: "+msg.getClass());

			CursorUtil.setWaitCursor(messageTable);
			try	{
				loadMessageInViewer(msg, messageViewer);
				messageViewer.organizeAttachmentPanels();
				messageViewer.revalidate();
				messageViewer.repaint();
			}
			catch (Exception ex)	{
				Err.error(ex);
			}
			finally	{
				CursorUtil.resetWaitCursor(messageTable);
			}
		}
	}


	private void loadMessageInViewer(Message msg, final MessageViewer messageViewer)
		throws Exception
	{
		ReceiveMail.MailVisitor visitor = new ReceiveMail.MailVisitor()	{	// opens and closes folder
			public void message(int msgCount, int msgNr, Message m)	{
				try	{
					System.err.println("selecting message "+m.getSubject());
					messageViewer.setMessage(m);
					MessageUtil.setMessageNew(m, false);	// mark this message as seen
					RefreshTable.refresh(messageTable.getSensorComponent());	// change rendering from bold to normal
				}
				catch (Exception ex)	{
					Err.error(ex);
				}
			}
			public void folder(int fldCount, int fldNr, Folder f)	{}
		};
		
		ReceiveMail rm = messageTable.getModel().getReceiveMail();
		rm.message(msg, visitor);
	}
	
	

	// callbacks

	/** Doubleclick on message. */
	public void cb_Open(Object selection)	{
		List l = (List)selection;
		MessageTableRow row = (MessageTableRow)l.get(0);
		Message msg = row.getMessage();
		
		try	{
			boolean openSendWindow =
					getCurrentFolderNode() == messageTable.getModel().getDraftNode() ||
					getCurrentFolderNode() == messageTable.getModel().getOutboxNode();
			
			if (openSendWindow)	{	// we are in draft or outbox folder, open send window
				new SendFrame(ConnectionSingletons.getSendInstance(), createSendFolderSet(), msg, null);
			}
			else	{	// not in drafts folder, open viewer window
				ViewerFrame f = new ViewerFrame(row.get(MessageTableModel.SUBJECT_COLUMN).toString());
				MessageViewer msgViewer = new MessageViewer();
				f.getContentPane().add(msgViewer);
				loadMessageInViewer(msg, msgViewer);
				f.start();
			}
		}
		catch (Exception ex)	{
			Err.error(ex);
		}
	}
	
	
	private SendFolderSet createSendFolderSet()	{
		ObservableReceiveMail curr = getReceiveMailClone(getCurrentFolderNode());
		ObservableReceiveMail drafts = getReceiveMailClone(messageTable.getModel().getDraftNode());
		ObservableReceiveMail outbox = getReceiveMailClone(messageTable.getModel().getOutboxNode());
		ObservableReceiveMail sent = getReceiveMailClone(messageTable.getModel().getSentNode());
		if (curr != null && drafts != null && outbox != null && sent != null)
			return new SendFolderSet(curr, drafts, outbox, sent);
		return null;
	}

	private ObservableReceiveMail getReceiveMailClone(FolderTreeNode n)	{
		ObservableReceiveMail rm = n != null ? n.getReceiveMail() : null;
		return rm != null ? (ObservableReceiveMail)rm.clone() : null;
	}
	
	
	/** Implements SendWindowOpener: Open a send message window. Public for usage in AddressController. */
	public SendFrame openSendWindow()	{
		return openSendWindow(null, null);
	}
	
	private SendFrame openSendWindow(Message msg, Boolean reply)	{
		SendFrame app = new SendFrame(ConnectionSingletons.getSendInstance(), createSendFolderSet(), msg, reply);
		return app;
	}


	
	/** Edit and send a new message. */
	public void cb_New(Object selection)	{
		openSendWindow();
	}
	
	public void cb_Reply(Object selection)	{
		List sel = (List)selection;
		for (int i = 0; sel != null && i < sel.size(); i++)	{
			MessageTableRow row = (MessageTableRow)sel.get(i);
			openSendWindow(row.getMessage(), Boolean.TRUE);
		}
	}
	
	public void cb_Forward(Object selection)	{
		List sel = (List)selection;
		for (int i = 0; sel != null && i < sel.size(); i++)	{
			MessageTableRow row = (MessageTableRow)sel.get(i);
			openSendWindow(row.getMessage(), Boolean.FALSE);
		}
	}
	
	public void cb_Set_Unread(Object selection)	{
		// set all selected messages to unread state
		List sel = (List)selection;
		for (int i = 0; sel != null && i < sel.size(); i++)	{
			MessageTableRow row = (MessageTableRow)sel.get(i);
			Message msg = row.getMessage();
			
			try	{
				MessageUtil.setMessageNew(msg, ! MessageUtil.isNewMessage(msg));
				RefreshTable.refresh(messageTable.getSensorComponent());	// change rendering from bold to normal
			}
			catch (MessagingException e)	{
				Err.error(e);
			}
		}
	}

	

	private ModelItem [] toModelItems(List sel)	{
		final ModelItem [] items = new ModelItem[sel.size()];
		for (int i = 0; i < sel.size(); i++)	{
			MessageTableRow row = (MessageTableRow)sel.get(i);
			items[i] = messageTable.getModel().createModelItem(row);
		}
		return items;
	}
	
	
		
	public void cb_Delete(Object selection)	{
		// move selected messages to trash folder
		List sel = (List)selection;
		
		// confirm delete
		if (messageTable.getModel().mustBeReallyDeleted())	{
			int ret = JOptionPane.showConfirmDialog(	// confirm real delete when in trash folder
					messageTable,
					Language.get("Do_You_Really_Want_To_Delete")+" "+sel.size()+" "+Language.get("Messages")+" "+Language.get("Without_Undo_Option")+"?",
					Language.get("Confirm_Delete"),
					JOptionPane.YES_NO_OPTION);

			if (ret != JOptionPane.YES_OPTION)	// cancel or window close
				return;
		}

		messageViewer.removeAll();	// clear mail viewer
		messageViewer.repaint();
		
		final ModelItem [] items = toModelItems(sel);

		// now remove messages with background observer dialog
		final CancelProgressDialog observer = new CancelProgressDialog(GuiApplication.globalFrame, Language.get("Deleting____"));
		Runnable todo = new Runnable()	{
			public void run()	{
				try	{
					// remove selection listener to avoid jumping selection to next mail
					messageTable.getSensorComponent().getSelectionModel().removeListSelectionListener(MessageController.this);
					
					for (int i = 0; i < items.length; i++)	{
						if (observer.canceled())
							return;
						
						observer.setNote(""+(i + 1));
						System.err.println("Deleting message "+items[i]);
						DefaultRemoveCommand cmd = new DefaultRemoveCommand(items[i], messageTable.getModel());
						if (cmd.doit() == null)
							Err.warning(Language.get("Could_Not_Remove")+"!");
					}
				}
				catch (RuntimeException e)	{
					System.err.println("Delete Runtime Exception was: "+e.toString());
					if (e.getMessage().equals("User Cancel") == false)
						Err.error(e);
				}
				finally	{
					messageTable.getSensorComponent().getSelectionModel().addListSelectionListener(MessageController.this);
					observer.endDialog();
				}
			}
		};
		Runnable finish = new Runnable()	{
			public void run()	{
				messageTable.getSelection().clearSelection();
				messageTable.finishedMessageLoading(false);
				setEnabledActions();
			}
		};
		observer.start(todo, finish);
	}
	


	public void cb_Cut(Object selection)	{
		clipboard.cut(toModelItems((List)selection));
		setEnabledActions();	// enable paste
		RefreshTable.refresh(messageTable.getSensorComponent());	// show disabled items
	}
	
	public void cb_Copy(Object selection)	{
		clipboard.copy(toModelItems((List)selection));
		setEnabledActions();	// enable paste
		RefreshTable.refresh(messageTable.getSensorComponent());	// show disabled items
	}
	
	
	
	public void cb_Save_As(Object selection)	{
		// save selected messages to filesystem, as raw mail code
		List sel = (List)selection;
		MessageTableRow row = (MessageTableRow)sel.get(0);
		final Message msg = row.getMessage();

		FileGUISaveLogicImpl saveImpl = new FileGUISaveLogicImpl(messageTable)	{
			public void write(Object toWrite)
				throws Exception
			{
				OutputStream os = null;
				try	{
					os = new BufferedOutputStream(new FileOutputStream((File)toWrite));
					msg.writeTo(os);
				}
				catch (Exception e)	{
					Err.error(e);
				}
				finally	{
					try	{ os.close(); } catch (Exception e)	{}
				}
				os.close();
			}
		};
		
		SaveLogic.saveAs(saveImpl, null);
	}
	

	public void cb_View_Mail_Code(Object selection)	{
		// view selected messages as raw mail code
		List sel = (List)selection;
		for (int i = 0; i < sel.size(); i++)	{
			MessageTableRow row = (MessageTableRow)sel.get(i);
			Message msg = row.getMessage();
			
			ByteArrayOutputStream out = null;
			try	{
				out = new ByteArrayOutputStream();
				msg.writeTo(out);
				new TextViewer(msg.getSubject(), out.toString());
			}
			catch (Exception e)	{
				Err.error(e);
			}
			finally	{
				try	{ out.close(); } catch (Exception e)	{}
			}
		}
	}


	public void cb_New_Window(Object selection)	{
		PersistentColumnsTable.remember(messageTable.getSensorComponent(), MessageTableModel.class);
		new MailFrame();
	}


	public void cb_Add_To_Addressbook(Object selection)	{
		List sel = (List)selection;
		for (int i = 0; i < sel.size(); i++)	{
			MessageTableRow row = (MessageTableRow)sel.get(i);
			Message msg = row.getMessage();

			try	{
				Vector addresses = new Vector();
				Address [] fromAddr = msg.getFrom();
				for (int j = 0; fromAddr != null && j < fromAddr.length; j++)
					if (fromAddr[j] instanceof InternetAddress)
						addresses.add(addressController.toRow((InternetAddress)fromAddr[j]));
						
				addressController.mergePackedAddresses(addresses);
			}
			catch (MessagingException e)	{
				Err.error(e);
			}
		}
	}


	public void cb_Rules_Editor(Object selection)	{
		if (rulesEditor == null)	{
			rulesEditor = new RulesFrame();
		}
		else	{
			rulesEditor.setVisible(true);
		}
	}

	public void cb_About(Object selection)	{
		JOptionPane.showMessageDialog(
				GuiApplication.globalFrame,
				"Mail Client "+MailFrame.version+"\n"+
					"(c) Fritz Ritzberger 2003.\n"+
					"fri@soon.com\n",
				Language.get("About"),
				JOptionPane.INFORMATION_MESSAGE,
				GuiApplication.getLogoIcon());
	}

}