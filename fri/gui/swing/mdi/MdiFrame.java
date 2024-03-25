package fri.gui.swing.mdi;

import java.awt.Component;
import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;

/**
	Internal interface that is implemented by either MdiInternalFrame or a MdiInternalTabPanel.
	It has a title and renders exactly one object, that can be
	either a Component or a MdiObjectResponsibilities object that
	can serve as delegate for the life cycle calls of MdiFrame
	(open, activate, close).
*/

public interface MdiFrame
{
	/** Returns the Object this container is rendering. This is for identification of MdiContainer in other MdiPane. */
	public Object getRenderedObject();

	/** Sets the rendered object. */
	public void setRenderedObject(Object renderedObject);

	/** Returns the String from titlebar. */
	public String getTitle();
	
	/** Sets a String to the titlebar. */
	public void setTitle(String title);

	/** Returns the MdiObjectResponsibilities if it was passed as Object to render. */
	public MdiObjectResponsibilities getManager();
	

	/** The contained editor was activated (gone to foreground). */
	public void activated();

	/** Close the contained editor. @exception PropertyVetoException when save failed or was canceled. */
	public void closing(PropertyChangeEvent e) throws PropertyVetoException;

	/** Returns the rendering component of this MDI-frame. */
	public Component getRenderingComponent();


	/**
		This is needed internally when switching between desktop view and tabbed view at runtime.
		Sets the Component that renders the object of interest.
	*/
	public void setExistingRenderingComponent(Component internalPanel);

	/**
		This is needed internally when switching between desktop view and tabbed view at runtime.
		Removes and returns the Component that renders the object of interest.
	*/
	public Component removeExistingRenderingComponent();
	
}
