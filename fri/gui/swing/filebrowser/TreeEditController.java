package fri.gui.swing.filebrowser;

import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.util.List;
import javax.swing.event.ChangeListener;
import fri.util.sort.quick.QSort;
import fri.util.sort.quick.Comparator;
import fri.util.os.OS;
import fri.util.FileUtil;
import fri.util.NumberUtil;
import fri.util.file.*;
import fri.util.file.archive.ArchiveFactory;
import fri.gui.CursorUtil;
import fri.gui.awt.clipboard.SystemClipboard;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.dnd.JavaFileList;
import fri.gui.swing.BugFixes;
import fri.gui.swing.ftpbrowser.FtpFrame;
import fri.gui.swing.tail.TailFrame;
import fri.gui.swing.IconUtil;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.yestoalldialog.*;
import fri.gui.swing.util.TextFieldUtil;
import fri.gui.swing.editor.EditorFrame;
import fri.gui.swing.commandmonitor.CommandMonitor;
import fri.gui.swing.application.GuiApplication;	// for icon
import fri.gui.swing.diff.*;
import fri.gui.swing.calculator.*;
import fri.gui.swing.sound.SoundPlayerFrame;
import fri.gui.swing.system.SystemInformationPanel;
import fri.gui.swing.hexeditor.HexEditorFrame;
import fri.gui.swing.crypt.CryptFrame;
import fri.gui.swing.mailbrowser.MailFrame;
import fri.gui.swing.xmleditor.XmlEditor;

/**
	User-Input Controller Objekt fuer Applikations-
	Standard Aktionen.
	Dieses Objekt sorgt fuer Konsistenz zwischen tree und
	zugrundeliegenden nodes. D.h. dass die Knoten, die im
	tree verschoben werden, mittels diese Objektes auch im
	zugrundeliegendem Informations-Netz verschoben werden.
	Beteiligt daran sind Tastatur-Aktionen, Maus-Aktionen,
	Popupmenu-Aktionen.
	<p>
	Einige Aktionen muessen im Hintergrund laufen und sind
	daher als "Runnable" codiert. Daher sind einige Methoden
	mit "invokeLater()" versehen, da aus einem Background-Thread
	nicht direkt auf das GUI zugegriffen werden darf.
*/


