package fri.gui.swing.filebrowser;

import javax.swing.undo.*;

/**
	Target: Command Pattern for copying a file to another name in the same container
	Behaviour: Parameter "to" is not the container of the new file, but the new
		file or container itself.
*/

public class SaveCopyCommand extends FileCommand
{
	/**
		@param from original file
		@param to save copied file, fully qualified, not only target container
	*/
	public SaveCopyCommand(Listable root, String [] from, String [] to)	{
		super(root, from, to);
		System.err.println("  new SaveCopyCommand "+getPresentationName());
	}
	
	
	/** human readable name of command */
	public String getPresentationName() {
		return "copy "+from[from.length-1]+" to "+to[to.length-1];
	}
	

	public void undo() throws CannotUndoException {
		super.undo();
	
		System.err.println("  SaveCopyCommand.undo "+getPresentationName());		
		NetNode toNode = (NetNode)NodeLocate.locate(root, to);
		
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

		System.err.println("  SaveCopyCommand.redo "+getPresentationName());		
		NetNode fromNode = (NetNode)NodeLocate.locate(root, from);		
		
		if (fromNode == null)
			error("redo");
			
		try	{
			if (fromNode.saveCopy() == null)
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
			node = (NetNode)NodeLocate.locate(root, to);
			
		if (node != null)	{
			node.setObserver(observer);
			return node.getRecursiveSize();			
		}
		return (long)0;
	}

	
	public void dump()	{
		System.err.println("------ save copy command -------");
		super.dump();
	}
}