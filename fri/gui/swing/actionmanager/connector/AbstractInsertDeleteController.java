package fri.gui.swing.actionmanager.connector;

import java.util.List;
import java.awt.event.*;
import javax.swing.Icon;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.model.*;
import fri.gui.mvc.view.swing.SwingView;

/**
	A controller that can insert and delete items.
	The published actions must be named "New", "Delete".
	
	@author Fritz Ritzberger
*/

public abstract class AbstractInsertDeleteController extends AbstractSwingController
{
	public static final String ACTION_NEW = "New";
	public static final String ACTION_DELETE = "Delete";


	/** Do-nothing constructor. */
	public AbstractInsertDeleteController()	{
		this(null);
	}
	
	/** Sets the passed View. */
	public AbstractInsertDeleteController(SwingView view)	{
		this(view, null);
	}
	
	/** Sets the passed View and fallback ActionListener for runtime-defined actions. */
	public AbstractInsertDeleteController(SwingView view, ActionListener fallback)	{
		super(view, fallback);
	}

	/** Publishes "New" and "Delete". */
	protected void insertActions()	{
		registerAction(ACTION_NEW, getIconForAction(ACTION_NEW), "Create New Item", KeyEvent.VK_INSERT, 0);
		registerAction(ACTION_DELETE, getIconForAction(ACTION_DELETE), "Delete Selected Items", KeyEvent.VK_DELETE, 0);
	}
	
	/** Provides a icon to action mapping. */
	protected abstract Icon getIconForAction(String actionName);


	/** Implemented to retrieve selection as <i>java.util.List</i> and call setEnabledSelection(list)</i>. */
	protected void setEnabledActions()	{
		List selection = getSelection() != null ? (List)getSelection().getSelectedObject() : null;
		setEnabledActions(selection);
	}
	
	/**
		This gets called from <i>setView()</i> after <i>insertActions()</i>.
		Subclasses should enable their Actions according to the new View's selection state.
	*/
	protected abstract void setEnabledActions(List selection);



	/**
		Create a new item. This calls
		<i>getInsertLocation(), newCreateCommand(), manageCommand(), setEnabledActions(), afterCreate()</i>.
	*/
	public void cb_New(Object selection)	{
		// get positional information where to insert new item
		Object insertLocation = getInsertLocation(selection);
			
		// launch a create command
		Command cmd = newCreateCommand(insertLocation);
		startCreate(cmd);
	}
	
	/**
		This gets called from <i>cb_New()</i>.
		Subclasses return an Integer position or ModelItem parent where to insert the new item.
	*/
	protected abstract Object getInsertLocation(Object selection);

	/**
		This gets called from <i>cb_New()</i>, <i>cb_Delete</i>
		and <i>toModelItems()</i> (AbstractClipboardController).
		Subclasses create a ModelItem for the passed view item.
		@param viewItem a TreeNode or table row Vector, or null if new item in a table view.
		@return a ModelItem that wraps the viewItem for MVC.
	*/
	protected abstract ModelItem createModelItem(Object viewItem);

	/**
		This gets called from <i>cb_New()</i>.
		Subclasses create a name or some data content for a new item.
		A tree implementation e.g. would loop all children of the insert location
		and seek a unique name.
		@param insertLocation a TreeNode or table row Vector where the creation happens.
		@return a name or some other creation data for the new item.
	*/
	protected abstract Object getCreateData(Object insertLocation);

	/**
		This default implementation returns null (which means append at end).
		This gets called from <i>cb_New()</i>. Subclasses return a insert position for the new item.
		@param insertLocation a TreeNode or table row Vector where the creation happens.
		@return a position for the new item.
	*/
	protected Integer getCreatePosition(Object insertLocation)	{
		return null;
	}

	/** Allocates and returns a new DefaultCreateCommand. */
	protected Command newCreateCommand(Object insertLocation)	{
		return new DefaultCreateCommand(
				insertLocation instanceof ModelItem ? (ModelItem)insertLocation : createModelItem(null),
				(MutableModel)getModel(),
				getCreateData(insertLocation),
				getCreatePosition(insertLocation));
	}
	
	/**
		Starts the "create" work by <i>cmd.doit()</i>.
		Does call <i>manageCommand()</i>, <i>setEnabledActions()</i> and <i>afterCreate()</i> after.
		This is the place to implement a progress observer.
	*/
	protected void startCreate(Command cmd)	{
		Object created = cmd.doit();
		manageCommand(cmd);
		
		setEnabledActions();
		afterCreate(created);
	}
	
	/**
		Does nothing. This gets called from <i>cb_New()</i>.
		Override to select the new item or start an editor on it.
		@param created the Object returned from Command.doit(), normally the created ModelItem
			(depends on used clipboard implementation).
	*/
	protected void afterCreate(Object created)	{
	}


	/**
		Delete selected items. This calls (for each item in selection)
		<i>newDeleteCommand(), manageCommand(), setEnabledActions(), afterDelete()</i>.
	*/
	public void cb_Delete(Object selection)	{
		List sel = (List)selection;
		Command [] cmds = new Command [sel.size()];
		for (int i = sel.size() - 1; i >= 0; i--)	{
			Object item = sel.get(i);
			cmds[i] = newDeleteCommand(createModelItem(item));
		}
		startDelete(cmds);
	}

	/** Allocates and returns a new DefaultRemoveCommand. */
	protected Command newDeleteCommand(ModelItem modelItem)	{
		return new DefaultRemoveCommand(
				modelItem,
				(MutableModel)getModel());
	}

	/**
		Starts the "delete" work by calling <i>cmd.doit()</i> for all passed Commands.
		Does call <i>manageCommand()</i> after every Command,
		does call <i>setEnabledActions()</i> and <i>afterDelete()</i> after loop.
		This is the place to implement a progress observer.
	*/
	protected void startDelete(Command [] cmds)	{
		for (int i = 0; i < cmds.length; i++)	{
			cmds[i].doit();
			manageCommand(cmds[i]);
		}
		setEnabledActions();
		afterDelete();
	}
	
	/** Does nothing. Override to set new selection after delete. */
	protected void afterDelete()	{
	}


	/** Does nothing. Override to pass the executed Command to an an UndoManager. */
	protected void manageCommand(Command cmd)	{
	}

}
