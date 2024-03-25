package fri.gui.swing.mdi;

import java.awt.Component;
import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;
import fri.gui.awt.resourcemanager.ResourceIgnoringComponent;
import fri.gui.swing.desktoppane.ManagedInternalFrame;

/**
	InternalFrame implementing MdiFrame to provide swapability to a TabbedPane.
*/

public class MdiInternalFrame extends ManagedInternalFrame implements
	MdiFrame,
	ResourceIgnoringComponent	// do not let customize titles
{
	private MdiObjectResponsibilities manager;
	

	/** Create an internal frame. Use an optional instanceof MdiObjectResponsibilities to populate the panel. */
	public MdiInternalFrame(Object renderedObject)	{
		super(renderedObject);
		
		if (renderedObject instanceof MdiObjectResponsibilities)	{
			manager = (MdiObjectResponsibilities)renderedObject;
			Component c = manager.getRenderingComponent(this);
			
			if (c != null)	{	// must have something to render
				setExistingRenderingComponent(c);
				setRenderedObject(manager.getRenderedObject());
			}
			else	{	// something went wrong
				manager = null;
				setRenderedObject(null);
			}
		}
		// else: must override this class and implement rendering Component
	}


	/** Returns the MdiObjectResponsibilities if it was passed as Object to render. */
	public MdiObjectResponsibilities getManager()	{
		return manager;
	}
	

	/**
		The contained editor was activated (gone to foreground).
		Does nothing than call manager.activated(component) when manager was passed in constructor.
	*/
	public void activated()	{
		if (manager != null)	{
			manager.activated(this);
		}
	}
	
	/**
		The internal frame gets closed.
		Does nothing than call <i>manager.closing(component, e)</i> when manager was passed in constructor.
	*/
	public void closing(PropertyChangeEvent e)
		throws PropertyVetoException
	{
		if (manager != null)	{
			manager.closing(this, e);
		}
	}

	
	/** Implements MdiFrame: sets the rendered object. */
	public void setRenderedObject(Object renderedObject)	{
		if (manager != null)	{
			manager.setRenderedObject(renderedObject);
			setTitle(manager.getTitle(this));
			setToolTipText(manager.getToolTip(this));
		}
		else	{
			super.setRenderedObject(renderedObject);
		}
	}

	/** Implements MdiFrame: returns the rendered object. */
	public Object getRenderedObject()	{
		if (manager != null)
			return manager.getRenderedObject();
		return super.getRenderedObject();
	}



	/** Returns the Component in content pane at position 0. */
	public Component getRenderingComponent()	{
		return getContentPane().getComponent(0);
	}
	


	/**
		Implements MdiFrame: 
		Sets the Component that renders the object of interest.
	*/
	public void setExistingRenderingComponent(Component renderingComponent)	{
		getContentPane().removeAll();
		getContentPane().add(renderingComponent);

		if (getContentPane().getComponentCount() != 1)	{
			throw new IllegalStateException("InternalFrame must contain exactly one Component to be swapable, having "+getComponentCount());
		}
	}

	/**
		Implements MdiFrame: 
		Removes and returns the Component at position 0.
	*/
	public Component removeExistingRenderingComponent()	{
		if (getContentPane().getComponentCount() != 1)	{
			throw new IllegalStateException("InternalFrame must contain exactly one Component to be swapable, having "+getComponentCount());
		}
		
		Component c = getRenderingComponent();
		getContentPane().removeAll();
		return c;
	}

}