public class TreeEditController extends TreeRequester implements
	ActionListener,
	ChangeListener	// slider
{
	// these statics are necessary for filebrowser instance communication
	// in the case of cut/copy/paste
	private static DefaultMutableTreeNode [] copiedNodes;	// cut, copy and paste
	private static DefaultMutableTreeNode [] movingNodes;

	private static OpenCommandList openCommands = new OpenCommandList();

	// one error log for all instances
	private static JTextArea montext;
	
	private static int transactions;
	private TransactionDialog observer;	// temporary valid, stored in transaction context

	private TreePanel treepanel;

	private DefaultMutableTreeNode nodeToSelect;	// after remove

	private TreeEditPopup popup;
	private JMenu newMenu, sortMenu, delMenu;
	
	private JPopupMenu nodepopup;
	private JMenuItem copy, move, cancel;
	
	private DefaultMutableTreeNode [] draggedNodes;	// temporary nodes copied from DnDListener
	private DefaultMutableTreeNode dropNode;
	private boolean dragCopy = false;
	private Vector nodesToSelect = new Vector();
	
	private JTextField errors;
	private TextLineHolder tf_filter;
	private JComboBox cmb_include;
	private JCheckBox ckb_dropmenu;

	private TreeMouseListenerJDK12 dndLsnr;	
		
	// table of actions, that implement ActionListener by themselves
	private Component openPopupComponent;
	private Point openPopupPoint;
	private Component mouseComponent;
	private Point mousePoint;

	private static EditorFrame editor;
	private static EditorFrame hexeditor;

	private SearchFrame finddialog;
	

	/**
		Construct controller object (user input) for the treeview.
		The standard keystrokes and mouseactions get registered in
		the component.
	*/
	public TreeEditController(TreePanel treepanel)	{
		super(treepanel.getTree());
		this.treepanel = treepanel;

		dndLsnr = new TreeMouseListenerJDK12(
				tree,
				treepanel.getModel(),
				treepanel.getTreeViewport(),
				this);

		buildPopups();
	}
	
	
	public void updateUI()	{
		System.err.println("TreeEditController.updateUI()");
		if (nodepopup != null)
			nodepopup.updateUI();
		popup.updateUI();
	}


	/**
		Pass the popup menu to edit controller.
		Menu folders are passed to be set disabled when all children are disabled.
	*/
	public void setPopupMenu(
		TreeEditPopup popup,
		JMenu newMenu,
		JMenu sortMenu,
		JMenu delMenu)
	{
		this.popup = popup;
		this.newMenu = newMenu;
		this.delMenu = delMenu;
		this.sortMenu = sortMenu;
		setEnabledActions();
	}


	public TreeEditPopup getPopupMenu()	{
		return popup;
	}

	
	/** decides if a popup menu is shown when dropping nodes */
	public void setUseDropMenuCheckBox(JCheckBox ckb_dropmenu)	{
		this.ckb_dropmenu = ckb_dropmenu;
		dndLsnr.setUseDropMenu(ckb_dropmenu.isSelected());
		ckb_dropmenu.getModel().addActionListener(this);
	}
	
	/** @return true if a drag&drop handler should use drop popup menu */
	public boolean getUseDropMenu()	{
		return dndLsnr.getUseDropMenu();
	}
	
		
	private void buildPopups()	{
		// helper popup for drag and drop
		nodepopup = new JPopupMenu();
		nodepopup.add(copy = new JMenuItem("Copy"));
		copy.addActionListener(this);
		nodepopup.add(move = new JMenuItem("Move"));
		move.addActionListener(this);
		nodepopup.addSeparator();
		nodepopup.add(cancel = new JMenuItem("Cancel"));
		cancel.addActionListener(this);
	}


	public void setErrorRenderer(JTextField errors)	{
		this.errors = errors;
	}


	public void setFilterAction(
		TextLineHolder tf_filter,
		JComboBox cmb_include,
		JCheckBox ckb_showfiles,
		JCheckBox ckb_hidden)
	{
		this.tf_filter = tf_filter;
		this.cmb_include = cmb_include;
		
		tf_filter.addActionListener(this);
		ckb_showfiles.getModel().addActionListener(this);
		ckb_hidden.getModel().addActionListener(this);
	}


	public boolean areThereDraggedNodes()	{
		return dndLsnr.areThereDraggedNodes();
	}
	
	public boolean transactionsInProgress()	{
		return TreeEditController.transactions > 0 || dndLsnr.areThereDraggedNodes();
	}
	
	public JTree getTree()	{
		return tree;
	}
	
	public BufferedTreeNode getRoot()	{
		return treepanel.getRoot();
	}
	
	public NetNode getRootNetNode()	{
		return treepanel.getRootNode();
	}
	
	public OpenCommandList getOpenCommands()	{
		return openCommands;
	}
	
	public Component getCursorComponent()	{
		Component c = delegateActive ? (Component)delegateFrame : (Component)treepanel;
		return c;
	}

	/**
		Rekursiv durch den Sub-Tree nach dem uebergebenen Knoten suchen. Wird verwendet
		von DragAndDrop handler.
		@param n Knoten, der einen kompletten Pfad enthaelt, der mit
			getPathComponents() abgerufen werden kann.
	*/
	public DefaultMutableTreeNode localizeNode(NetNode n)	{
		// Assoziieren des NetNode mit einem Pfad in der Treeview
		BufferedTreeNode root = getRoot();
		return root.localizeNode(n);
	}

	/**
		Rekursiv durch den Sub-Tree nach dem uebergebenen Knoten suchen. Wird verwendet
		von copy/move event.
		@param n Knoten, der einen kompletten Pfad enthaelt, der mit
			getPathComponents() abgerufen werden kann.
	*/
	private DefaultMutableTreeNode localizeNode(DefaultMutableTreeNode d)	{
		return localizeNode((NetNode)d.getUserObject());
	}



	// interface ChangeListener (autoscroll speed slider)

	public void stateChanged(ChangeEvent e)	{
		JSlider source = (JSlider)e.getSource();
		if (!source.getValueIsAdjusting()) {
			int value = (int)source.getValue();
			// set new drag autoscroll speed to drag and drop listener
			setAutoScrollSpeed(value);
		}
	}

	public void setAutoScrollSpeed(int speed)	{
		dndLsnr.setAutoscrollSpeed(speed);
	}


	private void ensureFilterIsCommitted()	{
		HistCombo tf = (HistCombo)tf_filter;
		if (tf.isCommitted() == false)
			tf.commit();
	}

	
	// interface ActionListener

	public void actionPerformed(ActionEvent e)	{
		System.err.println("actionPerformed: "+e.getActionCommand());

		Component cursorComponent = getCursorComponent();
		CursorUtil.setWaitCursor(cursorComponent);
		try	{
			// actions executable without selection
			
			// drag and drop popup callbacks
			if (e.getSource() == copy)	{	// copy dragged item
				dragCopy = true;
				finishDrag();
			}
			else
			if (e.getSource() == move)	{	// move dragged item
				dragCopy = false;
				finishDrag();
			}
			else
			if (e.getSource() == cancel)	{	// cancel drag n drop
				draggedNodes = null;
			}
			else
			if (e.getActionCommand() == null)	{
				Thread.dumpStack();
				System.err.println("FEHLER: action command is null in event: "+e);
			}
			else
			if (e.getActionCommand().equals("Filter"))	{	// Filter-Button
				tf_filter.setText(tf_filter.getText());	// save current text to combo
				refilter();
			}
			else
			// checkbox that influences the whole tree
			if (e.getActionCommand().equals("Showfiles"))	{
				refilterAll();
			}
			else
			if (e.getActionCommand().equals("Showhidden"))	{
				refilterAll();
			}
			else
			if (e.getActionCommand().equals("Dropmenu"))	{
				dndLsnr.setUseDropMenu(ckb_dropmenu.isSelected());
			}
			else
			// key shortcut for showing popup-menu
			if (e.getActionCommand().equals("Popup"))	{
				if (mousePoint != null)
					showActionPopup(mousePoint.x, mousePoint.y, mouseComponent);
				else
					showActionPopup(0, 0, mouseComponent);
			}
			else		
			// actions executable only with selection
			if (e.getSource() == tf_filter ||	// filter text changed
					e.getSource() == cmb_include ||	// filter semantics changed
					e.getActionCommand().equals("Refilter"))	{
				refilter();
			}
			else
			if (e.getActionCommand().equals("Refresh"))	{
				ensureFilterIsCommitted();
				DefaultMutableTreeNode [] d = getSelectedTreeNodes();
				if (d != null && d.length == 1 && d[0].isRoot())
					treepanel.getRoot().setListDrives();
				refresh();
			}
			else
			if (e.getActionCommand().equals("Remove"))	{
				ensureFilterIsCommitted();
				removeNodes();
			}
			else
			if (e.getActionCommand().equals("Empty"))	{
				ensureFilterIsCommitted();
				emptyNodes();
			}
			else
			if (e.getActionCommand().equals("Delete"))	{
				ensureFilterIsCommitted();
				deleteNodes();
			}
			else
			if (e.getActionCommand().equals("File"))	{
				if (inserter != null)	// delegate action
					inserter.insertNode();
				else
					insertNode();
			}
			else
			if (e.getActionCommand().equals("Folder"))	{
				if (inserter != null)	// delegate action
					inserter.insertContainer();
				else
					insertContainer();
			}
			else
			if (e.getActionCommand().equals("Cut"))	{
				beginMove();
			}
			else
			if (e.getActionCommand().equals("Copy"))	{
				beginCopy();
			}
			else
			if (e.getActionCommand().equals("Paste"))	{
				finishAction();
			}
			else
			if (e.getActionCommand().equals("Clear"))	{
				cancelAll();
				popup.setVisible(false);
				nodepopup.setVisible(false);
			}
			else
			if (e.getActionCommand().equals("Rename"))	{
				if (renamer != null)	// delegate action
					renamer.beginRename();
				else
					beginRename();
			}
			else
			if (e.getActionCommand().equals("Open"))	{
				openNode(e.getSource());
			}
			else
			if (e.getActionCommand().equals("Expand Recursive"))	{
				expandRecursive();
			}
			else
			if (e.getActionCommand().equals("Info"))	{
				infoDialog();
			}
			else
			if (e.getActionCommand().startsWith("Compress."))	{
				compressNodes(e.getActionCommand().substring("Compress.".length()));
			}
			else
			if (e.getActionCommand().equals("Split / Join"))	{
				splitJoinNodes();
			}
			else
			if (e.getActionCommand().equals("Find"))	{
				findNode();
			}
			else
			if (e.getActionCommand().equals("Select All"))	{
				if (selecter != null)
					selecter.selectAll();
			}
			else
			if (e.getActionCommand().equals("Print Contents As Text"))	{
				if (selecter != null)
					selecter.printAsText();
			}
			else
			if (e.getActionCommand().equals("View"))	{
				viewNodes();
			}
			else
			if (e.getActionCommand().equals("Edit"))	{
				editNodeObjects();
			}
			else
			if (e.getActionCommand().equals("HexEdit"))	{
				hexEditNodeObjects();
			}
			else
			if (e.getActionCommand().equals("XML"))	{
				xmlEditNodeObjects();
			}
			else
			if (e.getActionCommand().equals("View Rich Text"))	{
				viewNodesRichText();
			}
			else
			if (e.getActionCommand().equals("FTP"))	{
				new FtpFrame();
			}
			else
			if (e.getActionCommand().equals("Mail"))	{
				new MailFrame();
			}
			else
			if (e.getActionCommand().equals("Line Count"))	{
				linecount();
			}
			else
			if (e.getActionCommand().equals("File Differences"))	{
				diff(true);
			}
			else
			if (e.getActionCommand().equals("Directory Differences"))	{
				diff(false);
			}
			else
			if (e.getActionCommand().equals("Concordance"))	{
				concordance();
			}
			else
			if (e.getActionCommand().equals("Image"))	{
				viewNodesImages();
			}
			else
			if (e.getActionCommand().equals("Sound"))	{
				playSound();
			}
			else
			if (e.getActionCommand().equals("Configure Open Commands"))	{
				editOpenEventsForNodes();
			}
			else
			if (e.getActionCommand().equals("New Browser"))	{
				createNewBrowser();
			}
			else
			if (e.getActionCommand().equals("Home"))	{
				changeDir(1);
			}
			else
			if (e.getActionCommand().equals("Wastebasket"))	{
				changeDir(2);
			}
			else
			if (e.getActionCommand().equals("Floppy"))	{
				// explore floppy and keep expansion
				treepanel.getRoot().setListDrives();
				treepanel.expandiereAktuelle(false);
				
				// open the folder
				treepanel.explorePath(new String [] { "A:" }, true);
			}
			else
			if (e.getActionCommand().equals("Tail"))	{
				tail();
			}
			else
			if (e.getActionCommand().equals("FolderWatch"))	{
				folderWatcher();
			}
			else
			if (e.getActionCommand().equals("Launch Command"))	{
				NetNode [] n = getSelectedNodes();
				boolean done = false;
				for (int i = 0; i < n.length; i++)	{
					if (n[i].canCreateChildren() || n[i].isLeaf())	{
						new CommandMonitor((File)n[i].getObject());
						done = true;
					}
				}
				if (done == false)
					new CommandMonitor();
			}
			else
			if (e.getActionCommand().equals("System Properties"))	{
				Window pnt = ComponentUtil.getWindowForComponent(getCursorComponent());
				JDialog dlg;
				if (pnt instanceof Frame)
					dlg = new JDialog((Frame) pnt, "System Information", true);
				else
					dlg = new JDialog((Dialog) pnt, "System Information", true);
				dlg.getContentPane().add(new SystemInformationPanel());
				new GeometryManager(dlg).show();
			}
			else
			if (e.getActionCommand().equals("Log"))	{
				treepanel.getMonitor().show();
			}
			else
			if (e.getActionCommand().equals("About"))	{
				about();
			}
			else
			if (e.getActionCommand().equals("Close All"))	{
				treepanel.closeAll();
			}
			else
			if (e.getActionCommand().equals("Auto-Refresh"))	{
				treepanel.setAutoRefresh();
			}
			else
			if (e.getActionCommand().equals("Screenshot"))	{
				new ScreenshotDialog(getCursorComponent(), getRootNetNode());
			}
			else
			if (e.getActionCommand().equals("Calculator"))	{
				JFrame f = new CalculatorFrame();
				setApplicationIcon(f);
			}
			else
			if (e.getActionCommand().equals("Crypt"))	{
				File [] files = leafsToFileArray(getSelectedLeafNodes());
				JFrame f = (files != null && files.length > 0) ? new CryptFrame(files[0]) : new CryptFrame();
				setApplicationIcon(f);
			}
			else
			if (e.getActionCommand().equals("Customize"))	{
				// no action as this is served by GuiApplication
			}
			else	{	// look for names that implement their own action listener
				Thread.dumpStack();
				System.err.println("FEHLER: action not implemented: "+e.getActionCommand()+", event source "+e.getSource().hashCode());
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(cursorComponent);
		}
	}


	private void setApplicationIcon(JFrame f)	{
		Component pnt = ComponentUtil.getWindowForComponent(getCursorComponent());
		if (pnt instanceof JFrame)
			IconUtil.setFrameIcon((JFrame)pnt, f);
	}
	

	private void changeDir(int which)	{
		NetNode n = treepanel.getRootNode();
		String [][] sarr = new String[1][];

		if (which == 1)
			sarr[0] = n.getHomePathComponents();
		else
		if (which == 2 && n.getWastebasket() != null)
			sarr[0] = n.getWastebasket().getPathComponents();

		treepanel.getTree().clearSelection();	// sonst bleibt aktuelle Selektion bestehen
		treepanel.expandiere(sarr, sarr);
	}

	
	// status rendering helpers
	
	private void error(final String s)	{
		// must be able to be called from a background-thread
		if (s == null || s.equals(""))
			return;			

		if (SwingUtilities.isEventDispatchThread())	{
			renderError(s);
		}
		else	{
			try	{
				SwingUtilities.invokeAndWait(new Runnable()	{
					public void run()	{
						renderError(s);
					}
				});
			}
			catch (Exception e)	{
			}
		}
	}

	// must be called from event thread
	private void renderError(String s)	{
		String my = "ERROR: "+s;
		System.err.println(my);
		Toolkit.getDefaultToolkit().beep();
		appendLogText(my);
		setErrorText(my);
		renderFatal(s);
	}
	
	// must be called from event thread
	private void appendLogText(String text)	{
		if ((montext = treepanel.ensureLogMonitor()) != null)	{
			montext.append(text+"\n");
		}
	}

	// must be called from event thread
	private void setErrorText(String text)	{
		if (errors != null)	{
			errors.setText(text);
			TextFieldUtil.scrollToPosition(errors, 0);
		}
	}
	
	private void fatalError(final String err)	{
		System.err.println("FEHLER: "+err);
		if (SwingUtilities.isEventDispatchThread())	{
			renderFatal(err);
		}
		else	{
			try	{
				SwingUtilities.invokeAndWait(new Runnable()	{
					public void run()	{
						renderFatal(err);
					}
				});
			}
			catch (Exception e)	{
			}
		}
	}

	// must be called from event thread
	private synchronized void renderFatal(String err)	{
		JOptionPane.showMessageDialog(
				getCursorComponent(),
				err,
				"Error",
				JOptionPane.ERROR_MESSAGE);
	}

	private void setStatus(final String s)	{
		// must be able to be called from a background-thread
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				// success and errors to a status renderer
				setErrorText(s);
				appendLogText(s);
			}
		});
	}



	// action implementation

	private void createNewBrowser()	{
		treepanel.save();	// make current browser persistent as arguments for new one
		new FileBrowser(treepanel.getModel());	// create new window
	}


	// confirm in a dialog if filter is to be applied to action.
	// @return null if no, throws exception if canceled, else (yes) returns filter.
	private String confirmFiltering(
		String command,
		String filter,
		boolean include)
		throws UserCancelException
	{
		if (treepanel.getDialogMode())
			return null;
			
		if (NodeFilter.isFilterValid(filter, include))	{
			// ask for filtered action
			String msg = "Apply Filter '"+filter+
					"' ("+(include ? "Including" : "Excluding")+
					") To Action '"+
					command+"'?";
			
			ApplyFilterDialog dlg;
			Component pnt = ComponentUtil.getWindowForComponent(getCursorComponent());
			if (pnt instanceof JFrame)
				dlg = new ApplyFilterDialog((JFrame)pnt, msg);
			else
				dlg = new ApplyFilterDialog((JDialog)pnt, msg);

			int ret = dlg.showDialog();

			if (ret == JOptionPane.NO_OPTION)	{
     	filter = null;
			}
     else
     if (ret == JOptionPane.CLOSED_OPTION || ret == JOptionPane.CANCEL_OPTION)	{
     	throw new UserCancelException();
     }
		}
		else	{
			filter = null;
		}
		return filter;
	}
	
	
	// file transfer encapsulation

	private TransactionContext beginTransaction(String command, DefaultMutableTreeNode [] nodes)
		throws UserCancelException
	{
		String filter = null;
		boolean include = true;
		boolean showfiles = true;
		boolean showhidden = true;
		
		if (areAllNodesContainers(nodes) &&
				((command.equals("Copy") ||
					command.equals("Move") ||
					command.equals("Empty"))))
		{
			filter = treepanel.getFilter();
			include = treepanel.getInclude();
			showfiles = treepanel.getShowFiles();
			showhidden = treepanel.getShowHidden();
     System.err.println("got filter in TreeEditController "+hashCode()+" from TreePanel "+treepanel.hashCode()+": "+filter);
			
			filter = confirmFiltering(command, filter, include);
			// exception can be thrown from here
			
     //System.err.println("user answers, filter is "+filter);
     if (filter != null)	{	// YES is the answer
				// reset optional move pending as finishMove() does not know about filter
				for (int k = 0; k < nodes.length; k++)	{
					NetNode n = (NetNode)nodes[k].getUserObject();
					if (n.getMovePending())
						n.setMovePending(false);
				}
			}
		}
		
		return beginTransaction(command, filter, include, showfiles, showhidden);
	}

	private TransactionContext beginTransaction(String command)
		throws UserCancelException
	{
		return beginTransaction(command, null, false, false, false);
	}
	
	// central method to start a transaction
	private TransactionContext beginTransaction(
		String command,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
		throws UserCancelException
	{
		// if another instance is running a transaction, undo listener will be confused.
		if (TreeEditController.transactions > 0 && command.equals("Rename") == false)	{
			int ret = JOptionPane.showConfirmDialog(
					getCursorComponent(),
					"A transaction is running.\n\n"+
							"Starting a new transaction makes the undo option unreliable\n"+
							"and can conflict with the running transaction.\n"+
							"It is strongly recommended NOT to run parallel transactions!\n\n"+
							"Would you like to cancel?",
					"Running Transaction",
					JOptionPane.YES_NO_OPTION);
							
			if (ret != JOptionPane.NO_OPTION)	{
				throw new UserCancelException();
			}
		}
		
		TransactionContext context;
		if (filter != null)
			context = new TransactionContext(getRootNetNode(), filter, include, showfiles, showhidden);
		else
			context = new TransactionContext(getRootNetNode());

		// start transaction
		TreeEditController.transactions++;
		context.beginTransaction();
		System.err.println("beginning transaction "+transactions);
		
		return context;
	}
	
	
	private void endTransaction(TransactionContext context)	{
		System.err.println("ending transaction "+context);
		context.endTransaction();
		TreeEditController.transactions--;
	}
	


	// callbacks
	
	private void findNode()	{
		DefaultMutableTreeNode [] selected = getSelectedTreeNodes();
		BufferedTreeNode [] sel;
		if (selected == null || selected.length <= 0)	{
			sel = new BufferedTreeNode [] { getRoot() };
		}
		else	{
			sel = new BufferedTreeNode [selected.length];
			for (int i = 0; i < selected.length; i++)
				sel[i] = (BufferedTreeNode)selected[i];
		}
		
		if (finddialog == null || finddialog.isVisible())	{
			SearchFrame sf = new SearchFrame(tree, sel, this);
			finddialog = sf;
		}
		else	{
			finddialog.setTargets(sel);
		}
	}
	
	
	private void infoDialog()	{
		int anz = getSelectionCount();
		if (anz <= 0)
			return;
		infoDialog(getSelectedNodes());
	}


	private void infoDialog(NetNode [] n)	{
		infoDialog(n, false);
	}
	
	public void infoDialog(NetNode [] n, boolean forceArchive)	{
		boolean leaf = true;
		for (int i = 0; i < n.length; i++)
			if (n[i].isLeaf() == false)
				leaf = false;
				
		if (leaf == false)	{
			String filter = treepanel.getFilter();
			boolean include = treepanel.getInclude();
			boolean showfiles = treepanel.getShowFiles();
			boolean showhidden = treepanel.getShowHidden();
			try	{
				filter = confirmFiltering("Info", filter, include);
			}
			catch (UserCancelException e)	{
				return;
			}
			if (filter != null)	{
				new InfoFrame(this, n, filter, include, showfiles, showhidden);
				return;
			}
		}
		
		new InfoFrame(this, n, forceArchive);	// FRi TODO showhidden
	}
	
		
	
	private void refresh()	{
		refilter(true);	// do rescan
	}
	
	
	public void refilter()	{
		refilter(false);	// do not rescan	
	}
	
	
	public void refilter(boolean rescan)	{
		cancelAll();
		if (getSelectionCount() <= 0)
			refilterAll();
		else
			treepanel.expandiereSelektierte(rescan);
	}


	private void refilterAll()	{
		cancelAll();
		treepanel.expandiereAktuelle(true);
	}


	private void cancelAll()	{
		if (tree.isEditing())
			tree.getCellEditor().cancelCellEditing();

		if (movingNodes != null)	{
			for (int i = 0; i < movingNodes.length; i++)	{

				NetNode n = (NetNode)movingNodes[i].getUserObject();
				if (n.getMovePending())
					n.setMovePending(false);
			}
			tree.treeDidChange();
		}
		movingNodes = null;
		copiedNodes = null;

		setEnabledActions();
	}


	
	private void beginRename()	{
		if (getSelectionCount() != 1)	{
			error("Select Exactly One Node To Rename");
			return;
		}
		
		TreePath tp = tree.getSelectionPath();
		if (tp != null)	{
			try	{
				//System.err.println("start editing at path "+tp+", cell editor is "+tree.getCellEditor());
				tree.setEditable(true);
				tree.startEditingAtPath(tp);
			}
			catch (NullPointerException e)	{
				e.printStackTrace();
			}
		}
		// will be finished by treeNodesChanged()
	}


	/** Rename the node to given new name. Called by Table. */
	public NetNode finishRename(NetNode nn, String newname, String oldname)	{
		if (oldname.equals(newname))
			return nn;	// nothing to do
		
		final TransactionContext context;
		try	{
			context = beginTransaction("Rename");
		}
		catch (UserCancelException e)	{
			return null;
		}
		
		String oldPath = nn.getFullText();
		System.err.println("renaming node >"+oldPath+"< to new name >"+newname+"<");
		
		NetNode n = nn.rename(newname);
		if (n != null)
			setStatus("rename "+oldPath+" "+n.getFullText());
		else
			renderError(nn.getError());
		
		endTransaction(context);
		
		return n;
	}


	/** Rename the node to given new name. Called by TreeModelListener. */
	public NetNode finishRenameTreeNode(NetNode nn, String newname, String oldname)	{
		NetNode newOne = finishRename(nn, newname, oldname);

		if (newOne == null)	{
			if (nn.getErrorCode() == NetNode.EXISTS)
				tree.startEditingAtPath(tree.getSelectionPath());
		}
		else	{
			treepanel.display(nn);	// set info to status bar
		}
		return nn;
	}
	
	
	// cut or copy is pasted by keyboard
	private void finishAction()	{
		if (movingNodes != null)	{
			if (getSelectionCount() != 1)	{
				error("Select Exactly One Container As Move Target!");
				return;
			}
			System.err.println("finishAction: move");
			finishMove();
		}
		else
		if (copiedNodes != null)	{
			if (getSelectionCount() <= 0)	{
				error("Select Container(s) As Copy Target!");
				return;
			}
			System.err.println("finishAction: copy");
			finishCopy();	
		}
		else	{
			error("No Cut Or Copy Happened To Paste");
		}
	}


	void checkClipboard()	{	// frame got focus
		if (transactions > 0)
			return;
			
    Runnable r = new Runnable() {
      public void run() {
        System.err.println("checking system clipoard ...");
        List l = SystemClipboard.getFilesFromClipboard();
        System.err.println("... checked system clipoard, done.");

        if (l != null)  {
          final Vector v = new Vector();
          
          for (Iterator it = l.iterator(); it.hasNext(); )  {
            File f = (File) it.next();
            String [] path = FileUtil.getPathComponents(f, OS.isWindows);
            DefaultMutableTreeNode d = getRoot().localizeNode(path);
            
            if (d != null)  {
              //System.err.println("got copied file from clipboard: "+f);
              v.add(d);
            }
            else  { // files have been moved, their buffered path is wrong in clipboard
              return; // do nothing to avoid inconsistent actions
            }
          }
          
          if (v.size() > 0) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                DefaultMutableTreeNode[] cn = new DefaultMutableTreeNode[v.size()];
                v.copyInto(cn);
                copiedNodes = cn;
                treepanel.getAction("Paste").setEnabled(true);
              }
            });
          }
        }
      }
    };
    new Thread(r).start();
	}
	
	
	private void setNodesToClipboard(DefaultMutableTreeNode [] nodes)	{
		List list = new Vector();
		for (int i = 0; i < nodes.length; i++)	{
			NetNode n = (NetNode)nodes[i].getUserObject();
			File f = (File)n.getObject();
			list.add(f);
		}
		SystemClipboard.setToClipboard(new JavaFileList(list));
	}


	private void beginCopy()	{
		cancelAll();
		//System.err.println("beginCopy instance "+treepanel.getInstance());
		DefaultMutableTreeNode [] nodes = getSelectedTreeNodes();
		
		for (int i = 0; nodes != null && i < nodes.length; i++)	{
			if (nodes[i].isRoot())	{
				error("Can Not Copy Root");
				return;
			}
		}
		
		if (nodes == null || nodes.length <= 0)	{
			error("Select Items To Copy");
		}
		else	{
			copiedNodes = nodes;
			setNodesToClipboard(nodes);
			//setStatus("copy selected items: "+copiedNodes[0].toString()+" ...");
			treepanel.getAction("Paste").setEnabled(true);
			//treepanel.getAction("clear").setEnabled(true);
		}
	}


	private void finishCopy()	{
		finishCopyTo(getSelectedTreeNodes());	// do not get only containers, could be save-copy
	}


	// spawn a background thread and a cancel-dialog	
	private void finishCopyTo(final DefaultMutableTreeNode [] targets)	{	
		System.err.println("finishCopyTo, copied node[0] is now: "+copiedNodes[0]);
		final TransactionContext context;
		try	{
			context = beginTransaction("Copy", copiedNodes);
		}
		catch (UserCancelException e)	{
			return;
		}

		Runnable runnable = new Runnable()	{
			public	void run()	{
				DefaultMutableTreeNode [] sources = cloneArray(copiedNodes);
				//System.err.println("copied node[0] is now: "+copiedNodes[0]);
				boolean isSaveCopy = checkSaveCopy(targets, sources);
				
				try	{
					for (int i = 0; i < targets.length; i++)	{
						finishCopy(targets[i], isSaveCopy == false ? sources : new DefaultMutableTreeNode [] { sources[i] });
					}
				}
				catch (UserCancelException e)	{
				}
				catch (Exception e)	{
					e.printStackTrace();
					fatalError("Copy: "+e.toString());
				}
				finally	{
					endTransaction(context);
				}
			}
		};
		
		context.setObserver(
			observer = new TransactionDialog(
				getCursorComponent(),
				"Copy",
				runnable,
				null,
				getRecursiveSize(copiedNodes) * targets.length)
			);
	}


	private long getRecursiveSize(DefaultMutableTreeNode [] toCopy)	{
		long ret = 0L;
		Component c = getCursorComponent();
		CursorUtil.setWaitCursor(c);
		try	{
			for (int i = 0; i < toCopy.length; i++)	{
				NetNode n = (NetNode)toCopy[i].getUserObject();
				ret += n.getRecursiveSize();
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
		return ret;
	}


	// checks if both lists are of same size and all nodes are equal
	private boolean checkSaveCopy(DefaultMutableTreeNode [] srcs, DefaultMutableTreeNode [] tgts)	{
		int lenSrcs = srcs != null ? srcs.length : -1;
		int lenTgts = tgts != null ? tgts.length : -2;
		if (lenSrcs != lenTgts)
			return false;
		
		for (int i = 0; tgts != null && i < tgts.length; i++)	{
			NetNode tgt = (NetNode)tgts[i].getUserObject();
			NetNode src = (NetNode)srcs[i].getUserObject();
			if (src.equals(tgt) == false)
				return false;
		}
		
		return true;
	}
	

	private void finishCopy(
		DefaultMutableTreeNode target,
		DefaultMutableTreeNode [] sources)
		throws Exception
	{
		for (int i = 0; sources != null && i < sources.length; i++)	{
			finishCopy(target, sources[i]);
		}
	}


	private void finishCopy(
		DefaultMutableTreeNode target,
		DefaultMutableTreeNode source)
		throws Exception
	{
		System.err.println("TreeEditController.finishCopy source "+source+" to target "+target);
		if (target == null || source == null)	{
			System.err.println("WARNUNG: finishCopy, node is null");
			return;
		}

		DefaultMutableTreeNode local = localizeNode(source);
		//System.err.println("localized source is "+local);
		if (local != null)
			source = local;


		NetNode nnew = null;

		NetNode tgt = (NetNode)target.getUserObject();
		NetNode src = (NetNode)source.getUserObject();
		
		if (tgt.equals(src))	{	// copy to itself, produce a clone
			if (target.isRoot() == false)	{
				src.setObserver(observer);
				try	{
					nnew = src.saveCopy();
				}
				finally	{
					src.unsetObserver();
				}
			}
		}
		else	{
			// now ensure that target is a container
			if (tgt.isLeaf())	{	// get container of target leaf
				target = getParentNode(target);
				tgt = (NetNode)target.getUserObject();
			}

			// if target is a descendant of source: error
			if (local != null && source.isNodeDescendant(target))	{
				error(target.toString()+" Is Descendant Of "+source.toString());
				return;
			}
		
			src.setObserver(observer, tgt.getFullText());
			try	{
				nnew = src.copy(tgt);	// copy to another folder
			}
			finally	{
				src.unsetObserver();
			}
		}


		if (nnew != null)	{
			setStatus("copy "+src.getFullText()+" "+nnew.getFullText());
		}
		else	{
			error("Copy "+src.toString()+" To "+tgt.toString()+": "+
					(src.getError() != null ? src.getError() : "")+
					(tgt.getError() != null ? tgt.getError() : ""));
		}
	}




	private void beginMove()	{
		cancelAll();
		movingNodes = getSelectedTreeNodes();
		if (movingNodes == null || movingNodes.length <= 0)	{
			error("Select Items To Move");
			return;
		}

		// gray out moved item, disable for events
		for (int i = 0; i < movingNodes.length; i++)	{
			if (movingNodes[i].isRoot())	{
				error("Can Not Move Root");
				cancelAll();
				return;
			}
			NetNode n = (NetNode)movingNodes[i].getUserObject();
			n.setMovePending(true);
		}

		setNodesToClipboard(movingNodes);
		
		treepanel.getAction("Paste").setEnabled(true);
		//treepanel.getAction("clear").setEnabled(true);
		tree.treeDidChange();
		//setStatus("move selected items: "+movingNodes[0].toString()+" ...");
	}



	private void finishMove()	{
		finishMoveTo(getSelectedContainerTreeNode());	// ensure that target is a container
	}


	private void finishMoveTo(final DefaultMutableTreeNode target)	{
		nodesToSelect.removeAllElements();	// prepare visible selection of moved nodes
		
		final TransactionContext context;
		try	{
			context = beginTransaction("Move", movingNodes);
		}
		catch (UserCancelException e)	{
			return;
		}
		
		Runnable runnable = new Runnable()	{
			public	void run()	{
				DefaultMutableTreeNode [] sources = cloneArray(movingNodes);
				
				try	{
					for (int i = 0; i < sources.length; i++)	{
						DefaultMutableTreeNode d = finishMove(target, sources[i]);
		
						if (d != null && ((NetNode)target.getUserObject()).isExpanded())	{
							nodesToSelect.addElement(d);
						}
					}
				}
				catch (UserCancelException e)	{
				}
				catch (Exception e)	{
					e.printStackTrace();
					fatalError("Move: "+e.toString());
				}
				finally	{
					endTransaction(context);
				}
			}
		};

		Runnable finish = new Runnable()	{
			public	void run()	{
				setSelectionAfterMove();
			}
		};
		
		context.setObserver(
			observer = new TransactionDialog(	// assign temporary to membervar, else synchronize needed!
				getCursorComponent(),
				"Move",
				runnable,
				finish,
				getRecursiveSize(movingNodes))
			);
	}


	private DefaultMutableTreeNode finishMove(
		DefaultMutableTreeNode target,
		DefaultMutableTreeNode source)
		throws Exception
	{
		//System.err.println("finishMove, source = "+src+" target = "+tgt);
		if (target == null || source == null)	{
			System.err.println("WARNUNG: finishMove, node is null");
			return null;
		}

		DefaultMutableTreeNode local = localizeNode(source);	// instance-specific TreeNode
		if (local != null)
			source = local;

		NetNode nnew;
		NetNode src = (NetNode)source.getUserObject();
		NetNode tgt = (NetNode)target.getUserObject();
		
		// if target and source equal or source is in target folder: error
		if (tgt.equals(src) || isIn(target, source))	{
			error("Can Not Move "+src.toString()+" To Itself");
			return null;
		}
		// if target is a descendant of source: error
		if (local != null && source.isNodeDescendant(target))	{
			error(target.toString()+" Is Descendant Of "+source.toString());
			return null;
		}


		// now do the move action on medium
		src.setObserver(observer, tgt.getFullText());
		try	{
			nnew = src.move(tgt);
		}
		catch (UserCancelException e)	{
			return null;
		}
		finally	{
			src.unsetObserver();
		}
				

		// get the new node
		DefaultMutableTreeNode ret = null;

		if (nnew != null)	{	// a child was inserted, search it
			BufferedTreeNode bn = (BufferedTreeNode)target;
			bn.setMarkedForReScan(true);
			ret = bn.searchNode(nnew.getLabel());
			setStatus("move "+src.getFullText()+" "+tgt.getFullText());
		}
		else	{
			error("Move "+src.toString()+" To "+tgt.toString()+": "+
					(src.getError() != null ? src.getError() : "")+
					(tgt.getError() != null ? tgt.getError() : ""));
		}
		
		return ret;
	}


	
	private void setSelectionAfterMove()	{
		//System.err.println(">> run setting selection after move");
		if (nodesToSelect.size() > 0)	{
			boolean cleared = false;

			for (int i = 0; i < nodesToSelect.size(); i++)	{
				DefaultMutableTreeNode dn = (DefaultMutableTreeNode)nodesToSelect.elementAt(i);

				if ((dn = localizeNode(dn)) == null)	{
					System.err.println("FEHLER: failed to set selection in tree"); //+" to "+(new TreePath(dn.getPath())));
					continue;
				}
				if (cleared == false)	{
					cleared = true;
					tree.clearSelection();
				}
				addSelection(dn);
				//System.err.println(" ... adding selection "+(new TreePath(dn.getPath())));
			}
		}
	}




	public void finishDrag()	{
		// Beenden der Popup-Auswahl nach Drag and Drop
		finishDrag(dropNode, draggedNodes, dragCopy);
		//System.err.println("finishDrag(), target "+dropNode+", drag copy "+dragCopy+", draggedNodes[0] "+draggedNodes[0]);
	}


	public void passDraggedNodes(
		DefaultMutableTreeNode dropNode,	// TreeMouseListener ensures that it is a container
		DefaultMutableTreeNode [] draggedNodes,
		boolean dragCopy)
	{
		// dragged nodes kopieren, weil static im MouseListener, werden dort null gesetzt.
		this.dropNode = dropNode;
		this.draggedNodes = cloneArray(draggedNodes);
		this.dragCopy = dragCopy;
		//System.err.println("passDraggedNodes, target "+dropNode+", drag copy "+dragCopy+", dragged node [0] "+draggedNodes[0]);
	}


	/** Beenden des Drag and Drop, konkrete Aktion setzen. Wird vom DndListener gerufen */
	public void finishDrag(
		DefaultMutableTreeNode d,	// TreeMouseListener ensures that it is a container
		DefaultMutableTreeNode [] draggedNodes,
		boolean dragCopy)
	{
		//System.err.println("finishDrag called, dragCopy "+dragCopy+", dragged node[0] "+draggedNodes[0]);
		cancelAll();

		if (dragCopy)	{
			copiedNodes = draggedNodes;
			finishCopyTo(new DefaultMutableTreeNode [] { d });
		}
		else	{	// drag is move
			movingNodes = draggedNodes;
			finishMoveTo(d);
		}
		//System.err.println("finishDrag ended");
	}
		

	/** Anzeigen des Drag and Drop Popup-Menues: Auswahl zwischen copy, move und cancel */
	public void showDragNDropPopup(
		MouseEvent e,
		DefaultMutableTreeNode dropNode,
		DefaultMutableTreeNode [] draggedNodes,
		boolean dragCopy)
	{
		passDraggedNodes(dropNode, draggedNodes, dragCopy);
		int x = mouseComponent != null ? mousePoint.x : e.getX();
		int y = mouseComponent != null ? mousePoint.y : e.getY();
		Component parent = mouseComponent == null ? tree : mouseComponent;
		//System.err.println("showing DnD popup on "+parent.getClass()+", "+x+"/"+y+", dragged node[0] is: "+draggedNodes[0]);
		nodepopup.show(parent, x, y);
		nodepopup.setSelected(dragCopy ? copy : move);	// FRi: TODO: selection kommt nicht!
	}



	private void removeNodes()	{
		deleteNodes(false);
	}

	/** called from wastebasket at drag and drop end */
	public void removeNodes(DefaultMutableTreeNode [] d)	{
		deleteNodes(d, false, true);	// silent remove without GUI selceting nodes
	}


	private boolean checkDeleteNodes()	{
		if (getSelectionCount() <= 0)	{
			error("Select Nodes To Delete");
			return false;
		}
		return true;
	}


	private void deleteNodes()	{	// shift delete callback: unbuffered delete
		if (checkDeleteNodes() == false)	{
			return;
		}
		
		String whichOne = "\n";
		DefaultMutableTreeNode [] d = getSelectedTreeNodes();
		if (d.length <= 10)	{
			for (int i = 0; i < d.length; i++)	{
				whichOne = whichOne+"        "+((NetNode)d[i].getUserObject()).getFullText()+"\n";
			}
		}
		else	{
			whichOne = d.length+" Selected Objects";
		}
			
		int ret = JOptionPane.showConfirmDialog(
						getCursorComponent(),
						" Really Delete "+whichOne+" Without Undo Option?",
						"Definitely Delete?",
						JOptionPane.YES_NO_OPTION);
						
		if (ret != JOptionPane.YES_OPTION)	{
			return;
		}
		
		deleteNodes(true);
	}


	private void deleteNodes(boolean complete)	{
		if (checkDeleteNodes() == false)	{
			return;
		}

		DefaultMutableTreeNode [] d = getSelectedTreeNodes();
		deleteNodes(d, complete);
	}


	private void deleteNodes(
		DefaultMutableTreeNode [] d,
		boolean complete)
	{
		deleteNodes(d, complete, false);
	}


	private void deleteNodes(
		final DefaultMutableTreeNode [] d,
		final boolean complete,
		boolean silent)	// no GUI actions for moving external files to wastebasket
	{
		nodeToSelect = null;

		final TransactionContext context;
		try	{
			context = beginTransaction(complete ? "Delete" : "Remove");
		}
		catch (UserCancelException e)	{
			return;
		}

		Runnable runnable = new Runnable()	{
			public	void run()	{
				try	{
					for (int i = 0; i < d.length; i++)	{
						finishDelete(d[i], complete);
					}
				}
				catch (UserCancelException e)	{
					nodeToSelect = null;
				}
				catch (Exception e)	{
					nodeToSelect = null;
					error("Delete: "+e.toString());
				}
				finally	{
					endTransaction(context);
				}
			}
		};
		
		Runnable finish = null;
		
		if (silent == false)	{
			finish = new Runnable()	{
				public	void run()	{
					if (nodeToSelect != null)
						tree.setSelectionPath(new TreePath(nodeToSelect.getPath()));
				}
			};
		}
		
		context.setObserver(
			observer = new TransactionDialog(
				getCursorComponent(),
				complete ? "Delete" : "Remove",
				runnable,
				finish,
				getRecursiveSize(d))
			);
	}


	// called from background thread
	private boolean finishDelete(
		DefaultMutableTreeNode d,
		boolean complete)
		throws Exception
	{
		if (d.isRoot())	{
			error("Can Not Remove Root");
			return false;	// cannot remove root node
		}
		
		NetNode tgt = (NetNode)d.getUserObject();
		
		nodeToSelect = selectionAfterRemove(d);
		
		boolean ret;
		tgt.setObserver(observer);
		try	{
			if (complete)
				ret = tgt.delete();
			else
				ret = tgt.remove();
		}
		finally	{
			tgt.unsetObserver();
		}
			
		if (ret)	{
			setStatus((complete ? "delete " : "remove ")+tgt.getFullText());
			return true;
		}

		nodeToSelect = null;
		error("Error At Deleting "+tgt+" - "+(tgt.getError() != null ? tgt.getError() : "Files In Use?"));
		return false;
	}




	private void emptyNodes()	{
		if (getSelectionCount() <= 0)	{
			error("Select Container(s) To Empty");
			return;
		}
			
		final DefaultMutableTreeNode [] d = getSelectedTreeNodes();
		
		final TransactionContext context;
		try	{
			context = beginTransaction("Empty", d);
		}
		catch (UserCancelException e)	{
			return;
		}
		
		Runnable runnable = new Runnable()	{
			public	void run()	{
				finishEmpty(d);
				endTransaction(context);
			}
		};
		
		context.setObserver(
			observer = new TransactionDialog(
				getCursorComponent(),
				"Empty Folder",
				runnable,
				null,
				getRecursiveSize(d))
			);
	}


	private void finishEmpty(DefaultMutableTreeNode [] d)	{
		for (int i = 0; i < d.length; i++)	{
			NetNode n = (NetNode)d[i].getUserObject();

			if (n.isLeaf())
				continue;
			
			n.setObserver(observer);
			try	{
				n.empty();
			}
			catch (UserCancelException e)	{
				return;
			}
			catch (Exception e)	{
				error("Empty: "+e.toString());
				return;
			}
			finally	{
				n.unsetObserver();
			}
		}
	}
	



	public NetNode insertObject(boolean folder)	{
		DefaultMutableTreeNode [] d = getSelectedContainerTreeNodes();

		final TransactionContext context;
		try	{
			context = beginTransaction("Insert");
		}
		catch (UserCancelException e)	{
			return null;
		}
		
		NetNode nn = null;
		for (int i = 0; i < d.length; i++)	{
			System.err.println("inserting node into "+d[i]);
			NetNode n = (NetNode)d[i].getUserObject();
			if (folder)	{
				nn = n.createContainer();
				if (nn != null)
					setStatus("mkfile "+nn.getFullText());
			}
			else	{
				nn = n.createNode();
				if (nn != null)
					setStatus("mkdir "+nn.getFullText());
			}
		}
		
		endTransaction(context);
		
		return nn;
	}


	private void insertTreeObject(boolean isFolder)	{
		int i = getSelectionCount();
		if (i <= 0)	{
			error("Select Container(s) To Insert");
			return;
		}

		NetNode nn = insertObject(isFolder);
		
		tree.clearSelection();
		if (nn != null && i == 1)	{	// start editing
			DefaultMutableTreeNode dnew = localizeNode(nn);	// takes new node into TreeNode child list
			if (dnew != null)	{
				final TreePath tp = new TreePath(dnew.getPath());
				tree.setSelectionPath(tp);
				tree.startEditingAtPath(tp);
			}
		}
	}


	private void insertNode()	{
		insertTreeObject(false);
	}
	

	private void insertContainer()	{
		insertTreeObject(true);
	}


	public Long setTime(NetNode n, String newtime)	{
		return n.setModified(newtime);
	}
	

	// determine a point for a open-action-popup
	private void getEventPopupPoint(Object source)	{
		if (source instanceof JButton)	{
			JButton b = (JButton)source;
			Dimension dim = b.getSize();
			openPopupPoint = new Point(dim.width/2, dim.height/2);
			openPopupComponent = b;
		}
		else	{	// Enter key
			//JViewport port = treepanel.getTreeViewport();
			//Rectangle vr = port.getViewRect();
			//openPopupPoint = new Point(vr.width/2, vr.height/2);
			openPopupPoint = mousePoint != null ? mousePoint : new Point(0, 0);
			openPopupComponent = mouseComponent == null ? tree : mouseComponent;
			//System.err.println("openPopupComponent is "+openPopupComponent.getClass());
		}
	}


	// Action open von Toolbar
	public void openNode(Object source)	{
		if (getSelectionCount() <= 0)	{
			error("Select Object(s) To Open");
			return;
		}

		// get a point for a possible open event popup
		getEventPopupPoint(source);
		
		// loop all selected nodes
		DefaultMutableTreeNode [] d = getSelectedTreeNodes();
		Vector v = null, w = null;

		// open container nodes
		for (int i = 0; d != null && i < d.length; i++)	{
			NetNode n = (NetNode)d[i].getUserObject();
			if (n.isLeaf())	{
				if (v == null)
					v = new Vector(d.length);
				v.addElement(n);
			}
			else	{
				TreePath tp = new TreePath(d[i].getPath());
				if (tree.isExpanded(tp) && delegateActive == false)	{
					tree.collapsePath(tp);
				}
				else	{	// expand will be done in openContainers
					if (w == null)
						w = new Vector(d.length);
					w.addElement(n);
				}
			}
		}

		// open all leafs
		if (v != null)	{
			NetNode [] narr = new NetNode[v.size()];
			v.copyInto(narr);
			openNodes(narr, openPopupPoint, openPopupComponent);
		}
		// open all containers
		if (w != null)	{
			NetNode [] narr = new NetNode[w.size()];
			w.copyInto(narr);
			openContainers(narr, openPopupPoint, openPopupComponent, true);
		}
	}


	private void openNodes(NetNode [] n, Point p, Component c)	{
		new OpenLauncher(getCursorComponent(), this, n, getOpenCommands(), p, c);
	}

		
	private boolean openContainers(NetNode[] n, Point p, Component c)	{
		return openContainers(n, p, c, false);
	}
	
	private boolean openContainers(NetNode[] n, Point p, Component c, boolean expand)	{
		//System.err.println("openContainers "+n[0]+", expand "+expand);
		OpenLauncher launcher = new OpenLauncher(
				getCursorComponent(),
				this,
				n,
				getOpenCommands(),
				p,
				c,
				true);
		boolean success = launcher.succeeded();




		// if undefined nodes and expansion is anted, do it
		if (expand)	{
			Vector v = launcher.getUndefinedNodes();
			
			if (v != null && v.size() > 0)	{
				if (delegateActive)	{
					NetNode [] nodes = new NetNode [v.size()];
					v.copyInto(nodes);
					infoDialog(nodes);
				}
				else	{

					for (int i = 0; v != null && i < v.size(); i++)	{
						NetNode nn = (NetNode)v.elementAt(i);
						//System.err.println("open container, expanding "+nn.getFullText());
						treepanel.explorePath(nn.getPathComponents());
					}
				}
			}
		}
		return success;
	}


	/** A container node will get expanded */
	public boolean treeWillExpandEvent(DefaultMutableTreeNode d)	{
		getEventPopupPoint(null);
		NetNode [] n = new NetNode[] { (NetNode)d.getUserObject() };
		return openContainers(n, openPopupPoint, openPopupComponent);
	}

	/** Doppelklick vom MouseListener */
	public void openNode(MouseEvent e)	{
		NetNode n = getNodeFromMouseEvent(e);
		if (n != null && n.isLeaf())	{	// folders will expand by default on double click
			NetNode [] narr = new NetNode[] { n };
			Component c = getCursorComponent();
			CursorUtil.setWaitCursor(c);
			try	{
				openNodes(narr, e.getPoint(), tree);
			}
			finally	{
				CursorUtil.resetWaitCursor(c);
			}
		}
	}

	
	/** edit open events for all selected nodes */
	public void editOpenEventsForNodes()	{
		Component c = getCursorComponent();
		CursorUtil.setWaitCursor(c);
		try	{
			if (getSelectionCount() <= 0)	{
				OpenEventEditor.construct(getOpenCommands());
			}
			else	{
				NetNode [] n = getSelectedNodes();
				String [] sarr = new String [n.length];
				boolean [] barr = new boolean [n.length];
				for (int i = 0; i < n.length; i++)	{
					sarr[i] = n[i].isLeaf() ? n[i].getLabel() : n[i].getFullText();
					barr[i] = n[i].isLeaf();
				}
				OpenEventEditor.construct(getOpenCommands(), sarr, barr);
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
	}
	

	private void expandRecursive()	{
		// loop all selected container nodes
		DefaultMutableTreeNode [] d = getSelectedContainerTreeNodes();
		for (int i = 0; d != null && i < d.length; i++)	{
			expandRecursive(d[i]);
		}
	}
	
	private void expandRecursive(DefaultMutableTreeNode d)	{
		TreePath tp = new TreePath(d.getPath());
		NetNode n = (NetNode)d.getUserObject();
		if (n.isLink() == false)	{
			tree.expandPath(tp);
			int cnt = d.getChildCount();
			for (int i = 0; i < cnt; i++)	{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)d.getChildAt(i);
				if (child.getAllowsChildren())
					expandRecursive(child);
			}
		}
	}


	private void compressNodes(String kind)	{
		String filter = treepanel.getFilter();
		boolean include = treepanel.getInclude();
		boolean showfiles = treepanel.getShowFiles();
		boolean showhidden = treepanel.getShowHidden();
		try	{
			filter = confirmFiltering(kind, filter, include);
		}
		catch (UserCancelException e)	{
			return;
		}

		NetNode [] n = getSelectedNodes();
		try	{
			if (filter != null)	{
				System.err.println("applying filter "+filter);
				if (kind.equals("zip"))
					new ObservedZipWrite(getCursorComponent(), n, filter, include, showfiles, showhidden);
				else
				if (kind.equals("jar"))
					new ObservedJarWrite(getCursorComponent(), n, filter, include, showfiles, showhidden);
				else
				if (kind.startsWith("tar"))
					new ObservedTarWrite(getCursorComponent(), n, kind.endsWith("bz2"), filter, include, showfiles, showhidden);
			}
			else	{
				if (kind.equals("zip"))
					new ObservedZipWrite(getCursorComponent(), n);
				else
				if (kind.equals("jar"))
					new ObservedJarWrite(getCursorComponent(), n);
				else
				if (kind.startsWith("tar"))
					new ObservedTarWrite(getCursorComponent(), n, kind.endsWith("bz2"));
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
			error("Write to "+kind+": "+e);
		}
	}


	private void splitJoinNodes()	{
		NetNode n = getSelectedNodes()[0];
		boolean isSplit = n.isLeaf();
		
		if (isSplit == false)	{
			try	{
				FileJoin fj = new FileJoin((File)n.getObject());
				Vector v = fj.list();
			
				if (v == null || v.size() <= 0)	{
					throw new IOException("No Split Files Found In \""+n+"\".");
				}
			}
			catch (IOException e)	{
				error(e.getMessage());
				return;
			}
		}

		JFrame f = new JoinSplitFile(n, isSplit).showInFrame();
		setApplicationIcon(f);
	}


	/** load selected nodes into an viewer */
	public void viewNodes()	{
		Component c = getCursorComponent();
		CursorUtil.setWaitCursor(c);
		try	{
			NetNode [] n = getSelectedLeafNodes();
			viewNodes(n);
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
	}

	/** load nodes into an viewer */
	public void viewNodes(NetNode [] n)	{
		if (n.length <= 0)
			new FileViewer((File)null);
		else
			for (int i = 0; i < n.length; i++)
				new FileViewer((File)n[i].getObject());
	}
	

	/** load selected nodes into an viewer */
	public void viewNodesRichText()	{
		Component c = getCursorComponent();
		CursorUtil.setWaitCursor(c);
		try	{
			NetNode [] n = getSelectedLeafNodes();
			viewNodesRichText(n);
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
	}
	
	/** load selected nodes into an viewer */
	public void viewNodesRichText(NetNode [] n)	{
		if (n.length <= 0)
			new FileViewerRichText(null);
		else
			for (int i = 0; i < n.length; i++)
				new FileViewerRichText(n[i].getObject());
	}


	private File [] selectionToFileArray()	{
		NetNode [] n = getSelectedNodes();
		File [] farr = new File [n.length];
		for (int i = 0; n != null && i < n.length; i++)	{
			farr[i] = (File)n[i].getObject();
		}
		return farr;
	}
	
	private void linecount()	{
		new LineCount(selectionToFileArray());
	}

	private void concordance()	{
		File [] files = selectionToFileArray();

		if (files != null && files.length > 0)	{
			int ret = JOptionPane.showConfirmDialog(
				getCursorComponent(),
				"Ensure that the selected files and folders are not too big.\n"+
						"Concordance Search reads all lines of all files into memory!\n\n"+
						"Do you really want to start?",
				"Concordance Search Starting",
				JOptionPane.YES_NO_OPTION);

			if (ret != JOptionPane.YES_OPTION)
				return;
		}

		new ConcordanceFrame(files);
	}

	private void diff(boolean doFiles)	{
		NetNode [] n = getSelectedNodes();
		Vector v = new Vector();
		for (int i = 0; i < n.length; i++)	{
			if (doFiles && n[i].isLeaf() || !doFiles && !n[i].isLeaf())
				v.addElement(n[i].getObject());
		}
		
		String filter = treepanel.getFilter();
		boolean include = treepanel.getInclude();
		if (doFiles == false && v.size() >= 2)	{
			try	{
				filter = confirmFiltering("diff", filter, include);
			}
			catch (UserCancelException e)	{
				return;
			}
		}

		if (v.size() >= 2)	{
			// sort the files by name to get a chance to compare analogous files
			Comparator comparer = new Comparator()	{
				public int compare(Object o1, Object o2)	{ return ((File)o1).getName().compareTo(((File)o2).getName()); }
				public boolean equals(Object obj)	{ return false; }
			};
			v = new QSort(comparer).sort(v);
			
			for (int i = 0; i < v.size() - 1; i += 2)	{
				File file1 = (File)v.get(i);
				File file2 = (File)v.get(i + 1);

				if (doFiles)
					new FileDiffFrame(file1, file2);
				else
				if (filter == null)
					new DirDiffFrame(file1, file2);
				else
					new DirDiffFrame(file1, file2, filter, include);
			}
		}
		else	{
			if (doFiles)
				new FileDiffFrame();
			else
			if (filter == null)
				new DirDiffFrame();
			else
				new DirDiffFrame(filter, include);
		}
	}


	private void viewNodesImages()	{
		File [] f = leafsToFileArray(getSelectedLeafNodes());
		if (f != null && f.length > 0)
			ImageViewer.showImages(f);
	}
	
	
	private void playSound()	{
		File [] f = leafsToFileArray(getSelectedLeafNodes());
		boolean done = false;

		for (int i = 0; done == false && f != null && i < f.length; i++)	{
			SoundPlayerFrame.singleton().start(f[i].getAbsolutePath());
			done = true;
		}

		if (!done)
			SoundPlayerFrame.singleton();	// opens empty window
	}

	

	private File [] leafsToFileArray(NetNode [] n)	{
		File [] files = null;
		Vector v = new Vector();

		for (int i = 0; i < n.length; i++)	{
			if (n[i].isLeaf())
				v.addElement(n[i].getObject());
		}

		if (v.size() > 0)	{
			files = new File [v.size()];
			v.copyInto(files);
		}

		return files;
	}

	/** Show wait cursor and load selected nodes into MDI editor */
	public void editNodeObjects()	{
		Component c = getCursorComponent();
		CursorUtil.setWaitCursor(c);
		try	{
			editNodeObjects(getSelectedLeafNodes());
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
	}
	
	/** Load passed nodes into MDI editor */
	public void editNodeObjects(NetNode [] n)	{
		getEditor(leafsToFileArray(n));
	}

	/** Show an editor with passed object(s) */
	public static EditorFrame getEditor(Object toOpen)	{
		boolean justCreated = false;
		if (editor == null)	{
			justCreated = true;
			editor = EditorFrame.singleton();
		}
		handleEditor(editor, toOpen, justCreated);
		return editor;
	}
	
	private static void handleEditor(EditorFrame editor, Object toOpen, boolean justCreated)	{
		if (toOpen == null && !justCreated)
			editor.setVisible(true);
		else
		if (toOpen instanceof File)
			editor.addWindow((File)toOpen);
		else	// must be File[] array
			editor.addWindows((File[])toOpen);
	}
	

	private void hexEditNodeObjects()	{
		Component c = getCursorComponent();
		CursorUtil.setWaitCursor(c);
		try	{
			File [] files = leafsToFileArray(getSelectedLeafNodes());
			boolean justCreated = false;
			if (hexeditor == null)	{
				justCreated = true;
				hexeditor = HexEditorFrame.singleton();
			}
			handleEditor(hexeditor, files, justCreated);
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
	}

	public void xmlEditNodeObjects(NetNode [] nodes)	{
		File [] files = leafsToFileArray(nodes);
		xmlEditNodeObjects(files);
	}

	private void xmlEditNodeObjects()	{
		File [] files = leafsToFileArray(getSelectedLeafNodes());
		xmlEditNodeObjects(files);
	}
	
	private void xmlEditNodeObjects(File [] files)	{
		Component c = getCursorComponent();
		CursorUtil.setWaitCursor(c);
		try	{
			String [] uris = null;
			if (files != null && files.length > 0)	{
				uris = new String[files.length];
				for (int i = 0; i < files.length; i++)
					uris[i] = files[i].getPath();
			}
	
			XmlEditor.singleton(uris);
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
	}


	private void tail()	{
		NetNode [] n = getSelectedLeafNodes();
		for (int i = 0; i < n.length; i++)
			new TailFrame((File)n[i].getObject());
		if (n.length <= 0)
			new TailFrame();
	}
	
	
	private void folderWatcher()	{
		DefaultMutableTreeNode [] n = getSelectedContainerTreeNodes();
		if (n.length > 0)	{
			int ret = JOptionPane.showConfirmDialog(
				getCursorComponent(),
				"Ensure that the selected folders are not too big.\n"+
						"This is a recursive watcher that scans all subdirectories!\n\n"+
						"Do you really want to start?",
				"Folder Watcher Starting",
				JOptionPane.YES_NO_OPTION);
	
			if (ret != JOptionPane.YES_OPTION)
				return;

			File [] farr = new File [n.length];
			for (int i = 0; i < n.length; i++)
				farr[i] = (File)((NetNode)n[i].getUserObject()).getObject();

			new FolderMonitorFrame(farr);
		}
		else	{
			new FolderMonitorFrame(null);
		}
	}

	
	
	private void about()	{
		long m = Runtime.getRuntime().maxMemory();
		long f = Runtime.getRuntime().freeMemory();
		long t = Runtime.getRuntime().totalMemory();
		String max = NumberUtil.getFileSizeString(m);
		String free = NumberUtil.getFileSizeString(f);
		String total = NumberUtil.getFileSizeString(t);
		String used = NumberUtil.getFileSizeString(t - f);
		JOptionPane.showMessageDialog(
				getCursorComponent(),
				"File Browser "+FileBrowser.version+"\n"+
					"Author Fritz Ritzberger 1999-2024\n"+
					"Maximum Memory:  "+max+"\n"+
					"Currently: "+total+"\n"+
					"Used: "+used+"\n"+
					"Free:  "+free,
				"About ...",
				JOptionPane.INFORMATION_MESSAGE,
				GuiApplication.getLogoIcon());
	}
	
	
	/** set enabled or disabled all members of popup menu according to context */
	public void setEnabledActions()	{
		//System.err.println("setEnabledActions()");
		if (getSelectionCount() <= 0)	{
			setDisabledPopup();
		}
		else	{
			NetNode [] nn = getSelectedNodes();
			boolean leaf = true;
			boolean cancreate = true;
			boolean manipulable = true;
			boolean iswasted = true;
			boolean iszip = nn.length == 1 && ArchiveFactory.isArchive((File)nn[0].getObject());
			for (int i = 0; i < nn.length; i++)	{
				if (nn[i].canCreateChildren() == false)	// keine children ("Computer")
					cancreate = false;
				if (nn[i].isManipulable() == false)	// nicht copy/move/delete/rename-able
					manipulable = false;
				if (!nn[i].underWastebasket())
					iswasted = false;
				if (!nn[i].isLeaf())
					leaf = false;
			}
			
			//System.err.println("set enabled popup, delegate is "+delegateActive);
			if (delegateActive)	{	// a table passed selection
				enableDelegateNewMenu();
			}
			else	{	// tree selection
				treepanel.getAction("File").setEnabled(cancreate);
				treepanel.getAction("Folder").setEnabled(cancreate);
				newMenu.setEnabled(cancreate);
			}
			
			sortMenu.setEnabled(nn.length == 1 && !nn[0].isLeaf());
			
			treepanel.getAction("Delete").setEnabled(manipulable);
			treepanel.getAction("Remove").setEnabled(manipulable && !iswasted);
			treepanel.getAction("Empty").setEnabled(manipulable && cancreate);
			delMenu.setEnabled(manipulable);
			
			if (delegateActive)	{
				treepanel.getAction("Rename").setEnabled(manipulable && renamer != null);
			}
			else	{
				treepanel.getAction("Rename").setEnabled(manipulable && nn.length == 1 && !iswasted);
			}

			treepanel.getAction("Compress").setEnabled(manipulable && !iszip);
			treepanel.getAction("Split / Join").setEnabled((cancreate || leaf) && nn.length == 1);

			treepanel.getAction("Cut").setEnabled(manipulable);
			treepanel.getAction("Copy").setEnabled(manipulable);
			treepanel.getAction("Paste").setEnabled(
					(copiedNodes != null || movingNodes != null) && (manipulable || cancreate));
			//treepanel.getAction("clear").setEnabled(copiedNodes != null || movingNodes != null);

			treepanel.getAction("Info").setEnabled(true);
			treepanel.getAction("Open").setEnabled(true);
			//treepanel.getAction("Refresh").setEnabled(true);
			
			try	{
				treepanel.getAction("Image").setEnabled(leaf);
			}
			catch (NullPointerException e)	{	// is not present for FileChooser
			}
		}
	}


	private void setDisabledPopup()	{
		treepanel.getAction("File").setEnabled(false);
		treepanel.getAction("Folder").setEnabled(false);
		treepanel.getAction("Open").setEnabled(false);
		treepanel.getAction("Rename").setEnabled(false);
		treepanel.getAction("Delete").setEnabled(false);
		treepanel.getAction("Empty").setEnabled(false);
		treepanel.getAction("Cut").setEnabled(false);
		treepanel.getAction("Copy").setEnabled(false);
		treepanel.getAction("Paste").setEnabled(false);
		treepanel.getAction("Info").setEnabled(false);
		treepanel.getAction("Remove").setEnabled(false);
		treepanel.getAction("Compress").setEnabled(false);
		treepanel.getAction("Split / Join").setEnabled(false);
		try	{
			treepanel.getAction("Image").setEnabled(false);
		}
		catch (NullPointerException e)	{	// is not present for FileChooser
		}

		if (delegateActive)
			enableDelegateNewMenu();
		else
			newMenu.setEnabled(false);

		delMenu.setEnabled(false);
		sortMenu.setEnabled(false);
		//treepanel.getAction("clear").setEnabled(false);
	}

	private void enableDelegateNewMenu()	{
		boolean b = 
				inserter != null &&
				delegateFolder != null &&
				((NetNode)delegateFolder.getUserObject()).canCreateChildren();
		System.err.println("setting newMenu to "+b);
		treepanel.getAction("File").setEnabled(b);
		treepanel.getAction("Folder").setEnabled(b);
		newMenu.setEnabled(b);
	}
	

	/** actions auf einem oder mehreren selektierten Items ausfuehren */
	public void showActionPopup(MouseEvent e, Component c)	{
		Point p = BugFixes.computePopupLocation(e, c, popup);
		showActionPopup(p.x, p.y, c);
	}
	
	
	private void showActionPopup(int x, int y, Component c)	{
		popup.show(x, y, c);
	}


	public void setMousePoint(Point p, Component comp)	{
		//System.err.println("TreeEditController.setMousePoint()");
		mousePoint = new Point(p);
		mouseComponent = comp;
	}

	public Point getMousePoint()	{
		return mousePoint;
	}

}