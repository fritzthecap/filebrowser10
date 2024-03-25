package fri.gui.swing.filebrowser;

import javax.swing.undo.*;

/**
	Target: Basic command pattern for file transactions.
	Behaviour: The target is always a container if it is a copy or move command.
*/

public abstract class FileCommand extends AbstractUndoableEdit
{
	protected String [] from;
	protected String [] to;
	protected Listable root;
	protected boolean undone = false;	// state of the command
	private boolean strict = false;	// throw an exception at undo/redo error


	public void undo()	throws CannotUndoException	{
		super.undo();
		undone = true;
	}

	public void redo()	throws CannotRedoException	{
		super.redo();
		undone = false;
	}

	public abstract long getRecursiveSize(TransactionObserver observer);
	
	protected FileCommand(Listable root, String [] from, String [] to)	{
		super();
		this.root = root;
		this.from = from;
		this.to = to;
	}
	
	/** locate node with basename of from-file in to-container */
	protected Listable getTargetChild()	{
		String [] fullName = new String[to.length + 1];
		System.arraycopy(to, 0, fullName, 0, to.length);
		fullName[to.length] = from[from.length-1];
		return NodeLocate.locate(root, fullName);
	}

	/** locate parent-container of a path */
	protected Listable getParent(String [] node)	{
		String [] path = new String[node.length - 1];
		System.arraycopy(node, 0, path, 0, path.length);
		return NodeLocate.locate(root, path);
	}
	
	
	protected void error(String meaning)
		throws CannotUndoException, CannotRedoException
	{
		dump();
		if (strict)
			if (meaning.equals("undo"))
				throw new CannotUndoException();
			else
				throw new CannotRedoException();
	}
		
	protected void dump()	{
		System.err.print("....from = ");
		for (int i = 0; from != null && i < from.length; i++)
			System.err.print("\""+from[i]+"\" ");
		System.err.println();
		System.err.print("....to = ");
		for (int i = 0; to != null && i < to.length; i++)
			System.err.print("\""+to[i]+"\" ");
		System.err.println();
	}
}