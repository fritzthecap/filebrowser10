package fri.gui.swing.undo;

import javax.swing.undo.*;
import javax.swing.event.*;
import java.util.Enumeration;

/**
	<UL>
	<LI>Target: Collecting undoable edits (command patterns)
		within some transaction context marked by beginUpdate() and
		endUpdate().
	<LI>Behaviour: Collects edits in a CompoundEdit, that is
		added to the UndoManager when endUpdate() is called.
		Calls to beginUpdate() get counted and the undoable edit
		is created when all were closed with endUpdate().
	<LI>Lifcyle: Create it with Undo- and Redo-Action.
		Call beginUpdate() before each transaction,
		addEdit() for each action,
		endUpdate() after transaction.
	<LI>Example:
		<PRE>
		DoAction undoAction = new DoAction(DoAction.UNDO);
		DoAction redoAction = new DoAction(DoAction.REDO);
		DoListener doListener = new DoListener(undoAction, redoAction);
		...
		// this installs the DoAction as ActionListener and creates a Menuitem
		JMenuItem undo = menu.add(undoAction);
		JMenuItem redo = menu.add(redoAction);
		...
		// begin a undoable action
		doListener.beginUpdate();
		...
		// add 1-n command patterns
		doListener.addEdit(new SomeCommandPattern(...));
		doListener.addEdit(new AnotherCommandPattern(...));
		...
		// end the undoable action
		doListener.endUpdate();	// Actions get enabled or disabled
		</PRE>

		Any undoable Edit implements these methods:
		<PRE>
		public String getPresentationName() {
			// ... visual representation of this command pattern
		}
		public void undo() throws CannotUndoException {
			super.undo();
			// ... undo semantics
		}
		public void redo() throws CannotRedoException {
			super.redo();
			// ... redo semantics
		}
		</PRE>
	</UL>
*/
public class DoListener implements UndoableEditListener
{
	protected DoAction undoAction, redoAction;
	protected ListableUndoManager undoManager;
	protected ListableCompoundEdit compoundEdit = null;
	private int transactions = 0;
	
	/**
		Listen to undoable edits and manage associated buttons.
		@param undoAction the global abstract undo action
		@param redoAction the global abstract redo action
	*/
	public DoListener(DoAction undoAction, DoAction redoAction)	{
		this.undoAction = undoAction;
		this.redoAction = redoAction;

		undoAction.setCounterPart(redoAction);
		redoAction.setCounterPart(undoAction);

		undoManager = new ListableUndoManager();

		undoAction.setUndoManager(undoManager);
		redoAction.setUndoManager(undoManager);
	}
	
	
	/** Implements a simple UndoableEditListener, without CompoundEdits: beginUpdate, addEdit, endUpdate. */
	public void undoableEditHappened(UndoableEditEvent e) {
		undoManager.undoableEditHappened(e);	// event contains undoable edit "AbstractDocument.DefaultDocumentEvent"
		update();
	}


	/** Start a transaction of 1-n undoable edits */
	public synchronized void beginUpdate()	{
		if (compoundEdit == null)	{
			compoundEdit = new ListableCompoundEdit();
		}
		transactions++;
	}

	/** Pass a undoable action within transaction */
	public void addEdit(UndoableEdit e) {
		if (compoundEdit != null)	{
			compoundEdit.addEdit(e);
		}
		else	{
			System.err.println("WARNING: ignoring undoable edit without transaction context: "+e.getPresentationName());
		}	// There can be situations where this is inevitable!
	}

	/** End a transaction of 1-n undoable edits, add transaction to UndoManager */
	public synchronized void endUpdate()	{
		transactions--;
		
		if (transactions <= 0)	{
			compoundEdit.end();	// make it undoable
			
			if (compoundEdit.isSignificant())	{	// not empty
				insertEditUpdateActions(compoundEdit);
			}
			
			compoundEdit = null;
		}
	}

	private void insertEditUpdateActions(UndoableEdit e)	{
		undoManager.addEdit(e);
		update();
	}
	
	private void update()	{
		undoAction.update();
		redoAction.update();
	}

	/**
		Returns all UndoableEdits in this undo manager.
		This can be used to kill edits of editors that have been closed.
	*/
	public Enumeration elements()	{
		return undoManager.elements();
	}

	/**
		After killing edits by <i>edit.die()</i> this method removes
		all dead edits from undo manager.
	*/
	public void removeDeadEdits()	{
		undoManager.removeDeadEdits();
		update();
	}

	/** Remove all edits from undo manager. */
	public void discardAllEdits()	{
		undoManager.discardAllEdits();
		update();
	}

	/**
		Returns the edit that is about to be undone next.
		Can be used by a listeners using DoAction.addWillActionPerformListener().
	*/
	public UndoableEdit getFirstEditToBeUndone()	{
		return undoManager.getFirstEditToBeUndone();
	}

	/**
		Returns the edit that is about to be redone next.
		Can be used by a listeners using DoAction.addWillActionPerformListener().
	*/
	public UndoableEdit getFirstEditToBeRedone()	{
		return undoManager.getFirstEditToBeRedone();
	}

}
