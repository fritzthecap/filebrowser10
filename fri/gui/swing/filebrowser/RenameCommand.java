package fri.gui.swing.filebrowser;

import javax.swing.undo.*;

/**
	Target: Command Pattern for renaming a file
	Behaviour: Parameter "from" is the old name of file in first array-element
	with no further path
*/

public class RenameCommand extends FileCommand
{
	/**
		@param from old name of file in first array-element, no path
		@param to full path of file with new name
	*/
	public RenameCommand(Listable root, String [] from, String [] to)	{
		super(root, from, to);
		System.err.println("  new RenameCommand "+getPresentationName());
	}
	
	
	/** human readable name of command */
	public String getPresentationName() {
		return "rename "+from[from.length-1]+" to "+to[to.length-1];
	}


	public void undo() throws CannotUndoException {
		super.undo();

		System.err.println("  RenameCommand.undo "+getPresentationName());		
		NetNode toNode = (NetNode)NodeLocate.locate(root, to);

		if (toNode == null)
			error("undo");
		
		if (toNode.rename(from[from.length-1]) == null)
			error("undo");
	}
	
	
	public void redo() throws CannotRedoException {
		super.redo();

		System.err.println("  RenameCommand.redo "+getPresentationName());		
		
		String [] frompath = new String[to.length];
		System.arraycopy(to, 0, frompath, 0, to.length);
		frompath[to.length-1] = from[from.length-1];
		NetNode fromNode = (NetNode)NodeLocate.locate(root, frompath);
		
		if (fromNode == null)
			error("redo");
		
		if (fromNode.rename(to[to.length-1]) == null)
			error("redo");
	}


	public long getRecursiveSize(TransactionObserver observer)	{
		return (long)0;
	}


	public void dump()	{
		System.err.println("------ rename command -------");
		super.dump();
	}
}