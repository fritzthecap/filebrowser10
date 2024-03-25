package fri.gui.swing.treetable;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.*;

/**
 * A JTree is used as TableCellRenderer of JTable.
 * JTreeTable is a JTable derivate. The model is an
 * AbstractTreeTableModel derivate.
 *
 * @author Philip Milne
 * @author Scott Violet
 * @author Ritzberger Fritz
 */

public class JTreeTable extends JTable implements TreeWillExpandListener
{
	/** The table renderer is a subclass of JTree. */
	protected TreeTableCellRenderer tree = null;
	/** The JTree cell renderer. */
	protected TreeCellRenderer treeRenderer;
	/** Default true. No drag and drop means, that drag selection should take place. */
	protected boolean noDragAndDrop = true;
	/** only clicks in tree column should select a table row. */
	protected boolean onlyTreeSelectsRow = false;
	protected boolean isTreeColumnEditable;



	/** Konstruktion einer TreeTable mit einem implementierten Modell */
	public JTreeTable(TreeTableModel treeTableModel) {
		this(treeTableModel, null);
	}

	/**
		Konstruktion einer TreeTable mit einem implementierten Modell
		und editierbarer Tree-Spalte.
	*/
	public JTreeTable(TreeTableModel treeTableModel, boolean isTreeColumnEditable) {
		this(treeTableModel, null, isTreeColumnEditable);
	}

	/**
		Konstruktion einer TreeTable mit einem Modell und dem gewuenschten
		TreeCellRenderer zur Darstellung von Besonderheiten im Tree.
		In dieser TreeTable ist die Tree-Spalte nicht editierbar.
	*/
	public JTreeTable(
		TreeTableModel treeTableModel,
		TreeCellRenderer treeRenderer)
	{
		this(treeTableModel, treeRenderer, false);
	}

	/**
		Konstruktion einer TreeTable mit einem Modell und dem gewuenschten
		TreeCellRenderer zur Darstellung von Besonderheiten im Tree.
		Es ist abgebbar, ob die Tree-Spalte editierbar ist oder nicht.
	*/
	public JTreeTable(
		TreeTableModel treeTableModel,
		TreeCellRenderer treeRenderer,
		boolean isTreeColumnEditable)
	{
		super();

		this.treeRenderer = treeRenderer;
		
		setTreeEditor(isTreeColumnEditable);

		setModel(treeTableModel);

		// No intercell spacing
		setIntercellSpacing(new Dimension(getIntercellSpacing().width, 0));
		setShowHorizontalLines(false);

		// And update the height of the trees row to match that of the table.
		if (tree != null && tree.getRowHeight() < 1) {
			// Metal looks better like this.
			setRowHeight(18);
		}
		
		addTreeWillExpandListener(this);
	}


	/** Sets editable or uneditable TreeCellEditor */
	public void setTreeColumnEditable(boolean editable)	{
		setTreeEditor(editable);
	}

	protected void setTreeEditor(boolean isTreeColumnEditable)	{
		this.isTreeColumnEditable = isTreeColumnEditable;

		if (isTreeColumnEditable)	{
			setDefaultEditor(
					TreeTableModel.class,
					new TreeTableCellEditor(this));
		}
		else	{
			setDefaultEditor(
					TreeTableModel.class,
					new PassiveCellEditor(this));
		}
	}


	/** Installs the JTree as renderer for TreeTableModel.class. */
	protected void setTreeRenderer()	{
		// Install the tree renderer
		setDefaultRenderer(TreeTableModel.class, tree);
	}
	

	/** Set passed model to treetable. */
	public void setModel(TreeTableModel treeTableModel)	{
		if (tree == null)	{
			tree = new TreeTableCellRenderer(this, treeTableModel, treeRenderer);
		}
		else	{
			tree.setModel(treeTableModel, treeRenderer);
		}

		setTreeRenderer();

		TreeTableModelAdapter tma = new TreeTableModelAdapter(treeTableModel, tree);
		
		// Install a tableModel representing the visible rows in the tree.
		super.setModel(tma);

		// Force the JTable and JTree to share their row selection models.
		ListToTreeSelectionModelWrapper selectionWrapper = new ListToTreeSelectionModelWrapper();

		tree.setSelectionModel(selectionWrapper);
		setSelectionModel(selectionWrapper.getListSelectionModel());
	}
	

	/**
		The model that goes in with setModel()
		is not the same as goes out with getModel().
		@return a TreeTableModel that was set by constructor
		or by setModel() call
	*/
	public TreeTableModel getTreeTableModel()	{
		return (TreeTableModel)getTree().getModel();
	}
	

