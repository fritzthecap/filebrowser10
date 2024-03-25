package fri.gui.swing.filebrowser;

/** Delegate insertion of new nodes to another object */
public interface NodeInserter
{
	public void insertContainer();
	public void insertNode();
}