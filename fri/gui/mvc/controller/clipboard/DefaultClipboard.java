package fri.gui.mvc.controller.clipboard;

import java.util.List;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.controller.Command;
import fri.gui.mvc.controller.CommandArguments;
import fri.gui.mvc.controller.UndoableCommand;
import fri.gui.swing.undo.DoListener;

/**
 	Ein Clipboard fuer Cut/Copy/Paste mit Undo/Redo-Option.
	Dieses Objekt enthaelt die grundlegende Logik einer Zwischenablage:
 	Buffern der selektierten Items, durchfuehren der Commands
 	Cut/Copy/Paste mit optionalem Undo.
 	<p>
 	Es wickelt die Kopier- und Verschiebe-Logik fuer ein oder mehrere
 	Ziele ab und delegiert dabei die Vorgaenge an die ModelItem Knoten
 	und deren CommandArguments.
 	Es beschickt einen optionalen Undo-Listener mit Command-Patterns.
 	Es setzt Knoten en- oder disabled, wenn sie ausgeschnitten werden.

	<PRE>
	public class MyClipboard extends DefaultClipboard 
	{
		private static MyClipboard singleton = null;

		public static DefaultClipboard singleton()	{
		  if (singleton == null)
			singleton = new MyClipboard();
		  return singleton;
		}

		public Command createCopyCommand(ModelItem source, ModelItem target, CommandArguments args)	{
		  return new MyCopyEdit(source, target, null);
		}
		public Command createMoveCommand(ModelItem source, ModelItem target, CommandArguments args)	{
		  return new MyMoveEdit(source, target, source.getModelItemContext());
		}
	}
	</PRE>

	@author  Ritzberger Fritz
*/

public class DefaultClipboard
{
	protected static DefaultClipboard singleton;
	
	/** Returns a singleton to be used by all instances */
	public static DefaultClipboard singleton()	{
		if (singleton == null)
			singleton = new DefaultClipboard();
		return singleton;
	}


	protected ModelItem [] nodes;
	protected DoListener doListener;
	private MutableModel sourceModel;

	/** As an alternative to factory singleton this constructor provides individual clipboards. */
	public DefaultClipboard()	{
	}

	
	/** Returns the set of nodes that were copied or cutten.
		CAUTION: This can be a copy or the originally passed array!
	*/
	public ModelItem [] getSourceModelItems()	{
		return nodes;
	}


	/** Disable items by calling setMovePending(true) and store the passed 
		object(s) to be moved. No physical copy of the array is made! */
	public void cut(ModelItem [] nodes)	{
		clear();

		this.nodes = nodes;
		
		if (nodes == null || nodes.length <= 0)
			throw new IllegalArgumentException("DefaultClipboard: no nodes given!");
			
		for (int i = 0; nodes != null && i < nodes.length; i++)
			nodes[i].setMovePending(true);
	}
	
	/** Disable items by calling setMovePending(true) and store the passed
		object(s) to be moved. This call creates a physical copy of the List! */
	public void cut(List nodes)	{
		cut((ModelItem[]) nodes.toArray(new ModelItem[nodes.size()]));
	}



	/** Store the passed object(s) to be copied.
		No physical copy of the array is made! */
	public void copy(ModelItem [] nodes)	{
		clear();

		this.nodes = nodes;
	}
	
	/** Store the passed object(s) to be copied.
		This call creates a physical copy of the List! */
	public void copy(List nodes)	{
		copy((ModelItem[]) nodes.toArray(new ModelItem[nodes.size()]));
	}


	/** Delegates to paste(target, null), without creating a physical copy of list! */
	public Object [] paste(ModelItem [] nodes)	{
		return paste(nodes, null);
	}
	
	/** Delegates to paste(target, null) */
	public Object [] paste(List target)	{
		return paste(target, null);
	}
	
	/** Creates a physical copy of target-list and delegates to paste(target, pasteInfo) */
	public Object [] paste(List target, CommandArguments pasteInfo)	{
		return paste((ModelItem[]) target.toArray(new ModelItem[target.size()]), pasteInfo);
	}
	
