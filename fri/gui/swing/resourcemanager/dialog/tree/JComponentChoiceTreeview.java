package fri.gui.swing.resourcemanager.dialog.tree;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import fri.gui.awt.resourcemanager.persistence.HierarchicalName;
import fri.gui.swing.tree.*;

/**
	Every Component/MenuComponent of the ResourceManager's Window is represented by a Button.
	Default callback on that Button opens a customize dialog for the associated Component/MenuComponent.
*/

public class JComponentChoiceTreeview extends JTree implements
	KeyListener,
	MouseListener
{
	private Map map = new Hashtable();
	
	public JComponentChoiceTreeview()	{
		super(new DefaultTreeModel(new DefaultMutableTreeNode("Component Tree(s)")));
		setCellRenderer(new DelegatingTreeCellRenderer());
		setUI(new VariableRendererWidthTreeUI());
		addMouseListener(this);
		addKeyListener(this);
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		setToolTipText("");
		// setRowHeight(20);
	}
	
	public void addComponentTreeNode(String hierarchicalName, AbstractButton component)	{
		DefaultMutableTreeNode newChild;
		
		if (hierarchicalName.indexOf(HierarchicalName.HIERARCHICAL_SEPARATOR) < 0)	{	// is the root of one of the component trees
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
			newChild = new DefaultMutableTreeNode(hierarchicalName);
			root.add(newChild);
		}
		else	{	// is not a component tree root
			int lastIndex = hierarchicalName.lastIndexOf(HierarchicalName.HIERARCHICAL_SEPARATOR);
			String name = hierarchicalName.substring(lastIndex + 1);
			newChild = new DefaultMutableTreeNode(name);
			String path = hierarchicalName.substring(0, lastIndex);
			DefaultMutableTreeNode parent = findOrCreate((DefaultMutableTreeNode) getModel().getRoot(), path);
			parent.add(newChild);
		}
		
		map.put(newChild, component);
	}

	private DefaultMutableTreeNode findOrCreate(DefaultMutableTreeNode root, String path)	{
		DefaultMutableTreeNode result = null;
		for (StringTokenizer stok = new StringTokenizer(path, HierarchicalName.HIERARCHICAL_SEPARATOR); stok.hasMoreTokens(); )	{
			String name = stok.nextToken();
			boolean found = false;
			for (int i = 0; found == false && i < root.getChildCount(); i++)	{
				DefaultMutableTreeNode n = (DefaultMutableTreeNode) root.getChildAt(i);
				String s = n.toString();
				if (s.equals(name))	{
					result = n;
					found = true;
				}
			}
			
			if (found == false)	{
				result = new DefaultMutableTreeNode(name); 
				root.add(result);
			}
			root = result;
		}
		return result;
	}


	public String getToolTipText(MouseEvent e)	{
		int row = getRowForLocation(e.getX(), e.getY());
		if (row > 0)	{
			TreePath tp = getPathForRow(row);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
			AbstractButton button = (AbstractButton) map.get(node);
			if (button != null)	{
				return button.getToolTipText();
			}
		}
		return super.getToolTipText(e);
	}


	private boolean openCallback(TreePath tp)	{
		if (tp != null)	{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
			AbstractButton button = (AbstractButton) map.get(node);
			if (button != null)	{
				button.doClick();
				return true;
			}
		}
		return false;
	}

	/** Implements MouseListener to catch open event. */
	public void mouseClicked(MouseEvent e)	{
		if (e.getClickCount() >= 2)	{
			int row = getRowForLocation(e.getX(), e.getY());
			if (row > 0)	{
				if (openCallback(getPathForRow(row)))
					e.consume();
			}
		}
	}
	public void mousePressed(MouseEvent e)	{}
	public void mouseReleased(MouseEvent e)	{}
	public void mouseEntered(MouseEvent e)	{}
	public void mouseExited(MouseEvent e)	{}
	

	/** Implements KeyListener to catch open event. */
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_ENTER)	{
			if (openCallback(getSelectionPath()))
				e.consume();
		}
	}
	public void keyReleased(KeyEvent e)	{}
	public void keyTyped(KeyEvent e)	{}


	public void expandAllBranches() {
		int rows = -1, newRows = 0;
		while (rows < newRows) {
			rows = getRowCount();
			for (int i = 0; i < rows; i++) {
				expandRow(i);
			}
			newRows = getRowCount();
		}
	}
	

	private class DelegatingTreeCellRenderer extends DefaultTreeCellRenderer
	{
		private Border selectedBorder;
		
		public Component getTreeCellRendererComponent(
			JTree tree,
			Object value,
			boolean selected,
			boolean expanded,
			boolean leaf,
			int row,
			boolean hasFocus)
		{
			if (selectedBorder == null)	{
				selectedBorder = BorderFactory.createLineBorder(getBorderSelectionColor());
			}
			
			TreePath tp = getPathForRow(row);
			if (tp != null)	{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
				if (node != null)	{
					AbstractButton button = (AbstractButton) map.get(node);
					if (button != null)	{
						Border b = button.getBorder();	// look at the border, was it selected?
						if (b instanceof CompoundBorder)	{
							CompoundBorder cb = (CompoundBorder)b;
							if (cb.getOutsideBorder() == selectedBorder)
								b = cb.getInsideBorder();	// retrieve original border from compound selection border
						}
						// set a selection or non-selection border to button
						button.setBorder(selected ? BorderFactory.createCompoundBorder(selectedBorder, b) : b);
						return button;
					}
				}
			}
			return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		}
	}

}
