package fri.gui.awt.resourcemanager.dialog;

import fri.gui.awt.resourcemanager.resourceset.ResourceSet;

/**
	Any GUI that implements this interface can be a ResourceGUI,
	customizing a Window or a Component-type.
*/

public interface ResourceSetEditor
{
	/**
		Called after the modal customize dialog closed.
		Returns a ResourceSet that represents the user-made settings.
		This can be a newly constructed ResourceSet, but must not be null.
	*/
	public ResourceSet getResourceSet();
}
