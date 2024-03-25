package fri.gui.swing.filebrowser;

public interface TreeExpander
{
	/** Expand the path defined by a string array */
	public boolean explorePath(String [] path);
	public boolean collapsePath(String [] path);
}