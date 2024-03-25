package fri.gui.swing.filebrowser;

import java.util.Vector;

public interface NetNodeListener
{
	/** The children of the listening container-node are changed */
	public void childrenRefreshed(Vector children);
	
	/** The listening node was remnamed */
	public void nodeRenamed();
	
	/** The listening node was removed */
	public void nodeDeleted();
	
	/** An new node was inserted in the listening container-node */
	public void nodeInserted(Object newnode);
	
	/** A node was grayed (disabled) as it was cutten for paste */
	public void movePending();
}