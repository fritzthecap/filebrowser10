package fri.gui.swing.diff;

import java.io.File;
import java.text.DateFormat;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import fri.util.NumberUtil;
import fri.util.diff.*;
import fri.gui.CursorUtil;
import fri.gui.swing.tree.VariableRendererWidthTreeUI;

/**
	Tree that renders BalancedLines from a Diff comparison,
	attaching colors to their background. The BalancedLines.Line
	contains a File as Object.
*/

public class DirDiffTree extends JTree implements
	MouseListener,	// to open popup
	KeyListener,	// open, find
	ActionListener	// open, find
{
	private DiffFileTree treeWrapper;
	private DirDiffTree peer;
	private FileObjectOpenDialog dirLoader;
	private JMenuItem load;
	private JPopupMenu popup;
	public static DateFormat dateFormater =
			DateFormat.getDateTimeInstance(
				DateFormat.SHORT,
				DateFormat.SHORT);	


	public DirDiffTree(FileObjectOpenDialog dirLoader)	{
		super();
		
		this.dirLoader = dirLoader;
		
		setEditable(false);	// no editing here
		setToolTipText("");	// Bug? No tooltip without that ...
		
		build();
		
		addMouseListener(this);
		addKeyListener(this);
		
		setUI(new VariableRendererWidthTreeUI());
	}

	
	public void setDefaultModel(String dirName)	{
		TreeNode root = new DefaultMutableTreeNode(dirName)	{
			public boolean getAllowsChildren() {
				return true;
			}
		};
		DefaultTreeModel tm = new DefaultTreeModel(root);
		tm.setAsksAllowsChildren(true);
		setModel(tm);
	}
	

	/** Shows file information in tooltip. */
	public String getToolTipText(MouseEvent e)	{
		if (getRowForLocation(e.getX(), e.getY()) >= 0)	{
			TreePath currPath = getPathForLocation(e.getX(), e.getY());
			DefaultMutableTreeNode d = (DefaultMutableTreeNode)currPath.getLastPathComponent();
			
			if (d.getUserObject() instanceof DiffFileTree)	{
				DiffFileTree n = (DiffFileTree)d.getUserObject();
				File file = n.getFile();
				
				if (file != null)	{
					String readwrite = (file.canRead() ? "Read" : "");
					if (readwrite.equals(""))
						readwrite = (file.canWrite() ? "Write" : "-");
					else
						readwrite += (file.canWrite() ? "/Write" : "Only");
		
					long size = file.length();
					String len = NumberUtil.getFileSizeString(size, false);
		
					String date = dateFormater.format(new Date(file.lastModified()));
		
					String tooltip =
							(len.equals("") ? "" : len+"  |  ")+
							date+"  |  "+
							readwrite;
					
					return tooltip;
				}
			}
		}
		return null;
	}


	private void build()	{
		popup = new JPopupMenu();
		popup.add(load = new JMenuItem("Open"));
		load.addActionListener(this);
		
		setModel(null);
	}

	
	public JPopupMenu getPopupMenu()	{
		return popup;
	}
	
	
	/** Overrides actionPerformed() to catch load menuitem. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == load)	{
			dirLoader.openFile(this);
		}
	}


	/** Load passed directory into tree. */
	public void load(File dir)	{
		dirLoader.load(dir, this);
	}
	

	
	public DiffFileTree getDiffFileTree()	{
		return treeWrapper;
	}

	/** Set a new Document from diff result lines. */
	public void setDiffFileTree(DiffFileTree treeWrapper, DirDiffTree peer)	{
		this.treeWrapper = treeWrapper;
		this.peer = peer;
		
		TreeNode root = new DirDiffTreeNode(treeWrapper);
		DefaultTreeModel tm = new DefaultTreeModel(root);
		tm.setAsksAllowsChildren(true);
		setModel(tm);
		
		setCellRenderer(new DirDiffTreeCellRenderer());	// a colored background for cells
	}


	/** Associate colors with changed states. */
	protected Color getColorForChangeFlag(String changeFlag)	{
		return DiffTextArea.getColorForChangeFlag(changeFlag, this);
	}



	/** implements MouseListener: popup the menu. */	
	public void mousePressed(MouseEvent e)	{
		if (e.isPopupTrigger())
			popup.show(this, e.getX(), e.getY());
	}
	/** implements MouseListener: popup the menu. */	
	public void mouseReleased(MouseEvent e)	{
		if (e.isPopupTrigger())
			popup.show(this, e.getX(), e.getY());
	}

	/** implements MouseListener: double click - open file diff window. */	
	public void mouseClicked(MouseEvent e)	{
		if (e.getClickCount() >= 2)	{
			int row = getRowForLocation(e.getX(), e.getY());
			fileDiffsForTreeRow(row);
		}
	}

	public void mouseEntered(MouseEvent e)	{
	}
	public void mouseExited(MouseEvent e)	{
	}


	public void fileDiffsForSelection()	{
		int [] rows = getSelectionRows();
		
		for (int i = 0; rows != null && i < rows.length; i++)	{
			fileDiffsForTreeRow(rows[i]);
		}
	}

	public boolean canShowFileDiffsOnSelection(int [] rows)	{
		if (rows == null)
			return false;
			
		for (int i = 0;  i < rows.length; i++)	{
			TreePath tp = getPathForRow(rows[i]);
			Object o = tp.getLastPathComponent();
	
			if (o instanceof DirDiffTreeNode)	{
				DirDiffTreeNode node = (DirDiffTreeNode)o;
				DiffFileTree tw = node.getDiffFileTree();
				
				if (peer == null ||
						node.getAllowsChildren() ||
						tw.getChangeFlag() == null ||
						!tw.getChangeFlag().equals(DiffChangeFlags.CHANGED))
					return false;
			}
			else	{
				return false;
			}
		}
		return true;
	}
	
	private void fileDiffsForTreeRow(int row)	{
		TreePath tp = getPathForRow(row);
	
		if (tp != null && canShowFileDiffsOnSelection(new int [] { row }))	{
			DirDiffTreeNode node = (DirDiffTreeNode)tp.getLastPathComponent();
			DiffFileTree tw = node.getDiffFileTree();
			
			TreePath tp2 = peer.getPathForRow(row);
			DirDiffTreeNode node2 = (DirDiffTreeNode)tp2.getLastPathComponent();
			DiffFileTree tw2 = node2.getDiffFileTree();
			
			CursorUtil.setWaitCursor(this);
			try	{
				new FileDiffFrame(tw.getFile(), tw2.getFile());
			}
			finally	{
				CursorUtil.resetWaitCursor(this);
			}
		}
	}
	

	/** implements KeyListener: Ctl-O for open, F3, Ctl-F for find. */	
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_O && e.isControlDown())
			dirLoader.openFile(this);
