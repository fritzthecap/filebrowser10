package fri.gui.swing.filebrowser;

import javax.swing.undo.*;

/**
	Target: Command Pattern for copying a file to another container
*/

public class CopyCommand extends FileCommand
{
	public CopyCommand(Listable root, String [] from, String [] to)	{
		super(root, from, to);
		//System.err.println("  new CopyCommand "+getPresentationName());
	}
	
	
	/** human readable name of command */
	public String getPresentationName() {
		return "copy "+from[from.length-1]+" to "+to[to.length-1];
	}
	

	public void undo() throws CannotUndoException {
		super.undo();
	
		//System.err.println("  CopyCommand.undo "+getPresentationName());		
		NetNode toNode = (NetNode)getTargetChild();
		
		if (toNode == null)
			error("undo");
		
		try	{	
			if (toNode.delete() == false)	// unbuffered
				error("undo");
		}
		catch (Exception e)	{
			error("undo: "+e);
		}

		toNode.unsetObserver();
	}
	
	
	public void redo() throws CannotRedoException {
		super.redo();

		//System.err.println("  CopyCommand.redo "+getPresentationName());		
		NetNode fromNode = (NetNode)NodeLocate.locate(root, from);		
		NetNode toNode   = (NetNode)NodeLocate.locate(root, to);
		
		if (fromNode == null || toNode == null)
			error("redo");
		
		try	{
			if (fromNode.copy(toNode) == null)
				error("redo");
		}
		catch (Exception e)	{
			error("redo: "+e);
		}
			
		fromNode.unsetObserver();
	}
	
	
	public long getRecursiveSize(TransactionObserver observer)	{
		NetNode node = null;		
		if (undone)
			node = (NetNode)NodeLocate.locate(root, from);		
		else
			node = (NetNode)getTargetChild();
			
		if (node != null)	{
			node.setObserver(observer);
			return node.getRecursiveSize();
		}
		return (long)0;
	}


	public void dump()	{
		System.err.println("------ copy command -------");
		super.dump();
	}
}