	/**
		Set this to true if only clicks in tree column should select a table row.
	*/
	public void setOnlyTreeSelectsRow(boolean onlyTreeSelectsRow)	{
		this.onlyTreeSelectsRow = onlyTreeSelectsRow;
	}

	/**
		Set this to false if you install a drag&drop handler on this TreeTable.
		No drag and drop means, that drag selection should take place.
		Set this to false if a drag and drop handler gets installed from external.
		@param noDragAndDrop default is true, set to false to change.
	*/
	public void setNoDragAndDrop(boolean noDragAndDrop)	{
		this.noDragAndDrop = noDragAndDrop;
	}


	/** Listens for tree expansion to close all editors. */
	public void treeWillExpand(TreeExpansionEvent event)
		throws ExpandVetoException
	{
		stopCellEditing();
	}
	/** Listens for tree collapse to close all editors. */
	public void treeWillCollapse(TreeExpansionEvent event)
		throws ExpandVetoException
	{
		stopCellEditing();
	}
	
	private void stopCellEditing()	{
		try	{
			DefaultCellEditor ded = (DefaultCellEditor)getCellEditor();
			if (ded != null)
				ded.stopCellEditing();
		}
		catch (ClassCastException e)	{
		}
	}


	/** Update der Anzeige von JTree und JTable */
	public void treeDidChange()	{
		//getTree().treeDidChange();
		revalidate();
		repaint();
	}
	
	
	/** methods to add expansion listeners */	
	public void addTreeExpansionListener(TreeExpansionListener lsnr)	{
		removeTreeExpansionListener(lsnr);
		getTree().addTreeExpansionListener(lsnr);
	}

	public void removeTreeExpansionListener(TreeExpansionListener lsnr)	{
		getTree().removeTreeExpansionListener(lsnr);
	}

	public void addTreeWillExpandListener(TreeWillExpandListener lsnr)	{
		removeTreeWillExpandListener(lsnr);
		getTree().addTreeWillExpandListener(lsnr);
	}

	public void removeTreeWillExpandListener(TreeWillExpandListener lsnr)	{
		getTree().removeTreeWillExpandListener(lsnr);
	}



	/** Overridden to pass color to tree renderer. */
	public void setForeground(Color color) { 
		super.setForeground(color); 
		if (getTree() == null)
			return;
		TreeCellRenderer dtcr = getTree().getCellRenderer(); 
		if (dtcr != null && dtcr instanceof DefaultTreeCellRenderer) 
			((DefaultTreeCellRenderer)dtcr).setTextNonSelectionColor(color);
	}
	
	/** Overridden to pass color to tree renderer. */
	public void setBackground(Color color) { 
		super.setBackground(color);
		if (getTree() == null)
			return;
		TreeCellRenderer dtcr = getTree().getCellRenderer(); 
		if (dtcr != null && dtcr instanceof DefaultTreeCellRenderer)
			((DefaultTreeCellRenderer)dtcr).setBackgroundNonSelectionColor(color);
	}
	
	/** Overridden to pass font to tree and tree renderer. */
	public void setFont(Font f) { 
		super.setFont(f);
		if (getTree() == null)
			return;
		getTree().setFont(f); 
		TreeCellRenderer dtcr = getTree().getCellRenderer(); 
		if (dtcr != null && dtcr instanceof DefaultTreeCellRenderer)	{
			((DefaultTreeCellRenderer)dtcr).setFont(f);
			((DefaultTreeCellRenderer)dtcr).updateUI();
		}
	}
	
	
	/**
	 * Overridden to message super and forward the method to the tree.
	 * Since the tree is not actually in the component hieachy it will
	 * never receive this unless we forward it in this manner.
	 */
	public void updateUI() {
		super.updateUI();
		if (tree != null) {
			tree.updateUI();
			setTreeRenderer();
			// Do this so that the editor is referencing the current renderer
			// from the tree. The renderer can potentially change each time laf changes.
			setTreeEditor(isTreeColumnEditable);
		}
		// Use the tree's default foreground and background colors in the table.
		LookAndFeel.installColorsAndFont(this, "Tree.background", "Tree.foreground", "Tree.font");
		setUI(new BasicTreeTableUI());	// no selection by mouse dragging
	}

	/*
	 * Workaround for BasicTableUI anomaly. Make sure the UI never tries to
	 * paint the editor. The UI currently uses different techniques to
	 * paint the renderers and editors and overriding setBounds() below
	 * is not the right thing to do for an editor. Returning -1 for the
	 * editing row in this case, ensures the editor is never painted.
	 */
	public int getEditingRow() {
		//return editingRow;
		return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 : editingRow;
	}


