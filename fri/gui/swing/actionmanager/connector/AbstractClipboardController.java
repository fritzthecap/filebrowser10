package fri.gui.swing.actionmanager.connector;

import java.util.List;
import java.awt.event.*;
import fri.gui.mvc.controller.clipboard.*;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.model.*;
import fri.gui.mvc.view.swing.SwingView;

/**
	A controller that adds cut/copy/paste (by means of a clipboard) to inherited insert/delete.
	The published actions must be named "Cut", "Copy", "Paste".
	
	@author Fritz Ritzberger
*/

public abstract class AbstractClipboardController extends AbstractInsertDeleteController
{
	public static final String ACTION_CUT = "Cut";
	public static final String ACTION_COPY = "Copy";
	public static final String ACTION_PASTE = "Paste";


	/** Do-nothing constructor. */
	public AbstractClipboardController()	{
		this(null);
	}
	
	/** Sets the passed View. */
	public AbstractClipboardController(SwingView view)	{
		this(view, null);
	}
	
	/** Sets the passed View and fallback ActionListener for runtime-defined actions. */
	public AbstractClipboardController(SwingView view, ActionListener fallback)	{
		super(view, fallback);
	}
	
	/** Publishes "Cut", "Copy" and "Paste". */
	protected void insertActions()	{
		super.insertActions();
		
		registerAction(ACTION_CUT, getIconForAction(ACTION_CUT), "Cut Selected Item(s) To Clipboard", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		registerAction(ACTION_COPY, getIconForAction(ACTION_COPY), "Copy Selected Item(s) To Clipboard", KeyEvent.VK_C, InputEvent.CTRL_MASK);
		registerAction(ACTION_PASTE, getIconForAction(ACTION_PASTE), "Paste Item(s) From Clipboard", KeyEvent.VK_V, InputEvent.CTRL_MASK);
		
		getKeySensor().addKeyListener(new KeyAdapter()	{
			public void keyPressed(KeyEvent e)	{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
					getClipboard().clear();
					refreshActionState();
				}
			}
		});
	}


	/** Converts a list of view-items to an array of ModelItems. Needed by drag & drop for dragged nodes. */
	public ModelItem [] toModelItems(List selection)	{
		ModelItem [] items = new ModelItem[selection.size()];
		for (int i = 0; i < selection.size(); i++)
			items[i] = createModelItem(selection.get(i));
		return items;
	}

	/** Calls <i>setEnabledActions()</i> and <i>getView().refresh()</i> to update view after an action. */
	protected void refreshActionState()	{
		setEnabledActions();
		getSwingView().refresh();
	}


	/** Returns the clipboard for cut/copy/paste. When interchanging data between controllers, clipboard must be static. */
	protected abstract DefaultClipboard getClipboard();


	/** Default Cut callback: set selected ModelItems to clipboard. */
	public void cb_Cut(Object selection)	{
		clipboardCutOrCopy(selection, false);
	}

	/** Default Copy callback: set selected ModelItems to clipboard. */
	public void cb_Copy(Object selection)	{
		clipboardCutOrCopy(selection, true);
	}

	/** Called by cb_Copy() and cb_Cut(). Calls toModelItems(), setSourceModel() and refreshActionState(). */
	protected void clipboardCutOrCopy(Object selection, boolean isCopy)	{
		if (isCopy)
			getClipboard().copy(toModelItems((List)selection));
		else
			getClipboard().cut(toModelItems((List)selection));

		setSourceModel((MutableModel)getModel());
		refreshActionState();	// (1) show cutten items disabled (2) copy after cut must enable cutten items
	}

	/** Returns the clipboard's Model. Override to add special behaviour for data interchange between two editors. */
	public MutableModel getSourceModel()	{
		return getClipboard().getSourceModel();
	}
	
	/** Sets the Model to clipboard. Override to to add special behaviour for data interchange between two editors. */
	public void setSourceModel(MutableModel sourceModel)	{
		getClipboard().setSourceModel(sourceModel);
	}


	/** Overridden to clear clipboard when a cutten item is deleted. */
	protected Command newDeleteCommand(ModelItem modelItem)	{
		Object o = modelItem.getUserObject();
		if (o instanceof Movable && ((Movable)o).isMovePending())
			getClipboard().clear();
			
		return super.newDeleteCommand(modelItem);
	}


	/**
		Default Paste callback: paste ModelItems from clipboard to selection.
		Subclasses that need undo/redo must have set the <i>DoListener</i> to
		clipboard by calling <i>clipboard.setDoListener(doListener)</i>. This calls
		<i>getInsertLocation(), checkPaste(), newPasteArguments(), getPasteTargetModelItems(), startPaste()</i>.
	*/
	public void cb_Paste(Object selection)	{
		Object insertLocation = getInsertLocation(selection);
		
		ModelItem [] sourceItems = getClipboard().getSourceModelItems();
		if (checkPaste(insertLocation, sourceItems) == false)
			return;

		CommandArguments args = newPasteArguments(insertLocation);
		ModelItem [] items = getPasteTargetModelItems((List)selection, insertLocation);
		startPaste(items, args);
	}

	/**
		Check for impossible paste actions like pasting a treenode within one of its children.
		Show a dialog about the error. This implementation simply returns true.
		@param insertLocation Integer position for table view, ModelItem parent for tree view:
				whatever getInsertLocation() returned for view item.
		@param sourceModelItems the items that were set to clipboard.
		@return true if insertion is OK, else false (e.g. insertion of parent into a child folder).
	*/
	protected boolean checkPaste(Object insertLocation, ModelItem [] sourceModelItems)	{
		return true;
	}

	/**
		Allocates and returns a CommandArguments object for pasting by DefaultClipboard.
		This implementation simply returns arguments containing the MutableModel, and the
		insertLocation if it is instanceof Integer.
		Override if a tree child position is needed, or if other models are involved.
		@param insertLocation whatever getInsertLocation() returned.
	*/
	protected CommandArguments newPasteArguments(Object insertLocation)	{
		Integer itg = null;
		if (insertLocation instanceof Integer)
			itg = (Integer)insertLocation;
			
		return new CommandArguments.Paste(getSourceModel(), (MutableModel)getModel(), itg);
	}
	
	/**
		Returns the target items for pasting. Can return null for TableModel.
		@param selection the selection within View
		@param insertLocation whatever getInsertLocation() returned.
		@return array of ModelItem containing target nodes for trees, or null for tables.
	*/
	protected abstract ModelItem [] getPasteTargetModelItems(List selection, Object insertLocation);

	/**
		Starts the paste work by <i>getClipboard().paste(items, args)</i>.
		Does call <i>refreshActionState()</i> and <i>afterPaste()</i> after.
		This is the place to implement a progress observer.
	*/
	protected void startPaste(ModelItem [] items, CommandArguments args)	{
		Object [] pasted = getClipboard().paste(items, args);
		refreshActionState();
		afterPaste(pasted);
	}
		
	/**
		Does nothing. Override to select pasted items.
		@param pasted normally ModelItem [] array, for DefaultClipboard and DefaultCopyCommand/DefaultMoveCommand.
	*/
	protected void afterPaste(Object [] pasted)	{
	}

}
