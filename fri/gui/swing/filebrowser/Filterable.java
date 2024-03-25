package fri.gui.swing.filebrowser;

public interface Filterable
{
	/**
		Is the node a leaf-node or a directory-node
	*/
	public boolean isLeaf();
	/**
		Is this node a hidden node?
	*/
	public boolean isHiddenNode();
	public String toString();
}