	/**
		Put the registered object(s) to the passed parent(s) by copy or move,
		according to the state of the registered objects:
		move if node.getMovePending() returns true, else copy.
		This call does not create a physical copy of list.
		
		@param targets the target node for trees, or null for tables.
		@param pasteInfo the command argument holding copy/move information about models and positions.
		@return the moved/copied items.
	*/
	public Object [] paste(ModelItem [] targets, CommandArguments pasteInfo)	{
		if (nodes == null || nodes.length <= 0)
			return null;
		
		// make minimal target length one
		int targetLength = targets != null && targets.length > 0 ? targets.length : 1;
		Object [] result = new Object[nodes.length * targetLength];

		try	{
			if (doListener != null)
				doListener.beginUpdate();
			
			int position = 0;
			
			// loop from tail to create same sort order in target folder
			for (int i = nodes.length - 1; i >= 0; i--)	{
				if (nodes[i] == null)
					continue;
					
				if (nodes[i].isMovePending())	{	// was cutten, do move
					if (targetLength > 1)
						throw new IllegalArgumentException("Moving to multiple nodes is not possible!");
					
					ModelItem target = targets != null && targets.length > 0 ? targets[0] : null;
					result[position] = doCommand(true, nodes[i], target, pasteInfo);
					position++;
				}
				else	{	// was copied, do copy
					for (int j = 0; j < targetLength; j++)	{
						ModelItem target = targets != null && targets.length > 0 ? targets[j] : null;
						result[position] = doCommand(false, nodes[i], target, pasteInfo);
						position++;
					}
				}
			}
		}
		finally	{
			if (nodes[0].isMovePending())
				clear();	// set nodes movePending to false after move
			
			if (doListener != null)
				doListener.endUpdate();
		}
			
		return result;
	}	
	
	
	private Object doCommand(boolean isMove, ModelItem source, ModelItem target, CommandArguments pasteInfo)	{
		Command command = isMove 
			? createMoveCommand(source, target, pasteInfo)
			: createCopyCommand(source, target, pasteInfo);
			
		if (checkPreconditions(source, target, isMove) == false)
			return null;
		
		Object o = command.doit();

		if (o == null)
			errorHandling(source, target, isMove);
		else
		if (doListener != null && command instanceof UndoableCommand)
			doListener.addEdit((UndoableCommand) command);
		
		return o;
	}
	
	/** To be overridden for e.g. overwrite checks. This is called for every copied or moved item. */
	protected boolean checkPreconditions(ModelItem source, ModelItem target, boolean isMove)	{
		return true;
	}
	
	/** Override for a better error handling than "Thread.dumpStack()". */
	protected void errorHandling(ModelItem source, ModelItem target, boolean isMove)	{
		Thread.dumpStack();
		System.err.println("The "+(isMove ? "move" : "copy")+" of "+source+" to "+target+" returned null!");
	}


	/**
		Factory method to create a Command (or UndoableCommand) for a Copy-Action.
		Override this for application-typed commands.
	*/
	public Command createCopyCommand(ModelItem source, ModelItem target, CommandArguments pasteInfo)	{
		return new DefaultCopyCommand(source, target, pasteInfo);
	}

	/**
		Factory method to create a Command (or UndoableCommand) for a Move-Action.
		Override this for application-typed commands.
	*/
	public Command createMoveCommand(ModelItem source, ModelItem target, CommandArguments pasteInfo)	{
		return new DefaultMoveCommand(source, target, pasteInfo);
	}


	
	/** Set the registered object to null, clear (set enabled) pending moves */
	public void clear()	{
		for (int i = 0; nodes != null && i < nodes.length; i++)
			if (nodes[i].isMovePending())
				nodes[i].setMovePending(false);
		nodes = null;
	}

	/** @return true if there is no registered object */
	public boolean isEmpty()	{
		return nodes == null;
	}

	/** @return true if there are cutten objects for moving */
	public boolean movePending()	{
		if (nodes != null && nodes.length > 0)
			return nodes[0].isMovePending();
		return false;
	}


	/** Set a undo listener for actions */
	public void setDoListener(DoListener doListener)	{
		this.doListener = doListener;
	}
	
	/** @return the undo listener for actions */
	public DoListener getDoListener()	{
		return doListener;
	}


	/** Returns the source model of the copy or move action that might have been initiated. */
	public MutableModel getSourceModel()	{
		return sourceModel;
	}

	/** Sets the source model for a copy or move action. */
	public void setSourceModel(MutableModel sourceModel)	{
		this.sourceModel = sourceModel;
	}

}
