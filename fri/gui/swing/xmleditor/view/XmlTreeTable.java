package fri.gui.swing.xmleditor.view;

import java.awt.*;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import fri.util.props.*;
import fri.gui.CursorUtil;
import fri.gui.text.TextHolder;
import fri.gui.swing.treetable.*;
import fri.gui.swing.xmleditor.model.*;
import fri.gui.swing.table.PersistentColumnsTable;
import fri.gui.swing.text.MultilineTextField;
import fri.gui.swing.propertiestextfield.*;
import fri.gui.swing.tree.*;
import fri.gui.swing.table.header.*;
import fri.gui.swing.filechangesupport.FileChangeSupport;

/**
	JTreeTable configured for XML documents.
	<p>
	System Properties:
	<ul>
		<li><i>showHorizontalLines = true</i>
			show horizontal lines in treetable
			</li>
	</ul>
*/

public class XmlTreeTable extends JTreeTable implements TreeExpansionListener
{
	private boolean inited;
	private FileChangeSupport fileChangeSupport;
	private Configuration configuration;
	private TextHolder textHolder;

	// temporary values when loading data
	private TreePath [] open;
	private TreePath [] selected;
	private Point pos;


	/** Create a treetable that renders the passed URI. */
	public XmlTreeTable(String uri, Configuration configuration)
		throws Exception
	{
		super(
			MutableXmlTreeTableModel.getInstance(uri, configuration),
			new XmlTreeCellRenderer());

		this.configuration = configuration;
	}


	private void beforeLoad()	{
		if (inited == false)	{	// first call
			inited = true;
		}
		else// subsequent call, save expanded/selected pathes and column widths
		if (getTree() != null)	{
			rememberColumnWidth();	// keep same column widths in new model

			open = TreeExpander.getOpenTreePathes(getTree());	// keep expansion
			selected = TreeExpander.getSelectedTreePathes(getTree());

			if (getParent() instanceof JViewport)	{	// keep view position
				pos = ((JViewport)getParent()).getViewPosition();
			}
		}
	}
	
	private void afterLoad()	{
		// configure treetable
		getTree().setRootVisible(getConfiguration().showProlog);
		getTree().setShowsRootHandles(false);
		getTree().putClientProperty("JTree.lineStyle", "Angled");	// looks good

		// set expansion header
		FilterSortExpandHeaderEditor.setTableHeader(this, null, FilterSortExpandHeaderEditor.EXPAND);

		// set renderer for attributes list column
		setDefaultRenderer(PropertiesList.class, new ListTableRenderer());

		// set column widths from persistence
		boolean loaded = PersistentColumnsTable.load(this);

		// column default widths and editors

		TableColumnModel m = getColumnModel();
		TableColumn column;
		XmlNode root = (XmlNode)getTree().getModel().getRoot();

		if (root.hasTagMap())	{
			setTreeColumnEditable(true);
		}

		column = m.getColumn(XmlTreeTableModel.TAG_COLUMN);
		if (loaded == false)
			column.setPreferredWidth(100);

		// if attribute column exists, set column editor for attributes
		if (m.getColumnCount() > XmlTreeTableModel.ATTRIBUTES_COLUMN)	{
			column = m.getColumn(XmlTreeTableModel.ATTRIBUTES_COLUMN);
			
			column.setCellEditor(new PropertiesTableCellEditor(new PropertiesTextField()	{
				/** Set another editor than PropertiesComboEditor. */
				protected PropertiesComboEditor createEditor()	{
					return new PropertiesComboEditor()	{
						/** Set another textfield. */
						protected JTextField createTextField()	{
							return new MultilineTextField();
						}
					};
				}
			}));

			// collapse column if no values
			// this will not ever happen but keep it because of historical reasons
			if (root.hasAnyAttributes() == false)	{
				HeaderValueStruct hvs = (HeaderValueStruct)column.getHeaderValue();
				hvs.setExpanded(false);
			}
			else	{
				if (loaded == false)	// set hardcoded default width
					column.setPreferredWidth(100);
			}
		}

		// set column editor for text
		column = m.getColumn(XmlTreeTableModel.LONGTEXT_COLUMN);
		column.setCellEditor(new LongTextTableCellEditor());

		// collapse column if no values
		if (root.hasAnyTexts() == false)	{
			HeaderValueStruct hvs = (HeaderValueStruct)column.getHeaderValue();
			hvs.setExpanded(false);
		}
		else	{
			if (loaded == false)
				column.setPreferredWidth(160);
		}


		// expand pathes

		// expand document element
		for (int i = tree.getRowCount() - 1; i >= 0; i--)
			getTree().expandRow(i);

		if (getConfiguration().expandAllOnOpen)	{	// expand all branches when configured
			TreeExpander.expandAllBranches(getTree());
		}
		else
		if (selected != null || open != null)	{
			// expand previous pathes
			TreeExpander.setOpenTreePathes(getTree(), open, root.getNodeComparator());
			TreeExpander.setSelectedTreePathes(getTree(), selected, root.getNodeComparator());

			// restore view position
			if (pos != null)	{
				final Point fpos = pos;
				EventQueue.invokeLater(new Runnable()	{
					public void run()	{
						((JViewport)getParent()).setViewPosition(fpos);
					}
				});
			}
		}

		// add listener for wait cursor
		removeTreeExpansionListener(this);
		addTreeExpansionListener(this);
	}