	/**
	* Returns the actual row that is editing as <code>getEditingRow</code>
	* will always return -1.
	*/
	protected int realEditingRow() {
		return editingRow;
	}
	
	
	/**
	* This is overriden to invoke supers implementation, and then,
	* if the receiver is editing a Tree column, the editors bounds is
	* reset. The reason we have to do this is because JTable doesn't
	* think the table is being edited, as <code>getEditingRow</code> returns
	* -1, and therefore doesn't automaticly resize the editor for us.
	*/
	public void sizeColumnsToFit(int resizingColumn) { 
		super.sizeColumnsToFit(resizingColumn);
		if (getEditingColumn() != -1 && getColumnClass(editingColumn) == TreeTableModel.class) {
			Rectangle cellRect = getCellRect(realEditingRow(), getEditingColumn(), false);
			Component component = getEditorComponent();
			component.setBounds(cellRect);
			component.validate();
		}
	}



	/**
	 * Overridden to pass the new rowHeight to the tree.
	 */
	public void setRowHeight(int rowHeight) {
		super.setRowHeight(rowHeight);
		if (tree != null && tree.getRowHeight() != rowHeight) {
			tree.setRowHeight(getRowHeight());
		}
	}

	/**
	 * Returns the tree that is being shared between the model.
	 */
	public JTree getTree() {
		return tree;
	}


	/**
		Simulate tree.scrollPathToVisible();
		invoke it later as this path could just have been be created.
	*/
	public void scrollPathToVisible(final TreePath tp)	{
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				Rectangle r = tree.getPathBounds(tp);
				JTableHeader hdr = getTableHeader();
				if (hdr == null)
					return;
				Rectangle r2 = hdr.getHeaderRect(0);
				if (r == null || r2 == null)
					return;
				//System.err.println("JTreeTable.header height "+r2.height+", rectangle is "+r);
				r.y += r2.height;
				scrollRectToVisible(r);
			}
		});
	}

	/**
		Simulate tree.setSelectionPath();
		invoke it later as this path could just have been be created.
	*/
	public void setSelectionPath(TreePath tp)	{
		createSelectionPath(tp, false);
	}

	/**
		Simulate tree.setSelectionPath();
		invoke it later as this path could just have been be created.
	*/
	public void addSelectionPath(TreePath tp)	{
		createSelectionPath(tp, true);
	}

	private void createSelectionPath(final TreePath tp, final boolean add)	{
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				int row = tree.getRowForPath(tp);
				//System.err.println("setting selection to row "+row);
				if (add)
					getSelectionModel().addSelectionInterval(row, row);
				else
					getSelectionModel().setSelectionInterval(row, row);
			}
		});
	}


	/**
		Simulates tree.getSelectionPath()
	*/
	public TreePath getSelectionPath()	{
		int row = getSelectionModel().getLeadSelectionIndex();
		TreePath tp = getTree().getPathForRow(row);
		return tp;
	}

	/**
		Simulates tree.getSelectionPaths()
	*/
	public TreePath [] getSelectionPaths()	{
		ListSelectionModel sm = getSelectionModel();
		int min = sm.getMinSelectionIndex();
		int max = sm.getMaxSelectionIndex();
		Vector v = null;
		
		for (int i = min; min != -1 && max != -1 && i <= max; i++)	{
			if (sm.isSelectedIndex(i))	{
				TreePath tp = getTree().getPathForRow(i);

				if (tp != null)	{
					if (v == null)
						v = new Vector();
					v.addElement(tp);
				}
			}
		}
		
		if (v == null)
			return new TreePath[0];
			
		TreePath [] tps = new TreePath[v.size()];
		v.copyInto(tps);
		
		return tps;
	}




	/**
	 * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel
	 * to listen for changes in the ListSelectionModel it maintains. Once
	 * a change in the ListSelectionModel happens, the paths are updated
	 * in the DefaultTreeSelectionModel.
	 */
	class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel
	{
		/**
		 * Set to true when we are updating the ListSelectionModel.
		 */
		protected boolean updatingListSelectionModel;

		public ListToTreeSelectionModelWrapper() {
			super();
			getListSelectionModel().addListSelectionListener(createListSelectionListener());
		}

		/**
		 * Returns the list selection model. ListToTreeSelectionModelWrapper
		 * listens for changes to this model and updates the selected paths
		 * accordingly.
		 */
		ListSelectionModel getListSelectionModel() {
			return listSelectionModel;
		}

		/**
		 * This is overridden to set <code>updatingListSelectionModel</code>
		 * and message super. This is the only place DefaultTreeSelectionModel
		 * alters the ListSelectionModel.
		 */
		public void resetRowSelection() {
			if (!updatingListSelectionModel) {
				updatingListSelectionModel = true;
				try {
					super.resetRowSelection();
				}
				finally {
					updatingListSelectionModel = false;
				}
			}
		// Notice how we don't message super if
		// updatingListSelectionModel is true. If
		// updatingListSelectionModel is true, it implies the
		// ListSelectionModel has already been updated and the
		// paths are the only thing that needs to be updated.
		}

		/**
		 * Creates and returns an instance of ListSelectionHandler.
		 */
		protected ListSelectionListener createListSelectionListener() {
			return new ListSelectionHandler();
		}

		/**
		 * If <code>updatingListSelectionModel</code> is false, this will
		 * reset the selected paths from the selected rows in the list
		 * selection model.
		 */
		protected void updateSelectedPathsFromSelectedRows() {
			//System.err.println("updateSelectedPathsFromSelectedRows");
			if (!updatingListSelectionModel) {
				updatingListSelectionModel = true;
				
				try	{
					TreePath [] tps = JTreeTable.this.getSelectionPaths();
					
					if (tps.length > 0) {
						ListToTreeSelectionModelWrapper.this.clearSelection();
						ListToTreeSelectionModelWrapper.this.addSelectionPaths(tps);
					}
				}
				finally {
					updatingListSelectionModel = false;
				}
			}
		}


		/**
		 * Class responsible for calling updateSelectedPathsFromSelectedRows
		 * when the selection of the list changse.
		 */
		class ListSelectionHandler implements ListSelectionListener
		{
			public void valueChanged(ListSelectionEvent e) {
				updateSelectedPathsFromSelectedRows();
			}
		}

	}


