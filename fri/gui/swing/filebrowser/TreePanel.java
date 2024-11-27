package fri.gui.swing.filebrowser;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import fri.util.NumberUtil;
import fri.util.os.OS;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.combo.ConstantHeightComboBox;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.undo.DoAction;
import fri.gui.swing.text.ClipableJTextField;
import fri.gui.swing.toolbar.ScrollablePopupToolbar;
import fri.gui.swing.actionmanager.ActionManager;
import fri.gui.swing.iconbuilder.Icons;

/**
	Main-Panel des File Explorer. Dieses haelt Toolbars und einen
	Datei-Baum, der seine Knoten erst dann mit Kind-Knoten fuellt,
	wenn sie expandiert werden.
*/

public class TreePanel extends JPanel implements
	TreeSelectionListener,
	TreeExpansionListener,
	TreeWillExpandListener,
	TreeModelListener,
	TreeExpander
{
	// basic variables
	private static NetNode globalRoot;
	private static JTextArea montext;	// execution protocol
	private static Monitor monitor;
	private static ActionManager gam;
	private JFrame parent;
	private boolean dialogMode = false;
	private PathPersistent pp;
	private ActionManager am;
	private BufferedTreeNode root;
	private DefaultTreeModel model;	// this view of NetNodes
	private JTree tree;
	private JViewport treeViewport;
	private NetNode currNetNode;
	private BufferedTreeNode currTreeNode;
	private TreeEditController tc;
	private PathHistoryCombo pathTextField;
	private JButton filterbutton;
	private HistCombo cmb_filter;
	private JComboBox cmb_include;
	private JCheckBox ckb_showfiles, ckb_hidden;
	private int sortFlag = NetNode.SORT_DEFAULT;
	private JMenu sortMenu, newMenu, delMenu;
	private JMenuItem byname, bysize, bytime, byext;
	private boolean scanning = false;
	private String [][] persistSelected;
	private TreeEditPopup popup;
	// extended variables
	private static DefaultButtonModel autoRefresh = null;
	private JCheckBox ckb_dropmenu, ckb_refresh;
	private JTextField statusText;
	private Clock clock;
	private JSlider sl_speed;
	private AbstractButton customize;
	private String oldFilter;


	/**
		Anlegen einer TreeView mit dem uebergebenen Knoten
		als obersten Knoten (root).
		@param userobject Knoten, der oberster werden soll (root).
	*/
	public TreePanel(JFrame parent, NetNode rootObject, PathPersistent pp)	{
		this.parent = parent;
		this.pp = pp;

		BufferedTreeNode.initing = true;	// read selection from persistent data

		BufferedTreeNode root = new BufferedTreeNode(rootObject, this);
		model = new DefaultTreeModel(root);
		
		init(model);
	}

	/**
		Anlegen einer TreeView mit einem vorgefertigten TreeModell.
		Dieser Konstruktor wird zur Erzeugung von Dialog-Fenstern verwendet.
		@see fri.gui.swing.filebrowser.FileChooser
	*/
	TreePanel(NetNode rootObject, PathPersistent pp, String filter)	{
		this.pp = pp;
		this.dialogMode = true;

		BufferedTreeNode.initing = true;	// read selection from persistent data

		BufferedTreeNode root = new BufferedTreeNode(rootObject, this);
		model = new DefaultTreeModel(root);
		
		// make file filter work from begin
		root.setSelected(true);
		
		init(model);
		
		setFilter(filter);
		
		expandiere(pp.getSelected(), pp.getPathes());
	}
	
	/**
		Anlegen einer TreeView mit einem vorgefertigten TreeModell.
		Dieser Konstruktor wird zur Erzeugung neuer Explorer-Fenster verwendet,
		die das gesamte FileSystem anzeigen.
		@param model das TreeModel.
	*/
	TreePanel(JFrame parent, DefaultTreeModel model, PathPersistent pp)	{
		this.parent = parent;
		this.pp = pp;
		init(TreeModelClone.cloneTreeModel(model, this));
	}




	// Aufbau des JTree und der Navigations-Hilfen
	private void init(DefaultTreeModel m)	{
		setLayout(new BorderLayout());	// sizing of panel sizes tree
			
		// install tree
		model = m;
		model.setAsksAllowsChildren(true);
		root = (BufferedTreeNode)model.getRoot();
		//root.setAllowsChildren(true);
		
		globalRoot = (NetNode)root.getUserObject();
		
		if (this.pp != null)	{
			this.pp.setRoot(getRootNode().getLabel());
			this.sortFlag = pp.sortFlag;
		}
				
		tree = new JFileTree(model);	
		JScrollPane treeScrollPane = new JScrollPane(tree);
		treeViewport = treeScrollPane.getViewport();

		FileTreeCellRenderer rend = new FileTreeCellRenderer();
		tree.setCellRenderer(rend);
		
		NodeCellEditor edi = new NodeCellEditor(tree, (DefaultTreeCellRenderer)tree.getCellRenderer());
		tree.setCellEditor(edi);

		tree.addTreeSelectionListener(this);
		tree.addTreeExpansionListener(this);
		tree.addTreeWillExpandListener(this);

		// as JTree does not accept ENTER in its ActionMap we listen to enter: "Open" when leafs
		tree.addKeyListener(new KeyAdapter()	{
			public void keyPressed(KeyEvent e)	{
				if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0 &&
					(e.getModifiers() & InputEvent.CTRL_MASK) == 0 &&
					(e.getModifiers() & InputEvent.ALT_MASK) == 0)
				{
					if (e.getKeyCode() == KeyEvent.VK_ENTER)	{
						DefaultMutableTreeNode [] sel = tc.getSelectedTreeNodes();
						boolean allLeafs = true;
						for (int i = 0; allLeafs && i < sel.length; i++)
							allLeafs = sel[i].isLeaf();
							
						if (allLeafs)	{
							ActionEvent evt = new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, "Open");
							tc.actionPerformed(evt);
						}
					}
					else	{
						changeSelectionByKeypress(e.getKeyChar());
					}
				}
			}
		});
		
		tree.setEditable(true);

		model.setAsksAllowsChildren(true);
		model.addTreeModelListener(this);	// rename event



		// create edit controller object
		tc = new TreeEditController(this);
		System.err.println("allocating TreeEditController "+tc.hashCode()+" in TreePanel "+hashCode());

		// Actions
		am = new ActionManager(tree, tc);
		buildFileActions();	// needed for popup
		if (dialogMode == false)	{
			buildServiceActions();
		}

		// build navigation helpers
		JToolBar tbFilter = buildNavigationToolBar();
		if (dialogMode == false)	{
			buildMoreNavigationOptions(tbFilter);
		}
		// init edit controller object with filter controls
		tc.setFilterAction(cmb_filter, cmb_include, ckb_showfiles, ckb_hidden);


		if (dialogMode == false)	{
			statusText = buildStatusText();
			clock = buildClock();
			sl_speed = buildAutoScrollSpeedSlider();	// autoscroll speed slider
		}

		// Pass status controls to controller
		tc.setErrorRenderer(statusText);

		// pathtextfield
		pathTextField = buildPathHistoryCombo();

		// Build together all

		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(pathTextField, BorderLayout.NORTH);
		p1.add(treeScrollPane, BorderLayout.CENTER);	// the scrollable tree
		
		// File Actions
		JToolBar tbFileActions = null;
		if (dialogMode == false)	{
			tbFileActions = buildFileActionToolBar();
			tbFileActions.setFloatable(false);
			if (OS.isAboveJava13) tbFileActions.setRollover(true);
		}
		popup = buildTreeEditPopup();
		// Pass popup to controller for showing
		tc.setPopupMenu(popup, newMenu, sortMenu, delMenu);

		// Additional keyboard actions
		registerAdditionalKeyPresses();

		// Further Components
		JToolBar tbServices = null;
		if (dialogMode == false)	{
			tbServices = buildServiceToolBar();
			if (OS.isAboveJava13) tbServices.setRollover(true);
			tbServices.setFloatable(false);
		}

		
		if (statusText != null)	{
			JComponent toAdd = statusText;
			if (clock != null)	{
				JPanel p11 = new JPanel();
				p11.setLayout(new BoxLayout(p11, BoxLayout.X_AXIS));
				p11.add(statusText);
				p11.add(Box.createHorizontalGlue());
				p11.add(clock);
				JPanel p3 = new JPanel(new BorderLayout());
				p3.add(sl_speed, BorderLayout.NORTH);
				p3.add(p11, BorderLayout.CENTER);
				toAdd = p3;
			}
			//p1.add(toAdd, BorderLayout.SOUTH);
			ScrollablePopupToolbar tbSouth = new ScrollablePopupToolbar(p1, true, SwingConstants.BOTTOM);
			tbSouth.getToolbar().add(toAdd);
			//p1.add(tbSouth, BorderLayout.SOUTH);
		}
		
		tbFilter.setFloatable(false);
		add(tbFilter, BorderLayout.NORTH);
		
		add(p1, BorderLayout.CENTER);

		// not necessary to add toolbars, as they are PopupToolbars
		//if (tbFileActions != null)	{
		//	add(tbFileActions, BorderLayout.WEST);
		//}
		//if (tbServices != null)	{
		//	add(tbServices, BorderLayout.EAST);
		//}
	}



	private JToolBar buildNavigationToolBar()	{
		JToolBar tbFilter = new JToolBar(SwingConstants.HORIZONTAL);
		if (OS.isAboveJava13) tbFilter.setRollover(true);

		if (dialogMode == true)	{
			if (OS.isWindows)
				am.visualizeAction("Floppy", tbFilter);
			
			buildHomeAction();
			am.visualizeAction("Home", tbFilter);
			am.visualizeAction("Folder", tbFilter);
			tbFilter.addSeparator();
		}
		
		filterbutton = new JButton("Filter");
		filterbutton.setBorderPainted(false);
		filterbutton.setActionCommand("Filter");
		filterbutton.addActionListener(tc);
		filterbutton.setAlignmentY(CENTER_ALIGNMENT);
		filterbutton.setAlignmentX(CENTER_ALIGNMENT);
		filterbutton.setToolTipText("Filter Selected Folder(s)");
		tbFilter.add(filterbutton);

		if (dialogMode == false)	{	// not dialog
			cmb_filter = new FileNameComboBox();

			if (((FileNameComboBox)cmb_filter).isLoaded() == false)	{
				((FileNameComboBox)cmb_filter).setItems(new String [] {"*", ".*", "*.*"});
			}

			new FilterTextDndListener(cmb_filter.getTextEditor(), (FileNameComboBox)cmb_filter);
		}
		else	{	// allocate Combo with separate history
			cmb_filter = new HistCombo("*")	{
				public Dimension getMinimumSize()	{
					return new Dimension(70, super.getMinimumSize().height);
				}
			};
		}
		cmb_filter.setAlignmentX(CENTER_ALIGNMENT);
		cmb_filter.setAlignmentY(CENTER_ALIGNMENT);
		cmb_filter.setToolTipText("Filter Pattern. Drop Here To Create File Patterns.");
		tbFilter.add(cmb_filter);
		// store filter text to modification indicator
		oldFilter = cmb_filter.getText();

		cmb_include = new ConstantHeightComboBox()	{
			public Dimension getMaximumSize()	{
				return new Dimension(70, super.getMaximumSize().height);
			}
		};
		cmb_include.setMinimumSize(new Dimension(70, cmb_include.getMinimumSize().height));
		cmb_include.setEditable(false);
		cmb_include.addItem("Include");
	 	cmb_include.addItem("Exclude");
		String incl = (pp != null && pp.exclude) ? "Exclude" : "Include";
		cmb_include.setSelectedItem(incl);	// associated with filter setting!!!
		//cmb_include.setSelectedItem("Include");
		//System.err.println("ex/include is set to: "+cmb_include.getSelectedItem());
		cmb_include.setAlignmentY(CENTER_ALIGNMENT);
		cmb_include.setAlignmentX(CENTER_ALIGNMENT);
		cmb_include.setToolTipText("Filter Works In- Or Excluding");
		tbFilter.add(cmb_include);

		tbFilter.addSeparator();

		ckb_showfiles = new JCheckBox("Files");
		ckb_showfiles.setActionCommand("Showfiles");
		ckb_showfiles.setSelected(pp != null ? pp.showfiles : true);
		//System.err.println("showfiles is set to: "+ckb_showfiles.isSelected());
		ckb_showfiles.setAlignmentY(CENTER_ALIGNMENT);
		ckb_showfiles.setAlignmentX(CENTER_ALIGNMENT);
		ckb_showfiles.setToolTipText("Show Folders With Files Or Folders Only");
		tbFilter.add(ckb_showfiles);
		
		ckb_hidden = new JCheckBox("Hidden");
		ckb_hidden.setActionCommand("Showhidden");
		ckb_hidden.setSelected(pp != null ? pp.showhidden : false);
		ckb_hidden.setAlignmentY(CENTER_ALIGNMENT);
		ckb_hidden.setAlignmentX(CENTER_ALIGNMENT);
		ckb_hidden.setToolTipText("Show Hidden Folders And Files");
		tbFilter.add(ckb_hidden);

		return tbFilter;
	}
	
	
	private void buildMoreNavigationOptions(JToolBar tbFilter)	{
		if (autoRefresh == null)	{
			autoRefresh = new DefaultButtonModel();
			autoRefresh.setSelected(pp != null ? pp.refresh : false);
		}
		ckb_refresh = new JCheckBox("Refresh");
		ckb_refresh.setModel(autoRefresh);
		ckb_refresh.setActionCommand("Auto-Refresh");
		ckb_refresh.addActionListener(tc);
		ckb_refresh.setAlignmentY(CENTER_ALIGNMENT);
		ckb_refresh.setAlignmentX(CENTER_ALIGNMENT);
		ckb_refresh.setToolTipText("Switch On/Off Auto-Refresh");
		tbFilter.add(ckb_refresh);

		ckb_dropmenu = new JCheckBox("Drop Menu");
		ckb_dropmenu.setActionCommand("Dropmenu");
		ckb_dropmenu.setSelected(pp != null ? pp.dropmenu : true);
		ckb_dropmenu.setAlignmentY(CENTER_ALIGNMENT);
		ckb_dropmenu.setAlignmentX(CENTER_ALIGNMENT);
		ckb_dropmenu.setToolTipText("Use Popup Menu For Drag And Drop");
		tbFilter.add(ckb_dropmenu);
		tc.setUseDropMenuCheckBox(ckb_dropmenu);
	}


	private JTextField buildStatusText()	{	
		JTextField statusText = new ClipableJTextField();
		statusText.setEditable(false);
		return statusText;
	}


	public JTextArea ensureLogMonitor()	{	
		if (dialogMode == false && monitor == null)
			monitor = new Monitor(parent, montext = new JTextArea());
		return montext;
	}
	
	
	private void buildFileActions()	{
		if (OS.isWindows)	{
			am.registerAction("Floppy", Icons.get(Icons.save), "Explore Floppy Disk");		
		}
		am.registerAction("Open", Icons.get(Icons.openFolder), "Open Selection", KeyEvent.VK_ENTER, 0);		
		am.registerAction("Info", Icons.get(Icons.info), "File, Folder And Archive Information", KeyEvent.VK_ENTER, InputEvent.ALT_MASK);
		am.registerAction("Find", Icons.get(Icons.find), "Find Files And Folders", KeyEvent.VK_F, InputEvent.CTRL_MASK);
		am.registerAction("Refresh", Icons.get(Icons.refresh), "Refresh Selection", KeyEvent.VK_F5, 0);
		am.registerAction("File", Icons.get(Icons.newDocument), "Create New File", KeyEvent.VK_INSERT, 0);
		am.registerAction("Folder", Icons.get(Icons.newFolder), "Create New Folder", KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK);
		am.registerAction("Rename", Icons.get(Icons.fieldEdit), "Rename Selection", KeyEvent.VK_F2, 0);
		am.registerAction("Remove", Icons.get(Icons.remove), "Remove Selection To Wastebasket", KeyEvent.VK_DELETE, 0);
		am.registerAction("Delete", Icons.get(Icons.delete), "Delete Selection Without Undo Option", KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK);
		am.registerAction("Empty", Icons.get(Icons.empty), "Empty Selection Recursive (Filtered)", KeyEvent.VK_DELETE, InputEvent.ALT_MASK);
		am.registerAction("Cut", Icons.get(Icons.cut), "Cut Selection To Clipboard (Filtered)", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		am.registerAction("Copy", Icons.get(Icons.copy), "Copy Selection To Clipboard (Filtered)", KeyEvent.VK_C, InputEvent.CTRL_MASK);
		am.registerAction("Paste", Icons.get(Icons.paste), "Paste Clipboard To Selection", KeyEvent.VK_V, InputEvent.CTRL_MASK);
		// special global actions for undo
		if (gam == null)	{	// not yet globally created
			//System.err.println("creating new undo action");
			gam = new ActionManager(getTree(), tc);
			ObservableDoAction undo = new ObservableDoAction(DoAction.UNDO);
			ObservableDoAction redo = new ObservableDoAction(DoAction.REDO);
			gam.registerAction(undo, Icons.get(Icons.undo), "Undo Previous Action", KeyEvent.VK_Z, InputEvent.CTRL_MASK);
			gam.registerAction(redo, Icons.get(Icons.redo), "Redo Undone Action", KeyEvent.VK_Y, InputEvent.CTRL_MASK);
			getRootNode().createDoListener(undo, redo);
		}
		am.registerFillableAction("Compress", Icons.get(Icons.compress), "Archive Selection (Filtered)");
		am.registerFillableAction("Split / Join", Icons.get(Icons.box), "Split Or Join Selection (To Or From Floppy Disks)");
		am.registerAction("New Browser", Icons.get(Icons.newWindow), "New File Explorer Window");
		am.registerAction("Log", Icons.get(Icons.history), "View File Action Log");
		am.registerAction("Close All", Icons.get(Icons.stop), "Close All Windows And Exit");
		am.registerAction("About", GuiApplication.getApplicationIconURL(), "About ...");
		am.registerAction("Expand Recursive");
	}

	
	private JToolBar buildFileActionToolBar()	{
		//JToolBar tb = new JToolBar(JToolBar.VERTICAL);
		ScrollablePopupToolbar stb = new ScrollablePopupToolbar(tree, true, SwingConstants.LEFT);
		JToolBar tb = stb.getToolbar();

		if (OS.isWindows)	{
			am.visualizeAction("Floppy", tb);
			tb.addSeparator();
		}
		am.visualizeAction("Open", tb);
		am.visualizeAction("Info", tb);
		am.visualizeAction("Find", tb);
		am.visualizeAction("Refresh", tb);
		tb.addSeparator();
		am.visualizeAction("File", tb);
		am.visualizeAction("Folder", tb);
		am.visualizeAction("Rename", tb);
		am.visualizeAction("Remove", tb);
		am.visualizeAction("Delete", tb);
		am.visualizeAction("Empty", tb);
		tb.addSeparator();
		am.visualizeAction("Cut", tb);
		am.visualizeAction("Copy", tb);
		am.visualizeAction("Paste", tb);
		tb.addSeparator();
		gam.visualizeAction(DoAction.UNDO, tb);
		gam.visualizeAction(DoAction.REDO, tb);
		tb.addSeparator();
		am.visualizeAction("Log", tb);
		am.visualizeAction("System Properties", tb);
		am.visualizeAction("New Browser", tb);
		am.visualizeAction("Home", tb);
		AbstractButton waste = am.visualizeAction("Wastebasket", tb);
		// listen for drag and drop to remove items
		new WastebasketDndListener(waste, tc);
		tb.addSeparator();
		customize = am.visualizeAction("Customize", tb);
		tb.add(Box.createVerticalGlue());
		am.visualizeAction("Close All", tb);

		return tb;
	}
	

	private TreeEditPopup buildTreeEditPopup()	{
		TreeEditPopup popup = new TreeEditPopup(tc, "Open", 0);	// index 0 gets substituted by context menuitems
		
		am.visualizeAction("Open", popup, false);
		am.visualizeAction("Info", popup, false);
		am.visualizeAction("Find", popup, false);
		am.visualizeAction("Refresh", popup, false);
		popup.addSeparator();
		popup.add(newMenu = new JMenu("New"));
		am.visualizeAction("File", newMenu, false);
		am.visualizeAction("Folder", newMenu, false);
		am.visualizeAction("Rename", popup, false);
		popup.add(delMenu = new JMenu("Delete"));
		am.visualizeAction("Remove", delMenu, false);
		am.visualizeAction("Delete", delMenu, false);
		am.visualizeAction("Empty", delMenu, false);
		popup.addSeparator();

		am.visualizeAction("Cut", popup, false);
		am.visualizeAction("Copy", popup, false);
		am.visualizeAction("Paste", popup, false);
		popup.addSeparator();

		gam.visualizeAction(DoAction.UNDO, popup, false);
		gam.visualizeAction(DoAction.REDO, popup, false);
		popup.addSeparator();

		am.visualizeAction("Compress", popup, false);
		am.visualizeAction("Split / Join", popup, false);
		popup.addSeparator();

		if (am.get("Line Count") != null)
			am.visualizeAction("Line Count", popup, false);
		if (am.get("File Differences") != null)
			am.visualizeAction("File Differences", popup, false);
		if (am.get("Directory Differences") != null)
			am.visualizeAction("Directory Differences", popup, false);
		if (am.get("Directory Differences") != null)
			popup.addSeparator();

		ButtonGroup group = new ButtonGroup();
		sortMenu = new JMenu("Sort");
		popup.add(sortMenu);
		byname = createRadioButton(sortMenu, group, "By Name", NetNode.SORT_BY_NAME);
		byext = createRadioButton(sortMenu, group, "By Extension", NetNode.SORT_BY_EXTENSION);
		bysize = createRadioButton(sortMenu, group, "By Size", NetNode.SORT_BY_SIZE);
		bytime = createRadioButton(sortMenu, group, "By Time", NetNode.SORT_BY_TIME);

		am.visualizeAction("Expand Recursive", popup, false);

		return popup;
	}
	

	private void buildServiceActions()	{
		am.registerAction("Configure Open Commands", Icons.get(Icons.doubleClick), "Define Open-Commands (Doubleclicks)");
		am.registerAction("Launch Command", Icons.get(Icons.computer), "Operating System Commandline Execution");
		am.registerAction("View", Icons.get(Icons.eye), "File Viewer");
		am.registerAction("Edit", Icons.get(Icons.documentEdit), "Text Editor");
		am.registerAction("HexEdit", Icons.get(Icons.hexEdit), "Byte Editor");
		am.registerAction("XML", Icons.get(Icons.xmlEdit), "XML Editor");
		am.registerAction("View Rich Text", Icons.get(Icons.world), "HTML Viewer And HTTP Download Spider");
		am.registerAction("FTP", Icons.get(Icons.ftp), "FTP Client");
		am.registerAction("Mail", Icons.get(Icons.mail), "Mail Client");
		am.registerAction("Image", Icons.get(Icons.picture), "Image Viewer");
		am.registerAction("Sound", Icons.get(Icons.music), "Sound Player (MP3, WAV, AIF, AU, OGG)");
		am.registerAction("Line Count", Icons.get(Icons.lineCount), "Count Text Lines Of Files");
		am.registerAction("File Differences", Icons.get(Icons.diff), "View Differences Of Two Text Files");
		am.registerAction("Directory Differences", Icons.get(Icons.dirDiff), "View Differences Of Two Directories");
		am.registerAction("Concordance", Icons.get(Icons.concordance), "Search Textline Concordances");
		am.registerAction("Tail", Icons.get(Icons.tail), "Watch Changes Of Text Files");
		am.registerAction("FolderWatch", Icons.get(Icons.folderWatch), "Watch Changes In Folders");
		am.registerAction("Screenshot", Icons.get(Icons.photo), "Create Screenshots");
		am.registerAction("Calculator", Icons.get(Icons.calculator), "Calculator");
		am.registerAction("Crypt", Icons.get(Icons.key), "Cryptography Window");
		am.registerAction("Wastebasket", Icons.get(Icons.trash), "Explore Wastebasket (Drop Here To Remove)");
		am.registerAction("System Properties", Icons.get(Icons.question), "System Information");
		am.registerAction("Customize", Icons.get(Icons.palette), "Customize GUI");
		buildHomeAction();
	}

	private void buildHomeAction()	{	
		am.registerAction("Home", Icons.get(Icons.home), "Explore Home");		
	}
	
	private JToolBar buildServiceToolBar()	{
		//JToolBar tb2 = new JToolBar(JToolBar.VERTICAL);
		ScrollablePopupToolbar stb2 = new ScrollablePopupToolbar(tree, true, SwingConstants.RIGHT);
		JToolBar tb2 = stb2.getToolbar();

		am.visualizeAction("Configure Open Commands", tb2);
		AbstractButton cmd = (JButton)am.visualizeAction("Launch Command", tb2);
		new CommandMonitorDndListener(cmd);
		AbstractButton lookat = (JButton)am.visualizeAction("View", tb2);
		new FileViewerDndListener(lookat);
		AbstractButton edit = am.visualizeAction("Edit", tb2);
		new FileEditDndListener(edit);
		AbstractButton xmledit = am.visualizeAction("XML", tb2);
		new FileXmlEditDndListener(xmledit);
		AbstractButton hexedit = am.visualizeAction("HexEdit", tb2);
		new FileHexEditDndListener(hexedit);
		AbstractButton richText = am.visualizeAction("View Rich Text", tb2);
		new FileViewerRichTextDndListener(richText);	// Drag and Drop Bug JDK 1.3: created window kills GUI
		am.visualizeAction("FTP", tb2);
		am.visualizeAction("Mail", tb2);
		AbstractButton imgBtn = am.visualizeAction("Image", tb2);
		new FileImageViewerDndListener(imgBtn);
		am.visualizeAction("Sound", tb2);
		tb2.addSeparator();
		AbstractButton countBtn = am.visualizeAction("Line Count", tb2);
		new LineCountDndListener(countBtn);
		AbstractButton diffBtn = am.visualizeAction("File Differences", tb2);
		new DiffDndListener(diffBtn);
		diffBtn = am.visualizeAction("Directory Differences", tb2);
		new DiffDndListener(diffBtn);
		am.visualizeAction("Concordance", tb2);
		am.visualizeAction("Tail", tb2);
		am.visualizeAction("FolderWatch", tb2);
		tb2.addSeparator();
		am.visualizeAction("Screenshot", tb2);
		am.visualizeAction("Calculator", tb2);
		am.visualizeAction("Crypt", tb2);
		tb2.addSeparator();
		am.visualizeAction("Compress", tb2);
		am.fillAction("Compress", new String [] { "zip", "jar", "tar.gz", "tar.bz2" });
		am.visualizeAction("Split / Join", tb2);
		tb2.add(Box.createVerticalGlue());
		am.visualizeAction("About", tb2);

		return tb2;
	}


	private void registerAdditionalKeyPresses()	{
		addKeyboardAction("Clear", KeyEvent.VK_ESCAPE, 0);
		addKeyboardAction("Popup", KeyEvent.VK_F4, 0);
		addKeyboardAction("Popup", KeyEvent.VK_F10, InputEvent.SHIFT_MASK);
		addKeyboardAction("Find", KeyEvent.VK_F3, 0);
	}
	
	private void addKeyboardAction(String actionName, int key, int mod)	{
		//tree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, mod), actionName);
		getTree().getInputMap().put(KeyStroke.getKeyStroke(key, mod), actionName);
		getTree().getActionMap().put(actionName, new ActionListenerDelegate(tc, actionName));
	}



	private PathHistoryCombo buildPathHistoryCombo()	{	
		return new PathHistoryCombo(this);
	}
	

	private JSlider buildAutoScrollSpeedSlider()	{
		int speed = pp != null && pp.scrollspeed > 0 ? pp.scrollspeed : TreeMouseListenerJDK12.AUTOSCROLL_PERCENT;
		tc.setAutoScrollSpeed(speed);
		JSlider sl_speed = new JSlider(JSlider.HORIZONTAL, 1, 30, speed);
		sl_speed.addChangeListener(tc);
		sl_speed.setToolTipText("Mouse Drag Autoscroll Speed");
		
		return sl_speed;
	}


	private Clock buildClock()	{
		return new Clock();
	}
	
			
	private JRadioButtonMenuItem createRadioButton(
		JMenu sort,
		ButtonGroup group,
		String label,
		final int flag)
	{
		JRadioButtonMenuItem radio = new JRadioButtonMenuItem(label, sortFlag == flag);
		group.add(radio);
		sort.add(radio);
		radio.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				sortFlag = flag;
				System.err.println("setting sort to "+flag);
				tc.refilter();
			}
		});
		return radio;
	}
	


	public void closeAll()	{
		((FileBrowser)parent).closeAll();
	}


	/** @return die dem String entsprechende installierte AbstractAction */
	public Action getAction(String name)	{
		Action a = (Action)am.get(name);
		if (a == null)
			a = (Action)gam.get(name);
		return a;
	}


	/** Sichern aller Daten in property-file */
	public boolean save()	{
		if (pp != null)	{
			String [] waste = null;
			NetNode n = getRootNode().getWastebasket();
			if (n != null)
				waste = n.getPathComponents();
				
			pp.putPathes(
					waste,
					getShowFiles(),
					getShowHidden(),
					!getInclude(),
					sortFlag,
					sl_speed != null ? Integer.valueOf(sl_speed.getValue()) : null,
					ckb_dropmenu != null ? new Boolean(ckb_dropmenu.isSelected()) : null,
					ckb_refresh != null ? new Boolean(ckb_refresh.isSelected()) : null);
		}
		else
			System.err.println("no persistent path: is null");
		
		if (dialogMode == false)	{	// else "*.EXE" is top item next time
			cmb_filter.setText(cmb_filter.getText());	// avoid that first item is not first in history
			cmb_filter.save();
		}
		
		return true;
	}


	/** @return ob das Panel in einem Dialog eingebettet ist.. */
	public boolean getDialogMode()	{
		return dialogMode;
	}


	/** @return TreeEditPopup mit den wichtigsten Actions. */	
	public JPopupMenu getPopupMenu()	{
		return popup;
	}	
		
	/** @return TreeEditPopup mit den wichtigsten Actions. */	
	public AbstractButton getCustomizeButton()	{
		return customize;
	}	
		
	/** @return View (JTree) der TreeView */
	public JTree getTree()	{
		return tree;
	}

	public JViewport getTreeViewport()	{
		return treeViewport;
	}
	
	/** @return das Daten-Modell der TreeView  */
	public DefaultTreeModel getModel()	{
		return model;
	}

	public Monitor getMonitor()	{
		ensureLogMonitor();
		return monitor;
	}
	
	/** @return das Controller-Object der TreeView */
	public TreeEditController getEditController()	{
		return tc;
	}

	/** @return die lokale root des tree (nicht verwendet!) */
	public BufferedTreeNode getRoot()	{
		return root;
	}

	/** @return die lokale root des tree (nicht verwendet!) */
	public NetNode getRootNode()	{
		return (NetNode)getRoot().getUserObject();
	}

	/** @return die globale FileNode root des tree */
	public static NetNode getGlobalRootNode()	{
		return globalRoot;
	}
	
	/** @return den Text des Filterfeldes */
	public int getSortFlag()	{
		return sortFlag;
	}
	
	/** @return den Zustand der CheckBox "show files". */
	public boolean getShowFiles()	{
		return ckb_showfiles.isSelected();
	}

	/** @return den Zustand der CheckBox "show files". */
	public boolean getShowHidden()	{
		return ckb_hidden.isSelected();
	}

	/** @return den Text des Filterfeldes */
	public String getFilter()	{
		if (cmb_filter != null && cmb_filter.isCommitted())
			return cmb_filter.getText();
		System.err.println("TreePanel Filter Combo is NOT committed! TreePanel = "+hashCode());
		return "*";
	}

	/** @return das Filterfeld */
	public TextLineHolder getFilterTextHolder()	{
		return cmb_filter;
	}

	/** @return Filterfeld committieren, wenn focus auf anderem Fenster war. */
	public void commitFilterOnWindowActivated()	{
		cmb_filter.commit();
	}
	
	/** @return den Zustand der ComboBox include/excude Filter. */
	public boolean getInclude()	{
		if (cmb_include != null)
			return cmb_include.getSelectedIndex() == 0;
		return true;
	}

	/** @return die ComboBox include/excude */
	public JComboBox getIncludeCombo()	{
		return cmb_include;
	}


	/** Liefert die selektierten Dateien zurueck, fur MULTIPLE_SELECTION true. */
	public File [] getSelectedFiles()	{
		File [] files = null;
		TreePath [] tp = getTree().getSelectionPaths();
		for (int i = 0; tp != null && i < tp.length; i++)	{
			DefaultMutableTreeNode dn = (DefaultMutableTreeNode)tp[i].getLastPathComponent();
			NetNode n = (NetNode)dn.getUserObject();
			
			if (files == null)
				files = new File[tp.length];
				
			files[i] = (File)n.getObject();
		}
		return files;
	}


	/**
		Liefert die selektierte Datei zurueck, fur SINGLE_SELECTION true.
		Dabei wird der Name der Datei aus dem PathTextField bezogen.
	*/
	public File getSelectedFile()	{
		String s = pathTextField.getText();
		if (s.length() > 0)	{
			return new File(s);
		}
		return null;
	}
	
	
	/** Schaltet den NetWatcher ein/aus, wenn die Checkbox "autorefresh" den Fokus hat. */
	public void setAutoRefresh()	{
		if (ckb_refresh.hasFocus())	{
			autoRefresh.setSelected(!isAutoRefresh());
			((FileBrowser)parent).setAutoRefresh(isAutoRefresh());
		}
	}

	/** Liefert true wenn die Checkbox eingeschalten ist. */
	public boolean isAutoRefresh()	{
		return autoRefresh.isSelected();
	}
	
	/** Setzt die Actions neu, weil bei Wechsel zwischen Instanzen
		die globalen Actions sich je nach Selektion lokal anpassen muessen */
	public void setEnabledActions()	{
		tc.resetDelegateSelection();
		tc.setEnabledActions();
	}


	/** Set the tree to single selection mode. */
	public void setSingleSelect()	{
		getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	}


	/** Set the suggested filename for dialog mode. */
	public void setSuggestedFilename(String suggestedFileName)	{
		pathTextField.setKeepFilename(suggestedFileName);
	}


	/** Set the tree to single selection mode. */
	public void setFilter(String filter)	{
		if (filter != null)
			cmb_filter.setText(filter);
		else
			cmb_filter.setText("*");
	}



	/**
		The Frame is disposing. Remove all connections between TreeNodes and NetNodes
	*/
	public void removeNodeListeners()	{
		Enumeration e = root.depthFirstEnumeration();
		for (; e.hasMoreElements(); )	{
			BufferedTreeNode b = (BufferedTreeNode)e.nextElement();
			b.removeNodeListener();
		}
		   
		if (clock != null)
			clock.stopClock();
		   
		ckb_refresh.removeActionListener(tc);
	}




	/**
		Visuelles Oeffnen der Root des Trees bei Programmstart, wenn
		keine persistenten Daten existieren. Die erste Ebene unter der
		Root wird geoeffnet.
	*/
	public void expandiere()	{
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				System.err.println("expanding first row");
				getTree().expandRow(0);
				System.err.println("setting selection first row");
				getTree().setSelectionRow(0);
				BufferedTreeNode.initing = false;
			}
		});
	}


	/**
		Visuelles Oeffnen der uebergebenen Pfade bei Programmstart,
		wenn persistente Daten existieren.
		Jeder TreePath wird absteigend geprueft und expandiert.
		Sollte es eine Komponente nicht mehr geben, wird nur bis zur
		letzten existenten geoeffnet.
		@param selected Pfad, der sichtbar geoeffnet und selektiert werden soll
		@param pathes Alle Pfade, die sichtbar geoeffnet werden sollen.
	*/
	public void expandiere(final String [][] selected, final String [][] pathes)	{
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				TreePath tp = expandiereUndSelektiere(selected, pathes);
				if (tp == null || pathes == null || pathes.length <= 0)	{
					tp = new TreePath(root.getPath());
					getTree().expandPath(tp);
					BufferedTreeNode.initing = false;
				}
				else	{
					//System.err.println("... scrollPathToVisible "+tp);
					final TreePath ftp = tp;
					SwingUtilities.invokeLater(new Runnable()	{
						public void run()	{
							getTree().scrollPathToVisible(ftp);	// Selektion sichtbar machen
						}
					});
				}
			}
		});
	}



	/**
		Die uebergebenen Teilbaeume neu pruefen und oeffnen und
		die uebergebenen Selektionen setzen
	*/
	private TreePath expandiereUndSelektiere(
		String [][] selected,
		String [][] pathes)
	{
		scanning = true;	// valueChanged() wird ausgeloest
		// geoeffnete Pfade erforschen
		if (pathes != null)	{
			//System.err.println("EXPAND "+pathes.length+" PATHES ...");
			for (int i = 0; i < pathes.length; i++)	{
				explorePath(pathes[i], true);
			}
		}
		scanning = false;
		
		// selektierte Pfade erforschen
		TreePath tp = null;
		
		if (selected != null)	{
			//System.err.println("SETTING "+selected.length+" SELECTIONS ...");
			for (int i = 0; i < selected.length; i++)	{
				TreePath curr = explorePath(selected[i], false);
				if (curr != null)	{
					getTree().addSelectionPath(tp = curr);	// Selektion setzen
				}
			}
		}
		return tp;
	}


	/** 
		Den ganzen Tree neu anzeigen. Geoeffnete Pfade werden neu geoeffnet.
		Diese Funktion wird benoetigt, um files herauszufiltern,
		wenn die CheckBox "showfiles" false gesetzt wird.
	*/
	public void expandiereAktuelle(boolean refilter)	{
		if (pp != null)	{
			String [][] selected = pp.selectedToStringArrays();
			String [][] pathes = pp.treePathesToStringArrays();			
			//tree.clearSelection();	// damit kein valueChanged ausgeloest wird
			// FRi: aber es wird sonst nicht gefiltert!
			
			if (refilter)	{	// showfiles was toggled
	     Enumeration e = root.depthFirstEnumeration();
	     for (; e.hasMoreElements(); )	{
	     	BufferedTreeNode b = (BufferedTreeNode)e.nextElement();
	     	b.setMarkedForFilter(true);
	     }
			}
			
			final TreePath ftp = expandiereUndSelektiere(selected, pathes);
			if (ftp != null && refilter)
				SwingUtilities.invokeLater(new Runnable()	{
					public void run()	{
						getTree().scrollPathToVisible(ftp);	// Selektion sichtbar machen
					}
				});
		}
	}


	/**
		Selektierte Teilbaeume neu anzeigen. Darunterliegende Pfade
		werden neu geoeffnet.
	*/
	public void expandiereSelektierte(boolean rescan)	{
		if (pp != null)	{
			//System.err.println("expandiereSelektierte, rescan "+rescan);
			String [][] toSelect = pp.selectedToStringArrays();
			// markedForReScan setzen, wenn rescan true
			String [][] toOpen = pp.getOpenTreeStringsUnderSelection(rescan);
			// neu oeffnen
			expandiereUndSelektiere(toSelect, toOpen);
		}
	}



	// interface TreeExpander begin
	
	public boolean explorePath(String [] path)	{
		TreePath tp = explorePath(path, true);
		if (tp != null)	{
			getTree().setSelectionPath(tp);
			getTree().scrollPathToVisible(tp);
		}
		return tp != null;
	}
	
	public boolean collapsePath(String [] path)	{
		TreePath tp = explorePath(path, false);
		if (tp == null)
			return false;
		getTree().collapsePath(tp);
		return true;
	}
	
	// interface TreeExpander end
	
	
	/**
		Expandieren des als Stringarray uebergebenen Pfades
		Bei der Gelegenheit wird geprueft, ob die Namen noch
		aktuell sind.
		@param doExpand soll visuell expandiert oder nur "logisch".
	*/
	public TreePath explorePath(String [] path, boolean doExpand)	{
		if (path == null)	{	// || path.length <= 0 || path[0].equals(""))	{
			System.err.println("no path to explore: "+path);
			return null;
		}
		
		/*System.err.print("======> explorePath expand="+doExpand+" >"+(path.length > 0 ? path[0] : "")+"<");
		for (int l = 1; l < path.length; l++)
			System.err.print(" "+path[l]);
		System.err.println();*/
		
		Object [] oarr = new Object [path.length + 1];	// mit root, path hat keine root
		BufferedTreeNode node = root, found = root;
		NetNode nn = (NetNode)found.getUserObject();
		int i;
		
		for (i = 1; i <= path.length && found != null; i++)	{
			//System.err.println(i+", searching "+path[i-1]+" in "+node);
			
			oarr[i-1] = node;
			
			NetNode n = (NetNode)node.getUserObject();
			if (n.isLeaf())
				break;
	
			found = node.searchNode(path[i-1]);
			
			if (found == null)	{
				System.err.println("WARNUNG: "+i+", "+node+", Knoten existiert nicht: "+path[i-1]);
				Toolkit.getDefaultToolkit().beep();
				break;
			}

			node = found;

			nn = (NetNode)found.getUserObject();
			if (nn.isLeaf())	{
				if (getShowFiles() == false)	// leaf, but no files shown
					break;
			}
			else
			if (nn.isHiddenNode())	{
				if (ckb_hidden.isSelected() == false)	// hidden, but no hidden nodes shown
					break;
			}
			else	
			if (doExpand)
				buildArray(oarr, i, true);	// expand true
		}

		if (found != null && nn != null)	{			
			oarr[i-1] = found;
			if (nn.isLeaf() == false)	{
				found.fillNode();	// fill container if not filled
			}
			else
			if (doExpand == false)	{	// setting selection
				NetNode n = (NetNode)found.getUserObject();
				//System.err.println("  try to refresh leaf "+found);
				n.init();	// refresh selected leaf
			}
		}

		return buildArray(oarr, i, doExpand);
	}



	private TreePath buildArray(Object [] oarr, int len, boolean expand)	{
		if (len <= 0)
			return null;
		Object [] oarr1;
		if (oarr.length == len)	{
			oarr1 = oarr;
		}
		else	{
			oarr1 = new Object [len];	// if path is shorter eliminate nulls
			System.arraycopy(oarr, 0, oarr1, 0, len);
		}
		TreePath tp = new TreePath(oarr1);
		if (expand)	{
			//System.err.println("... calling tree.expandPath "+tp);
			NetNode n = (NetNode)((BufferedTreeNode)oarr1[oarr1.length - 1]).getUserObject();
			n.setExpanded(true);
			getTree().expandPath(tp);
		}
		return tp;
	}


	private void enableFilter(boolean isFolder, String fullText)	{
		boolean enableFilter = true;	// isFolder
		filterbutton.setEnabled(enableFilter);
		cmb_filter.setEnabled(enableFilter);
		cmb_include.setEnabled(enableFilter);

		if (isFolder)	{	// visualize sort criterium
			if (currTreeNode.getSortFlag() == NetNode.SORT_BY_NAME)
				byname.setSelected(true);
			else
			if (currTreeNode.getSortFlag() == NetNode.SORT_BY_EXTENSION)
				byext.setSelected(true);
			else
			if (currTreeNode.getSortFlag() == NetNode.SORT_BY_SIZE)
				bysize.setSelected(true);
			else
			if (currTreeNode.getSortFlag() == NetNode.SORT_BY_TIME)
				bytime.setSelected(true);
		}
	}




	// interface TreeExpansionListener

	public void treeExpanded(TreeExpansionEvent e)	{
		//System.err.println("TreePanel.treeExpanded "+e.getPath());
		TreePath tp = e.getPath();
		BufferedTreeNode node = (BufferedTreeNode)(tp.getLastPathComponent());
		NetNode n = (NetNode)node.getUserObject();
		
		if (n.isLink())	{	// expand real path: resolve symbolic links
			getTree().collapsePath(tp);
			tp = explorePath(n.getPathComponents(), true);
			if (tp != null)	{
				getTree().scrollPathToVisible(tp);
				getTree().setSelectionPath(tp);	// Selektion setzen
			}
		}
		else	{	// fill node with new scanned or buffered nodes
			if (!scanning)	{
				node.fillNode();
				n.setExpanded(true);
			}
		}
	}


	public void treeCollapsed(TreeExpansionEvent e)	{
		TreePath tp = e.getPath();
		BufferedTreeNode node = (BufferedTreeNode)(tp.getLastPathComponent());
		NetNode n = (NetNode)node.getUserObject();
		n.setExpanded(false);		
	}


	// interface TreeWillExpandListener
	
	public void treeWillExpand(TreeExpansionEvent e)
		throws ExpandVetoException
	{
		//Thread.dumpStack();
		TreePath tp = e.getPath();
		BufferedTreeNode node = (BufferedTreeNode)(tp.getLastPathComponent());

		if (tc.treeWillExpandEvent(node) == false)	{
			throw new ExpandVetoException(e, "folder not accessible");
		}
		
		// if filter changed, mark for filter, recursive
		if (filterChanged())	{
			node.setMarkedForFilter(true);
			Enumeration en = node.depthFirstEnumeration();

			for (; en.hasMoreElements(); )	{
				BufferedTreeNode b = (BufferedTreeNode)en.nextElement();
				b.setMarkedForFilter(true);
			}
		}
	}

	public void treeWillCollapse(TreeExpansionEvent e)
		throws ExpandVetoException
	{
	}
	
	
	private boolean filterChanged()	{
		if (oldFilter.equals(getFilter()) == false)	{
			oldFilter = getFilter();
			return true;
		}
		return false;
	}


	// interface TreeSelectionListener
	
	public void valueChanged(TreeSelectionEvent e)	{
		// Ausgeloest, wenn ein Knoten selektiert wird.		
		if (scanning)	// Selektion nicht setzen, wenn intern neu aufgebaut wird
			return;

		// revision of selected nodes
		TreePath [] tp = e.getPaths();
		
		for (int i = 0; i < tp.length; i++)	{
			BufferedTreeNode d = (BufferedTreeNode)(tp[i].getLastPathComponent());
			d.setSelected(e.isAddedPath(i));
			//System.err.println(" ... TreePanel, selected "+d.isSelected()+" "+d);
		}

		setEnabledActions();
		getTree().setEditable(true);

		// Anzeige  aller selektierten unter Mengenangabe
		tp = getTree().getSelectionPaths();
		DefaultMutableTreeNode [] selectedNodes = new DefaultMutableTreeNode [tp != null ? tp.length : 0];
		for (int i = 0; tp != null && i < tp.length; i++)	{
			selectedNodes[i] = (DefaultMutableTreeNode)tp[i].getLastPathComponent();
			//System.err.println("TreePanel, selected node is "+selectedNodes[i]);

			// memorize rename node
			currTreeNode = (BufferedTreeNode)selectedNodes[i];
			currNetNode = (NetNode)currTreeNode.getUserObject();
		}
		setStatus(selectedNodes);

		// Anzeige des Objektes oben als Langtext
		display(currNetNode);
	}


	private void setStatus(DefaultMutableTreeNode [] selectedNodes)	{
		if (statusText == null)
			return;
			
		String s = "";
		
		if (selectedNodes.length > 1)	{	// make string of count of items and sum of sizes
			long size = 0L;
			int c = 0, n = 0;
			
			for (int i = 0; i < selectedNodes.length; i++)	{
				DefaultMutableTreeNode node = selectedNodes[i];
				NetNode nn = (NetNode)node.getUserObject();
				size += nn.getSize();
				if (nn.isLeaf())
					n++;
				else
					c++;
			}
			
			s = folderContentString(selectedNodes.length, c, n)+
					(size > 0L ? ": "+NumberUtil.getFileSizeString(size) : "");
		}
		else
		if (selectedNodes.length == 1)	{	// make info string about one node
			DefaultMutableTreeNode node = selectedNodes[0];
			NetNode nn = (NetNode)node.getUserObject();
			
			if (nn.isLink())	{
				s = "-> "+nn.getFullLinkText();
			}
			else
			if (nn.isLeaf() == false)	{	// is folder
				int max = node.getChildCount(), c = 0, n = 0;

				for (int i = 0; i < max; i++)	{
					DefaultMutableTreeNode d = (DefaultMutableTreeNode)node.getChildAt(i);
					NetNode n1 = (NetNode)d.getUserObject();

					if (n1.isLeaf())
						n++;
					else
						c++;
				}
				
				if (max > 0)
					s = folderContentString(max, c, n)+
							(nn.recursiveSizeReady() ?
									"  (contains "+NumberUtil.getFileSizeString(nn.getRecursiveSize())+")" :
									"");
				else
					s = nn.getInfoText();	// unexplored folder
			}
			else	{	// is leaf
				s = nn.getInfoText();
			}
		}
		
		statusText.setText(s);
		statusText.setCaretPosition(0);
	}
	
	
	/** form a string that tells "6 object(s): 3 folder(s), 3 file(s)" */
	public static String folderContentString(int anz, int c, int n)	{
		String s = "";
		if (anz < 0)
			return s;

		if (anz != 1)
			s = anz+" Objects: ";
		else
			s = anz+" Object: ";

		if (c > 0)
			if (c != 1)
				s += c+" Folders";
			else
				s += c+" Folder";

		if (n > 0)	{
			if (c > 0)
				s += ", ";

			if (n != 1)
				s += n+" Files";
			else
				s += n+" File";
		}					
		return s;
	}


	/** Set the name of the node to textfield */
	public void display(NetNode n)	{
		// display full path and set filter-field enabled state
		if (n == null)
			return;

		String disp = n.getFullText();
		
		if (pathTextField != null)	{
			if (n.isLeaf() == false)	{	// add to history if folder
				pathTextField.insertText(disp);
			}
			else	{
				NetNode pnt = n.getParent();
				if (n != null)
					pathTextField.insertText(pnt.getFullText());
				pathTextField.setText(disp);
			}
		}
			
		enableFilter(!n.isLeaf(), disp);
	}


	// interface TreeModelListener

	public void treeNodesChanged(TreeModelEvent e) {
		// a node was renamed
		DefaultMutableTreeNode node =
				(DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());
		try	{	// to recognize a leaf rename
			int index = e.getChildIndices()[0];
			node = (DefaultMutableTreeNode) node.getChildAt(index);
		}
		catch (NullPointerException exc) {	// folder was renamed
		}
		// Achtung, das user-object ist jetzt ein String ...
		
		String oldname = new String(currNetNode.getLabel());	// for localization in other models
		String newname = node.toString();	// for localization in other models

		// auf alle Faelle wieder das user-object einsetzen
		node.setUserObject(currNetNode);

		tc.finishRenameTreeNode(currNetNode, newname, oldname);
	}

	public void treeNodesInserted(TreeModelEvent e) {
	}
	public void treeNodesRemoved(TreeModelEvent e) {
	}
	public void treeStructureChanged(TreeModelEvent e) {
	}


	public boolean checkForSelection(NetNode n)	{
		// compare to persistent pathes for filtering
		//System.err.println("checkForSelection: "+n);
		if (pp != null)	{
			String [] sarr = n.getPathComponents();
			if (persistSelected == null)
				persistSelected = pp.getSelected();
			for (int i = 0; persistSelected != null && i < persistSelected.length; i++)	{
				//System.err.println("  comparing ....");
				if (compareStringArrays(persistSelected[i], sarr))	{
					//System.err.println("... setting selected from persistent: "+n);
					return true;
				}
			}
		}
		return false;
	}

	private boolean compareStringArrays(String [] a1, String [] a2)	{
		if (a1 == null || a2 == null || a1.length != a2.length)
			return false;
		for (int i = 0; i < a1.length; i++)
			if (a1[i].equals(a2[i]) == false)
				return false;
		return true;
	}
	

	public void updateUI()	{
		super.updateUI();
		if (tc != null)
			tc.updateUI();
	}
	
	
	// KeyListener
	private void changeSelectionByKeypress(char c)	{
		if (OS.isAboveJava13)
			return;
		
		int [] selectedRows = tree.getSelectionRows();
		int rows = tree.getRowCount();
		if (selectedRows == null || selectedRows.length <= 0 || rows <= 1)
			return;
		
		int selectedRow = selectedRows[0];
		c = Character.toLowerCase(c);
		for (int i = 0; i < rows; i++)	{
			int next = (i + selectedRow + 1) % rows;
			TreePath tp = tree.getPathForRow(next);
			String name = tp.getLastPathComponent().toString();
			if (name.length() > 0 && Character.toLowerCase(name.charAt(0)) == c)	{
				tree.setSelectionInterval(next, next);
				tree.scrollPathToVisible(tp);
				return;
			}
		}
	}

}





class FileNameComboBox extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();
	private static File globalFile;


	public FileNameComboBox()	{
		super();
		manageTypedHistory(this, new File(HistConfig.dir()+"FileNameHistory.list"));
	}


	public Dimension getMinimumSize()	{
		return new Dimension(100, super.getMinimumSize().height);
	}

	public void setItems(String [] items)	{
		for (int i = items.length - 1; i >= 0; i--)
			if (items[i] != null)
					insertUniqueOnTop(items[i]);
		setSelectedIndex(0);
	}
	
	public boolean isLoaded()	{
		return history.getSize() > 0;
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
