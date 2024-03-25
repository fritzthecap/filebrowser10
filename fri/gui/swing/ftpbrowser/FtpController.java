package fri.gui.swing.ftpbrowser;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import fri.util.ftp.*;
import fri.util.NumberUtil;
import fri.util.file.FileString;
import fri.util.props.*;
import fri.util.os.OS;
import fri.gui.CursorUtil;
import fri.gui.mvc.model.*;
import fri.gui.mvc.model.swing.AbstractMutableTreeModel;
import fri.gui.mvc.model.swing.TreeModelItemUtil;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.view.swing.TreeSelectionDnd;
import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.util.RefreshTree;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.spinnumberfield.*;
import fri.gui.swing.tree.TreeExpander;
import fri.gui.swing.textviewer.TextViewer;
import fri.gui.swing.iconbuilder.Icons;

/**
	The controller for the FTP window. Edits left (local filesystem) and right (FTP server) view.
	
	@author Fritz Ritzberger
*/

public class FtpController extends ActionConnector implements
	TreeSelectionListener,	// button enabling
	TreeModelListener	// tree editing
{
	public static final String MENUITEM_CONNECT = "Connect";
	public static final String MENUITEM_DISCONNECT = "Disconnect";
	public static final String MENUITEM_CUT = "Cut";
	public static final String MENUITEM_COPY = "Copy";
	public static final String MENUITEM_PASTE = "Paste";
	public static final String MENUITEM_INSERT = "Insert Folder";
	public static final String MENUITEM_RENAME = "Rename";
	public static final String MENUITEM_DELETE = "Delete";
	public static final String MENUITEM_NEW_WINDOW = "New Window";
	public static final String MENUITEM_REFRESH = "Refresh";
	public static final String MENUITEM_SLOWDIRLIST_MODE = "Slow But Safe Directory Listing";
	public static final String MENUITEM_VIEW = "View";
	public static final String MENUITEM_PROXY = "Proxy";
	public static final String MENUITEM_ACTIVE_FTP = "Active FTP";
	
	private boolean isFileView = true;
	private HistCombo host, user;
	private JPasswordField password;
	private JTextField port;
	private JTree ftpServerTree, fileTree;
	private JTree currentTree;
	private ObservableFtpClient ftpClient;
	private JLabel ftpServerLabel;
	private JTextField ftpServerStatus, fileStatus;
	private SpinNumberField timeoutField;
	private FtpClipboard clipboard = FtpClipboard.getFtpClipboard();
	private File selectedFile;
	private String selectedPath;
	private int timeout = FtpClient.DEFAULT_TIMEOUT;
	private KeyListener escapeKeyListener;
	private Point viewPosition;
	private LogTextArea logArea;
	

	public FtpController(
		HistCombo host,
		JTextField port,
		HistCombo user,
		JPasswordField password,
		JTextField fileStatus,
		JLabel ftpServerLabel,
		JTextField ftpServerStatus,
		SpinNumberField timeoutField,
		LogTextArea logArea)
	{
		super(null, null, null);
		
		this.ftpServerLabel = ftpServerLabel;
		this.fileStatus = fileStatus;
		this.ftpServerStatus = ftpServerStatus;
		
		ActionListener al = new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				cb_Connect(null);
			}
		};
		
		this.host = host;
		host.addActionListener(al);
		// associate host combo with user combo
		host.addItemListener(new ItemSelectAssociation(user, ClassProperties.getProperties(FtpController.class), "hostUser."));
		if (host.getText().length() > 0)	{	// ensure the association is done
			user.setText(ClassProperties.get(FtpController.class, "hostUser."+host.getText()));
		}

		this.port = port;
		port.addActionListener(al);

		this.user = user;
		user.addActionListener(al);

		this.password = password;
		password.addActionListener(al);
		
		ActionListener al2 = new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				timeout = (int)((WholeNumberField)e.getSource()).getValue();
				
				if (ftpClient != null)	{
					try	{
						ftpClient.setTimeout(timeout);
					}
					catch (Exception ex)	{
						ProgressAndErrorReporter.error(ex);
					}
				}
			}
		};
		this.timeoutField = timeoutField;
		timeoutField.addActionListener(al2);

		this.logArea= logArea;


		registerAction(MENUITEM_CONNECT, Icons.get(Icons.start), "Connect To Remote FTP Server");
		registerAction(MENUITEM_DISCONNECT, Icons.get(Icons.stop), "Disconnect From Remote FTP Server");
		registerAction(MENUITEM_PROXY, Icons.get(Icons.configure), "FTP And Socks Proxy Settings");

		registerAction(MENUITEM_CUT, Icons.get(Icons.cut), "Cut Selection To Clipboard", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_COPY, Icons.get(Icons.copy), "Copy Selection To Clipboard", KeyEvent.VK_C, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_PASTE, Icons.get(Icons.paste), "Paste Clipboard Into Selection", KeyEvent.VK_V, InputEvent.CTRL_MASK);

		registerAction(MENUITEM_INSERT, Icons.get(Icons.newFolder), "Insert New Folder", KeyEvent.VK_INSERT, 0);
		registerAction(MENUITEM_RENAME, Icons.get(Icons.fieldEdit), "Rename Selection", KeyEvent.VK_F2, 0);
		registerAction(MENUITEM_DELETE, Icons.get(Icons.delete), "Delete Selection", KeyEvent.VK_DELETE, 0);
		
		registerAction(MENUITEM_NEW_WINDOW, Icons.get(Icons.newWindow), "New Window", KeyEvent.VK_N, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_REFRESH, Icons.get(Icons.refresh), "Refresh Selected File View", KeyEvent.VK_F5, 0);
		registerAction(MENUITEM_VIEW, Icons.get(Icons.eye), "View Contents Of File");

		registerAction(MENUITEM_SLOWDIRLIST_MODE, (String)null, "Recognize Directories By Trying To \"cd\" To There");

		registerAction(MENUITEM_ACTIVE_FTP, (String)null, "Active FTP is rarely used, most servers support passive FTP.");

		setAllDisabled();
		setEnabled(MENUITEM_ACTIVE_FTP, true);
		setEnabled(MENUITEM_CONNECT, true);
		setEnabled(MENUITEM_SLOWDIRLIST_MODE, true);
		setEnabled(MENUITEM_NEW_WINDOW, true);
		setEnabled(MENUITEM_REFRESH, true);
		setEnabled(MENUITEM_PROXY, true);
		
		FtpProxyDialog.load();
	}

	
	/** Returns the FTP clipboard singleton. */
	public FtpClipboard getClipboard()	{
		return clipboard;
	}
	

	
	public void setView(JTree tree)	{
		if (currentTree != null)	{
			((JComponent)currentTree.getParent().getParent()).setBorder(BorderFactory.createLineBorder(Color.lightGray));
		}
		
		tree.removeTreeSelectionListener(this);
		tree.addTreeSelectionListener(this);
		tree.getModel().removeTreeModelListener(this);
		tree.getModel().addTreeModelListener(this);
		
		this.currentTree = tree;
		this.selection = new TreeSelectionDnd(tree);
		isFileView = tree.getModel().getRoot() instanceof FilesystemTreeNode;

		if (escapeKeyListener == null)	{
			escapeKeyListener = new KeyAdapter()	{
				public void keyPressed(KeyEvent e)	{
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
						clearClipboard();
					}
				}
			};
		}
		tree.removeKeyListener(escapeKeyListener);
		tree.addKeyListener(escapeKeyListener);
		
		if (isFileView == false)	{
			boolean first = (ftpServerTree == null);
			ftpServerTree = tree;
			
			if (first)	{
				new FtpDndPerformer(ftpServerTree, (JScrollPane)ftpServerTree.getParent().getParent(), this);
			}
		}
		else	{
			boolean first = (fileTree == null);
			fileTree = tree;
			
			if (first)	{	// first call, load pathes
				loadPathes(fileTree);
				new FtpDndPerformer(fileTree, (JScrollPane)fileTree.getParent().getParent(), this);
			}
		}
		
		((JComponent)currentTree.getParent().getParent()).setBorder(BorderFactory.createLineBorder(Color.red));
		
		changeAllKeyboardSensors(tree);	// set keyboard bindings to view
		
		setEnabledActions();	// set buttons enabled according to current selection
		//System.err.println("set view to tree "+tree.getClass()+", is file view "+isFileView+", clipboard is empty: "+clipboard.isEmpty());
	}



	private void setEnabledActions()	{
		setEnabledActions(getSelection().getSelectedObject());
	}
	
	private void setEnabledActions(Object selection)	{
		List sel = (List)selection;

		boolean selectionExists = sel != null && sel.size() > 0;

		boolean rootSelected = false;
		for (int i = 0; sel != null && !rootSelected && i < sel.size(); i++)	{
			DefaultMutableTreeNode dn = (DefaultMutableTreeNode)sel.get(i);
			if (!isFileView && dn.isRoot() ||
					isFileView && (dn.getParent() == null || ((DefaultMutableTreeNode)dn.getParent()).isRoot()))
				rootSelected = true;
		}

		boolean canDelete = selectionExists && rootSelected == false;
		boolean canRename = canDelete && sel.size() == 1;
		boolean canInsert = sel != null && sel.size() == 1;
		boolean canPaste = canInsert && clipboard.isEmpty() == false;
		boolean canView = sel != null && sel.size() == 1 && ((DefaultMutableTreeNode)sel.get(0)).getAllowsChildren() == false;

		setEnabled(MENUITEM_INSERT, canInsert);
		setEnabled(MENUITEM_RENAME, canRename);
		setEnabled(MENUITEM_DELETE, canDelete);
		setEnabled(MENUITEM_CUT, canDelete);
		setEnabled(MENUITEM_COPY, canDelete);
		setEnabled(MENUITEM_PASTE, canPaste);
		setEnabled(MENUITEM_REFRESH, currentTree != null && currentTree.getModel().getRoot() != null);
		setEnabled(MENUITEM_VIEW, canView);
	}
		 

	/** Implements TreeSelectionListener to enable buttons and to set file size and time to status panel. */
	public void valueChanged(TreeSelectionEvent e)	{
		if (currentTree != e.getSource())	{	// focusGained will come immediate
			setView((JTree)e.getSource());
		}
		
		System.err.println("valueChanged, is file view "+isFileView+" tree class is "+e.getSource().getClass());
		List l = (List)getSelection().getSelectedObject();
		
		setEnabledActions(l);
		
		// set length and time of last selected file to current status bar
		if (l != null && l.size() == 1)	{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode)l.get(0);
			
			if (isFileView)	{
				selectedFile = (File)n.getUserObject();
				FilesystemTreeNode fsn = (FilesystemTreeNode)n;
				fileStatus.setText((fsn.isLink() ? "-> " : "")+fsn.getLinkName()+fsn.getFileInfo());
			}
			else
			if (n instanceof FtpServerTreeNode)	{
				Component c = GuiApplication.globalFrame;
				CursorUtil.setWaitCursor(c);
				try	{
					FtpServerTreeNode ftpn = (FtpServerTreeNode)n;
					selectedPath = ftpn.getAbsolutePath();
					ftpServerStatus.setText(selectedPath+(ftpn.getAllowsChildren() ? "" : ftpn.getFileInfo()));
				}
				finally	{
					CursorUtil.resetWaitCursor(c);
				}
			}
		}
		else	{
			String labelText = (l == null || l.size() <= 0) ? "" : " Selected "+countFiles(l)+" Files: "+NumberUtil.getFileSizeString(sumLength(l));
			
			if (isFileView)
				fileStatus.setText(labelText);
			else
				ftpServerStatus.setText(labelText);
		}
	}


	private long sumLength(List l)	{
		long sum = 0L;
		for (int i = 0; i < l.size(); i++)	{
			AbstractTreeNode n = (AbstractTreeNode)l.get(i);
			if (n.getAllowsChildren() == false)
				sum += n.getRecursiveSize();
		}
		return sum;
	}

	private long countFiles(List l)	{
		int cnt = 0;
		for (int i = 0; i < l.size(); i++)	{
			AbstractTreeNode n = (AbstractTreeNode)l.get(i);
			if (n.getAllowsChildren() == false)
				cnt++;
		}
		return cnt;
	}

	
	/** Watch cell editing in both trees. */
	public void treeNodesChanged(TreeModelEvent e)	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.getTreePath().getLastPathComponent();
		System.err.println("renaming tree node "+node.getClass());
		try	{	// to recognize a leaf rename
			int index = e.getChildIndices()[0];
			node = (DefaultMutableTreeNode)node.getChildAt(index);
		}
		catch (NullPointerException e1) {	// folder was renamed
		}

		// Achtung: das user-object des node ist nun ein String
		String newname = node.toString();	// for localization in other models

		if (isFileView)	{	// rename a File
			File oldFile = selectedFile;
			if (newname.equals(oldFile.getName()))
				return;
				
			// check if file exists
			File newFile = new File(oldFile.getParent(), newname);
			// to enable case insensitive renames, we check for platform specific name of file
			String oldName = OS.supportsCaseSensitiveFiles() ? oldFile.getName() : oldFile.getName().toLowerCase();
			String newName = OS.supportsCaseSensitiveFiles() ? newname : newname.toLowerCase();
			if (oldName.equals(newName) == false && newFile.exists())	{
				ProgressAndErrorReporter.error("Local File Already Exists: "+newName);
				node.setUserObject(selectedFile);
				cb_Rename(null);
				return;
			}
			
			oldFile.renameTo(newFile);
			
			if (oldName.equals(newName) || newFile.exists() && !oldFile.exists())	{
				node.setUserObject(selectedFile = newFile);
			}
			else	{
				ProgressAndErrorReporter.error("Could Not Rename \""+oldName+"\" To \""+newName+"\"");
				node.setUserObject(selectedFile);
			}
		}
		else	{	// rename a remote file over FTP
			int i = selectedPath.lastIndexOf("/");
			String oldName = selectedPath.substring(i + 1);
			if (newname.equals(oldName))
				return;

			String newpath = selectedPath.substring(0, i + 1) + newname;
			System.err.println("Renaming FTP node "+selectedPath+" to new name "+newpath);

			Component c = GuiApplication.globalFrame;
			CursorUtil.setWaitCursor(c);
			boolean existsNotWorking = false;
			try	{
				if (ftpClient.exists(newpath))	{	// check if file exists
					ProgressAndErrorReporter.error("FTP File Already Exists: "+newname);
					node.setUserObject(oldName);
					cb_Rename(null);
					return;
				}
			}
			catch (Throwable ex)	{
				existsNotWorking = true;
				System.err.println("WARNING: Received exception when testing if node "+newname+" exists: "+ex);
			}

			try	{
				ftpClient.renameTo(selectedPath, newpath);

				if (existsNotWorking == false && ftpClient.exists(newpath) == false)
					throw new IOException("File \""+oldName+"\" Could Not Be Renamed To \""+newname+"\"");
			}
			catch (Exception ex)	{
				ProgressAndErrorReporter.error(ex);
				node.setUserObject(oldName);
			}
			finally	{
				CursorUtil.resetWaitCursor(c);
			}
		}
	}
	
	public void treeNodesInserted(TreeModelEvent e)	{
	}
	public void treeNodesRemoved(TreeModelEvent e)	{
	}
	public void treeStructureChanged(TreeModelEvent e)	{
	}


	private void clearClipboard()	{	// clear clipboard, enable cutten items
		clipboard.clear();
		RefreshTree.refresh(currentTree);
		setEnabledActions();
	}


	// callbacks

	public void cb_Rename(Object selection)	{
		TreePath tp = currentTree.getSelectionPath();
		if (tp != null)	{
			try	{
				currentTree.startEditingAtPath(tp);
			}
			catch (NullPointerException e)	{
				e.printStackTrace();
			}
		}
	}

	public void cb_Connect(Object selection)	{
		disconnect();
		
		String h = host.getText().trim();
		String u = user.getText().trim();
		String p = port.getText().trim();
		int portNumber = p.length() > 0 ? Integer.parseInt(p) : FtpClient.DEFAULT_PORT;
		char [] pc = password.getPassword();
		byte [] pw = new String(pc).getBytes();
		timeout = (int)timeoutField.getValue();
		if (timeout <= 0)
			timeout = FtpClient.DEFAULT_TIMEOUT;
		
		ftpClient = FtpClientFactory.getFtpClient(null, h, portNumber, u, pw, logArea.getPrintStream());
		ftpClient.setActiveFtp(FtpServerTreeNode.activeFtp);
		
		Runnable todo = new Runnable()	{
			public void run()	{
				try	{	// must catch Exceptions to be compatible with Runnable.run()
					ftpClient.setTimeout(timeout);
					ftpClient.connect();
				}
				catch (Exception e)	{
					throw new ProgressAndErrorReporter.RunnableException(e);
				}
			}
		};
		Runnable onSuccess = new Runnable()	{
			public void run()	{
				ftpServerTree.setModel(TreeModelFactory.getFtpServerTreeModel(ftpClient));
				
				if (ftpClient.isConnected())
					try	{ ftpServerLabel.setText("Remote System: "+ftpClient.system()); }	catch (Exception e)	{}
				
				setEnabled(MENUITEM_CONNECT, false);
				setEnabled(MENUITEM_DISCONNECT, true);
				
				if (isFileView == false)	{	// FTP tree is focused, reinstall listeners lost by setModel() call
					JTree old = currentTree;
					setView(ftpServerTree);
					setView(old);
				}
				System.err.println("loading pathes for host "+ftpClient.getHost());
				loadPathes(ftpServerTree, ftpClient.getHost());
				
				ClassProperties.put(FtpController.class, "hostUser."+ftpClient.getHost(), ftpClient.getUser());
			}
		};
		Runnable onException = new Runnable()	{
			public void run()	{
				disconnect();
			}
		};
		
		ProgressAndErrorReporter.createBackgroundMonitor(
				ftpClient,
				"Connecting to "+h+" ...",
				-1L,	// no size known
				todo,
				onSuccess,
				onException);
	}


	public void cb_Disconnect(Object selection)	{
		disconnect();
	}
	
	private void disconnect()	{
		if (ftpClient != null)	{
			System.err.println("saving pathes for host "+ftpClient.getHost());
			savePathes(ftpServerTree, ftpClient.getHost(), false);

			FtpClientFactory.freeFtpClient(ftpClient);	// disconnects if no other reference
			if (TreeModelFactory.freeFtpServerTreeModel(ftpClient) != null)	// last instance was released
				clipboard.freeFtpClient(ftpClient);

			ftpServerTree.setModel(TreeModelFactory.getFtpServerTreeModel(null));
			ftpClient = null;
			
			setEnabled(MENUITEM_CONNECT, true);
			setEnabled(MENUITEM_DISCONNECT, false);

			ftpServerLabel.setText("Remote System (Unconnected)");
			ftpServerStatus.setText("");
		}
	}


	private DefaultMutableTreeNode getParentWhenLeaf(DefaultMutableTreeNode node)	{
		if (node.getAllowsChildren() == false)
			return (DefaultMutableTreeNode)node.getParent();
		return node;
	}

	/** Create a new folder in exactly one selected folder. */
	public void cb_Insert_Folder(Object selection)	{
		List l = (List)selection;
		DefaultMutableTreeNode parent = getParentWhenLeaf((DefaultMutableTreeNode)l.get(0));
		String defaultNewName = "newFolder";
		
		// look for a unused name for new folder
		int i = 1;
		boolean exists;
		String name;
		do	{
			name = defaultNewName+i;
			i++;

			exists = false;
			for (int j = 0; exists == false && j < parent.getChildCount(); j++)	{
				if (parent.getChildAt(j).toString().toLowerCase().equals(name.toLowerCase()))
					exists = true;
			}
		}
		while (exists);

		// create the item by a command
		ModelItem createParent = createModelItem(parent);
		DefaultCreateCommand cmd = new DefaultCreateCommand(createParent, (MutableModel)currentTree.getModel(), name);
		
		ModelItem newChild = (ModelItem)cmd.doit();

		if (newChild != null)	{
			getSelection().setSelectedObject((DefaultMutableTreeNode)newChild.getUserObject());
			cb_Rename(getSelection().getSelectedObject());
		}
	}

	public void cb_Delete(Object selection)	{
		List l = (List)selection;
		
		// confirm delete by showing a dialog
		String s = "";
		if (l.size() <= 10)
			for (int i = 0; i < l.size(); i++)
				s = s+"\n    "+l.get(i);
		else
			s = "\n"+l.size()+" Files";

		int ret = JOptionPane.showConfirmDialog(
				GuiApplication.globalFrame,
				"Do You Really Want To Delete "+s+"\nFrom "+(isFileView ? "Local" : "Remote")+" Filesystem?\nThere Is No Undo Option!",
				"Confirm Delete",
				JOptionPane.YES_NO_OPTION);
		if (ret != JOptionPane.YES_OPTION)
			return;

		final ModelItem [] items = toModelItems(l);

		// delete selected item by a command, in background
		Runnable todo = new Runnable()	{
			public void run()	{
				try	{
					for (int i = 0; i < items.length; i++)	{
						ModelItem toDelete = items[i];
						DefaultRemoveCommand cmd = new DefaultRemoveCommand(toDelete, (MutableModel)currentTree.getModel());
						cmd.doit();
					}
				}
				catch (Exception e)	{
					throw new ProgressAndErrorReporter.RunnableException(e);
				}
			}
		};
		Runnable onSuccess = new Runnable()	{
			public void run()	{
				setEnabledActions();
			}
		};
		
		ProgressAndErrorReporter.createBackgroundMonitor(
				null,
				"Deleting ...",
				-1L,	// delete might be faster than size counting
				todo,
				onSuccess,
				null);
	}


	private ModelItem createModelItem(DefaultMutableTreeNode dn)	{
		return createModelItem(dn, (AbstractMutableTreeModel)currentTree.getModel());
	}
	
	private ModelItem [] toModelItems(List selection)	{
		return toModelItems(selection, (AbstractMutableTreeModel)currentTree.getModel());
	}
	
	private static ModelItem createModelItem(DefaultMutableTreeNode dn, AbstractMutableTreeModel m)	{
		return m.createModelItem(dn);
	}
	
	/** Converts a list of TreeNodes to a list of generated ModelItems. Needed by drag and drop. */
	public static ModelItem [] toModelItems(List selection, AbstractMutableTreeModel m)	{
		ModelItem [] items = new ModelItem[selection.size()];
		
		for (int i = 0; i < selection.size(); i++)	{
			DefaultMutableTreeNode dn = (DefaultMutableTreeNode)selection.get(i);
			items[i] = createModelItem(dn, m);
		}
		
		return items;
	}

	/** Converts a list of Files to a list of generated ModelItems. Needed by drag and drop. */
	public static ModelItem [] filesToModelItems(List files)	{
		String [] pathes = new String[files.size()];

		for (int i = 0; i < files.size(); i++)	{
			pathes[i] = ((File)files.get(i)).getAbsolutePath();
		}

		FilesystemTreeModel m = TreeModelFactory.getFilesystemTreeModel();
		TreeModelFactory.freeFilesystemTreeModel();
		List treeNodes = m.locatePathes(pathes);	// convert to list of DefaultMutableTreeNodes
		
		return toModelItems(treeNodes, m);
	}


	public void cb_Cut(Object selection)	{
		clipboard.cut(toModelItems((List)selection));
		clipboard.setSourceModel((MutableModel)currentTree.getModel());
		
		setEnabledActions();
		RefreshTree.refresh(currentTree);
	}

	public void cb_Copy(Object selection)	{
		clipboard.copy(toModelItems((List)selection));
		clipboard.setSourceModel((MutableModel)currentTree.getModel());
		
		setEnabledActions();
		RefreshTree.refresh(currentTree);	// as a Copy after a Cut would enable cutten items
	}


	public void cb_Paste(Object selection)	{
		List l = (List)selection;
		
		// take folder if selection is leaf
		DefaultMutableTreeNode target = (DefaultMutableTreeNode)l.get(0);
		target = getParentWhenLeaf(target);
		l.set(0, target);
		
		ModelItem [] sourceItems = clipboard.getSourceModelItems();

		// check for impossible hierarchical action
		if (checkForDescendants(target, sourceItems) == false)
			return;
		
		// paste
		Component c = GuiApplication.globalFrame;
		CursorUtil.setWaitCursor(c);
		try	{
			long size = 0L;	// no size count for internal move action
			
			// transfer to another model will result in copy, count transfer size
			if (clipboard.getSourceModel() != currentTree.getModel())	{
				ModelItem [] items = clipboard.getSourceModelItems();
				for (int i = 0; i < items.length; i++)	{
					AbstractTreeNode an = (AbstractTreeNode)items[i].getUserObject();
					size += an.getRecursiveSize();
				}
			}
			
			ftpServerTree.setEnabled(false);	// do not let perform actions on FTP server now
			
			final ModelItem [] items = toModelItems(l);
			
			Runnable todo = new Runnable()	{
				public void run()	{
					try	{
						CommandArguments arg = new CommandArguments.Paste(
								clipboard.getSourceModel(),
								(MutableModel)currentTree.getModel());
			
						clipboard.paste(items, arg);
					}
					catch (Exception e)	{
						throw new ProgressAndErrorReporter.RunnableException(e);
					}
				}
			};
			Runnable onSuccess = new Runnable()	{
				public void run()	{
					// do cleanups
					clipboard.setSourceModel(null);
	
					if (FtpServerTreeModelItem.ftpClientClone != null)	{
						try	{ FtpServerTreeModelItem.ftpClientClone.disconnect(); }	catch (Exception e)	{}
						FtpServerTreeModelItem.ftpClientClone = null;
					}
			
					// set new state, enable moved items
					ftpServerTree.setEnabled(true);
					setEnabledActions();
					RefreshTree.refresh(currentTree);
				}
			};
			Runnable onError = new Runnable()	{
				public void run()	{
					ftpServerTree.setEnabled(true);
				}
			};
			
			ProgressAndErrorReporter.createBackgroundMonitor(
					null,
					"Pasting ...",
					size,
					todo,
					onSuccess,
					onError);
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
	}


	private boolean checkForDescendants(DefaultMutableTreeNode target, ModelItem [] sourceItems)	{
		DefaultMutableTreeNode [] conflict = TreeModelItemUtil.checkForDescendants(target, sourceItems);
		if (conflict != null)	{
			ProgressAndErrorReporter.error(conflict[0]+" Is Descendant Of "+conflict[1]);
			return false;
		}
		return true;
	}
	

	public void cb_Slow_But_Safe_Directory_Listing(Object selection)	{
		FtpServerTreeNode.doSlowButSafeListing = isChecked(getCurrentActionEvent().getSource());
		System.err.println("slow but safe listing is now "+FtpServerTreeNode.doSlowButSafeListing);
	}
	
	public void cb_New_Window(Object selection)	{
		save();
		new FtpFrame();
	}

	public void cb_Refresh(Object selection)	{
		String [][] pathes = memorizePathes(currentTree);
		AbstractTreeNode root = (AbstractTreeNode)currentTree.getModel().getRoot();
		root.releaseChildren();
		((DefaultTreeModel)currentTree.getModel()).reload();
		restorePathes(currentTree, pathes);
	}
	
	public void cb_View(Object selection)	{
		DefaultMutableTreeNode dn = (DefaultMutableTreeNode)((List)selection).get(0);
		String text;

		if (isFileView)	{
			FilesystemTreeNode fn = (FilesystemTreeNode)dn;
			text = FileString.get((File)fn.getUserObject());
		}
		else	{
			try	{
				FtpServerTreeNode fn = (FtpServerTreeNode)dn;
				text = fn.getFtpClient().cat(fn.getAbsolutePath());
			}
			catch (IOException e)	{
				ProgressAndErrorReporter.error(e);
				return;
			}
		}

		new TextViewer(text);
	}
	
	public void cb_Proxy(Object selection)	{
		new FtpProxyDialog(GuiApplication.globalFrame);
	}

	public void cb_Active_FTP(Object selection)	{
		FtpServerTreeNode.activeFtp = isChecked(getCurrentActionEvent().getSource());
		if (ftpClient != null)
			ftpClient.setActiveFtp(FtpServerTreeNode.activeFtp);
	}



	public void close()	{
		disconnect();

		TreeModelFactory.freeFilesystemTreeModel();
		FtpClipboard.freeFtpClipboard();
		logArea.close();
		
		save();
	}


	private void save()	{
		host.commit();
		user.commit();
		host.save();
		user.save();
		savePathes(fileTree);
	}



	// put pathes into memory structure and get view position
	private String [][] memorizePathes(JTree tree)	{
		JViewport viewPort = (JViewport)tree.getParent();
		viewPosition = viewPort.getViewPosition();

		String [] openPathes = TreeExpander.getOpenPathes(tree);
		String [] selectedPathes = TreeExpander.getSelectedPathes(tree);
		return new String [][] { openPathes, selectedPathes };
	}

	// restore pathes from memory structure and set view position
	private void restorePathes(JTree tree, String [][] pathes)	{
		TreeExpander.setOpenPathes(tree, pathes[0]);
		TreeExpander.setSelectedPathes(tree, pathes[1]);
		
		if (viewPosition != null)
			((JViewport)tree.getParent()).setViewPosition(viewPosition);
	}

	// put pathes to disk
	private void savePathes(JTree tree)	{
		savePathes(tree, null, true);
	}

	private void savePathes(JTree tree, String hostName, boolean doSave)	{
		if (tree.getModel().getRoot() == null)
			return;
			
		String [][] pathes = memorizePathes(tree);
		String [] openPathes = pathes[0];
		String [] selectedPathes = pathes[1];
		String prefix = hostName != null ? hostName+"." : "";
		
		for (int i = 0; openPathes != null && i < openPathes.length; i++)	{
			ClassProperties.put(FtpController.class, prefix+"openPath"+i, openPathes[i]);
		}
		for (int i = 0; selectedPathes != null && i < selectedPathes.length; i++)	{
			ClassProperties.put(FtpController.class, prefix+"selectedPath"+i, selectedPathes[i]);
		}
		
		if (doSave)
			ClassProperties.store(FtpController.class);
	}

	// load pathes from disk
	private void loadPathes(JTree tree)	{
		loadPathes(tree, null);
	}

	private void loadPathes(JTree tree, String hostName)	{
		Properties props = ClassProperties.getProperties(FtpController.class);
		Vector open = new Vector();
		Vector selected = new Vector();
		String prefix = hostName != null ? hostName+"." : "";
		
		for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			String value = props.getProperty(name);
			
			if (name.startsWith(prefix+"openPath"))	{
				open.add(value);
			}
			else
			if (name.startsWith(prefix+"selectedPath"))	{
				selected.add(value);
			}
		}
		
		String [] openPathes = new String [open.size()];
		open.copyInto(openPathes);
		String [] selectedPathes = new String [selected.size()];
		selected.copyInto(selectedPathes);

		restorePathes(tree, new String [][] { openPathes, selectedPathes });
		
		TreePath tp = tree.getSelectionPath();
		if (tp != null)
			TreeExpander.scrollTo(tp, tree);

		// clean all properties of given host or no-host
		Vector toDelete = new Vector();
		for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			
			if (name.startsWith(prefix+"selectedPath") || name.startsWith(prefix+"openPath"))
				toDelete.add(name);
		}
		for (int i = 0; i < toDelete.size(); i++)	{
			props.remove(toDelete.get(i));
		}
	}

}