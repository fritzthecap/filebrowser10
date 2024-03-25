package fri.gui.swing.expressions;

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.event.*;
import java.util.List;
import java.util.Date;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import fri.patterns.interpreter.expressions.*;
import fri.util.file.ValidFilename;
import fri.gui.mvc.controller.clipboard.*;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.model.swing.*;
import fri.gui.mvc.model.*;
import fri.gui.mvc.view.*;
import fri.gui.mvc.view.swing.*;
import fri.gui.swing.iconbuilder.*;
import fri.gui.swing.combo.history.CrudCombo;
import fri.gui.swing.actionmanager.*;
import fri.gui.swing.actionmanager.connector.AbstractClipboardController;

/**
	Contoller for editing filter expression trees.
	
	@author Fritz Ritzberger 2004
*/

public class FilterTreeController extends AbstractClipboardController implements
	TreeSelectionListener,
	TreeModelListener,
	CrudCombo.CrudListener,
	CrudCombo.CrudPendingListener
{
	public static final String ACTION_NEW_MODEL = "New Model";
	public static final String ACTION_DELETE_MODEL = "Delete Model";
	public static final String ACTION_LOAD_MODEL = "Load Model";
	
	public static final String
			STRING_COMPARE = "String Compare",
			NUMBER_COMPARE = "Number Compare",
			DATE_COMPARE = "Date Compare",
			OBJECT_COMPARE = "Object Compare";
	private static final String
			AND_SEQUENCE = "AND-Sequence",
			OR_SEQUENCE = "OR-Alternatives";
	private static final String
			BEFORE = "Before",
			WITHIN = "Within",
			AFTER = "After";
		
	private static DefaultClipboard clipboard = new DefaultClipboard();
	
	private boolean changed;
	private String insertLocationType;
	private String insertItemType;
	private Integer position;
	private FilterTreeModelFactory factory = FilterTreeModelFactory.singleton();


	public FilterTreeController(FilterTreeView view)	{
		super(view);
		
		view.getModelManager().setDataItems(FilterTreePersistence.list());
		
		String modelName = view.getModelManager().getText();	// get item at 0
		setModel(factory.get(modelName));
		
		getTree().addTreeSelectionListener(this);
		getTree().getModel().addTreeModelListener(this);
		view.getModelManager().addCrudListener(this);
		view.getModelManager().addCrudPendingListener(this);

		new FilterTreeDndPerformer(view.getSensorComponent(), this);
	}
	
	
	/** Overridden to save the current model, and add listeners to tree selection and model. */
	public void setModel(Model model)	{
		save();

		getTree().removeTreeSelectionListener(this);	// remove listeners from old model
		if (getTree().getModel() != null)
			getTree().getModel().removeTreeModelListener(this);

		super.setModel(model);	// view sets TreeModel into treeview and installs renderers
		
		if (model != null)	{	// add listeners again
			getTree().getModel().addTreeModelListener(this);
			getTree().addTreeSelectionListener(this);
			
			// check the name of the model within combo box
			if (getFilterTreeView().getModelManager().getSelectedItem().equals(getFilterTreeModel().getName()) == false)
				getFilterTreeView().getModelManager().setSelectedItem(getFilterTreeModel().getName());
		}
	}
	

	/** Overridden to save the model implicitely at close. */
	public boolean close()	{
		save();	// saves the current model if changed
		FilterTreeModelFactory.free();
		factory = null;
		return super.close();
	}


	private void save()	{
		getFilterTreeView().commit();	// close open editors

		if (isChanged())	{	// save current model when changed
			getFilterTreeModel().save();
			setChanged(false);
		}
	}


	/** Overridden to exchange NEW and PASTE by fillable actions. */
	protected void insertActions()	{
		super.insertActions();

		remove(ACTION_NEW);
		registerFillableAction(ACTION_NEW, Icons.get(Icons.newLine), "Create New Item", KeyEvent.VK_INSERT, 0);
		
		remove(ACTION_PASTE);
		registerFillableAction(ACTION_PASTE, Icons.get(Icons.paste), "Paste Item(s) From Clipboard Into Selection", KeyEvent.VK_V, InputEvent.CTRL_MASK);
		
		registerAction(ACTION_NEW_MODEL, Icons.get(Icons.newDocument), "Create New Filter Model");
		registerAction(ACTION_DELETE_MODEL, Icons.get(Icons.delete), "Delete Current Filter Model");
		registerAction(ACTION_LOAD_MODEL, Icons.get(Icons.start), "Load Selected Filter Model");
	}

	/** Implemented to get specific icons from IconActionMap. */
	protected Icon getIconForAction(String actionName)	{
		if (actionName.equals(AbstractClipboardController.ACTION_NEW))
			return Icons.get(Icons.newLine);
		if (actionName.equals(AbstractClipboardController.ACTION_DELETE))
			return Icons.get(Icons.deleteLine);
		return IconActionMap.get(actionName);
	}


	/** Implements AbstractClipboardController. */
	protected DefaultClipboard getClipboard()	{
		return clipboard;
	}

	
	// interface TreeSelectionListener
	
	/** Implements TreeSelectionListener to set enabled actions. */
	public void valueChanged(TreeSelectionEvent e)	{
		setEnabledActions();
	}


	// interface TreeModelListener

	/** Implements TreeModelListener to set SAVE enabled when changing. */
	public void treeNodesChanged(TreeModelEvent e)	{
		setChanged(true);
	}
	/** Implements TreeModelListener to set SAVE enabled when changing. */
	public void treeNodesInserted(TreeModelEvent e)	{
		setChanged(true);
	}
	/** Implements TreeModelListener to set SAVE enabled when changing. */
	public void treeNodesRemoved(TreeModelEvent e)	{
		setChanged(true);
	}
	/** Implements TreeModelListener to set SAVE enabled when changing. */
	public void treeStructureChanged(TreeModelEvent e)	{
		setChanged(true);
	}


	private boolean isChanged()	{
		return changed;
	}

	private void setChanged(boolean changed)	{
		this.changed = changed;
	}


	// AbstractSwingController overrides

	protected void setEnabledActions(List selection)	{
		// set enabled actions
		DefaultMutableTreeNode node = getFirstSelectedOrRoot(selection);
		boolean isFolder = node == null || node.getAllowsChildren();
		boolean selectionExists = (selection != null && selection.size() > 0);
		boolean singleSelection = (selectionExists && selection.size() == 1);
		boolean canManipulate = (selectionExists && ((FilterTreeNode)selection.get(0)).isDragable());
		boolean canDelete = (canManipulate && node.getParent().getChildCount() > LogicalCondition.MINIMUM_CHILD_CONDITIONS);
		
		setEnabled(ACTION_DELETE, canDelete);
		setEnabled(ACTION_CUT, canDelete);
		setEnabled(ACTION_COPY, canManipulate);
		
		// fill dynamic actions
		if (getModel() != null)	{	// ensure actions have been rendered, as setEnabledActions() gets called on setView()!
			fillPASTE(singleSelection, canManipulate, isFolder);
			fillNEW(singleSelection, canManipulate, isFolder);
		}
		else	{
			setEnabled(ACTION_NEW, false);
			setEnabled(ACTION_PASTE, false);
		}

		// set popup point for keypresses
		Point p = null;
		if (node != null)	{
			Rectangle r = getTree().getPathBounds(new TreePath(node.getPath()));
			if (r != null)
				p = new Point(r.x, r.y + r.height - 1);
		}
		setPopupPoint(ACTION_NEW, p);
		setPopupPoint(ACTION_PASTE, p);
	}

	private void fillPASTE(boolean singleSelection, boolean canManipulate, boolean isFolder)	{
		boolean pasteEnabled = singleSelection && clipboard.isEmpty() == false;
		System.err.println("filling action paste, pasteEnabled "+pasteEnabled+", canManipulate "+canManipulate+", isFolder "+isFolder);
		if (pasteEnabled)	{
			if (canManipulate)
				fillAction(ACTION_PASTE,
						new String [] { BEFORE, WITHIN, AFTER },
						new boolean [] { true, isFolder, true });
			else
				fillAction(ACTION_PASTE, (String[])null);	// root selected

			setEnabled(ACTION_PASTE, true);
		}
		else	{
			setEnabled(ACTION_PASTE, false);
		}
	}
	
	private void fillNEW(boolean singleSelection, boolean canManipulate, boolean isFolder)	{
		if (singleSelection)	{
			MenuTree menuTree = new MenuTree(ACTION_NEW);

			MenuTree andMenu = new MenuTree(AND_SEQUENCE);
			menuTree.add(andMenu);
			MenuTree orMenu = new MenuTree(OR_SEQUENCE);
			menuTree.add(orMenu);
			menuTree.add(null);	// separator

			MenuTree [] menus = getFilterTreeView().getCreationTypeMenuItems();
			for (int i = 0; i < menus.length; i++)
				menuTree.add(menus[i]);
			
			if (canManipulate)	{
				fillNEWLocations(andMenu, isFolder);
				fillNEWLocations(orMenu, isFolder);
				
				for (int i = 0; i < menus.length; i++)
					fillNEWLocations(menus[i], isFolder);
			}
	
			fillAction(ACTION_NEW, menuTree);
		}
		else	{
			setEnabled(ACTION_NEW, false);
		}
	}

	private void fillNEWLocations(MenuTree menu, boolean isFolder)	{
		menu.add(new MenuTree(BEFORE));
		menu.add(new MenuTree(WITHIN, isFolder));
		menu.add(new MenuTree(AFTER));
	}



	/** Overridden to catch fillable actions. */
	public void actionPerformed(ActionEvent e)	{
		System.err.println("actionPerformed on FilterTreeController: "+e.getActionCommand());
		
		String cmd = e.getActionCommand();
		String sep = ((FillableManagedAction)get(ACTION_NEW)).menuItemSeparator;
		
		if (cmd.startsWith(ACTION_NEW+sep) || cmd.startsWith(ACTION_PASTE+sep))	{
			if (cmd.startsWith(ACTION_NEW+sep))	{
				// interpret the menu path
				int sepIndex1 = cmd.indexOf(sep);
				int sepIndex2 = cmd.lastIndexOf(sep);
				insertItemType = sepIndex1 != sepIndex2
						? cmd.substring(sepIndex1 + 1, sepIndex2)
						: cmd.substring(sepIndex2 + 1);
				insertLocationType = sepIndex1 != sepIndex2
						? cmd.substring(sepIndex2 + 1)
						: null;

				cb_New(getSelection().getSelectedObject());
			}
			else	{	// must be paste
				int sepIndex = cmd.indexOf(sep);
				insertLocationType = cmd.substring(sepIndex + 1);

				cb_Paste(getSelection().getSelectedObject());
			}
		}
		else	{
			super.actionPerformed(e);
		}
		
		insertLocationType = insertItemType = null;	// reset temporary menu choice
	}
	


	// AbstractClipboardController overrides
	
	protected ModelItem createModelItem(Object viewItem)	{
		return getFilterTreeModel().createModelItem((FilterTreeNode)viewItem);
	}

	/**
		Gets called on create and paste. When a folder is selected and "create/paste within"
		was not triggered by popup, the parent node will be returned, and position will be
		set to the position of the selected child within the parent.
		Else returns the first selected item, position will be null (append behind).
	*/
	protected Object getInsertLocation(Object selection)	{
		DefaultMutableTreeNode node = getFirstSelectedOrRoot(selection);
		System.err.println("getInsertLocation FilterTreeController, selection: "+selection+", node "+node+" insertion type "+insertLocationType);

		position = null;

		// position is significant when pasting before or after
		if (insertLocationType != null && insertLocationType.equals(WITHIN) == false)	{
			position = TreeNodeUtil.getPosition(node);
			
			if (insertLocationType.equals(AFTER))
				position = new Integer(position.intValue() + 1);
				
			node = (DefaultMutableTreeNode)node.getParent();
		}
		
		insertLocationType = null;	// reset temporary popup result state

		return createModelItem(node);
	}


	// create callbacks
	
	protected Object getCreateData(Object insertLocation)	{
		if (insertItemType.equals(OR_SEQUENCE))
			return new LogicalCondition(
					new StringComparison(new BeanVariable(), StringComparison.CONTAINS, new Constant("")),
					LogicalCondition.OR,
					new StringComparison(new BeanVariable(), StringComparison.CONTAINS, new Constant("")));
			
		if (insertItemType.equals(AND_SEQUENCE))
			return new LogicalCondition(
					new StringComparison(new BeanVariable(), StringComparison.CONTAINS, new Constant("")),
					LogicalCondition.AND,
					new StringComparison(new BeanVariable(), StringComparison.CONTAINS, new Constant("")));
			
		if (insertItemType.equals(STRING_COMPARE))
			return new StringComparison(new BeanVariable(), StringComparison.CONTAINS, new Constant(""));
			
		if (insertItemType.equals(NUMBER_COMPARE))
			return new NumberComparison(new BeanVariable(), NumberComparison.EQUAL, new Constant(new Integer(0)));
			
		if (insertItemType.equals(DATE_COMPARE))
			return new DateComparison(new BeanVariable(), DateComparison.SAME_DAY, new Constant(new Date()));
			
		if (insertItemType.equals(OBJECT_COMPARE))
			return new ObjectComparison(new BeanVariable(), ObjectComparison.IDENTICAL, new Constant(new Object()));
			
		throw new IllegalArgumentException("Unknown argument: "+insertItemType);
	}

	protected Integer getCreatePosition(Object insertLocation)	{
		return position;
	}

	protected void afterCreate(Object created)	{
		AbstractMutableTreeModelItem mi = (AbstractMutableTreeModelItem)created;
		DefaultMutableTreeNode tn = (DefaultMutableTreeNode)mi.getUserObject();
		getTree().expandPath(new TreePath(tn.getPath()));
		getSelection().setSelectedObject(mi.getUserObject());
	}


	// paste callbacks

	/** Overridden to use the paste position (paste before, after, within). */
	protected CommandArguments newPasteArguments(Object insertLocation)	{
		return super.newPasteArguments(position);
	}

	/** Returns the target items for pasting. */
	protected ModelItem [] getPasteTargetModelItems(List selection, Object insertLocation)	{
		return new ModelItem [] { (ModelItem)insertLocation };
	}

	protected boolean checkPaste(Object insertLocation, ModelItem [] sourceModelItems)	{
		Object o = ((AbstractMutableTreeModelItem)insertLocation).getUserObject();
		DefaultMutableTreeNode target = (DefaultMutableTreeNode)o;

		DefaultMutableTreeNode [] conflict = TreeModelItemUtil.checkForDescendants(target, sourceModelItems);
		if (conflict != null)	{
			error(conflict[0]+" Is Descendant Of "+conflict[1]);
			return false;
		}

		for (int i = 0; i < sourceModelItems.length; i++)	{
			AbstractMutableTreeModelItem mi = (AbstractMutableTreeModelItem)sourceModelItems[i];
			if (mi.getUserObject() == target)	{
				error("Can Not Copy/Move "+target+" To "+mi+" !");
				return false;
			}
		}

		return true;
	}

	protected void afterPaste(Object [] pasted)	{
		for (int i = 0; pasted != null && i < pasted.length; i++)	{
			if (i == 0)
				getSelection().clearSelection();
			((MultipleSelection)getSelection()).addSelectedObject(((AbstractMutableTreeModelItem)pasted[i]).getUserObject());
		}
		setSourceModel(null);
	}




	public void cb_New_Model(Object selection)	{
		boolean repeat = false;
		String target;

		do	{
			target = JOptionPane.showInputDialog(getDialogParent(), "Enter Name For The New Filter Model:");
			if (target == null)
				return;

			repeat = ValidFilename.checkFilename(target) == false;

			if (repeat)
				JOptionPane.showMessageDialog(
						getDialogParent(),
						"Not A Valid Filename: \""+target+"\"",
						"Error",
						JOptionPane.ERROR_MESSAGE);
		}
		while (repeat);

		getFilterTreeView().getModelManager().createInput(target);
	}

	public void cb_Delete_Model(Object selection)	{
		getFilterTreeView().getModelManager().deleteInput();
	}

	public void cb_Load_Model(Object selection)	{
		getFilterTreeView().getModelManager().commitInput();
	}



	// interface CrudCombo.CrudListener

	/** A new model name was created. */
	public void itemCreated(Object item)	{
		System.err.println("itemCreated "+item);
		setModel(factory.get(item.toString()));
		setChanged(true);	// is not yet persistent!
	}

	/** A model name was changed. */
	public void itemRenamed(Object oldName, Object newName)	{
		System.err.println("itemRenamed "+getModelName()+" = "+oldName+" to "+newName);
		//if (oldName.equals(getModelName()) == false)
		factory.rename(oldName.toString(), newName.toString());
	}

	/** A model name was deleted. */
	public void itemDeleted(Object item)	{
		System.err.println("itemDeleted "+getModelName()+" = "+item);
		factory.delete(item.toString());
	}

	/** Another model name was selected. */
	public void itemSelected(Object item)	{
		System.err.println("itemSelected "+item);
		if (getModel() != null && item.equals(getModelName()))
			return;
		setModel(factory.get(item.toString()));
	}


	// interface CrudCombo.CrudPendingListener

	/** A new item is about to be created. @return false to veto creation. */
	public boolean canCreate(Object item)	{
		return checkDefaultModelName(item);
	}

	/** An item is about to be renamed. @return false to veto rename. */
	public boolean canRename(Object oldName, Object newName)	{
		return checkDefaultModelName(oldName);
	}

	/** An item is about to be deleted. @return false to veto deletion. */
	public boolean canDelete(Object item)	{
		return checkDefaultModelName(item);
	}

	private boolean checkDefaultModelName(Object name)	{
		return name.equals(FilterTreePersistence.DEFAULT_FILTER_NAME) == false;
	}



	private FilterTreeModel getFilterTreeModel()	{
		return (FilterTreeModel)getModel();
	}

	private JTree getTree()	{
		return (JTree)((SwingView)getView()).getSensorComponent();
	}

	private DefaultMutableTreeNode getFirstSelectedOrRoot(Object selection)	{
		List l = (List)selection;
		DefaultMutableTreeNode node = (l != null && l.size() > 0)
				? (DefaultMutableTreeNode)l.get(0)
				: getModel() != null
					? (DefaultMutableTreeNode)getFilterTreeModel().getRoot()
					: null;
		return node;
	}

	private FilterTreeView getFilterTreeView()	{
		return (FilterTreeView)getView();
	}

	private String getModelName()	{
		if (getFilterTreeModel() != null)
			return getFilterTreeModel().getName();
		return null;
	}

}
