package fri.gui.swing.xmleditor.controller;

import java.awt.Point;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.Vector;
import java.util.List;
import javax.swing.*;
import gnu.regexp.*;
import fri.util.props.*;
import fri.util.text.lines.TextlineList;
import fri.gui.text.TextHolder;
import fri.gui.swing.searchdialog.AbstractSearchReplace;
import fri.gui.swing.xmleditor.model.MutableXmlNode;
import fri.gui.swing.xmleditor.model.XmlTreeTableModel;
import fri.gui.swing.xmleditor.model.UpdateObjectAndColumn;

/**
	A Search and Replace implementation for XML TreeTable.
	It defines additional checkboxes for search semantics.
	This implementation depends strongly on the behaviour
	of AbstractSearchReplace, which calls search() after
	every replace- or replace-all-action. The delegate
	match list is a wrapper that uses TextlineList, which is
	the same as in SearchReplace class (made for text,
	same replace mechanism).

	@author Fritz Ritzberger
*/

public class XmlSearchReplace extends AbstractSearchReplace
{
	private static final String TEXTPREFIX = "TEXT";
	private static final String ATTRPREFIX = "ATTR";
	private boolean doTexts;
	private boolean doAttributes;
	private JCheckBox searchTexts, searchAttributes;
	private XmlEditController controller;


	/** Create a nonmodal search dialog that offers replacing only when textarea is editable. */
	public XmlSearchReplace(JFrame f, XmlEditController controller, TextHolder textarea) {
		super(f, textarea);
		this.controller = controller;
	}

	/** Input other options below search/replace textfields: search texts, search attributes. */
	protected JPanel createOtherOptions()	{
		// initialize variables from super constructor
		doTexts = PropertyUtil.checkClassProperty("searchTexts", getClass(), true);
		doAttributes = PropertyUtil.checkClassProperty("searchAttributes", getClass(), true);

		// build additional panel with XML options
		JPanel p = new JPanel(new GridLayout(1, 2));
		p.setBorder(BorderFactory.createTitledBorder("Search XML"));
		p.add(searchTexts = new JCheckBox("Element Texts", doTexts));
		p.add(searchAttributes = new JCheckBox("Attribute Values", doAttributes));
		searchTexts.addItemListener(this);
		searchAttributes.addItemListener(this);
		return p;
	}


	/** Overridden to catch local checkboxes. */
	public void itemStateChanged (ItemEvent e)	{
		if (e.getSource() == searchTexts || e.getSource() == searchAttributes)	{
			searchChanged = true;
		}
		else	{
			super.itemStateChanged(e);
		}
	}

	/** Overridden to save checkbox states. */
	protected void save()	{
		ClassProperties.put(getClass(), "searchTexts", searchTexts.isSelected() ? "true" : "false");
		ClassProperties.put(getClass(), "searchAttributes", searchAttributes.isSelected() ? "true" : "false");
		super.save();
	}

	/** Overridden to warn when all checkboxes disabled. */
	protected void search()	{
		if (searchTexts.isSelected() == false && searchAttributes.isSelected() == false)	{
			JOptionPane.showMessageDialog(
					window,
					"One of \""+searchTexts.getText()+"\" and \""+searchAttributes.getText()+"\" must be selected!",
					"Error",
					JOptionPane.ERROR_MESSAGE);
		}
		else	{
			super.search();
		}
	}

	/**
		Called on F3.<br>
		Caret is the row number of currently selected tree row.
		The node of this row must be retrieved, then the search result
		node with the same or next higher row number must be found and expanded.
		The index of this node in the list of search results is returned. 
	*/
	protected int getMatchIndexByCaretImpl(int caret)	{
		MutableXmlNode current = getSearchTextHolder().getNodeForRow(caret);
		SearchNodeTextList nodeList = (SearchNodeTextList)getListData();
		int match = nodeList.getNextMatchIndexFromNode(current);
		MutableXmlNode toExpand = nodeList.getNodeForMatchIndex(match);

		controller.ignoreSelectionChanged = true;
		getSearchTextHolder().expand(toExpand);
		controller.ignoreSelectionChanged = false;

		return match;
	}

