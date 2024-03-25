package fri.gui.swing.filebrowser;

/**
	Target: Command Pattern for moving a file to a wastebasket
*/

public class RemoveCommand extends MoveCommand
{
	public RemoveCommand(Listable root, String [] from, String [] to)	{
		super(root, from, to);
	}
	
	
	/** human readable name of command */
	public String getPresentationName() {
		return "remove "+from[from.length-1]+" to wastebasket";
	}
	
	
	public void dump()	{
		System.err.println("------ remove command -------");
		super.dump();
	}
}