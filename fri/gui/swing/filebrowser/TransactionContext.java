package fri.gui.swing.filebrowser;

/**
	A file transaction wrapper.
	<p>
	REMARK: as FileNode implementation is to complex to hold
	filter and state flags here in a context, these things continue
	to be in static FileNode variables. This makes FileNode implementation
	unsafe against concurrent transactions, as states will be confused.
	<br>
	The states are in conjunction with undoable edits, the filter
	affects the set of transfered files. Both can not be bound to the
	observer, as the observer is not passed through consistently.
	This can not be the aim, as observer needs not to know progress about
	e.g. saving something to wastebasket when moving ...
*/

public class TransactionContext
{
	private TransactionDialog observer;
	private NetNode root;
	private String filter = null;
	private boolean include;
	private boolean showfiles, showhidden;


	public TransactionContext(NetNode root)	{
		this.root = root;
	}
	
	public TransactionContext(
		NetNode root,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
	{
		this.root = root;
		this.filter = filter;
		this.include = include;
		this.showfiles = showfiles;
		this.showhidden = showhidden;
	}


	public void setObserver(TransactionDialog observer)	{
		this.observer = observer;
		observer.setContext(this);
	}
	
		
	public void beginTransaction()	{
		if (filter != null)
			root.setListFiltered(filter, include, showfiles, showhidden);

		// activate the undo listener
		root.getDoListener().beginUpdate();
	}

	public void endTransaction()	{
		suspendTransaction();
		// deactivate the undo listener
		root.getDoListener().endUpdate();

		if (observer != null)	{
			observer.endDialog();
			observer = null;
		}
	}
	
	public void suspendTransaction()	{
		// pause: reset filter
		root.resetListFiltered();
	}

	public void resumeTransaction()	{
		if (filter != null)
			root.setListFiltered(filter, include, showfiles, showhidden);
	}

}