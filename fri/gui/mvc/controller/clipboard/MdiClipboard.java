package fri.gui.mvc.controller.clipboard;

import java.util.List;
import java.util.Enumeration;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.Command;
import fri.gui.mvc.controller.AbstractEditCommand;
import fri.gui.mvc.controller.CommandArguments;
import fri.gui.swing.undo.*;

/**
 	A Clipboard that can manage multiple editors that interchange data.
 	It stores the editors and passes them to the created Commands.
 	This is for applications that use one UndoManager for multiple editor windows.

	@author  Ritzberger Fritz
*/

public class MdiClipboard extends DefaultClipboard
{
	protected Object sourceEditor;
	protected Object targetEditor;
	
	
	/** @return a singleton to be used by all instances */
	public static DefaultClipboard singleton()	{
		if (singleton == null)
			singleton = new MdiClipboard();
		return singleton;
	}


	/** Alternately to factory singleton this constructor provides individual clipboards. */
	public MdiClipboard()	{
	}

	
	/** Additionally store the originator editor. */
	public void cut(Object sourceEditor, List nodes)	{
		cut(sourceEditor, (ModelItem[]) nodes.toArray(new ModelItem[nodes.size()]));
	}

	/** Additionally store the originator editor. */
	public void cut(Object sourceEditor, ModelItem [] nodes)	{
		// keep order!
		super.cut(nodes);
		setSourceEditor(sourceEditor);
	}
	


	/** Additionally store the originator editor. */
	public void copy(Object sourceEditor, List nodes)	{
		copy(sourceEditor, (ModelItem[]) nodes.toArray(new ModelItem[nodes.size()]));
	}

	/** Additionally store the originator editor. */
	public void copy(Object sourceEditor, ModelItem [] nodes)	{
		// keep order!
		super.copy(nodes);
		setSourceEditor(sourceEditor);
	}
	

	
	/** Additionally store the target editor. */
	public Object [] paste(Object targetEditor, List target, CommandArguments pasteInfo)	{
		return paste(targetEditor, (ModelItem[]) target.toArray(new ModelItem[target.size()]), pasteInfo);
	}
	
	/** Additionally store the target editor. */
	public Object [] paste(Object targetEditor, ModelItem [] target, CommandArguments pasteInfo)	{
		// keep order!
		setTargetEditor(targetEditor);
		return super.paste(target, pasteInfo);
	}	


	/** Set editor variables to null. */
	public void freeEditors()	{
		setSourceEditor(null);
		setTargetEditor(null);
	}

	/** Returns the target editor if it was set. */
	public Object getTargetEditor()	{
		return targetEditor;
	}
	private void setTargetEditor(Object targetEditor)	{
		this.targetEditor = targetEditor;
	}

	/** Returns the source editor if it was set. */
	public Object getSourceEditor()	{
		return sourceEditor;
	}
	protected void setSourceEditor(Object sourceEditor)	{
		this.sourceEditor = sourceEditor;
	}


	/**
		Factory method to create a Command (or UndoableCommand) for a Copy-Action.
		Override this for application-typed commands.
	*/
	public Command createCopyCommand(ModelItem source, ModelItem target, CommandArguments pasteInfo)	{
		return new DefaultCopyCommand(getSourceEditor(), getTargetEditor(), source, target, pasteInfo);
	}

	/**
		Factory method to create a Command (or UndoableCommand) for a Move-Action.
		Override this for application-typed commands.
	*/
	public Command createMoveCommand(ModelItem source, ModelItem target, CommandArguments pasteInfo)	{
		return new DefaultMoveCommand(getSourceEditor(), getTargetEditor(), source, target, pasteInfo);
	}



	/**
		Remove all edits that belong to the passed closed editor. No edit that has another
		editor registered will be removed (copy/move), but as soon as all involved editors are closed,
		the edit will be removed from UndoManager.
		<p>
		This method knows ListableCompoundEdit and AbstractEditCommand. Any other edit type will cause
		a stack dumped, but no exception thrown!
	*/
	public void cleanEdits(Object closedEditor)	{
		// remove all edits from undo listener that belong to this view
		for (Enumeration e = getDoListener().elements(); e.hasMoreElements(); )	{
			Object o = e.nextElement();

			if (o instanceof ListableCompoundEdit)	{
				ListableCompoundEdit edit = (ListableCompoundEdit)o;
				boolean allDead = true;

				for (Enumeration e2 = edit.elements(); e2.hasMoreElements(); )	{
					AbstractEditCommand edit2 = (AbstractEditCommand)e2.nextElement();

					if (edit2.diesWhenEditorClosed(closedEditor))	{
						edit2.die();
					}
					else	{
						allDead = false;
					}
				}

				if (allDead)
					edit.die();
			}
			else
			if (o instanceof AbstractEditCommand)	{
				AbstractEditCommand edit2 = (AbstractEditCommand)o;

				if (edit2.diesWhenEditorClosed(closedEditor))	{
					edit2.die();
				}
			}
			else	{
				Thread.dumpStack();	// someone changed DoListener implementation?
			}
		}

		getDoListener().removeDeadEdits();
	}


	/**
		Returns the target editor that belongs to the pending undo or redo action.
		This method knows only subclasses of AbstractEditCommand. It is able to list
		ListableCompoundEdits.
	*/
	public Object getEditorPendingForUndo(String undoOrRedo)	{
		Object edit =
				undoOrRedo.equals(DoAction.UNDO) ? doListener.getFirstEditToBeUndone()
				: undoOrRedo.equals(DoAction.REDO) ? doListener.getFirstEditToBeRedone() : null;

		if (edit instanceof AbstractEditCommand)	{	// not null
			return ((AbstractEditCommand)edit).getTargetEditor();
		}
		else	{
			Thread.dumpStack();	// someone changed DoListener implementation?
		}
		
		return null;
	}
	
}