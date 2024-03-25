package fri.gui.swing.filebrowser;

import java.util.Vector;

/** Rendering and listing tree nodes. */

public interface Listable
{
	/** @return list of children of this node. */
	public Vector list();

	/** @return String name of node (to identify a child in a list) */
	public String getLabel();
}