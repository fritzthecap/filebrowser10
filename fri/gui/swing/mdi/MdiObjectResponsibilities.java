package fri.gui.swing.mdi;

import java.awt.Component;
import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;

/**
	Implementers build a Component to add to the MdiFrame
	and expose a method to close it. Manages the Object to render and its
	lifecycle as seen from the MDI framework (create, select, close).
	<p>
	This interface can be used optionally as Object to pass to createMdiFrame()
	method, which tests the Object with instanceof for such capacity.
*/

public interface MdiObjectResponsibilities
{
	/**
		Returns the Component that renders the Object passed in constructor.
		If the Component was not yet built, build it and store it to a member variable.
		The Component will then be added by the container to CENTER.
		@param ic the MdiFrame instance that will add the returned Component. Just for type checks.
		@return the existing rendering Component or a newly built one.
	*/
	public Component getRenderingComponent(MdiFrame ic);
	
	/**
		Called every time an internal container gets activated (becomes visible).
	*/
	public void activated(MdiFrame ic);

	/**
		The built Component gets closed.
		This can be used for close confirmation when Object toRender was changed.
	*/ 	
	public void closing(MdiFrame ict, PropertyChangeEvent e) throws PropertyVetoException;

	/**
		Called every time an internal container was closed (removed from GUI parent).
	*/
	public void closed(MdiFrame ic);

	/** Sets the rendered object (e.g. when it was renamed). */
	public void setRenderedObject(Object renderedObject);

	/** Returns the rendered object. */
	public Object getRenderedObject();

	/**
		Returns the title of the rendered Object that will be displayed in titlebar or on tab header.
		As titles will be shorter on TabbedPane the caller is passed for instanceof test.
	*/
	public String getTitle(MdiFrame containerType);
	public String getToolTip(MdiFrame containerType);
	
}