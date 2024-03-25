package fri.gui.swing.desktoppane;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;

/**
	InternalFrame holding its radiomenuitem (window choice).
*/

public class ManagedInternalFrame extends JInternalFrame
{
	private JMenuItem windowMenuItem;
	private Object renderedObject;


	/** Create an internal frame. */
	public ManagedInternalFrame()	{
		this(null);
	}
	

	/** Create an internal frame. */
	public ManagedInternalFrame(Object renderedObject)	{
		super("", true, true, true, true);
		setRenderedObject(renderedObject);
	}
	

	/** Set the menuitem in Window Menu that represents this internal frame. */
	public void setWindowMenuItem(JMenuItem windowMenuItem)	{
		this.windowMenuItem = windowMenuItem;
	}

	/** Return menuitem in Window Menu that represents this internal frame. */
	public JMenuItem getWindowMenuItem()	{
		return windowMenuItem;
	}


	/** Returns the object this frame is rendering. */
	public Object getRenderedObject()	{
		return renderedObject;
	}

	/** Sets the object this frame is rendering. */
	public void setRenderedObject(Object renderedObject)	{
		this.renderedObject = renderedObject;
	}

	/** Overridden to keep consistent radio menu item with title. */
	public void setTitle(String title)	{
		super.setTitle(title);

		if (windowMenuItem != null)
			windowMenuItem.setText(title);
	}
	
}