//		else
//		if (e.getKeyCode() == KeyEvent.VK_F3 || e.getKeyCode() == KeyEvent.VK_F && e.isControlDown())
//			find();
	}
	public void keyReleased(KeyEvent e)	{
	}
	public void keyTyped(KeyEvent e)	{
	}


	/** Bug on LINUX: trees with different row heigths! This method ensures they have same height. */
	public void ensureSameRowHeight(JTree tree2)	{
		setRowHeight(tree2.getRowHeight());	// does not work, just for correctness

		// need to set a variable in cell renderers to same value
		DirDiffTreeCellRenderer tr1 = (DirDiffTreeCellRenderer)getCellRenderer();
		DirDiffTreeCellRenderer tr2 = (DirDiffTreeCellRenderer)tree2.getCellRenderer();
		Dimension d1 = tr1.getPreferredSize();
		Dimension d2 = tr2.getPreferredSize();
		if (d1.height <= 0 || d2.height <= 0)
			throw new IllegalStateException("Applied ensureSameRowHeight in wrong state, having common height <= 0!");
			
		tr1.commonHeight = tr2.commonHeight = Math.min(d1.height, d2.height);
	}



	/** DirDiff Tree node. */
	private class DirDiffTreeNode extends DefaultMutableTreeNode
	{
		DirDiffTreeNode(DiffFileTree treeWrapper)	{
			super(treeWrapper, true);
		}
		
		private DiffFileTree getDiffFileTree()	{
			return (DiffFileTree)getUserObject();
		}

		public boolean getAllowsChildren() {
			return getDiffFileTree().getChildren() != null;
		}
		
		/** Called when enumerating children, time to fill children list. */
		public int getChildCount()	{
			if (children != null)	{
				return children.size();
			}
			
			children = new Vector();
			Vector v = getDiffFileTree().getChildren();

			for (int i = 0; v != null && i < v.size(); i++)	{
				DiffFileTree tdw = (DiffFileTree)getDiffFileTree().getChildren().get(i);
				DirDiffTreeNode n = new DirDiffTreeNode(tdw);
				n.setParent(this);
				children.add(n);
			}
			
			return children.size();
		}
	
		public String toString()	{
			String s;
			if (getDiffFileTree().getFile() != null)
				s = getDiffFileTree().getFile().getName();
			else
				s = super.toString();
			return s;
		}
	}


	

	// CellRenderer to set colors into lines
	private class DirDiffTreeCellRenderer extends DefaultTreeCellRenderer
	{
		private Color color = null;
		int commonHeight = -1;	// accessible by tree
		
		public Component getTreeCellRendererComponent(
			JTree tree,
			Object value,
			boolean selected,
			boolean expanded,
			boolean leaf,
			int row,
			boolean hasFocus)
		{
			hasFocus = selected;
			Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

			if (color == null)	{
				color = getBackgroundNonSelectionColor();
			}
			else	{
				setBackgroundNonSelectionColor(color);
				setBackgroundSelectionColor(color);
			}
					
			try	{
				DirDiffTreeNode node = (DirDiffTreeNode)value;
				String flag = node.getDiffFileTree().getChangeFlag();
				Color clr = getColorForChangeFlag(flag);
				if (flag != null)	{
					setBackgroundNonSelectionColor(clr);
					setBackgroundSelectionColor(clr);
				}
			}
			catch (ClassCastException e)	{
			}
			
			return c;
		}
		

		/** Overridden to spread cell over tree width, and ensuring common height. */
		public Dimension getPreferredSize()	{
			Dimension d = super.getPreferredSize();

			d.width = DirDiffTree.this.getWidth();

			if (commonHeight > 0)
				d.height = commonHeight;

			//System.err.println("DirDiffTreeCellRenderer for tree "+DirDiffTree.this.hashCode()+" getPreferredSize: "+d);
			return d;
		}

	}
	
}