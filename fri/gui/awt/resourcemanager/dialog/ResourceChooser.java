package fri.gui.awt.resourcemanager.dialog;

/** Any Panel that implements this interface can be a GUI for customizing one Resurce like Font, Color, ... */

public interface ResourceChooser
{
	/** Type identifier for text language ResourceChooser. */
	public static final String LANGUAGE = "Language";


	/** Returns a value like Font, Color, ... that represents the current user-made settings. */
	public Object getValue();

	/** Returns the String Resource type this editor can change: ResourceFactory.FONT, ... */
	public String getResourceTypeName();

	/** Subclasses return true when the resource is set to be component-type-bound. */
	public boolean isComponentTypeBound();

}
