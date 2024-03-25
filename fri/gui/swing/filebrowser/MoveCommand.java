package fri.gui.swing.filebrowser;

import javax.swing.undo.*;

/**
	Target: Command Pattern for moving a file to another
*/

public class MoveCommand extends FileCommand
{
	public MoveCommand(Listable root, String [] from, String [] to)	{
		super(root, from, to);
		//System.err.println("  new MoveCommand "+getPresentationName());
	}


	/** human readable name of command */
	public String getPresentationName() {
		return "move "+from[from.length-1]+" to "+to[to.length-1];
	}	
	
	
	public void undo() throws CannotUndoException {
		super.undo();

		//System.err.println("  MoveCommand.undo "+getPresentationName());
		NetNode fromNode = (NetNode)getParent(from);		
		NetNode toNode   = (NetNode)getTargetChild();
		
		if (fromNode == null || toNode == null)
			error("undo");
		
		try	{	
			if (toNode.move(fromNode) == null)
				error("undo");
		}
		catch (Exception e)	{
			error("undo: "+e);
		}

		toNode.unsetObserver();
	}
	
	
	public void redo() throws CannotRedoException {
		super.redo();

		//System.err.println("  MoveCommand.redo "+getPresentationName());
		NetNode fromNode = (NetNode)NodeLocate.locate(root, from);		
		NetNode toNode   = (NetNode)NodeLocate.locate(root, to);
		
		if (fromNode == null || toNode == null)	{
			//System.err.println("------> redo error, from "+from+", to "+to);
			error("redo");
		}
		
		try	{
			if (fromNode.move(toNode) == null)
				//System.err.println("------> redo error, move failed");
				error("redo");
		}
		catch (Exception e)	{
			error("redo: "+e);
		}
	
		fromNode.unsetObserver();
	}

	
	public long getRecursiveSize(TransactionObserver observer)	{
		NetNode node = null;		
		if (undone)	// redo is enabled
			node = (NetNode)NodeLocate.locate(root, from);		
		else	// undo is enabled
			node = (NetNode)getTargetChild();
			
		if (node != null)	{
			node.setObserver(observer);
			return node.getRecursiveSize();
		}
		return (long)0;
	}

	
	public void dump()	{
		System.err.println("------ move command -------");
		super.dump();
	}
}