	/**
		Called on result list selection change (show another result node).<br>
		Index is the number of the match to be shown. The tree node of the match
		must be retrieved and expanded.
		Returns the tree row of the retrieved result node, in a Point (row, row+1).
	*/
	protected Point getMatchRangeImpl(int match)	{
		setWaitCursor(true);
		try	{
			SearchNodeTextList nodeList = (SearchNodeTextList)getListData();
			MutableXmlNode toExpand = nodeList.getNodeForMatchIndex(match);
	
			controller.ignoreSelectionChanged = true;
			getSearchTextHolder().expand(toExpand);
			controller.ignoreSelectionChanged = false;
	
			int row = getSearchTextHolder().getRowForNode(toExpand);
			return new Point(row, row + 1);
		}
		finally	{
			setWaitCursor(false);
		}
	}


	/**
		Search for given pattern in TextHolder, generate result
		list, and insert results into JList.
		Following variables are evaluated: matches (list of found locations),
		currMatchNo (the number of the match that was selected in textarea).
		@param expr the compiled regular expression to search for.
		@returns the new search result that is to be put into result list.
	*/
	protected Vector newSearch(RE expr)	{
		// Text neu holen und Liste aufbauen
		List allNodes = getSearchTextHolder().getAllNodes();
		SearchNodeTextList nodeList = new SearchNodeTextList(allNodes);

		boolean currentMatchFound = false;
		currMatchNo = 0;
		Vector allMatches = new Vector();

		// loop all nodes
		for (int i = 0; i < nodeList.getNodesCount(); i++)	{
			MutableXmlNode node = nodeList.getNode(i);
			int oldsize = allMatches.size();

			if (searchTexts.isSelected())	{
				String textbuffer = getSearchTextHolder().getText(node);
				TextlineList textlines = insertMatches(expr, textbuffer, allMatches);
				nodeList.insertMatches(node.toString(), i, textlines, null);
			}

			if (searchAttributes.isSelected())	{
				PropertiesList attrs = getSearchTextHolder().getAttributes(node);

				for (int k = 0; attrs != null && k < attrs.size(); k++)	{
					PropertiesList.Tuple t = (PropertiesList.Tuple)attrs.get(k);
					String attrName = t.name;
					String textbuffer = t.value;
					TextlineList textlines = insertMatches(expr, textbuffer, allMatches);
					nodeList.insertMatches(node.toString(), i, textlines, attrName);
				}
			}

			if (!currentMatchFound && oldsize < allMatches.size() && isOnOrBehindSelection(node, nodeList))	{
				currMatchNo = oldsize;
				currentMatchFound = true;
			}
		}

		matches = new REMatch [allMatches.size()];
		allMatches.copyInto(matches);
		
		return nodeList;
	}


	private TextlineList insertMatches(RE expr, String textbuffer, Vector allMatches)	{
		TextlineList textlines = null;

		if (textbuffer != null)	{
			// Suchen in Text
			REMatch [] matches = expr.getAllMatches(textbuffer);

			for (int mi = 0; matches != null && mi < matches.length; mi++)	{
				allMatches.add(matches[mi]);

				int [] startEnd = getRealStartEnd(matches[mi], textbuffer);
				int start = startEnd[0];
				int end   = startEnd[1];

				if (textlines == null)
					textlines = new TextlineList(textbuffer);
				textlines.insertMatch(start, end);
			}
		}

		return textlines;
	}


	private SearchTextHolder getSearchTextHolder()	{
		return (SearchTextHolder)textarea;
	}


	private boolean isOnOrBehindSelection(MutableXmlNode node, SearchNodeTextList list)	{
		if (startSelection < 0)
			return true;

		MutableXmlNode sel = getSearchTextHolder().getNodeForRow(startSelection);
		if (node == null)
			return true;

		return list.isOnOrBehind(node, sel);
	}


