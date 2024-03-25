package fri.gui.swing.filebrowser;

import javax.swing.undo.*;

/**
	Target: Command Pattern for inserting a new created empty node into a container.
	Behaviour: Parameter "from" is always null, not used
*/

public class InsertCommand extends FileCommand
{
	private boolean isLeaf;
	
	/**
		@param to full path of new file
	*/
	public InsertCommand(Listable root, String [] to)	{
		super(root, null, to);
		System.err.println("  new InsertCommand "+getPresentationName());
	}
	
	
	/** human readable name of command */
	public String getPresentationName() {
		return "insert "+to[to.length-1]+" into "+to[to.length-2];
	}
	
	
	public void undo() throws CannotUndoException {
		super.undo();

		System.err.println("  InsertCommand.undo "+getPresentationName());		
		NetNode toNode = (NetNode)NodeLocate.locate(root, to);

		if (toNode == null)
			error("undo");
	
		isLeaf = toNode.isLeaf();
			
		try	{
			if (toNode.delete() == false)
				error("undo");
		}
		catch (Exception e)	{
			error("undo: "+e);
		}
	}
	
	
	public void redo() throws CannotRedoException {
		super.redo();

		System.err.println("  InsertCommand.redo "+getPresentationName());		
		NetNode toNode = (NetNode)getParent(to);
	
		if (toNode == null)
			error("redo");
		
		if (isLeaf)	{
			if (toNode.createNode() == null)
				error("redo");
		}
		else	{
			if (toNode.createContainer(to[to.length-1]) == null)
				error("redo");
		}
	}
	
	
	public long getRecursiveSize(TransactionObserver observer)	{
		return (long)0;
	}
	

	public void dump()	{
		System.err.println("------ insert command -------");
		super.dump();
	}
}