	/** Set a new root to the treetable model. */
	public void setRoot(TreeNode newRoot)	{
		beforeLoad();

		// set the new root
		((AbstractTreeTableModel)getTreeTableModel()).setRoot(newRoot);

		afterLoad();
	}

	/** Overridden to configure treetable every time new data are loaded. */
	public void setModel(TreeTableModel newModel)	{
		beforeLoad();

		// set the new model
		super.setModel(newModel);

		afterLoad();
	}



	/** Overriddden to check system property when switching off horizontal lines. */
	public void setShowHorizontalLines(boolean b)	{
		if (b || PropertyUtil.checkSystemProperty("showHorizontalLines", false) == false)
			super.setShowHorizontalLines(b);
	}


	/** Overridden to return tooltip from XmlNode. */
	public String getToolTipText(MouseEvent e) {
		int col = columnAtPoint(e.getPoint());
		if (getColumnClass(col) != TreeTableModel.class)
			return null;

		JTree tree = getTree();
		int row = rowAtPoint(e.getPoint());
		TreePath tp = tree.getPathForRow(row);
		if (tp == null)
			return null;

		XmlNode n = (XmlNode)tp.getLastPathComponent();
		return n.getToolTipText();
	}




	/**
		Returns the URI this treepanel is rendering.
	*/
	public String getURI()	{
		TreeTableModel m = getTreeTableModel();
		XmlNode root = (XmlNode)m.getRoot();
		String uri = root.getURI();
		System.err.println("XmlTreeTable retrieved URI from tree root "+root.hashCode()+": "+uri);
		return uri;
	}

	/**
		Returns true if the XML document was changed.
	*/
	public boolean isChanged()	{
		return ((MutableXmlTreeTableModel)getTreeTableModel()).isChanged();
	}



	/** Storing the column widths of table to a file. */
	public void saveColumnWidth()	{
		rememberColumnWidth();
		ClassProperties.store(getClass());
	}

	public void rememberColumnWidth()	{
		PersistentColumnsTable.remember(this);
		removeCollapsedTableColumnWidths();
	}

	// if attribute column is collapsed, remove it from persistence
	private void removeCollapsedTableColumnWidths()	{
		removeCollapsedTableColumnWidth(XmlTreeTableModel.ATTRIBUTES_COLUMN);
		removeCollapsedTableColumnWidth(XmlTreeTableModel.LONGTEXT_COLUMN);
	}
	
	private void removeCollapsedTableColumnWidth(int columnNr)	{
		TableColumnModel m = getColumnModel();

		if (columnNr < m.getColumnCount())	{
			TableColumn column = m.getColumn(columnNr);
		
			HeaderValueStruct hvs = (HeaderValueStruct)column.getHeaderValue();
	
			if (hvs.getExpanded() == false)	{
				ClassProperties.remove(getClass(), PersistentColumnsTable.getColumnKey(m, columnNr));
			}
		}
	}


	/** Returns the obtained FileChangeSupport to the controller. */
	public FileChangeSupport getFileChangeSupport()	{
		return fileChangeSupport;
	}

	/** Receive a FileChangeSupport object to obtain for the controller. */
	public void setFileChangeSupport(FileChangeSupport fileChangeSupport)	{
		this.fileChangeSupport = fileChangeSupport;
	}


	/**
		Returns the XML Configuration this treeview is mentioning.
		This contains parser and display options.
	*/
	public Configuration getConfiguration()	{
		return configuration == null ? Configuration.getDefault() : configuration;
	}

	/** Sets a new Configuration object dedicated for this treeview.  */
	public void setConfiguration(Configuration configuration)	{
		this.configuration = configuration;
	}


	/** Returns the TextHolder of this treetable. */
	public TextHolder getTextHolder()	{
		return textHolder;
	}

	/** Sets the TextHolder of this treetable. */
	public void setTextHolder(TextHolder textHolder)	{
		this.textHolder = textHolder;
	}



	// tree expansion listener

	/** Setting wait cursor. */
	public void treeWillExpand(TreeExpansionEvent e)
		throws ExpandVetoException
	{
		CursorUtil.setWaitCursor(this);
		super.treeWillExpand(e);
	}

	/** Setting wait cursor. */
	public void treeWillCollapse(TreeExpansionEvent e)
		throws ExpandVetoException
	{
		CursorUtil.setWaitCursor(this);
		super.treeWillCollapse(e);
	}
	
	/** Setting default cursor. */
	public void treeExpanded(TreeExpansionEvent e)	{
		CursorUtil.resetWaitCursor(this);
	}

	/** Setting default cursor. */
	public void treeCollapsed(TreeExpansionEvent e)	{
		CursorUtil.resetWaitCursor(this);
	}

}