	/**
		Replace one or all found locations.
		This is done in one undoable transaction.
		@param all if true, replace all.
		@return true if anything was replaced
	*/
	protected boolean replace(boolean all)	{
		SearchNodeTextList nodeList = (SearchNodeTextList)getListData();
		
		if (nodeList != null && nodeList.size() > 0)	{
			String replacement = getReplacementText();
			int start = all ? 0 : getSelectedMatch();
			int end = all ? nodeList.size() : start + 1;

			controller.getClipboard().getDoListener().beginUpdate();

			TextlineList prevList = null;

			// loop match list
			for (int i = start; i < end; i++)	{
				TextlineList tl = nodeList.getTextlineList(i);
				if (prevList != null && prevList == tl)
					continue;
				prevList = tl;

				MutableXmlNode node = nodeList.getNodeForMatchIndex(i);
				try	{
					node.setSearchMode(true);
					String attrName = nodeList.getAttrName(i);
					//System.err.println("replace, attrName is "+attrName+" match index is "+i);
					boolean isText = (attrName == null);
					String newText = nodeList.replace(replacement, i, all);
					int column = isText ? XmlTreeTableModel.LONGTEXT_COLUMN : XmlTreeTableModel.ATTRIBUTES_COLUMN;
	
					Object oldValue = node.getColumnObject(column);
					Object newValue;
	
					if (isText)	{
						newValue = newText;
					}
					else	{
						PropertiesList newList = (PropertiesList) ((PropertiesList)oldValue).clone();
						newList.setValue(attrName, newText);
						newValue = newList;
					}
	
					controller.updateNode(
							node,
							new UpdateObjectAndColumn(oldValue, column),	// old value
							new UpdateObjectAndColumn(newValue, column),	// new value
							false);
				}
				finally	{
					node.setSearchMode(false);
				}
			}

			controller.getClipboard().getDoListener().endUpdate();

			getSearchTextHolder().getTreeTableModel().setChanged(true);

			return true;
		}

		return false;
	}





	// helper class, as match list, holding a list of all nodes.
	private class SearchNodeTextList extends Vector
	{
		private List allNodes;

		private class IndexAndTextlineList
		{
			int index;
			TextlineList textlines;
			String line;
			String attrName;

			IndexAndTextlineList(int index, TextlineList textlines, String line, String attrName)	{
				this.index = index;
				this.textlines = textlines;
				this.line = line;
				this.attrName = attrName;
			}

			public String toString()	{
				return line;
			}
		}


		SearchNodeTextList(List allNodes)	{
			this.allNodes = allNodes;
		}

		public void insertMatches(
			String nodeName,
			int nodeIndex,
			TextlineList textlines,
			String attrName)
		{
			for (int i = 0; textlines != null && i < textlines.size(); i++)	{
				String prefix = (attrName == null ? TEXTPREFIX : ATTRPREFIX)+" ["+nodeName+"]: ";
				String line = textlines.get(i).toString();
				add(new IndexAndTextlineList(nodeIndex, textlines, prefix+line, attrName));
			}
		}

		public String replace(String replacement, int matchIndex, boolean all)	{
			TextlineList tl = getTextlineList(matchIndex);
			return all ? tl.replaceAll(replacement) : tl.replace(replacement, 0);
		}


		public String getAttrName(int matchIndex)	{
			IndexAndTextlineList e = (IndexAndTextlineList)get(matchIndex);
			return e.attrName;
		}

		public TextlineList getTextlineList(int matchIndex)	{
			IndexAndTextlineList e = (IndexAndTextlineList)get(matchIndex);
			return e.textlines;
		}

		private int matchToNodeIndex(int matchIndex)	{
			IndexAndTextlineList e = (IndexAndTextlineList)get(matchIndex);
			return e.index;
		}


		public int getNodesCount()	{
			return allNodes.size();
		}

		public MutableXmlNode getNode(int nodeIndex)	{
			return (MutableXmlNode)allNodes.get(nodeIndex);
		}

		public MutableXmlNode getNodeForMatchIndex(int matchIndex)	{
			int i = matchToNodeIndex(matchIndex);
			return getNode(i);
		}


		public int getNextMatchIndexFromNode(MutableXmlNode current)	{
			int found = -1;

			for (int i = 0; found < 0 && i < allNodes.size(); i++)	{
				if (allNodes.get(i) == current)
					found = i;
			}

			for (int i = 0; i < size(); i++)	{
				int nodeIndex = matchToNodeIndex(i);
				if (nodeIndex > found)
					return i;
			}

			return 0;
		}


		public boolean isOnOrBehind(MutableXmlNode node, MutableXmlNode selected)	{
			if (node == selected)
				return true;

			for (int i = 0; i < allNodes.size(); i++)	{
				MutableXmlNode n = (MutableXmlNode)allNodes.get(i);

				if (n == selected)
					return true;

				if (n == node)
					return false;
			}

			return true;	// selected not found?
		}

	}

}