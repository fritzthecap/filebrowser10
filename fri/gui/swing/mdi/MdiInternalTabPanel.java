package fri.gui.swing.mdi;

import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;
import java.awt.Component;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;

/**
	JPanel implementing MdiFrame to provide swapability to a TabbedPane.
*/

public class MdiInternalTabPanel extends JPanel
	implements MdiFrame
{
	private Object renderedObject;
	private String title;
	private MdiObjectResponsibilities manager;
	
	
	/** Create an internal frame. */
	public MdiInternalTabPanel(Object renderedObject)	{
		super(new BorderLayout());
		
		if (renderedObject instanceof MdiObjectResponsibilities)	{
			manager = (MdiObjectResponsibilities)renderedObject;
			setExistingRenderingComponent(manager.getRenderingComponent(this));
			renderedObject = manager.getRenderedObject();
		}
		// else: must override this class and implement rendering Component

		setRenderedObject(renderedObject);
	}

	/**
		The contained editor was activated (gone to foreground).
		Calls manager.activated(component) when manager was passed in constructor, else does nothing.
	*/
	public void activated()	{
		if (manager != null)	{
			manager.activated(this);
		}
	}
	
	/**
		The internal panel gets closed.
		Does nothing than call <i>manager.close(component, e)</i> when manager was passed in constructor.
	*/
	public void closing(PropertyChangeEvent e)
		throws PropertyVetoException
	{
		if (manager != null)	{
			manager.closing(this, e);
		}
	}
	

	/** Returns the MdiObjectResponsibilities if it was passed as Object to render. */
	public MdiObjectResponsibilities getManager()	{
		return manager;
	}
	

	/** Implements MdiFrame: sets the rendered object. */
	public void setRenderedObject(Object renderedObject)	{
		if (manager != null)	{
			manager.setRenderedObject(renderedObject);
			setTitle(manager.getTitle(this));
			setToolTipText(manager.getToolTip(this));
			
			// need to repaint tabbed pane
			if (getParent() != null)	{
				((JComponent)getParent()).revalidate();
				((JComponent)getParent()).repaint();
			}
		}
		else	{
			this.renderedObject = renderedObject;
		}
	}

	/** Implements MdiFrame: returns the rendered object. */
	public Object getRenderedObject()	{
		if (manager != null)
			return manager.getRenderedObject();
		return renderedObject;
	}


	/** Implements MdiFrame: sets the title label. */
	public void setTitle(String title)	{
		this.title = title;
	}

	/** Implements MdiFrame: returns the title label. */
	public String getTitle()	{
		return title;
	}


	/** Returns the Component at position 0. */
	public Component getRenderingComponent()	{
		return getComponent(0);
	}
	

	/**
		Implements MdiFrame: 
		Sets the Component that renders the object of interest.
	*/
	public void setExistingRenderingComponent(Component renderingComponent)	{
		removeAll();
		add(renderingComponent);

		if (getComponentCount() != 1)	{
			throw new IllegalStateException("InternalTabPanel must contain exactly one Component to be swapable, having "+getComponentCount());
		}
	}

	/**
		Implements MdiFrame: 
		Removes and returns the Component at position 0.
	*/
	public Component removeExistingRenderingComponent()	{
		if (getComponentCount() != 1)	{
			throw new IllegalStateException("InternalTabPanel must contain exactly one Component to be swapable, having "+getComponentCount());
		}
		
		Component c = getRenderingComponent();
		removeAll();
		return c;
	}

}