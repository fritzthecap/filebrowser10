package fri.gui.swing.mailbrowser;

import java.util.*;
import java.text.SimpleDateFormat;
import javax.mail.*;
import gnu.regexp.*;
import fri.util.regexp.*;
import fri.util.Equals;
import fri.util.error.Err;
import fri.util.mail.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.swing.*;

public class MessageTableModel extends AbstractMutableTableModel
{
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd z HH:mm:ss, E");
	public static final int SUBJECT_COLUMN = 0;
	public static final int FROM_COLUMN = 1;
	public static final int SENT_DATE_COLUMN = 2;
	public static final int TEXT_COLUMN = 3;
	public static final int COLUMN_COUNT = 4;
	private static final Vector columnNames = new Vector(COLUMN_COUNT);

	static	{
		columnNames.add(Language.get("Subject"));
		columnNames.add(Language.get("From"));
		columnNames.add(Language.get("Sent"));
		columnNames.add(Language.get("Text"));
		// size, attachments, ...
	};
	
	/** Keeps order of static contents! */
	public static void buildMessageTableRow(MessageTableRow row, Object subject, Object from, Object sent)	{
		row.add(subject);
		row.add(from);
		row.add(sent);
		row.add("");	// dummy for text
	}
	
	private FolderTreeModel folderTreeModel;
	private FolderTreeNode currentNode;
	private String [] filters = new String[COLUMN_COUNT];
	private Vector oldDataVector;
	private MessageTableRow deleteRow;



	/** Create an empty model. */
	public MessageTableModel()	{
		super(new Vector(), columnNames);
	}


	/** Always returns String class to be filterable. */
	public Class getColumnClass(int column)	{
		return String.class;
	}
	
	/** Always returns false as this table is not editable. */
	public boolean isCellEditable(int row, int column)	{
		return false;
	}

	
	/** Clear table. You MUST call this when folder selection changes! */
	public void clear()	{
		setRowCount(0);	// sets an empty model
		oldDataVector = null;	// clear filter backup Vector
		filters = new String[getColumnCount()];	// deactivate all filters, they remain on GUI for further activation
	}


	/** Returns the row at given model index (not view index!). */
	public MessageTableRow getMessageTableRow(int modelRow)	{
		return (MessageTableRow)getRow(modelRow);
	}


	// MVC framework
	
	/** Implements AbstractMutableTableModel: returns a ModelItem wrapper for a table row. */
	public ModelItem createModelItem(Vector row)	{
		return new MessageTableModelItem((DefaultTableRow)row);
	}

	// handle original data vector when filter is active
	
	/** Overridden to handle original unfiltered data vector. */
	public void insertRow(int row, Vector rowData)	{
		super.insertRow(row, rowData);
		
		if (oldDataVector != null)	{
			if (row == 0)
				oldDataVector.add(0, rowData);
			else
				oldDataVector.add(rowData);
		}
	}

	/** Overridden to handle original unfiltered data vector. */
	public boolean doDelete(final ModelItem item)	{
		this.deleteRow = (MessageTableRow)item.getUserObject();
		boolean ret = super.doDelete(item);
		this.deleteRow = null;
		return ret;
	}
	
	/** Overridden to handle original unfiltered data vector. */
	public void removeRow(int row)	{
		super.removeRow(row);

		if (oldDataVector != null)	{
			row = getRowPosition(deleteRow, oldDataVector);
			oldDataVector.remove(row);
		}
	}


	/** Returns true if the passed filter in passed column is a change. */
	public boolean filterChanged(String filter, int column)	{
		filter = filter.length() <= 0 || filter.equals("*") ? null : filter;
		return Equals.equals(filters[column], filter) == false;
	}

