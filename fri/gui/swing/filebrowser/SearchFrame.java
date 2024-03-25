package fri.gui.swing.filebrowser;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import gnu.regexp.REMatch;	// pass through argument
import fri.util.NumberUtil;
import fri.util.FileUtil;
import fri.util.regexp.*;
import fri.util.os.OS;
import fri.util.props.*;
import fri.util.text.TextUtil;
import fri.gui.CursorUtil;
import fri.gui.swing.combo.ConstantHeightComboBox;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.table.sorter.*;
import fri.gui.swing.progresslabel.*;
import fri.gui.swing.spinnumberfield.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.IconUtil;
import fri.gui.swing.textviewer.TextViewer;
import fri.gui.swing.button.NoInsetsButton;
import fri.gui.swing.searchdialog.*;
import fri.gui.swing.splitpane.SplitPane;

/**
	Such-Fenster fuer Datei-Namen, -Inhalt, -Datum, -Groesse.
	Gesucht wird mittels eines unterbrechbaren Hintergrund-Threads.
	Es kann ueber ein oder mehrere Ordner oder Dateien gesucht werden.
	Auch Archiv-Dateien koennen durchsucht werden.
*/

public class SearchFrame extends JFrame implements
	ActionListener,
	ListSelectionListener,
	Runnable,
	InfoRenderer,
	SearchResultDispatcher
{
	private static String helpText;

	private JTree tree;
	private JButton startSearch, stop;
	private JButton startPoint;
	private SearchPathComboBox startPointCombo;

	private JCheckBox cb_SearchArchives;
	private boolean searchArchives = false;	// default
		
	private static String prevFilePattern = "";
	private static String prevContentPattern = "";

	private SearchComboBox tf_such;
	private SearchHistoryCombo tf_string;
	
	private JCheckBox cb_IgnoreCase;
	private boolean ignoreCase = PropertyUtil.checkClassProperty("ignoreCase", getClass(), "true", true);

	private JCheckBox cb_ContIgnoreCase;
	private boolean contIgnoreCase = PropertyUtil.checkClassProperty("contentIgnoreCase", getClass(), "true", true);
	private JCheckBox cb_WordMatch;
	private boolean wordMatch = PropertyUtil.checkClassProperty("wordMatch", getClass(), "true", false);
	private JCheckBox cb_ShowLines;
	private boolean showLines = PropertyUtil.checkClassProperty("showLines", getClass(), "true", false);
	private JComboBox cmb_include, cmb_positive;
	private boolean include = true, include_string = true;	// defaults
	private JButton help;

	private JComboBox cmb_time;
	private SpinNumberField tf_time;
	private JComboBox cmb_timedim;
	public static final String YOUNGER = "Younger Than";
	public static final String OLDER = "Older Equal";
	
	private JComboBox cmb_size;
	private SpinNumberField tf_size;
	private JComboBox cmb_sizedim;
	public static final String BIGGER = "Bigger Equal";
	public static final String SMALLER = "Smaller Than";

	private JLabel status;
	private InfoTableModel model;
	private TableSorter sorter;
	private FileTableData data;
	private InfoDataTable table;
	
	private BufferedTreeNode [] startNodes;
	private BufferedTreeNode root;
	private Thread thread;
	private boolean interrupted = false;
	private ProgressLabel lb_progress;
		
	private SearchCondition condition;

	private JComboBox cmb_doRegExp;
	
	private TreeEditController tc;
	
	private Object lock = new Object();
	
	private SearchResultFrame searchResult;
	
	private SearchReplaceWriter searchReplaceWriter;
	private boolean isReplacing = false;
	private String replaceString = null;
	private JCheckBox cb_replace;
	private ReplaceHistoryCombo tf_replace;



	public SearchFrame(JTree tree, BufferedTreeNode [] startNodes, TreeEditController tc) {
		this();
		
		this.tree = tree;
		this.startNodes = startNodes;
		this.tc = tc;
		this.root = (BufferedTreeNode)startNodes[0].getRoot();
		
		model.setTreeEditController(tc);	// for rename
		init();
	}


	private SearchFrame() {
		IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());

		// filechooser panel
		JPanel mainToolbar = new JPanel();
		mainToolbar.setLayout(new BoxLayout(mainToolbar, BoxLayout.X_AXIS));
		mainToolbar.add(Box.createHorizontalStrut(8));
		startPoint = new JButton("Path");
		startPoint.addActionListener(this);
		startPoint.setToolTipText("Drag Directories to here or Push to Change Search Path");
		mainToolbar.add(startPoint);
		
		startPointCombo = new SearchPathComboBox();
		startPointCombo.addActionListener(this);
		startPointCombo.setToolTipText("Drag Directories to here or Edit to Add Another Search Path");
		mainToolbar.add(startPointCombo);

		mainToolbar.add(Box.createRigidArea(new Dimension(10, 0)));
		
		JPanel panelOptions = new JPanel();
		panelOptions.setLayout(new BoxLayout(panelOptions, BoxLayout.Y_AXIS));
		cb_SearchArchives = new JCheckBox("Search in Archives", searchArchives);
		cb_SearchArchives.setToolTipText("Include Files within ZIP, JAR, TAR, TGZ Archives");
    cb_SearchArchives.addActionListener(this);
		panelOptions.add(cb_SearchArchives);
		panelOptions.add(Box.createHorizontalGlue());
		cb_ShowLines = new JCheckBox("Show Lines", showLines);
		cb_ShowLines.setToolTipText("Show Found Lines in a Result Window");
		panelOptions.add(cb_ShowLines);
		mainToolbar.add(panelOptions);

		// start/stop buttons
		lb_progress = new ProgressLabel();
		mainToolbar.add(lb_progress);

		startSearch = new JButton("Start");
		startSearch.addActionListener(this);
		startSearch.setToolTipText("Start Search");
		mainToolbar.add(startSearch);

		stop = new JButton("Stop");
		stop.addActionListener(this);
		stop.setToolTipText("Interrupt Search");
		stop.setEnabled(false);
		mainToolbar.add(stop);


		// drag and drop listeners for start path
		new SearchFrameDndListener(startPointCombo.getTextEditor(), this, true);	// dropped on combo
		new SearchFrameDndListener(startPoint, this);	// dropped on path button
		new SearchFrameDndListener(this, this);	// dropped on frame
			
		// panel for file pattern
		JPanel panelFilePattern = new JPanel();
		panelFilePattern.setLayout(new BoxLayout(panelFilePattern, BoxLayout.Y_AXIS));
		panelFilePattern.setBorder(BorderFactory.createTitledBorder("Name:"));
		
		tf_such = new SearchComboBox();
		tf_such.setText(prevFilePattern);
		//System.err.println("setting file pattern text file to "+prevFilePattern);
		tf_such.setAlignmentX(Component.CENTER_ALIGNMENT);
		tf_such.setToolTipText("File Pattern");
		tf_such.addActionListener(this);

		JPanel panelFilePatternLower = new JPanel();
		panelFilePatternLower.setLayout(new BoxLayout(panelFilePatternLower, BoxLayout.X_AXIS));
		
		cmb_include = new ConstantHeightComboBox();
		cmb_include.addItem("Include");
	 	cmb_include.addItem("Exclude");
		cmb_include.setToolTipText("Include/Exclude Matching Files");
		String incl = include ? "Include" : "Exclude";
		cmb_include.setSelectedItem(incl);
		panelFilePatternLower.add(cmb_include);
		panelFilePatternLower.add(Box.createRigidArea(new Dimension(6, 0)));
		cb_IgnoreCase = new JCheckBox("Ignore Case", ignoreCase);
		panelFilePatternLower.add(cb_IgnoreCase);

		panelFilePattern.add(tf_such);
		panelFilePattern.add(panelFilePatternLower);

		// panel for containment pattern
		JPanel panelContainmentPattern = new JPanel();
		panelContainmentPattern.setLayout(new BoxLayout(panelContainmentPattern, BoxLayout.Y_AXIS));
		panelContainmentPattern.setBorder(BorderFactory.createTitledBorder("Containing:"));

		JPanel panelContainmentPatternUpper = new JPanel();
		panelContainmentPatternUpper.setLayout(new BoxLayout(panelContainmentPatternUpper, BoxLayout.X_AXIS));
		
		tf_string = new SearchHistoryCombo();
		tf_string.setText(prevContentPattern);
		tf_string.setAlignmentX(Component.CENTER_ALIGNMENT);
		tf_string.setToolTipText("Text Search Pattern");
		tf_string.addActionListener(this);
		panelContainmentPatternUpper.add(tf_string);
		cmb_doRegExp = new ConstantHeightComboBox(Syntaxes.getSyntaxes());
		cmb_doRegExp.setToolTipText("Choose Regular Expression Syntax");
		cmb_doRegExp.setEditable(false);
		//cmb_doRegExp.setSelectedIndex(0);
		panelContainmentPatternUpper.add(cmb_doRegExp);
		help = new NoInsetsButton("Help");
		help.setToolTipText("Regular Expression Help");
		help.addActionListener(this);
		help.setBorderPainted(false);
		panelContainmentPatternUpper.add(help);
		
		JPanel panelContainmentPatternLower = new JPanel();
		panelContainmentPatternLower.setLayout(new BoxLayout(panelContainmentPatternLower, BoxLayout.X_AXIS));

		cmb_positive = new ConstantHeightComboBox();
		cmb_positive.addItem("Include");
	 	cmb_positive.addItem("Exclude");
		cmb_positive.setToolTipText("Include/Exclude Matching Files");
		String positive = include_string ? "Include" : "Exclude";
		cmb_positive.setSelectedItem(positive);
	 	cmb_positive.addActionListener(this);
		panelContainmentPatternLower.add(cmb_positive);
		panelContainmentPatternLower.add(Box.createRigidArea(new Dimension(6, 0)));
		cb_ContIgnoreCase = new JCheckBox("Ignore Case", contIgnoreCase);
		panelContainmentPatternLower.add(cb_ContIgnoreCase);
		cb_WordMatch = new JCheckBox("Match Whole Word", wordMatch);
		panelContainmentPatternLower.add(cb_WordMatch);
		
		panelContainmentPattern.add(panelContainmentPatternUpper);
		panelContainmentPattern.add(panelContainmentPatternLower);

		// panel for replacement
		JPanel panelReplace = new JPanel();
		panelReplace.setLayout(new BoxLayout(panelReplace, BoxLayout.X_AXIS));
		panelReplace.setBorder(BorderFactory.createTitledBorder("Replace By:"));
		cb_replace = new JCheckBox("", false);
		cb_replace.setToolTipText("Click To Activate Replacement");
		cb_replace.addActionListener(this);
		tf_replace = new ReplaceHistoryCombo()	{
			public Dimension getMinimumSize()	{
				return new Dimension(80, 0);	// Overridden to return 80/0, TextField must be usable
			}
		};
		tf_replace.setEnabled(cb_replace.isSelected());
		tf_replace.setToolTipText("Replaces All Occurences By Given Text");
		tf_replace.addActionListener(this);
		panelReplace.add(cb_replace);
		panelReplace.add(tf_replace);

		SplitPane splitContainmentAndReplace = new SplitPane(JSplitPane.HORIZONTAL_SPLIT, panelContainmentPattern, panelReplace);
		splitContainmentAndReplace.setDividerSize(4);
		splitContainmentAndReplace.setDividerLocation(0.7);
		
		// panel for file and contents pattern
		JSplitPane splitFilePatternAndContainmentPattern = new SplitPane(JSplitPane.HORIZONTAL_SPLIT, panelFilePattern, splitContainmentAndReplace);
		splitFilePatternAndContainmentPattern.setDividerSize(4);
		splitFilePatternAndContainmentPattern.setDividerLocation(0.3);

		// other panels
		
		// tabbed pane
		JTabbedPane tab = new JTabbedPane();

		tab.addTab("Name, Containing", null, splitFilePatternAndContainmentPattern, "File name and containment patterns");
		
		// time and size panel
		JPanel p11 = new JPanel();
		p11.setLayout(new BoxLayout(p11, BoxLayout.X_AXIS));
		
		// time panel
		JPanel p12 = new JPanel();
		p12.setLayout(new BoxLayout(p12, BoxLayout.X_AXIS));
		p12.setBorder(BorderFactory.createTitledBorder("Time:"));
		cmb_time = new ConstantHeightComboBox();
		cmb_time.addItem(OLDER);
		cmb_time.addItem(YOUNGER);
		p12.add(cmb_time);
		tf_time = new SpinNumberField(0, Integer.MAX_VALUE - 1)	{
			public Dimension getMaximumSize()	{
				Dimension d = cmb_size.getPreferredSize();
				return new Dimension(40, d.height);
			}
		};
		tf_time.addActionListener(this);
		p12.add(tf_time);
		cmb_timedim = new ConstantHeightComboBox();
		cmb_timedim.addItem("Minutes");
		cmb_timedim.addItem("Hours");
		cmb_timedim.addItem("Days");
		cmb_timedim.addItem("Weeks");
		cmb_timedim.addItem("Months");
		cmb_timedim.addItem("Years");
		cmb_timedim.setSelectedItem("Days");
		p12.add(cmb_timedim);
		

		// size panel
		JPanel p13 = new JPanel();
		p13.setLayout(new BoxLayout(p13, BoxLayout.X_AXIS));
		p13.setBorder(BorderFactory.createTitledBorder("Size:"));
		cmb_size = new ConstantHeightComboBox();
		cmb_size.addItem(BIGGER);
		cmb_size.addItem(SMALLER);
		p13.add(cmb_size);
		tf_size = new SpinNumberField(0, Integer.MAX_VALUE - 1)	{
			public Dimension getMaximumSize()	{
				Dimension d = cmb_size.getPreferredSize();
				return new Dimension(40, d.height);
			}
		};
		tf_size.addActionListener(this);
		p13.add(tf_size);
		cmb_sizedim = new ConstantHeightComboBox();
		cmb_sizedim.addItem("Byte");
		cmb_sizedim.addItem("KB");
		cmb_sizedim.addItem("MB");
		cmb_sizedim.addItem("GB");
		cmb_sizedim.setSelectedItem("KB");
		p13.add(cmb_sizedim);

		p11.add(p12);
		p11.add(p13);
		
		tab.addTab("Time, Size", null, p11, "File time and size limits");
		
		// add search tabbed pane
		
		JPanel p3 = new JPanel (new BorderLayout());
		
		p3.add(mainToolbar, BorderLayout.NORTH);	// location button		
		p3.add(tab, BorderLayout.CENTER);	// search conditions


		JPanel p4 = new JPanel (new BorderLayout());

		status = new JLabel(" ");
		p4.add(status, BorderLayout.SOUTH);

		// allocate the table
		data = new FileTableData();
		model = new InfoTableModel(data, this);
		model.setWhichPath(InfoTableModel.ABSOLUTE_PATH);	// render full path
		table = new InfoDataTable(model, this);
		sorter = table.getSorter();

		InfoTableDndListener dndLsnr = new InfoTableDndListener(table, this);
		table.setDndListener(dndLsnr);

		ListSelectionModel lm = table.getSelectionModel();
		lm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		lm.addListSelectionListener(this);
		JScrollPane sp = new JScrollPane(table);
		new InfoTableDndListener(table.getParent(), this);		

		p4.add(sp, BorderLayout.CENTER);

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(p3);
		p3.setMinimumSize(new Dimension());
		split.setBottomComponent(p4);

		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(split, BorderLayout.CENTER);
	}


	private void init()	{
		setTitle("Search");
		setTargetNodesToCombo(startNodes);
		
		InfoDataTableMouseListener ml = new InfoDataTableMouseListener(tc, table);
		table.addMouseListener(ml);
		table.addMouseMotionListener(ml);
		InfoTable.addKeyboardActions(table, tc);
		
		new GeometryManager(this).pack();

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				CursorUtil.setWaitCursor(SearchFrame.this);
				try	{
					interruptThreadAndWait();
					
					prevFilePattern = tf_such.getText();
					prevContentPattern = tf_string.getText();
	
					tf_string.save();
					tf_such.save();
					tf_replace.save();
					table.close();
					startPointCombo.save();
					saveProperties();
	
					if (condition != null)
						condition.clean(root);
					
					InfoTable.removeKeyboardActions(table);
				}
				finally	{
					CursorUtil.resetWaitCursor(SearchFrame.this);
				}
				setVisible(false);
			}
			
			public void windowActivated(WindowEvent e)	{	// connect to controller
				InfoTable.setSelectedListLine(SearchFrame.this, table, sorter, data, tree, root, tc, null, null);
			}

			public void windowOpened(WindowEvent e)	{
				openDefaults();
			}
		});

		setVisible(true);
	}


	private void openDefaults()	{
		if (((NetNode)startNodes[0].getUserObject()).isLeaf())	{	// do not constrain file pattern when leaf as it could be excluded
			tf_such.setText("");
			tf_string.requestFocus();
		}
		else	{
			tf_such.requestFocus();
		}
		cb_replace.setSelected(false);	// start in non-replace mode
	}
	
	
	public void setVisible(boolean visible)	{
		super.setVisible(visible);
		
		if (visible)	{
			openDefaults();
		}
	}
	

	private void saveProperties()	{
		ClassProperties.put(getClass(), "ignoreCase", cb_IgnoreCase.isSelected() ? "true" : "false");
		ClassProperties.put(getClass(), "contentIgnoreCase", cb_ContIgnoreCase.isSelected() ? "true" : "false");
		ClassProperties.put(getClass(), "wordMatch", cb_WordMatch.isSelected() ? "true" : "false");
		ClassProperties.put(getClass(), "showLines", cb_ShowLines.isSelected() ? "true" : "false");
		ClassProperties.store(getClass());
	}
	
	
	public File [] getSelectedFileArray()	{
		return InfoTable.getSelectedFileArray(table, sorter, data);
	}
	
	
	// interface InfoRenderer
	
	public JFrame getFrame()	{
		return this;
	}	

	public JTable getTable()	{
		return table;
	}	
	
	// interface ListSelectionListener

	public void valueChanged(ListSelectionEvent e)	{
		if (e.getValueIsAdjusting())
			return;
		
		InfoTable.setSelectedListLine(this, table, sorter, data, tree, root, tc, null, null);
		
		int [] iarr = table.getSelectedRows();	
		long size = 0L;
		for (int i = 0; iarr != null && i < iarr.length; i++)	{
			NetNode n = (NetNode)data.getObjectAt(sorter.convertRowToModel(iarr[i]));
			size += n.getSize();
		}
		
		String addInfo = ".";
		if (iarr != null && iarr.length > 0)	{
			addInfo = ", Selected "+iarr.length+" Item(s): "+NumberUtil.getFileSizeString(size);
		}
		status.setText("Found "+data.size()+" Item(s)"+addInfo);
	}


	// interface ActionListener

	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == startSearch ||
				e.getSource() == tf_such ||
				e.getSource() == tf_string ||
				e.getSource() == tf_replace ||
				e.getSource() == tf_time.getNumberEditor() ||
				e.getSource() == tf_size.getNumberEditor())
		{
			if (checkStartPoint())	{
				startSearch();
			}
			else	{
				Toolkit.getDefaultToolkit().beep();
			}
		}
		else
		if (e.getSource() == startPointCombo)	{
			if (checkStartPoint())	{
				startSearch();
			}
			else	{	// start a dialog to choose path
				chooseNewStartPoint(new File(startPointCombo.getText()));
			}
		}
		else
		if (e.getSource() == stop)	{
			interruptThreadAndWait();
		}
		else
		if (e.getSource() == startPoint)	{
			chooseNewStartPoint(new File(startPointCombo.getText()));
		}
		else
		if (e.getSource() == help)	{
			if (helpText == null)	{
				helpText = RegExpUtil.getHelpText();
			}
			TextViewer.singleton("Regular Expression Help", helpText);
		}
		else
		if (e.getSource() == cb_replace)	{
			tf_replace.setEnabled(cb_replace.isSelected());
		}
		else
		if (e.getSource() == cmb_positive)	{
			boolean b = cmb_positive.getSelectedItem().equals("Include");
			cb_replace.setEnabled(b);
			if (cb_replace.isSelected())
				tf_replace.setEnabled(b);
		}
		else
    if (e.getSource() == cb_SearchArchives)  {
      if (cb_SearchArchives.isSelected())  {
        cb_replace.setSelected(false);
        cb_replace.setEnabled(false);
        tf_replace.setEnabled(false);
      }
      else  {
        cb_replace.setEnabled(true);
      }
    }
	}


	private void chooseNewStartPoint(File f)	{
		// start a file/directory chooser
		NetNode n = (NetNode)startNodes[0].getUserObject();

		// file from textfield can be rubbish
		String pnt = f.getParent();
		if (pnt == null)	{
			f = (File)n.getObject();
		}
		
		File [] files = FileChooser.showDialog(
			"Search",
			this,
			n.getRoot(),
			null,
			f,
			true,
			null,
			null,	// deny neither dirs nor files
			false);	// choose more items
			//true);	// choose only one item
			
		if (files != null && files.length > 0) {
			startSearch(files);
		}
	}


	public void addStartPoints(File [] files)	{
		File [] f = new File[startNodes.length + files.length];
		
		for (int i = 0; i < startNodes.length; i++)
			f[i] = (File)((NetNode)startNodes[i].getUserObject()).getObject();
		
		for (int i = 0; i < files.length; i++)
			f[startNodes.length + i] = files[i];
			
		changeStartPoint(f);
	}
	
	
	private void setTargetNodesToCombo(BufferedTreeNode [] nodes)	{
		this.startNodes = nodes;
		
		String tgts = null;
		for (int i = 0; i < startNodes.length; i++)	{
			NetNode n = (NetNode)startNodes[i].getUserObject();
			String tgt = ((File)n.getObject()).getPath();
			if (tgt.indexOf(" ") >= 0)
				tgt = "\""+tgt+"\"";
			tgts = tgts == null ? tgt : tgts+" "+tgt;
		}
		startPointCombo.setText(tgts);
	}
	
	
	/** sets the dialog visible and sets a new search target node */
	public void setTargets(BufferedTreeNode [] node)	{
		setTargetNodesToCombo(node);
		
		clear();	// remove all search results
		
		setVisible(true);
		if (getState() == Frame.ICONIFIED)
			setState(Frame.NORMAL);
	}



	/** Search from some Files, needed by DnDListener and FileChooser. */
	public void startSearch(File [] f)	{
		if (changeStartPoint(f))
			startSearch();
	}


	private boolean checkStartPoint()	{
		String path = startPointCombo.getText();
		
		if (path.length() <= 0 || path.equals(NetNode.ARTIFICIAL_ROOT))	{
			this.startNodes = new BufferedTreeNode [] { root };
			model.setWhichPath(InfoTableModel.ABSOLUTE_PATH);	// from root: render absolute path
			return true;
		}
		else	{
			String [] sarr = TextUtil.tokenizeDoubleQuote(path);
			File [] farr = new File[sarr.length];
			for (int i = 0; i < sarr.length; i++)	{
				farr[i] = new File(sarr[i]);
			}
			return changeStartPoint(farr);
		}
	}
	
	
	private boolean changeStartPoint(File [] f)	{
		Vector v = new Vector();
		boolean isAbsolute = false;
		
		for (int i = 0; i < f.length; i++)	{
			BufferedTreeNode n = BufferedTreeNode.fileToBufferedTreeNode(f[i], root);
			
			if (n != null)	{
				v.addElement(n);
				if (f[i].exists() == false)	{	// WINDOWS root does NOT exist
					isAbsolute = true;
					model.setWhichPath(InfoTableModel.ABSOLUTE_PATH);	// from root: render absolute path
				}
			}
		}
		
		if (v.size() > 0)	{
			BufferedTreeNode [] barr = new BufferedTreeNode [v.size()];
			v.copyInto(barr);
			setTargetNodesToCombo(barr);
			
			if (isAbsolute == false)	{
				NetNode [] narr = new NetNode[barr.length];
				for (int i = 0; i < barr.length; i++)
					narr[i] = (NetNode)barr[i].getUserObject();
					
				NetNode commonPath = InfoFrame.getCommonPath(narr);
				
				if (commonPath != null)
					model.setWhichPath(InfoTableModel.RELATIVE_PATH, commonPath.getFullText()); // render relative pathe
				else
					isAbsolute = true;
			}
			
			if (isAbsolute == true)	{
				model.setWhichPath(InfoTableModel.ABSOLUTE_PATH);	// render absolute pathes
			}

			return true;
		}
		
		return false;
	}
	

	private void startSearch()	{
		prevFilePattern = tf_such.getText();
		prevContentPattern = tf_string.getText();
		
		interruptThreadAndWait();

		if ((condition = prepareCondition()) == null)
			return;

		if (searchResult != null &&
				cb_ShowLines.isSelected() &&
				tf_string.getText().length() > 0 &&
				searchResult.hasEntries())
		{
			int ret = JOptionPane.showConfirmDialog(
							this,
							"Dismiss Current Search Results?",
							"Search Results",
							JOptionPane.YES_NO_CANCEL_OPTION);			
	
			if (ret == JOptionPane.CANCEL_OPTION)	{
				return;
			}
			if (ret == JOptionPane.YES_OPTION)	{
				searchResult.removeAllFoundLines();
			}
		}
		
		clear();

		thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}


	private void clear()	{
		model.clear();
		status.setText(" ");
	}


	private SearchCondition prepareCondition()	{
		if (condition != null)	{
			condition.clean(root);	// remove temporarily extracted files
		}
		
		String syntax = (String)cmb_doRegExp.getSelectedItem();

		if (tf_such.getText().length() > 0)
			tf_such.commit();	// tolerate empty string
		String s = tf_such.getText();	// is containment pattern empty?

		boolean include = cmb_include.getSelectedItem().equals("Include");
		boolean fileAlwaysMatches = RegExpUtil.alwaysMatches(s, null);

		if (tf_string.getText().length() > 0)
			tf_string.commit();	// tolerate empty string
		s = tf_string.getText();	// is containment pattern empty?
		
		boolean positive = cmb_positive.getSelectedItem().equals("Include");
		boolean containmentAlwaysMatches = RegExpUtil.alwaysMatches(s, syntax);

		// check for impossible settings
		if (!include && fileAlwaysMatches || !positive && containmentAlwaysMatches)	{
			JOptionPane.showMessageDialog(
					this,
					"Impossible search settings, nothing would be found!",
					"Error",
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		// look if replacement is required
		isReplacing = s.length() > 0 && cb_replace.isEnabled() && cb_replace.isSelected();

		if (isReplacing)	{
			int ret = JOptionPane.showConfirmDialog(
					this,
					"Do you really want to replace \""+s+"\" by \""+tf_replace.getText()+"\" in "+tf_such.getText()+"?\n"+
						"There is no Undo option for this action!",
					"Confirm Replacment",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			
			if (ret == JOptionPane.NO_OPTION)	{
				isReplacing = false;
				replaceString = null;
				cb_replace.setSelected(false);
				tf_replace.setEnabled(false);
			}
			else
			if (ret == JOptionPane.YES_OPTION)	{
				replaceString = tf_replace.getText();
			}
			else	{
				return null;
			}
		}

		// set archive search flag
		searchArchives = cb_SearchArchives.isSelected();
		
		// add name and containment patterns, catch RegExp-Exceptions
		try	{
			if (containmentAlwaysMatches == false)	{
				// search for file-pattern and containment-pattern
				condition = new SearchCondition(
						this,
						tf_such.getText(),
						cb_IgnoreCase.isSelected(),
						include,
						positive,
						tf_string.getText(),
						cb_ContIgnoreCase.isSelected(),
						syntax,
						cb_WordMatch.isSelected(),
						cb_ShowLines.isSelected(),
						searchArchives);
			}
			else	{	// search only for file-pattern 
				condition = new SearchCondition(
						this,
						tf_such.getText(),
						cb_IgnoreCase.isSelected(),
						include,
						searchArchives);
			}
		}
		catch (Exception e)	{
			JOptionPane.showMessageDialog(
					this,
					e.toString(),
					"Error",
					JOptionPane.ERROR_MESSAGE);
			//tf_such.requestFocus();
			return null;
		}

		// add time condition
		int iTime = (int)tf_time.getValue();
		if (iTime > 0)	{
			SearchTimePattern timeCond = new SearchTimePattern(
					iTime,
					(String)cmb_time.getSelectedItem(),
					(String)cmb_timedim.getSelectedItem());
			condition.insertElementAt(timeCond, 0);
		}
		
		// add size condition
		int iSize = (int)tf_size.getValue();
		if (iSize >= 0)	{
			SearchSizePattern sizeCond = new SearchSizePattern(
					iSize,
					(String)cmb_size.getSelectedItem(),
					(String)cmb_sizedim.getSelectedItem());
			condition.insertElementAt(sizeCond, 0);			
		}
		
		return condition;
	}


	
	private void interruptThreadAndWait()	{
		if (thread != null)	{
			if (thread.isAlive())	{
				CursorUtil.setWaitCursor(this);
				try	{
					synchronized(lock)	{
						try	{
							//System.err.println("interrupting and waiting for thread ...");
							interrupted = true;
							lock.wait(2000);	// wait for termination
						}
						catch (InterruptedException e)	{
						}
						//System.err.println("  thread returned, finish.");
					}
				}
				finally	{
					CursorUtil.resetWaitCursor(this);
				}
			}
			else	{	// There was an Exception. What shall we do?
				thread = null;
				setButtonStates(false);
			}
		}
	}
	
	
	// begin thread methods
	
	/**
		Implements Runnable to search in background, interruptable.
	*/
	public void run()	{
		interrupted = false;
		//System.err.println("run() starts ...");
		setButtonStates(true);
		
		for (int i = 0; i < startNodes.length; i++)	{
			NetNode n = (NetNode)startNodes[i].getUserObject();
			getAllMatches((File)n.getObject(), n);
		}
		
		//System.err.println("thread terminating, locking lock object ...");
		synchronized(lock)	{
			thread = null;
			//System.err.println("  notify() waiting thread.");
			lock.notify();
		}
		
		setNote("Found "+data.size()+" Item(s).");
		setButtonStates(false);
		//System.err.println("run() returns.");
	}


	private void setButtonStates(final boolean running)	{
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				startSearch.setEnabled(!running);
				stop.setEnabled(running);
				lb_progress.clear();
				if (!running) { // avoid unintended replace with next search
				  cb_replace.setSelected(false);
				  tf_replace.setEnabled(false);
				}
			}
		});
	}
	
	
	private void getAllMatches(File f, NetNode n)	{
		if (interrupted)
			return;

		setNote(n != null ? n.getFullText() : f.getPath());
		
		Vector v = null;
		if (n != null)	{	// root node or drive
			v = n.listSilent();

			if (v != null && searchArchives)	{
				v = (Vector)v.clone();	// do not let created archive folders come into list
			}
		}
		else	{	// below first level
			String [] sarr = f.list();
			v = new Vector(sarr != null ? sarr.length : 0);
			for (int i = 0; sarr != null && i < sarr.length; i++)
				v.addElement(new File(f, sarr[i]));
		}
			
		if (v == null || v.size() <= 0)	{
			if (n != null && n.isLeaf())	{	// add target for containment search
				v = new Vector(1);
				v.addElement(n);
			}
			else
				return;
		}
		
		// loop all file objects: files, dirs, archives
		for (int i = 0; i < v.size(); i++)	{
			if (interrupted)
				return;
				
			progress();

			Object o = v.elementAt(i);
			File file;
			if (o instanceof NetNode)
				file = (File)((NetNode)o).getObject();
			else
				file = (File)o;
			
			//System.err.println("  ... searching in: "+file);
			Vector found;
			if ((found = condition.match(file)) != null)	{
				//System.err.println("    ... and found files: "+found);
				addFound(found);
			}
		}

		// containers after leafs
		for (int i = 0; i < v.size(); i++)	{
			if (interrupted)
				return;
				
			Object o = v.elementAt(i);
			if (o instanceof NetNode)	{
				NetNode nn = (NetNode)o;
				if (nn.isLeaf() == false)
					getAllMatches((File)nn.getObject(), nn.isManipulable() ? null : nn);
			}
			else	{
				File file = (File)o;
				if (file.isDirectory())
					getAllMatches(file, null);
			}
		}
	}
	
	
	/** Animates the progress label */
	public void progress()	{
		//Thread.yield();	// give interrupt a chance: VM does not stop Thread if it never waits for something
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				lb_progress.progress();
			}
		});
	}


	private void addFound(final Vector files)	{
		if (interrupted)
			return;
			
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				for (Enumeration e = files.elements(); e.hasMoreElements(); )	{
					File f = (File)e.nextElement();
					
					if (!interrupted)	{
						NetNode n = NodeLocate.fileToNetNode(
								(NetNode)root.getUserObject(),
								FileUtil.getPathComponents(f, OS.isWindows));

						model.addRow(n);
						status.setText("Found "+data.size()+" Items");
					}
				}
			}
		});
	}
	
	
	
	
	// interface SearchResultDispatcher
	
	public void showGrepResult(
		final File f,
		final int found,
		final String lines,
		final String pattern,
		final String syntax,
		final boolean ignoreCase,
		final boolean wordMatch)
	{
		if (interrupted)
			return;
			
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				if (interrupted == false)	{
					ensureSearchResultFrame().addFoundLines(f, found, lines, pattern, syntax, ignoreCase, wordMatch);
				}
			}
		});
	}

	private SearchResultFrame ensureSearchResultFrame()	{
		if (searchResult == null)
			searchResult = new SearchResultFrame();
		return searchResult;
	}


	public boolean isReplacing()	{
		return isReplacing;
	}

	public void openReplacement(SearchFile comingFile)	{
		ensureSearchReplaceWriter().openReplacement(comingFile);
	}

	public void replaceLine(String line, String newline, REMatch [] matches, boolean wordMatch)	{
		ensureSearchReplaceWriter().replaceLine(line, newline, matches, replaceString, wordMatch);
	}

	public void replaceText(String text, String newline, REMatch [] matches, boolean wordMatch)	{
		ensureSearchReplaceWriter().replaceText(text, newline, matches, replaceString, wordMatch);
	}

	public void closeReplacement(SearchFile passedFile)	{
		ensureSearchReplaceWriter().closeReplacement(passedFile);
	}
	
	private SearchReplaceWriter ensureSearchReplaceWriter()	{
		if (searchReplaceWriter == null)
			searchReplaceWriter = new SearchReplaceWriter();
		return searchReplaceWriter;
	}
		

	// interface SearchResultDispatcher
	
	/**
		Used by observed object to ask for user dialog cancel. 
		@return true if observed object should end interrupted
	*/
	public boolean canceled()	{
		return interrupted;
	}

	/**
		Used by observed object to tell the observer about done work.
		Calls progress() here.
		@param portion e.g. written bytes
	*/
	public void progress(long portion)	{
		progress();
	}

	/**
		Used when observed object changes. Does nothing here. 
	*/
	public void setNote(final String note)	{
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				status.setText(note);
			}
		});
	}

	/**
		Used to end the progress dialog. Does nothing here.
	*/
	public void endDialog()	{
	}

}



class SearchComboBox extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile = null;


	public SearchComboBox()	{
		this(new File(HistConfig.dir()+"SearchName.list"));
	}
	
	/** Anlegen einer SearchComboBox.
		@param f file aus dem die Datei-Patterns zu lesen sind. */
	public SearchComboBox(File f)	{
		super();
		manageTypedHistory(this, f);
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