//	private static final String kennstmi = "\u0066\u0072\u0069\u0077\u0061\u0072\u0065\u0020\u0032\u0030\u0030\u0031";

//	public void paintComponent(Graphics g) {
//		super.paintComponent(g);
//		FontMetrics fm = getFontMetrics(g.getFont());
//		int w = fm.stringWidth(kennstmi);
//		g.setColor(Color.gray);
//		g.drawString(kennstmi, getSize().width-w-2, getSize().height-3);
//	}




	/**
		Do not allow selection by mouse dragging as this is concurrent with drag&drop.
	*/
	protected class BasicTreeTableUI extends BasicTableUI
	{
		protected MouseInputListener createMouseInputListener() {
			return new TreeTableMouseInputHandler();
		}
		
		public class TreeTableMouseInputHandler extends BasicTableUI.MouseInputHandler
		{		
			public void mouseDragged(MouseEvent e) {
				if (noDragAndDrop)	{
					// do not conflict with drag & drop!
					try	{
						super.mouseDragged(e);
					}
					catch (IllegalComponentStateException ex)	{
					}
				}
			}
			
			
			private void ensureRowsAreVisible(int row, int rows, JTable table)	{
				if (table.getParent() instanceof JViewport == false)	{
					return;
				}

				JViewport port = (JViewport)table.getParent();
				Rectangle visRect = port.getViewRect();
				//Rectangle tableRect = table.getVisibleRect();
				
				Rectangle beginRect = table.getCellRect(row, 0, false);
	
				Rectangle testRect = beginRect;
				int beginY = beginRect.y;
				int maxY = beginY + visRect.height;
				int beginRow = row;
				int endRow = beginRow + rows;
				
				//System.err.println(" testing for view height "+viewHeight+", beginY="+beginY+", maxY="+maxY+", dy="+dy);
				for (int i = beginRow + 1; i <= endRow; i++)	{
					testRect = table.getCellRect(i, 0, false);
					boolean finish = testRect.y + testRect.height > maxY;
					
					if (finish)	{
						//System.err.println("  not all new rows will be in view");
						i = endRow;
					}
					//System.err.println("  testing cell rect "+testRect);
				}
	
				Rectangle tgt = new Rectangle(
								visRect.x,
								beginY,
								1,
								testRect.y + testRect.height - beginY);
				
				table.scrollRectToVisible(tgt);
			}
			
			
			public void mousePressed(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}

				// we want selection only from tree column mouse click
				JTable table = (JTable)e.getSource();
				Point p = e.getPoint();
				int row = table.rowAtPoint(p);
				int column = table.columnAtPoint(p);
				if (column == -1 || row == -1) {
					return;
				}
				
				// look if mouse was pressed in tree column
				int treecol = -1;
				for (int i = getColumnCount() - 1; treecol < 0 && i >= 0; i--) {
					if (getColumnClass(i) == TreeTableModel.class) {
						treecol = i;
						//System.err.println("tree column is "+treecol+", column is "+column);
					}
				}
				
				if (onlyTreeSelectsRow && column != treecol)	{	// is other column than tree
					// do what "super" would do, but do not select cell
					if (table.editCellAt(row, column, e)) {
						setDispatchComponent(e); 
						repostEvent(e); 
					} 
					else { 
						table.requestFocus();
					}
					return;
				}
				
				if (column == treecol)	{	// is tree column
					// look if tree will be expanded or collapsed
					JTree tree = ((JTreeTable)table).getTree();
					int treeX = e.getX() - table.getCellRect(0, treecol, true).x;
					Rectangle r = tree.getRowBounds(row);
					//System.err.println("tree x = "+treeX+", row bounds x = "+r.x);

					if (treeX < r.x /*&& treeX >= r.x - 20*/)	{
						TreePath path = tree.getPathForRow(row);
						Object node = path.getLastPathComponent();
					
						if (tree.getModel().isLeaf(node) == false)	{
							selectedOnPress = true;
							MouseEvent	newME = new MouseEvent(
									tree,
									e.getID(),
									e.getWhen(),
									e.getModifiers(),
									treeX,
									e.getY(),
									e.getClickCount(),
									e.isPopupTrigger());
							
							tree.dispatchEvent(newME);
							
							((DefaultTreeSelectionModel)tree.getSelectionModel()).resetRowSelection();
							//((ListToTreeSelectionModelWrapper)tree.getSelectionModel()).updateSelectedPathsFromSelectedRows();
							
							if (tree.isExpanded(path))	{
								int children = tree.getModel().getChildCount(node);
								
								ensureRowsAreVisible(
										row,
										children,
										table);
							}
						}
						return;
					}
				}
					
				super.mousePressed(e);
			}


			// following code had to be copied from BasicTableUI as it is private

			private Component dispatchComponent;
			private boolean selectedOnPress;

			public void mouseReleased(MouseEvent e) {
				if (selectedOnPress) {
					if (shouldIgnore(e)) {
						return;
					}
					
					repostEvent(e);
					dispatchComponent = null;
					setValueIsAdjusting(false);
				}
				else {
					adjustFocusAndSelection2(e);
				}
			}
		
			private boolean shouldIgnore(MouseEvent e) {
				return e.isConsumed() || (!(SwingUtilities.isLeftMouseButton(e) && table.isEnabled()));
			}
			
			private void adjustFocusAndSelection2(MouseEvent e) {
				if (shouldIgnore(e)) {
					return;
				}
			
				Point p = e.getPoint();
				int row = table.rowAtPoint(p);
				int column = table.columnAtPoint(p);
				// The autoscroller can generate drag events outside the Table's range.
				if ((column == -1) || (row == -1)) {
					return;
				}
			
				if (table.editCellAt(row, column, e)) {
					setDispatchComponent(e);
					repostEvent(e);
				}
				else if (table.isRequestFocusEnabled()) {
					table.requestFocus();
				}
			
				CellEditor editor = table.getCellEditor();
				if (editor == null || editor.shouldSelectCell(e)) {
					boolean adjusting = (e.getID() == MouseEvent.MOUSE_PRESSED) ? true : false;
					setValueIsAdjusting(adjusting);
					table.changeSelection(row, column, e.isControlDown(), e.isShiftDown());
				}
			}

			private void setValueIsAdjusting(boolean flag) {
				table.getSelectionModel().setValueIsAdjusting(flag);
				table.getColumnModel().getSelectionModel().setValueIsAdjusting(flag);
			}


			private void setDispatchComponent(MouseEvent e) {
				JTable table = (JTable)e.getSource();
				Component editorComponent = table.getEditorComponent();
				Point p = e.getPoint();				
				Point p2 = SwingUtilities.convertPoint(table, p, editorComponent);
				dispatchComponent = SwingUtilities.getDeepestComponentAt(editorComponent, p2.x, p2.y);
			}
			
			private boolean repostEvent(MouseEvent e) { 
				if (dispatchComponent == null) {
					return false; 
				}
				JTable table = (JTable)e.getSource();
				MouseEvent e2 = SwingUtilities.convertMouseEvent(table, e, dispatchComponent);
				dispatchComponent.dispatchEvent(e2); 
				return true;
			}
			
			// end of code that had to be copied from BasicTableUI
		}
		
	}

}