	/** Filters messages and shows only those that match the passed filter in passed column. */
	public void setFilter(String filter, int column)	{
		filter = filter.length() <= 0 || filter.equals("*") ? null : filter;
		filters[column] = filter;
		
		if (oldDataVector == null)	{
			oldDataVector = getDataVector();
		}
		
		Vector newDataVector = new Vector(oldDataVector.size());
		ReceiveMail rm = null;
		
		// compile all patterns before loop
		RE [] patterns = new RE[getColumnCount()];
		RE [] longTextPatterns = new RE[getColumnCount()];
		for (int i = 0; i < getColumnCount(); i++)	{
			patterns[i] = filters[i] == null ? null : getRE(filters[i]);
			longTextPatterns[i] = filters[i] == null ? null : getRE(ensureLeadingTrailingWildcard(filters[i]));
		}

		for (int i = 0; i < oldDataVector.size(); i++)	{	// loop all messages
			Vector row = (Vector)oldDataVector.get(i);
			boolean matched = true;
			
			for (int j = 0; j < getColumnCount(); j++)	{	// loop all columns
				RE pattern = patterns[j];
				
				if (pattern != null)	{	// ignore empty filters
					if (j != TEXT_COLUMN)	{
						String s = (String)row.get(j);	// throw exception if not string!
						if (pattern.isMatch(s) == false)
							matched = false;
					}
					else	{	// filter defined in TEXT_COLUMN
						Message msg = ((MessageTableRow)row).getMessage();
						SearchMessageTextVisitor visitor = new SearchMessageTextVisitor(longTextPatterns[j]);
						try	{
							if (rm == null)
								rm = getCurrentFolder().getReceiveMail();
							rm.messageParts(msg, visitor);
							if (visitor.isFound() == false)
								matched = false;
						}
						catch (Exception e)	{
							Err.error(e);
						}
					}
				}
			}
			
			if (matched)
				newDataVector.add(row);
		}
		
		setDataVector(newDataVector, columnNames);
	}


	private String ensureLeadingTrailingWildcard(String filter)	{
		if (filter.startsWith("*") == false)
			filter = "*"+filter;
		if (filter.endsWith("*") == false)
			filter = filter+"*";
		return filter;
	}
	
	private RE getRE(String filter)	{
		try	{
			return new RE(RegExpUtil.setDefaultWildcards(filter), RE.REG_ICASE, Syntaxes.getSyntax("PERL5"));
		}
		catch (REException e)	{
			Err.error(e);
		}
		return null;
	}
	


	// state holding methods
	
	public void setCurrentFolderAndModel(FolderTreeNode currentNode, FolderTreeModel folderTreeModel)	{
		this.currentNode = currentNode;
		this.folderTreeModel = folderTreeModel;
	}
	
	
	/** Returns the mail client, passed on selection event from FolderController, to be given to MessageController. */
	public ObservableReceiveMail getReceiveMail()	{
		return getCurrentFolder().getReceiveMail();
	}

	public FolderTreeNode getCurrentFolder()	{
		return currentNode;
	}

	public FolderTreeNode getTrashNode()	{
		return getFolderTreeModel().getTrashNode(getCurrentFolder());
	}
	
	public FolderTreeNode getDraftNode()	{
		return getFolderTreeModel().getDraftNode(getCurrentFolder());
	}
	
	public FolderTreeNode getOutboxNode()	{
		return getFolderTreeModel().getOutboxNode(getCurrentFolder());
	}
	
	public FolderTreeNode getSentNode()	{
		return getFolderTreeModel().getSentNode(getCurrentFolder());
	}
	
	public FolderTreeModel getFolderTreeModel()	{
		return folderTreeModel;
	}

	public boolean mustBeReallyDeleted()	{
		FolderTreeNode n = getTrashNode();
		return n == null || n == getCurrentFolder();
	}

	public boolean rendersToAdress()	{
		FolderTreeNode s = getSentNode();
		FolderTreeNode d = getDraftNode();
		FolderTreeNode o = getOutboxNode();
		return
				s != null && s == getCurrentFolder() ||
				d != null && d == getCurrentFolder() ||
				o != null && o == getCurrentFolder();
	}



	private static class SearchMessageTextVisitor implements ReceiveMail.MessagePartVisitor
	{
		private RE pattern;
		private boolean found = false;
		
		SearchMessageTextVisitor(RE pattern)	{
			this.pattern = pattern;
		}
		
		public void finalPart(int absolutePartNumber, int treeLevel, Part part) throws Exception	{
			if (found == false && part.getContentType().toLowerCase().startsWith("text/plain"))	{	// is plain text
				String text = part.getContent().toString();
				
				for (StringTokenizer stok = new StringTokenizer(text, "\r\n"); found == false && stok.hasMoreTokens(); )	{
					String line = stok.nextToken();
					found = pattern.isMatch(line);
					//System.err.println("text matched with pattern "+pattern+": "+found+", in >"+line+"<");
				}
			}
		}
		
		public void multiPart(int absolutePartNumber, int treeLevel, Part part) throws Exception {
		}
		
		public boolean isFound()	{

			return found;
		}
	}

}