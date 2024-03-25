package fri.gui.swing.mailbrowser;

import java.util.*;
import java.net.URL;
import java.awt.event.*;
import java.awt.EventQueue;
import java.awt.Toolkit;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.event.*;
import fri.util.error.Err;
import fri.util.mail.*;
import fri.util.ruleengine.*;
import fri.gui.CursorUtil;
import fri.gui.mvc.model.*;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.model.swing.*;
import fri.gui.mvc.view.swing.SelectionDnd;
import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.tree.EditNotifyingTreeModel;
import fri.gui.swing.util.RefreshTree;
import fri.gui.swing.util.RefreshTable;
import fri.gui.swing.progressdialog.CancelProgressDialog;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.mailbrowser.addressbook.AddressController;
import fri.gui.swing.mailbrowser.rules.MessageRuleWrapper;

/**
	The controller for folder tree.
	@author  Ritzberger Fritz
*/

public class FolderController extends ActionConnector implements
	TreeSelectionListener,
	EditNotifyingTreeModel.Listener,
	FocusListener,
	MessageCountListener,
	MessageChangedListener
{
	public static final String ACTION_CONFIGURE = "Configure";
	public static final String ACTION_RECEIVE = "Receive";
	public static final String ACTION_NEW_CONNECTION = "New Mail Connection";
	public static final String ACTION_NEW_LOCAL_STORE = "New Local Store";
	public static final String ACTION_NEW_FOLDER = "New Folder";
	public static final String ACTION_DELETE = "Delete";
	public static final String ACTION_CUT = "Cut";
	public static final String ACTION_COPY = "Copy";
	public static final String ACTION_PASTE = "Paste";
	public static final String ACTION_TO_ADDRESSBOOK = "All To Addressbook";
	private static ReceiveThread receiveThread;
	private FolderTree folderTree;
	private MessageTable messageTable;
	private FolderTreeNode lastSelectedFolder;
	private CancelProgressDialog progressDialog;
	private AuthenticatorDialog authenticatorDialog;
	private MailClipboard clipboard;
	private JMenu connItem;
	private AddressController addressController;
	private boolean receiving;
	private JPanel glassPane;
	private Object lock = new Object();

	// internally used interface for dispatching messages in different ways (receive from server or from local store)
	private interface MessageDispatcher
	{
		public void dispatch(Message msg);
	}

	
	
	public FolderController(FolderTree folderTree, MessageTable messageTable)	{
		super(folderTree.getSensorComponent(), folderTree.getSelection(), null);
		
		this.folderTree = folderTree;
		this.messageTable = messageTable;
		
		clipboard = MailClipboard.getMailClipboard();

		registerAction(ACTION_CONFIGURE, Icons.get(Icons.configure), "Configure Mail Connection", KeyEvent.VK_O, InputEvent.CTRL_MASK);
		registerAction(ACTION_RECEIVE, Icons.get(Icons.refresh), "Receive New Mail", KeyEvent.VK_F5, 0);
		registerAction(ACTION_NEW_FOLDER, Icons.get(Icons.newFolder), "Create New Folder", KeyEvent.VK_INSERT, 0);
		registerAction(ACTION_NEW_CONNECTION);
		registerAction(ACTION_NEW_LOCAL_STORE);
		registerAction(ACTION_DELETE, Icons.get(Icons.remove), "Delete Selected Folders", KeyEvent.VK_DELETE, 0);
		registerAction(ACTION_CUT, Icons.get(Icons.cut), "Cut Selected Folders", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		registerAction(ACTION_COPY, Icons.get(Icons.copy), "Copy Selected Folders", KeyEvent.VK_C, InputEvent.CTRL_MASK);
		registerAction(ACTION_PASTE, Icons.get(Icons.paste), "Paste Folders Or Mails", KeyEvent.VK_V, InputEvent.CTRL_MASK);
		registerAction(ACTION_TO_ADDRESSBOOK);
		
		folderTree.getSensorComponent().addTreeSelectionListener(this);
		folderTree.getSensorComponent().addFocusListener(this);
		getFolderTreeModel().setRenameListener(this);
		folderTree.getSensorComponent().addKeyListener(new KeyAdapter()	{
			public void keyPressed(KeyEvent e)	{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
					FolderController.this.clipboard.clear();
					RefreshTree.refresh(FolderController.this.folderTree.getSensorComponent());
					setEnabledActions();
				}
			}
		});
		
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

	/** Set the menu item for "new connection", to disable it when that action is disabled. */
	public void setConnectionMenuItem(JMenu connItem)	{
		this.connItem = connItem;
	}



	/** Select local store -> show its messages. */
	public void setFirstSelection()	{
		EventQueue.invokeLater(new Runnable()	{	// do this in event thread, else conflicts with cd() on treenode
			public void run()	{
				if (ConnectionSingletons.getReceiveInstance().isValid() == false)	{	// init dialog for configuring send and receive server
					configureReceiveSend();
				}
				// set selection to inbox
				FolderTreeNode inbox = getFolderTreeModel().getLocalInbox();
				folderTree.getSensorComponent().setSelectionPath(new TreePath(inbox.getPath()));
				
				int interval = ConnectionSingletons.getReceiveInstance().getCheckForNewMailsInterval();
				if (ConnectionSingletons.getReceiveInstance().isValid() && interval > 0)	{
					receiveThread = new ReceiveThread(FolderController.this, interval);
				}
			}
		});
	}


	public boolean isReceiving()	{
		synchronized(lock)	{
			return receiving;
		}
	}

	public void setReceiving(boolean receiving)	{
		synchronized(lock)	{
			this.receiving = receiving;
		}
	}


	public SelectionDnd getSelectionDnd()	{
		return (SelectionDnd)getSelection();
	}
	
	public FolderTreeModel getFolderTreeModel()	{
		return folderTree.getModel();
	}
	
	public MailClipboard getClipboard()	{
		return clipboard;
	}
	

	/** Implements FocusListener to set enabled paste action when messages were copied or cutten. */
	public void focusGained(FocusEvent e)	{
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				setEnabledActions();
			}
		});
	}
	public void focusLost(FocusEvent e)	{
	}


	private void handleFolderListeners(FolderTreeNode tn, ObservableReceiveMail rm, boolean add)	{
		if (tn.isFolder())	{
			try	{
				Folder f = rm.pwd();
				if (add)
					f.addMessageChangedListener(this);
				else
					f.removeMessageChangedListener(this);
	
				if (add)
					f.addMessageCountListener(this);
				else
					f.removeMessageCountListener(this);
			}
			catch (Exception e)	{
				Err.error(e);
			}
		}
	}

	/** Implements MessageChangedListener to refresh messageTable (SEEN, RECENT flag changes). */
	public void messageChanged(MessageChangedEvent e)	{
		RefreshTable.refresh(messageTable.getSensorComponent());
	}


	/** Implements MessageCountListener to check for new/removed messages in messageTable, in sync with actions. */
	public void messagesAdded(MessageCountEvent e)	{
		System.err.println("MessageCountListener.messagesAdded, type is "+(e.getType() == MessageCountEvent.ADDED ? "ADDED" : "REMOVED"));
		Message [] msgs = e.getMessages();
		for (int i = 0; i < msgs.length; i++)	{
			insertMessageWhenVisible(msgs[i], 0);
		}
	}

	public void messagesRemoved(MessageCountEvent e)	{
		System.err.println("MessageCountListener.messagesRemoved, type is "+(e.getType() == MessageCountEvent.ADDED ? "ADDED" : "REMOVED"));
		Message [] msgs = e.getMessages();
		for (int i = 0; i < msgs.length; i++)	{
			deleteMessageWhenVisible(msgs[i]);
		}
	}
 
 
	
	private void setEnabledActions()	{
		List sel = (List)getSelection().getSelectedObject();
		boolean selectionExists = sel != null && sel.size() > 0;

		boolean rootSelected = false;
		boolean storeSelected = false;
		boolean canRename = selectionExists;
		boolean canCreateFolder = selectionExists;
		for (int i = 0; selectionExists && i < sel.size(); i++)	{
			FolderTreeNode tn = (FolderTreeNode)sel.get(i);
			if (tn.isRoot())
				rootSelected = true;
			else
			if (tn.isStore())
				storeSelected = true;
			else

			if (canRename(tn) == false)
				canRename = false;

			if (canCreateFolder(tn) == false)
				canCreateFolder = false;
		}

		boolean canCreateConnection = rootSelected && sel.size() == 1;
		boolean canDelete = selectionExists && rootSelected == false && canRename == true;
		System.err.println("setEnabledAction, canCreateFolder "+canCreateFolder+", canRename "+canRename+", selected folders "+sel);
		
		// for paste look at clipboard
		boolean isMessages = clipboard.isEmpty() == false && clipboard.getSourceModelItems()[0] instanceof MessageTableModelItem;
		boolean canPaste = rootSelected == false && clipboard.isEmpty() == false && (isMessages || canCreateFolder);

		if (connItem != null)
			connItem.setEnabled(canCreateConnection);
		setEnabled(ACTION_NEW_FOLDER, canCreateFolder);
		setEnabled(ACTION_DELETE, canDelete);
		setEnabled(ACTION_CUT, canDelete && storeSelected == false);
		setEnabled(ACTION_COPY, selectionExists && rootSelected == false && storeSelected == false);
		setEnabled(ACTION_PASTE, canPaste);
		setEnabled(ACTION_TO_ADDRESSBOOK, selectionExists);
	}

	private boolean canRename(FolderTreeNode tn)	{
		if (tn.isRoot() ||
				tn.getParent() != null && ((FolderTreeNode)tn.getParent()).isStore() &&
				tn.getReceiveMail() != null && tn.getReceiveMail().canRename() == false)
			return false;
		return true;
	}

	private boolean isTrash(FolderTreeNode tn)	{
		return isUnderStoreAndHasName(tn, LocalStore.TRASH);
	}

	private boolean isSent(FolderTreeNode tn)	{
		return isUnderStoreAndHasName(tn, LocalStore.SENT);
	}

	private boolean isOutbox(FolderTreeNode tn)	{
		return isUnderStoreAndHasName(tn, LocalStore.OUTBOX);
	}

	private boolean isUnderStoreAndHasName(FolderTreeNode tn, String name)	{
		return
				tn.toString().equals(name) &&
				tn.getParent() != null &&
				((FolderTreeNode)tn.getParent()).isStore();
	}

	/** Needed in drag&Drop handler. Returns true if the passed folder can be copied and deleted. */
	public boolean canMove(FolderTreeNode sourceFolder)	{
		return canRename(sourceFolder);
	}

	/** Needed in drag&Drop handler. Returns true if the passed folder can host other folders. */
	public boolean canCreateFolder(FolderTreeNode folderNode)	{
		if (folderNode.isRoot() ||
				canRename(folderNode) == false && isTrash(folderNode) == false)
			return false;
		return true;
	}
	
	/** Needed in drag&Drop handler. Returns true if the passed folder can receive dropped messages. */
	public boolean canCreateMessages(FolderTreeNode folderNode)	{
		if (folderNode.isRoot() || isOutbox(folderNode) || isSent(folderNode))
			return false;
		return true;
	}
	
	
	/**
		Implements EditNotifyingTreeModel.Listener to rename folders or change a mail store URL.
		The passed path contains the original unchanged node.
	*/
	public Object valueForPathChanging(final TreePath path, Object newValue)	{
		// save the old user object
		FolderTreeNode tn = (FolderTreeNode)path.getLastPathComponent();
		Object old = tn.getUserObject();
		
		CursorUtil.setWaitCursor(folderTree);
		try	{
			if (tn.isStore())	{	// prepare a new store URL
				ObservableReceiveMail rmOld = tn.getReceiveMail();	// need old store for close
				if (rmOld != null)
					rmOld.close();	// close when no exception was thrown

				ObservableReceiveMail rmNew = folderTree.createStore((String)newValue);

				folderTree.getSensorComponent().collapsePath(path);	// ensure treenode is not expanded, as it gets new children
				getFolderTreeModel().reload(tn);
				return rmNew;	// new store will be user object
			}
			else
			if (tn.isFolder())	{
				ReceiveMail rm = tn.getReceiveMail();	// do the rename by mail-logic class
				rm.rename((String)newValue);
				return rm.pwd();	// new folder will be the user object
			}
		}
		catch (Exception e)	{
			Err.error(e);
		}
		finally	{
			CursorUtil.resetWaitCursor(folderTree);
		}
		
		return old;
	}



	/** Implements TreeSelectionListener to set messages into message table, using a background thread. */
	public void valueChanged(TreeSelectionEvent evt)	{
		boolean progressRunning = progressDialog != null;
		
		interruptDialog();	// as message table gets cleared

		// cleanup last selected folder
		synchronized(lock)	{
			if (lastSelectedFolder != null)	{
				ObservableReceiveMail rm = lastSelectedFolder.getReceiveMail();
				
				if (rm != null && rm.isLocalStore() && progressRunning == false)	{	// when local store and not loading
					try	{
						rm.leaveCurrentFolder();
					}
					catch (Exception e)	{
						e.printStackTrace();
					}
				}

				handleFolderListeners(lastSelectedFolder, lastSelectedFolder.getReceiveMail(), false);
				lastSelectedFolder = null;
			}
		}

		setEnabledActions();

		// list new selected folder
		List selection = (List)getSelection().getSelectedObject();
		if (selection == null || selection.size() > 1)
			return;
		
		FolderTreeNode tn = (FolderTreeNode)selection.get(0);
		System.err.println("selected node class is "+tn.getUserObject().getClass());
		ObservableReceiveMail rm = tn.getReceiveMail();

		messageTable.clear();
		
		if (rm != null && rm.isConnected())	{	// not at root, not unconnected remote folder
			CursorUtil.setWaitCursor(folderTree);

			lastSelectedFolder = tn;
			handleFolderListeners(tn, rm, true);
			
			// pass all necessary objects to messageTable
			messageTable.getModel().setCurrentFolderAndModel(tn, getFolderTreeModel());

			try	{
				int msgCount = rm.getMessageCount();	// count messages of folder
				if (msgCount <= 0)	{
					messageTable.finishedMessageLoading(false);
					return;
				}
				
				MessageDispatcher dispatcher = new MessageDispatcher()	{
					public void dispatch(Message m)	{
						//System.err.println("loading a message ...");
						insertMessageWhenVisible(m, messageTable.getModel().getRowCount());
						//System.err.println("... done loading a message.");
					}
				};
				
				ObservableReceiveMail clone = (ObservableReceiveMail)rm.clone();	// keeps current cd path
				receiveMessages(clone, msgCount, dispatcher, false);
			}
			catch (Exception e)	{
				Err.error(e);
			}
			finally	{
				CursorUtil.resetWaitCursor(folderTree);
			}
		}
	}


	private void insertMessageWhenVisible(Message m, int position)	{
		synchronized(lock)	{
			if (lastSelectedFolder != null && isMessageInFolder(lastSelectedFolder.getUserObject(), m))	{
				MessageTableModel model = messageTable.getModel();
				ModelItem item = new MessageTableModelItem(new MessageTableRow(m, model.rendersToAdress()));
				model.doInsert(item, new CommandArguments.Position(new Integer(position)));
			}
		}
	}

	private void deleteMessageWhenVisible(Message m)	{
		synchronized(lock)	{
			if (lastSelectedFolder != null && isMessageInFolder(lastSelectedFolder.getUserObject(), m))	{
				MessageTableModel model = messageTable.getModel();
	
				for (int i = model.getRowCount() - 1; i >= 0; i--)	{
					MessageTableRow row = model.getMessageTableRow(i);
					Message msg = row.getMessage();
	
					try	{
						if (msg.equals(m) && MessageUtil.isNewMessage(msg) == false)	{	// was moved from "cur" to "new"
							//System.err.println("deleteMessageWhenVisible deleting row "+i+", msg "+msg+", m "+m);
							ModelItem item = new MessageTableModelItem(row);
							model.doDelete(item);
						}
					}
					catch (MessagingException e)	{
						e.printStackTrace();
					}
				}
			}
		}
		//System.err.println("... after deleteMessageWhenVisible ");
	}
	
	
	private boolean isMessageInFolder(Object folder, Message msg)	{
		//System.err.println("isMessageInFolder, msg.folder="+msg.getFolder()+" folder="+folder+", msg.folder "+msg.getFolder().getClass()+", folder "+folder.getClass()+", msg.folder.hashCode "+msg.getFolder().hashCode()+", folder.hashCode "+folder.hashCode());
		if (folder instanceof Folder)	// could even be store or root
			return ((Folder)folder).getFullName().equals(msg.getFolder().getFullName());
			//return folder == msg.getFolder();	// does not work for remote folders!
		return false;
	}

	
	private void interruptDialog()	{
		if (progressDialog != null)	{
			progressDialog.setCanceled();
			progressDialog.endDialog();
			progressDialog = null;
			Thread.yield();
		}
	}
	

	private void receiveMessages(
		final ObservableReceiveMail rm,
		final int msgCount,
		final MessageDispatcher dispatcher,
		final boolean closeConnection)
	{
		interruptDialog();	// cleanup
		
		// prepare a background observer dialog
		progressDialog = new CancelProgressDialog(messageTable, Language.get("Loading_Messages")+" ...", msgCount);
		progressDialog.setIsByteSize(false);	// progress is message count
		progressDialog.setCloseWhenMaximumReached(false);	// dialog will stay until endDialog() is called
		rm.setObserver(progressDialog, Language.get("Message"), Language.get("Of"));

		// visitor to loop all mails of folder
		final ReceiveMail.MailVisitor visitor = new ReceiveMail.MailVisitor()	{
			public void message(int msgCnt, int msgNr, final Message m)	{
				dispatcher.dispatch(m);
			}
			public void folder(int fldCount, int fldNr, Folder f)	{
			}
		};
		
		// create runnable for visiting messages
		Runnable fillMessages = new Runnable()	{
			public void run()	{
				CancelProgressDialog dlg = progressDialog;
				try	{
					System.err.println("starting message loop in FolderController.receiveMessages()");
					rm.messages(visitor);
					System.err.println("... ending message loop in FolderController.receiveMessages()");
				}
				catch (Throwable e)	{
					Err.error(e);
				}
				finally	{
					if (progressDialog == dlg)	{	// could have been interrupted
						progressDialog.endDialog();
						progressDialog = null;
					}
				}
			}
		};
		
		// create runnable for doing final actions
		Runnable showMessageCount = new Runnable()	{	// class FolderController$7
			public void run()	{
				setReceiving(false);
				ensureGlassPane().setVisible(false);

				if (closeConnection == false)
					messageTable.finishedMessageLoading(true);	// sort when loading a folder
				else
					messageTable.finishedNewMessageLoading(msgCount);
				
				if (closeConnection)	{
					try	{
						rm.close();
					}
					catch (Throwable e)	{
						Err.error(e);
					}
				}
			}
		};

		// start thread by dialog start, with both runnables
		setReceiving(true);
		ensureGlassPane().setVisible(true);
		progressDialog.start(fillMessages, showMessageCount);
		if (closeConnection)	// if receiving new mail, show dialog anyway
			progressDialog.getDialog();	// forces dialog to show
	}


	private JPanel ensureGlassPane()	{
		// because of hanging background thread (GUI deadlock) we need a glasspane to prevent inputs on folderTree and messageTable
		// create the glasspane
		if (glassPane == null)	{
			glassPane = new JPanel();
			glassPane.setOpaque(false);
			MouseInputAdapter adapter =  new MouseInputAdapter()	{};
			glassPane.addMouseListener(adapter);
			glassPane.addMouseMotionListener(adapter);
			JFrame f = (JFrame)ComponentUtil.getWindowForComponent(folderTree);
			f.setGlassPane(glassPane);
		}
		return glassPane;
	}



	private AuthenticatorDialog getAuthenticator(String password)	{
		if (authenticatorDialog == null)	{
			authenticatorDialog = new AuthenticatorDialog(ComponentUtil.getFrame(folderTree), password);
		}
		else	{
			authenticatorDialog.setPassword(password);
		}
		return authenticatorDialog;
	}


	// callbacks

	
	public void cb_Configure(Object selection)	{
		configureReceiveSend();

		int interval = ConnectionSingletons.getReceiveInstance().getCheckForNewMailsInterval();
		if (ConnectionSingletons.getReceiveInstance().isValid())	{
			if (receiveThread != null && interval <= 0)	{
				receiveThread.setStopped();
				receiveThread = null;
			}
			else
			if (receiveThread == null && interval > 0)	{
				receiveThread = new ReceiveThread(this, interval);
			}
		}
	}

	private ConfigureDialog configureReceiveSend()	{
		return new ConfigureDialog(GuiApplication.globalFrame, ConnectionSingletons.getReceiveInstance(), ConnectionSingletons.getSendInstance());
	}



	/** Receive new messages from default mail connection to INBOX. */
	public void cb_Receive(Object selection)	{
		// if a remote folder is selected, refresh it
		if (selection != null && ((List)selection).size() == 1)	{
			FolderTreeNode n = (FolderTreeNode)((List)selection).get(0);
			
			if (n.isFolder() && n.getReceiveMail().isLocalStore() == false)	{
				System.err.println("just refreshing selected folder "+n);
				valueChanged(null);
				return;	// do not receive from default connection! (Remote foldes are for experts)
			}
		}

		if (LocalStore.isWriteable() == false)	{
			Err.warning(Language.get("Local_Store_Not_Writeable")+" "+LocalStore.localStoreDirectory());
			return;
		}

		// ensure the default connection is valid
		if (ConnectionSingletons.getReceiveInstance().isValid() == false)	{
			Err.warning(Language.get("Invalid_Mail_Properties"));
			return;
		}
		
		if (isReceiving())	{
			System.err.println("**************** Can not recive: there is a running receive process! ****************");
			return;
		}

		setReceiving(true);
		
		ObservableReceiveMail rm;
		ReceiveProperties mailProps = ConnectionSingletons.getReceiveInstance();
		try	{
			rm = new ObservableReceiveMail(
					mailProps,
					getAuthenticator(mailProps.getPassword()));
			
			rm.cd("INBOX");	// connects to server, shows authentication dialog if no password present
		}
		catch (Throwable e)	{
			setReceiving(false);

			if (authenticatorDialog.wasCanceled() == false)	{
				if (e.toString().indexOf("Connect failed") >= 0)	{
					Err.warning(Language.get("Not_Connected_To_Internet")+"\n\n"+e.toString());
				}
				else
				if (e.toString().indexOf("invalid user name or password") >= 0 || e.toString().indexOf("a password MUST be entered") >= 0)	{
					String error;
					if (e.getMessage().indexOf("invalid user name or password") >= 0)
						error = Language.get("invalid user name or password");
					else
						error = Language.get("a password MUST be entered");

					Err.warning(error);
					mailProps.setPassword(null);	// reset the password
					cb_Receive(selection);	// repeat connect
				}
				else	{
					Err.error(e);
				}
			}
			return;
		}
		
		try	{
			int msgCount = rm.getMessageCount();	// count messages of folder
			
			if (msgCount > 0)	{
				final boolean leaveMailsOnServer = rm.getLeaveMailsOnServer();	// get configuration flag
				final DefaultRuleSession session = ensureRuleSession();	// allocate mail rules
				
				// allocate the message dispatcher that actually receives new messages
				MessageDispatcher dispatcher = new MessageDispatcher()	{
					public void dispatch(Message m)	{
						try	{
							MessageRuleWrapper cmd = executeMailRules(m, session);	// let rules decide what to do with message
							
							if (cmd != null && cmd.canReceive())	{	// passed all mail rules
								MessageUtil.setMessageNew(m, true);	// ensure the mail is considered to be new by this implementation
								
								Toolkit.getDefaultToolkit().beep();	// signalize new message

								FolderTreeNode folderNode = cmd.isMove()
										? getFolderTreeModel().getFolderByPath(cmd.receiveFolder())	// find given move folder
										: getFolderTreeModel().getLocalInbox();

								if (folderNode == null)	{
									folderNode = getFolderTreeModel().getLocalInbox();
									System.err.println("ERROR in mail rule, receiving to INBOX. Could not find folder >"+cmd.receiveFolder()+"<");
								}
									
								folderNode.getReceiveMail().append(new Message [] { m });	// put the mail to its destination

								if (cmd.isCopy())	{	// check for copy to some folder
									FolderTreeNode copyFolderNode = getFolderTreeModel().getFolderByPath(cmd.receiveFolder());
									if (copyFolderNode != null)
										copyFolderNode.getReceiveMail().append(new Message [] { m });
									else
										System.err.println("ERROR in mail rule, could not find copy target folder >"+cmd.receiveFolder()+"<");
								}
							}
							else	{
								System.err.println("Rejecting message because mail rules denied: "+m.getSubject());
							}
							
							if (leaveMailsOnServer == false && (cmd == null || cmd.canDeleteFromServer()))	{	// delete from server
								System.err.println("... deleting new message from server: "+m.getSubject());
								MessageUtil.setMessageDeleted(m);
							}
						}
						catch (Throwable e)	{
							Err.error(e);
						}
					}
				};
				
				receiveMessages(rm, msgCount, dispatcher, true);
			}
			else	{	// no messages received
				setReceiving(false);
				rm.close();
				messageTable.finishedNewMessageLoading(0);
			}
		}
		catch (Throwable e)	{
			Err.error(e);
			setReceiving(false);
		}
	}




	private DefaultRuleSession ensureRuleSession()	{
		URL resourceUrl = RulesUrl.getMailRulesUrl();
		if (resourceUrl == null || RulesUrl.exists() == false)
			return null;
			
		String url = resourceUrl.toString();
		PropertiesRuleExecutionSet ruleSet = PropertiesRuleExecutionSet.getRuleSet(url);
		if (ruleSet == null)
			return null;

		DefaultRuleServiceProvider provider = DefaultRuleServiceProviderManager.getRuleServiceProvider();
		provider.getRuleAdministrator().registerRuleExecutionSet(url, ruleSet, null);
		DefaultRuleRuntime runtime = provider.getRuleRuntime();

		return runtime.createRuleSession(
				url,	// bind uri
				ConnectionSingletons.getSendInstance(),	// additional properties
				DefaultRuleRuntime.STATELESS_SESSION_TYPE);	// type of rule session
	}
	
	private MessageRuleWrapper executeMailRules(Message m, DefaultRuleSession session)	{
		if (session != null)	{
			try	{
				List input = new Vector();
				input.add(new MessageRuleWrapper(m));
	
				List output = session.executeRules(input);
				if (output != null && output.size() > 0)
					return (MessageRuleWrapper)output.get(0);
					
				return null;
			}
			catch (Throwable e)	{
				Err.error(e);
			}
		}
		return new MessageRuleWrapper(m);	// not loosing message because rule engine crashed
	}
	


	public void cb_New_Mail_Connection(Object selection)	{
		ReceiveProperties mailProps = new ReceiveProperties();
		ConfigureDialog conf = new ConfigureDialog(folderTree, mailProps);
		
		if (conf.wasCanceled() == false)	{
			if (mailProps.isValid())	{
				try	{
					getFolderTreeModel().addStore(
							folderTree.createStore(mailProps, mailProps.getURLName().toString()));
				}
				catch (Throwable e)	{
					Err.error(e);
				}
			}
			else	{
				Err.warning(Language.get("Invalid_Mail_Properties"));
			}
		}
	}


	public void cb_New_Local_Store(Object selection)	{
		LocalStoreDialog dlg = new LocalStoreDialog(folderTree, null);
		String path = dlg.getChosenPath();
		if (path == null || path.length() <= 0)
			return;
			
		try	{
			getFolderTreeModel().addStore(
					folderTree.createStore(LocalStore.LOCALSTORE_PROTOCOL+":"+path));
		}
		catch (Throwable e)	{
			Err.error(e);
		}
	}
	

	public void cb_New_Folder(Object selection)	{
		List sel = (List)selection;

		try	{
			FolderTreeNode parent = (FolderTreeNode)sel.get(0);
			ModelItem createParent = getFolderTreeModel().createModelItem(parent);
			String name = getFolderTreeModel().createNewDefaultName(parent);
			DefaultCreateCommand cmd = new DefaultCreateCommand(createParent, getFolderTreeModel(), name);
			ModelItem newItem = (ModelItem)cmd.doit();
			FolderTreeNode newNode = (FolderTreeNode)newItem.getUserObject();
			TreePath tp = new TreePath(newNode.getPath());
			folderTree.getSensorComponent().startEditingAtPath(tp);	// start cell editing
		}
		catch (Throwable e)	{
			Err.error(e);
		}
	}



	public void cb_Delete(Object selection)	{
		List sel = (List)selection;
		
		// as folder was selected, its messages are showing
		messageTable.getModel().clear();	// clear current table
		
		Vector folderList = new Vector();
		
		// remove mail stores on first level, collect others into folders list
		for (int i = 0; i < sel.size(); i++)	{
			FolderTreeNode tn = (FolderTreeNode)sel.get(i);

			if (tn.isStore())	{
				int index = ((FolderTreeNode)tn.getParent()).getIndex(tn);
				if (index == 0)	{	// ensure that default local store should be removed
					int ret = JOptionPane.showConfirmDialog(
							folderTree,
							Language.get("Close_Your_Default_Local_Store"),
							Language.get("Confirm_Close"),
							JOptionPane.YES_NO_CANCEL_OPTION);
							
					if (ret != JOptionPane.YES_OPTION)	{
						if (ret == JOptionPane.NO_OPTION)
							continue;	// answer was: no close
						else
							return;	// was canceled
					}
				}
				
				try	{
					getFolderTreeModel().removeNodeFromParent(tn);
					ObservableReceiveMail rm = tn.getReceiveMail();
					rm.close();
				}
				catch (Exception e)	{
					// Err.error(e);
				}
			}
			else
			if (tn.isRoot() == false)	{
				folderList.add(tn);
			}
		}

		Vector toConfirm = new Vector();
		
		// collect removal of folders
		for (int i = 0; i < folderList.size(); i++)	{
			FolderTreeNode tn = (FolderTreeNode)folderList.get(i);
			if (getFolderTreeModel().mustBeReallyDeleted(tn))	{
				toConfirm.add(tn);
			}
		}
		
		// confirm removal of folders
		if (toConfirm.size() > 0)	{
			String names = "";
			for (int i = 0; i < toConfirm.size(); i++)
				names = names+"\n    "+toConfirm.get(i).toString();

			int ret = JOptionPane.showConfirmDialog(	// confirm real delete when in trash or remote folder
					folderTree,
					Language.get("Do_You_Really_Want_To_Delete")+names+"\n"+Language.get("Without_Undo_Option")+"?",
					Language.get("Confirm_Delete"),
					JOptionPane.YES_NO_OPTION);

			if (ret == JOptionPane.NO_OPTION)	{
				folderList.removeAll(toConfirm);	// keep those folders
			}
			else
			if (ret != JOptionPane.YES_OPTION)	{	// cancel or window close
				return;
			}
		}
		
		if (folderList.size() <= 0)
			return;	// nothing more to delete
		
		// now remove folders
		final CancelProgressDialog observer = new CancelProgressDialog(GuiApplication.globalFrame, Language.get("Deleting____"));
		final Vector toDelete = folderList;
		final OverwriteDialog overwriteDialog = new OverwriteDialog(observer);
		Runnable todo = new Runnable()	{
			public void run()	{
				try	{
					for (int i = 0; i < toDelete.size(); i++)	{
						if (observer.canceled())
							return;
						
						FolderTreeNode tn = (FolderTreeNode)toDelete.get(i);
						observer.setNote(tn.toString());
						
						FolderTreeModelItem item = (FolderTreeModelItem)getFolderTreeModel().createModelItem(tn);
						item.setOverwriteDialog(overwriteDialog);
			
						DefaultRemoveCommand cmd = new DefaultRemoveCommand(item, getFolderTreeModel());
			
						if (cmd.doit() == null)
							Err.warning(Language.get("Could_Not_Remove")+": "+tn);
					}
				}
				catch (RuntimeException e)	{
					System.err.println("Delete Runtime Exception was: "+e.toString());
					if (e.getMessage().equals("User Cancel") == false)
						Err.error(e);
				}
				finally	{
					observer.endDialog();
				}
			}
		};
		Runnable finish = new Runnable()	{
			public void run()	{
				setEnabledActions();
			}
		};
		observer.start(todo, finish);
	}


	private ModelItem [] toModelItems(List selection, OverwriteDialog overwriteDialog)	{
		ModelItem [] items = new ModelItem[selection != null ? selection.size() : 0];
		for (int i = 0; selection != null && i < selection.size(); i++)	{
			items[i] = getFolderTreeModel().createModelItem((FolderTreeNode)selection.get(i));
			((FolderTreeModelItem)items[i]).setOverwriteDialog(overwriteDialog);
		}
		return items;
	}
	

	public void cb_Cut(Object selection)	{
		clipboard.cut(toModelItems((List)selection, new OverwriteDialog(null)));
		setEnabledActions();	// enable paste
		RefreshTree.refresh(folderTree.getSensorComponent());	// show disabled items
	}


	public void cb_Copy(Object selection)	{
		clipboard.copy(toModelItems((List)selection, new OverwriteDialog(null)));
		setEnabledActions();	// enable paste
		RefreshTree.refresh(folderTree.getSensorComponent());	// as a Copy after a Cut would enable the cutten item(s)
	}


	public void cb_Paste(Object selection)	{
		List l = (List)selection;
		
		ModelItem [] sourceItems = clipboard.getSourceModelItems();
		final boolean isMessages = sourceItems[0] instanceof MessageTableModelItem;	// avoid checking non-treenodes

		// check for hierarchical impossible paste
		for (int i = 0; isMessages == false && i < l.size(); i++)	{
			DefaultMutableTreeNode [] conflict = TreeModelItemUtil.checkForDescendants((FolderTreeNode)l.get(i), sourceItems);
			
			if (conflict != null)	{
				Err.warning(conflict[0]+" "+Language.get("Is_Descendant_Of")+" "+conflict[1]);
				return;
			}
		}
		
		final CancelProgressDialog observer = new CancelProgressDialog(GuiApplication.globalFrame, Language.get("Pasting____"));
		
		// bind the overwrite dialog to progress dialog
		OverwriteDialog overwriteDialog = null;
		for (int i = 0; isMessages == false && i < sourceItems.length; i++)	{
			FolderTreeModelItem item = (FolderTreeModelItem)sourceItems[i];
			overwriteDialog = item.getOverwriteDialog();
			overwriteDialog.setCancelProgressDialog(observer);
		}
		
		// build target items
		final ModelItem [] targetItems = toModelItems(l, overwriteDialog);

		// build paste command argument: no sending model, as item contexts will not be found in current TableModel!
		final CommandArguments arg = isMessages
				? new CommandArguments.Paste(null, messageTable.getModel(), new Integer(0))
				: new CommandArguments.Paste(getFolderTreeModel());

		// allocate runnables for pasting and after paste
		Runnable paste = new Runnable()	{
			public void run()	{
				try	{
					clipboard.paste(targetItems, arg);
				}
				catch (RuntimeException e)	{
					System.err.println("ERROR: Paste Runtime Exception was: "+e.toString());
					if (e.getMessage() == null || e.getMessage().equals("User Cancel") == false)
						Err.error(e);
				}
				finally	{
					observer.endDialog();
				}
			}
		};
		Runnable finish = new Runnable()	{
			public void run()	{
				if (isMessages)	{
					messageTable.finishedMessageLoading(false);
					RefreshTable.refresh(messageTable.getSensorComponent());
				}
				else	{
					setEnabledActions();
					RefreshTree.refresh(folderTree.getSensorComponent());
				}
			}
		};
		
		observer.start(paste, finish);
	}




	/** Bring all senders of messages in this folder and its subfolders to addressbook. */
	public void cb_All_To_Addressbook(Object selection)	{
		List sel = (List)selection;
		Vector addresses = new Vector();
		
		for (int i = 0; i < sel.size(); i++)	{
			FolderTreeNode ftn = (FolderTreeNode)sel.get(i);
			collectAddresses(ftn, addresses);
		}
		
		addressController.mergePackedAddresses(addresses);
	}

	private void collectAddresses(FolderTreeNode ftn, final Vector addresses)	{
		if (ftn.isFolder())	{
			System.err.println("searching folder for addresses: "+ftn);
			
			// visitor to loop all mails of folder
			final ReceiveMail.MailVisitor visitor = new ReceiveMail.MailVisitor()	{
				public void message(int msgCnt, int msgNr, final Message m)	{
					try	{
						Address [] fromAddr = m.getFrom();
						for (int i = 0; fromAddr != null && i < fromAddr.length; i++)
							if (fromAddr[i] instanceof InternetAddress)
								addresses.add(addressController.toRow((InternetAddress)fromAddr[i]));
					}
					catch (MessagingException e)	{
						Err.error(e);
					}
				}
				public void folder(int fldCount, int fldNr, Folder f)	{
				}
			};
			
			try	{
				ObservableReceiveMail rm = ftn.getReceiveMail();
				rm.messages(visitor);
			}
			catch (Exception e)	{
				Err.error(e);
			}
		}

		// loop all children recursively
		for (int i = 0; i < ftn.getChildCount(); i++)	{
			FolderTreeNode f = (FolderTreeNode)ftn.getChildAt(i);
			collectAddresses(f, addresses);
		}
	}


	void close()	{
		if (receiveThread != null)
			receiveThread.setStopped();
		receiveThread = null;
	}

}
