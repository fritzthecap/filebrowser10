package fri.gui.swing.treetable;
/*
 * @(#)AbstractTreeTableModel.java	1.2 98/10/27
 * 
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
 
import javax.swing.tree.*;

/**
 * An abstract implementation of the TreeTableModel interface,
 * containing basic methods of treetable model.
 * <p>
 * CHANGE FRi 990501: extends DefaultTreeModel, um einfuegen und loeschen zu koennen.
 * @version 1.2 10/27/98
 * @author Philip Milne
 */ 

public abstract class AbstractTreeTableModel extends DefaultTreeModel implements
	TreeTableModel
{
	public AbstractTreeTableModel(TreeNode root) {
		super(root);
	}

	public AbstractTreeTableModel(TreeNode root, boolean askChildren) {
		super(root, askChildren);
	}

	
	// Default implementations for methods in the TreeModel interface.

	/*
	public boolean isLeaf(Object node) {
		return getChildCount(node) == 0;
	}
	*/

	// This is not called in the JTree's default mode: use a naive implementation.
	public int getIndexOfChild(Object parent, Object child) {
		for (int i = 0; i < getChildCount(parent); i++) {
			if (getChild(parent, i).equals(child)) {
				return i;
			}
		}
		return -1;
	}

	/*
	public Class getColumnClass(int column) {
		return Object.class;
	}
	*/

	/**
	 * Making tree column editable causes the JTable to forward mouse
	 * and keyboard events in the Tree column to the underlying JTree.
	 */
	public boolean isCellEditable(Object node, int column) {
		return getColumnClass(column) == TreeTableModel.class;
	}

	/**
		This method does nothing, as subclasses might not need this method.
		If a subclass needs cell editing, it should override this.
	*/
	public void setValueAt(Object aValue, Object node, int column) {
	}

}
