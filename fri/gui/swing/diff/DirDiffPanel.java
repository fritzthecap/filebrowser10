package fri.gui.swing.diff;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import fri.util.sort.quick.QSort;
import fri.util.sort.quick.Comparator;
import fri.util.diff.*;
import fri.util.props.ClassProperties;
import fri.gui.CursorUtil;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.tree.TreeExpander;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.progressdialog.CancelProgressDialog;

/**
	A Panel that hosts two trees that render the result of a Diff
	comparison.
*/

public class DirDiffPanel extends DiffPanel implements
	TreeExpansionListener,
	TreeSelectionListener
{
	private DirDiffTree tree1, tree2;
	private DiffFileTree treeWrapper1, treeWrapper2;
	private HistCombo filter;
	private JComboBox include;
	private JButton filediff, expandall;
	private int lineNr;	// temporary line number when scanning directory
	private CancelProgressDialog observer;
	
	
	/** New empty window. */
	public DirDiffPanel()	{
		this(null, null, null, true);
	}

	/**
		New window from a two File directory objects.
		@param file1 left File object to render, can be null.
		@param file2 right File object to render, can be null.
	*/
	public DirDiffPanel(File file1, File file2)	{
		this(file1, file2, (String)null, true);
	}
	
	/**
		New window from a two File directory objects.
		@param pattern "*.java" if only .java files should be compared
		@param include if true, only files matching pattern are compared, if false, all others
	*/
	public DirDiffPanel(String pattern, boolean include)	{
		this(null, null, pattern, include);
	}

	/**
		New window from a two File directory objects.
		@param file1 left File object to render, can be null.
		@param file2 right File object to render, can be null.
		@param pattern "*.java" if only .java files should be compared
		@param include if true, only files matching pattern are compared, if false, all others
	*/
	public DirDiffPanel(File file1, File file2, String pattern, boolean incl)	{
		super(file1, file2);
		
		build();
		
		if (pattern != null)	{
			filter.setText(pattern);
			include.setSelectedItem(incl ? "Include" : "Exclude");
		}
		
		setFiles(file1, file2);

		recompare.setToolTipText("Re-Compare Directories");
	}



	/** Returns the popupmenus from textareas or customization. */
	public JPopupMenu [] getPopups()	{
		return new JPopupMenu[] { tree1.getPopupMenu(), tree2.getPopupMenu() };
	}


	/** Create a color-enabled JTree for the diffs. */
	protected DirDiffTree createTree()	{
		return new DirDiffTree(this);
	}

	protected void buildViewers()	{	
		tree1 = createTree();
		tree2 = createTree();
		
		tree1.addTreeExpansionListener(this);
		tree2.addTreeExpansionListener(this);
		
		tree1.addTreeSelectionListener(this);
		tree2.addTreeSelectionListener(this);
	}

	protected void buildToolbarOptions(JToolBar tb)	{
		filter = new DirDiffFilterCombo();
		if (filter.getModel().getSize() <= 0)	{
			filter.addItem("*");
			filter.addItem("*.java");
			filter.addItem("*.java|*.properties");
			filter.addItem("*.java|*.properties|*.xml");
		}
		filter.addActionListener(this);
		filter.setToolTipText("Filter Files To Compare");
		tb.add(filter);
		
		include = new JComboBox()	{
			public Dimension getMaximumSize()	{
				return new Dimension(70, super.getMaximumSize().height);
			}
		};
		include.addItem("Include");
		include.addItem("Exclude");
		include.setMinimumSize(new Dimension(70, include.getMinimumSize().height));
		include.setEditable(false);
		include.setToolTipText("Filter Works Ex- Or Including");
		String prop = ClassProperties.get(getClass(), "IncludeFilter");
		if (prop != null)
			include.setSelectedItem(prop);
		
		tb.add(include);

		tb.addSeparator();
		
		filediff = new JButton(Icons.get(Icons.diff));
		filediff.setToolTipText("Show Details Of File Differences");
		filediff.addActionListener(this);
		filediff.setEnabled(false);
		tb.add(filediff);

		expandall = new JButton(Icons.get(Icons.tree));
		expandall.setToolTipText("Expand All Branches");
		expandall.addActionListener(this);
		tb.add(expandall);

		super.buildToolbarOptions(tb);
	}


	/** Do not add this button, as it would trigger a fully new comparison. */
	protected void buildToolbarToggleLeftRight(JToolBar tb)	{
	}
	

	protected void addDndListeners()	{		
		new DirDndListener(this, tree1);
		new DirDndListener(this, tree2);
	}


	protected JComponent getView1()	{
		return tree1;
	}

	protected JComponent getView2()	{
		return tree2;
	}


	/** Catches Enter on Filter ComboBox. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == filter)	{
			setFiles(file1, file2);
		}
		else
		if (e.getSource() == filediff)	{
			tree1.fileDiffsForSelection();
		}
		else
		if (e.getSource() == expandall)	{
			TreeExpander.expandAllBranches(tree1);
		}
		else	{
			super.actionPerformed(e);
		}
	}


	/** Implements TreeSelectionListener to synchronize both selections. */
	public void valueChanged(TreeSelectionEvent e)	{
		DirDiffTree source = (DirDiffTree)e.getSource();
		DirDiffTree other  = source == tree1 ? tree2 : tree1;
		int [] rows = source.getSelectionRows();
		
		filediff.setEnabled(source.canShowFileDiffsOnSelection(rows));

		other.removeTreeSelectionListener(this);
		other.setSelectionRows(rows);
		other.addTreeSelectionListener(this);
	}
	

	/** Implements TreeExpansionListener to synchronize expansion on both sides. */
	public void treeExpanded(TreeExpansionEvent e)	{
		treeExpandEvent(e, true);
	}
	
	/** Implements TreeExpansionListener to synchronize expansion on both sides. */
	public void treeCollapsed(TreeExpansionEvent e)	{
		treeExpandEvent(e, false);
	}

	private void treeExpandEvent(TreeExpansionEvent e, boolean expand)	{
		JTree eventTree = e.getSource() == tree1 ? tree1 : tree2;
		JTree otherTree = e.getSource() == tree1 ? tree2 : tree1;
		otherTree.removeTreeExpansionListener(this);
		
		TreePath tp = e.getPath();
		int row = eventTree.getRowForPath(tp);
		if (expand)
			otherTree.expandRow(row);
		else
			otherTree.collapseRow(row);
			
		otherTree.addTreeExpansionListener(this);
	}
	
	
	
	// argument checking: normal files?
	protected boolean checkValidFile(File file)	{
		if (file == null || file.isDirectory() == false)	{
			if (file != null)	{
				JOptionPane.showMessageDialog(
						this,
						"\""+file+"\" does not exist or is not a directory!",
						"Directory Error",
						JOptionPane.ERROR_MESSAGE);
			}
			return false;
		}
		return true;
	}


	/** Overridden to set both files to null to avoid comparison with an old directory. */
	public void setFiles(File file1, File file2)	{
		this.file1 = this.file2 = null;
		super.setFiles(file1, file2);
	}

	/** Set a file into one view. @param isLeft true if first view. */
	protected void setFile(File file, boolean isLeft)	{
		// set one file, working in background
		DirDiffTree tree;
		JTextField tf;
		
		if (isLeft)	{
			file1 = file;
			loadTime1 = file.lastModified();
			tree = tree1;
			tf = tf1;
		}
		else	{
			file2 = file;
			loadTime2 = file.lastModified();
			tree = tree2;
			tf = tf2;
		}
		
		// set only one node into tree when there is no peer directory defined
		tree.setDefaultModel(file.getName()+" ...");
		tf.setText(file.getAbsolutePath());

		//System.err.println("DirDiffPael, setFile "+file1+" and "+file2);
		if (file1 != null && file2 != null)	{	// start comparing the directories in background
			compare();
		}
	}


	/** Start comparison in thread. */
	protected synchronized void compare()	{
		//System.err.println("compare directories ...");
		if (observer != null)	{	// loading
			return;	// ignore. TODO: show an error message!
		}
			
		try	{
			treeWrapper1 = new DiffFileTree(
					file1,
					ignoreSpaces.isSelected(),
					exceptLeadingSpaces.isSelected(),
					filter.getText(),
					include.getSelectedItem().equals("Include"));
			treeWrapper2 = new DiffFileTree(
					file2,
					ignoreSpaces.isSelected(),
					exceptLeadingSpaces.isSelected(),
					filter.getText(),
					include.getSelectedItem().equals("Include"));
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		
		observer = new CancelProgressDialog(this, "Comparing  "+file1+"  with  "+file2);

		Runnable runnable = new Runnable()	{
			public void run()	{
				// wait until frame window is showing
				while (ComponentUtil.getFrame(DirDiffPanel.this) == null || ComponentUtil.getFrame(DirDiffPanel.this).isShowing() == false)	{
					try	{ Thread.sleep(500); }	catch (InterruptedException e)	{}
				}
				
				CursorUtil.setWaitCursor(DirDiffPanel.this);
				// do the work
				observer.setNote("Exploring ...");	// show immediately

				treeWrapper1.equalsCompound(treeWrapper2, observer);	// calls endDialog()
			}
		};
		
		Runnable finish = new Runnable()	{
			public void run()	{
				renderDiffs();
			}
		};
		
		observer.start(runnable, finish);
	}
	
	


	/** Comparison has finished in thread, render results. */
	protected synchronized void renderDiffs()	{
		try	{
			// compare the files, render diffs, fill overview combo
			diffList.removeActionListener(this);
			diffList.removeAllItems();
			lineNr = 1;	// root is line 0, is not in list
			
			int diffs = formatWrapperTree(treeWrapper1, treeWrapper2);
			
			diffList.addActionListener(this);
			diffList.takePopupSize();
			diffCount.setText(""+diffs+" Diffs: ");
			
			tree1.setDiffFileTree(treeWrapper1, tree2);
			tree2.setDiffFileTree(treeWrapper2, tree1);
	
			// Bug on LINUX: trees with different row heights!
			tree1.ensureSameRowHeight(tree2);
			
			observer = null;
			treeWrapper1 = treeWrapper2 = null;
	
			restoreViewPosition();
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}

	private int formatWrapperTree(DiffFileTree tw1, DiffFileTree tw2)	{
		// delete elements with no changeflag from both lists
		deleteUnchangedElements(tw1);
		deleteUnchangedElements(tw2);

		Vector v1 = tw1.getChildren();
		Vector v2 = tw2.getChildren();

		// sort both lists by: inserted, changed, deleted
		v1 = sortElements(v1);
		v2 = sortElements(v2);
		
		// make these groups same length on both sides
		makeSameLength(v1, v2);
		
		tw1.setChildren(v1);
		tw2.setChildren(v2);
		
		int diffs = 0;
		
		// now collect changes and repeat recursively
		for (int i = 0; i < v1.size(); i++)	{
			DiffFileTree t1 = (DiffFileTree)v1.get(i);
			DiffFileTree t2 = (DiffFileTree)v2.get(i);
			//System.err.println("tree formatted: "+t1+"("+(t1.getChildren() != null)+")	<->	"+t2+"("+(t2.getChildren() != null)+")");
			
			if (t1.getChangeFlag() != null || t2.getChangeFlag() != null)	{
				DiffFileTree t = t1.getChangeFlag() != null ? t1 : t2;
				String flag = t.getChangeFlag();
				
				if (!flag.equals(DiffChangeFlags.CHANGED) || !t.getFile().isDirectory())	{
					String name = t.getFile().getName();
					diffList.addItem(new DirComboDiffItem(flag, name, lineNr));
					diffs++;
				}
			}
			
			lineNr++;

			if (t1.getChildren() != null && t2.getChildren() != null)	{
				diffs += formatWrapperTree(t1, t2);
			}
		}
		
		return diffs;
	}


	private void deleteUnchangedElements(DiffFileTree tw)	{
		Vector v = tw.getChildren();
		
		for (Iterator it = v.iterator(); it.hasNext(); )	{
			DiffFileTree twr = (DiffFileTree)it.next();
			
			if (twr.getChangeFlag() == null)	{
				it.remove();
			}
		}
	}


	private Vector sortElements(Vector v)	{
		// class that sorts the children of both sides by their change flag
		class DiffGroupSorter implements Comparator
		{
			public int compare(Object o1, Object o2)	{
				DiffFileTree tw1 = (DiffFileTree)o1;
				DiffFileTree tw2 = (DiffFileTree)o2;
				String flag1 = tw1.getChangeFlag();
				String flag2 = tw2.getChangeFlag();
				
				if (flag1.equals(flag2))	{
					if (tw1.getChildren() != null && tw2.getChildren() == null)
						return -1;	// directories first
					if (tw1.getChildren() == null && tw2.getChildren() != null)
						return 1;	// directories first
					// same type: name order
					return tw1.getFile().getName().toLowerCase().compareTo(tw2.getFile().getName().toLowerCase());
				}

				return assignPosition(flag1) < assignPosition(flag2) ? -1 : 1;
			}
			
			private int assignPosition(String flag)	{
				if (flag.equals(DiffChangeFlags.DELETED))
					return 1;	// map to first position
				else
				if (flag.equals(DiffChangeFlags.CHANGED))
					return 2;	// map to middle position
				else
					return 3;
			}
		}

		v = new QSort(new DiffGroupSorter()).sort(v);
		return v;
	}


	private void makeSameLength(Vector v1, Vector v2)	{
		// count inserted and deleted elements
		int deletedCount1 = 0, insertedCount2 = 0;
		int changedCount2 = 0;

		for (int i = 0; i < v1.size(); i++)	{
			DiffFileTree tw1 = (DiffFileTree)v1.get(i);
			if (tw1.getChangeFlag().equals(DiffChangeFlags.DELETED))	{
				deletedCount1++;
			}
		}
		for (int i = 0; i < v2.size(); i++)	{
			DiffFileTree tw2 = (DiffFileTree)v2.get(i);
			if (tw2.getChangeFlag().equals(DiffChangeFlags.INSERTED))	{
				if (insertedCount2 == 0)
					changedCount2 = i;

				insertedCount2++;
			}
		}
		
		// keep order of following code!!!
		for (int i = 0; i < insertedCount2; i++)	{
			DiffFileTree tw2 = (DiffFileTree)v2.get(i + changedCount2);
			v1.add(new DiffFileTree(tw2.getChildren() != null));
		}
		for (int i = 0; i < deletedCount1; i++)	{
			DiffFileTree tw1 = (DiffFileTree)v1.get(i);
			v2.add(i, new DiffFileTree(tw1.getChildren() != null));
		}
	}



	
	protected class DirComboDiffItem extends ComboDiffItem
	{
		private String filename;
		private int currentLine;
		
		DirComboDiffItem(String changeFlag, String filename, int treeRow)	{
			super(changeFlag, treeRow);
			this.filename = filename;
		}
		
		public void showItem()	{
			JTree tree = changeFlag.equals(DiffChangeFlags.DELETED) ? tree2 : tree1;
			currentLine = 0;
			DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
			DefaultMutableTreeNode node = searchNodeForLine(root);
			TreePath tp = new TreePath(node.getPath());
			tree.expandPath(tp);
			tree.setSelectionPath(tp);
			System.err.println("selecting tree row "+visibleLine+", treepath "+tp+" level"+node.getLevel());
		}

		public String toString()	{
			return
					(changeFlag.equals(DiffChangeFlags.INSERTED) ? "Right Inserted: " :
					changeFlag.equals(DiffChangeFlags.DELETED) ? "Left Deleted: " :
					"Changed: ")+
					filename;
		}
		
		private DefaultMutableTreeNode searchNodeForLine(DefaultMutableTreeNode root)	{
			for (int i = 0; i < root.getChildCount(); i++)	{
				currentLine++;
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)root.getChildAt(i);
				if (currentLine == visibleLine || (node = searchNodeForLine(node)) != null)
					return node;
			}
			return null;
		}
	}



	protected int getFileDialogMode()	{
		return JFileChooser.DIRECTORIES_ONLY;
	}



	/** Implements WindowListener to interrupt loading files. */
	public synchronized void windowClosing(WindowEvent e)	{
		filter.save();
		ClassProperties.put(getClass(), "IncludeFilter", include.getSelectedItem().toString());
		ClassProperties.store(getClass());
		
		if (observer != null)	{
			observer.setCanceled();
			observer.endDialog();
		}
	}
	



	// test main
	public static void main(String [] args)	{
		File f1 = new File("fri/util/diff");
		File f2 = new File("old/diff/");
		if (args.length == 2)	{
			f1 = new File(args[0]);
			f2 = new File(args[1]);
		}
		DirDiffPanel p = new DirDiffPanel(f1, f2);
		JFrame f = new JFrame("Directory Differences");
		f.addWindowListener(p);
		f.getContentPane().add(p);
		f.setSize(600, 400);
		f.setVisible(true);
	}
	
}



class DirDiffFilterCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist;
	private static File globalFile = null;

	public DirDiffFilterCombo()	{
		super();
		manageTypedHistory(this, new File(HistConfig.dir()+"DirDiffFilter.list"));
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
	
	public Dimension getMinimumSize()	{
		return new Dimension(100, super.getMinimumSize().height);
	}
}