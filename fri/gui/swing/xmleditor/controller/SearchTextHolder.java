package fri.gui.swing.xmleditor.controller;

import java.util.List;
import java.util.Vector;
import java.awt.Component;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import fri.util.props.PropertiesList;
import fri.gui.text.TextHolder;
import fri.gui.swing.xmleditor.view.XmlTreeTable;
import fri.gui.swing.xmleditor.model.MutableXmlNode;
import fri.gui.swing.xmleditor.model.MutableXmlTreeTableModel;
import fri.gui.swing.xmleditor.model.XmlTreeTableModel;

/**
	TextHolder implementation that holds texts of a XmlTreeTable document.
	It offers methods to get all XML nodes and substitute them.
	The caret position is identical with selection for a treetable.
	The selection always affects a single tree row, never a range of rows.
	setText() and getText() methods are throwing exceptions and are
	over-implemented by methods that return and let set texts of single nodes.

	@author Fritz Ritzberger
*/

class SearchTextHolder implements
	TextHolder
{
	private XmlTreeTable treetable;


	SearchTextHolder(XmlTreeTable treetable)	{
		this.treetable = treetable;
	}

	/** Does nothing, as this is used only to restore caret position after replacement. */
	public void setCaretPosition(int pos)	{
	}

	/** Returns 0 if no selection present, else selection start. */
	public int getCaretPosition()	{
		int sel = getSelectionStart();
		return sel < 0 ? 0 : sel;
	}

	/** Returns the tree row that is selected (anchor), 0 if no selection. */
	public int getSelectionStart()	{
		return treetable.getSelectionModel().getAnchorSelectionIndex();
	}

	/** Returns getSelectionStart(), as this is not used by AbstractSearchReplace. */
	public int getSelectionEnd()	{
		return getSelectionStart();
	}

	/** Selects the tree row that represents start index, clears selection if start >= end or start < 0. */
	public void select(int start, int end)	{
		//System.err.println("SearchTextHolder selecting start "+start+" end "+end);
		if (start >= end || start < 0)	{
			treetable.getSelectionModel().clearSelection();
		}
		else	{
			treetable.getSelectionModel().setSelectionInterval(start, start);
		}
	}

	/** Returns empty string, as default text selection is done when textfield gets focus. */
	public String getSelectedText()	{
		return "";
	}

	/** Delegates to getTextComponent().requestFocus(). */
	public void requestFocus()	{
		getTextComponent().requestFocus();
	}

	/** Returns true if the treetable is enabled. */
	public boolean isEditable()	{
		return treetable.isEnabled();
	}

	/** Returns the XML treetable. */
	public Component getTextComponent()	{
		return treetable;
	}

	/** Throws IlleaglStateException, as there are many texts. Overrides in XmlSearchReplace manage this. */
	public String getText()	{
		throw new IllegalStateException("Can not be called for a XML treetable: getText()");
	}

	/** Throws IlleaglStateException, as there are many texts. Overrides in XmlSearchReplace manage this. */
	public void setText(String text)	{
		throw new IllegalStateException("Can not be called for a XML treetable: setText(text)");
	}


	// Implementation used by XmlSearchReplace

	/** Returns all MutableXmlNodes to search and replace. */
	public List getAllNodes()	{
		TreeNode root = (TreeNode)getTreeTableModel().getRoot();
		Vector v = new Vector();
		loopChildren(root, v);
		return v;
	}

	private void loopChildren(TreeNode node, List list)	{
		list.add(node);
		for (int i = 0; i < node.getChildCount(); i++){
			TreeNode tn = node.getChildAt(i);
			loopChildren(tn, list);
		}
	}

	/** Returns the text of passed node. */
	public String getText(MutableXmlNode node)	{
		return (String)getTreeTableModel().getValueAt(node, XmlTreeTableModel.LONGTEXT_COLUMN);
	}

	/** Returns the attribute list of passed node. Empty attributes are contained, too. */
	public PropertiesList getAttributes(MutableXmlNode node)	{
		try	{
			node.setSearchMode(true);
			return (PropertiesList)getTreeTableModel().getValueAt(node, XmlTreeTableModel.ATTRIBUTES_COLUMN);
		}
		finally	{
			node.setSearchMode(false);
		}
	}

	/** Returns the treetable model, needed when a updateNode() call happens. */
	public MutableXmlTreeTableModel getTreeTableModel()	{
		return (MutableXmlTreeTableModel)treetable.getTreeTableModel();
	}


	public MutableXmlNode getNodeForRow(int row)	{
		TreePath tp = treetable.getTree().getPathForRow(row);
		if (tp != null)
			return (MutableXmlNode)tp.getLastPathComponent();
		return null;
	}

	public int getRowForNode(MutableXmlNode node)	{
		TreePath tp = new TreePath(node.getPath());
		int row = treetable.getTree().getRowForPath(tp);
		return row;
	}

	public void expand(MutableXmlNode toExpand)	{
		TreePath tp = new TreePath(toExpand.getPath());
		treetable.getTree().expandPath(tp);
		treetable.getTree().addSelectionPath(tp);	// path not expanded: error in JDK?
		treetable.scrollPathToVisible(tp);
	}

}
