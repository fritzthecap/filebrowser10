package fri.gui.awt.geometrymanager;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import fri.gui.LocationUtil;
import fri.util.collections.AggregatingHashtable;
import fri.gui.awt.component.WindowTypeName;

public abstract class AbstractGeometryManager extends WindowAdapter implements
	ComponentListener
{
	public static final int CASCADING = 1;
	public static final int TILING = 2;
	private static AggregatingHashtable windowTypeGeometries = new AggregatingHashtable();
	private static final Hashtable templateCache = new Hashtable();
	private Window window;
	private int style;
	private boolean doSize;
	private boolean isPacked;
	
	
	/** Creates a GeometryManager that cascades windows. */
	public AbstractGeometryManager(Window window)	{
		this(window, CASCADING);
	}
	
	/** Creates a GeometryManager that cascades windows and optionally does no sizing. */
	public AbstractGeometryManager(Window window, boolean doSize)	{
		this(window, CASCADING, doSize);
	}
	
	/** Creates a GeometryManager that cascades or tiles windows. @param style one of CASCADING or TILING. */
	public AbstractGeometryManager(Window window, int style)	{
		this(window, style, true);
	}
	
	private AbstractGeometryManager(Window window, int style, boolean doSize)	{
		this.window = window;
		this.style = style;
		this.doSize = doSize;
	}
	
	private void listen()	{
		window.addWindowListener(this);
		window.addComponentListener(this);
	}
	
	private WindowGeometry getWindowGeometry()	{
		String key = WindowTypeName.windowTypeName(window);
		List list = (List) windowTypeGeometries.get(key);
		WindowGeometry geometry = list != null ? findWindowGeometry(list) : null;
		
		if (geometry == null)	{	// is first or new one of this window type
			geometry = newWindowGeometry(key);	// gets the template values
			if (list != null && list.size() > 0)	{	// others are present, set cascaded or tiled geometry to new window
				WindowGeometry wtg = (WindowGeometry) list.get(list.size() - 1);	// get last element
				geometry.setPoint(style == CASCADING ? wtg.getNextCascadingPoint() : wtg.getNextTilingPoint());
				geometry.setDimension(wtg.getDimension());
			}
			windowTypeGeometries.put(key, geometry);
		}

		return geometry;
	}

	private WindowGeometry findWindowGeometry(List list)	{
		for (int i = 0; i < list.size(); i++)	{
			WindowGeometry wtg = (WindowGeometry) list.get(i);
			if (wtg.getWindowHashCode() == window.hashCode())
				return wtg;
		}
		return null;
	}

	private WindowGeometry newWindowGeometry(String key)	{
		WindowGeometry geometry = new WindowGeometry(window.hashCode());
		WindowGeometry template = (WindowGeometry) templateCache.get(key);
		if (template != null)	{
			geometry.setPoint(template.getPoint());
			geometry.setDimension(template.getDimension());
		}
		else	{	// must load from persistence
			load(key, geometry);
			templateCache.put(key, geometry);
		}
		return geometry;
	}
	
	/** Creates a new instance of WindowGeometry. To be overridden by an persistence providing class. */
	protected abstract void load(String key, WindowGeometry geometry);
	
	
	/**
		Sets persistent size and location to the window. Installs window-close and component listeners.
		<p>
		As a WindowListener could call System.exit(), the GeometryManager must be
		added before it as WindowListener, as it must save its geometry to persistence
		before the application exits. Therefore a pack() call is provided here.
		The show() call would do a pack if it was not already done.
	*/
	public void pack()	{
		setLocation();
		
		if (window.getSize() == null || window.getSize().equals(new Dimension(0, 0)))
			window.pack();
			
		if (doSize)
			setSize();
			
		listen();
		isPacked = true;
	}
	
	/**
		Makes the window visible on screen, sets its persistent size and location,
		cascaded or tiled as given in the constructor.
	*/
	public void show()	{
		if (isPacked == false)
			pack();
			
		if (window instanceof Frame)
			((Frame)window).setVisible(true);
		else
		if (window instanceof Dialog)
			((Dialog)window).setVisible(true);
		else
			window.show();
	}
	
	private void setLocation()	{
		WindowGeometry geometry = getWindowGeometry();
		Point p = geometry.getPoint();
		if (p != null)
			window.setLocation(p);
		else
			LocationUtil.centerOverParent(window, (Window) window.getParent());
	}
	
	private void setSize()	{
		WindowGeometry geometry = getWindowGeometry();
		Dimension d = geometry.getDimension();
		if (d != null)    {	// persistent size present
			window.setSize(d);
            //window.pack();    // causes bottom part to be invalid on Java 1.4
		}
		else  {	// avoid being bigger than screen
			LocationUtil.ensureSizeWithinScreen(window);
		}
	}
	
	
	private void close()	{
		if (window != null)	{
			String key = WindowTypeName.windowTypeName(window);
			saveTemplate(key, getWindowGeometry());

			window.removeWindowListener(this);
			window.removeComponentListener(this);

			// remove the geomety of this window instance
			List list = (List) windowTypeGeometries.get(key);
			WindowGeometry geometry = findWindowGeometry(list);
			list.remove(geometry);

			window = null;
		}
	}

	private void saveTemplate(String key, WindowGeometry geometry)	{
		templateCache.put(key, geometry);
		save(key, geometry);
	}

	/** Saves passed geometry. To be overridden by an persistence providing class. */
	protected abstract void save(String key, WindowGeometry windowGeometry);



	// interface ComponentListener

	public void componentResized(ComponentEvent e)	{
		Dimension size = window.getSize();
		if (size != null && size.width > 0 && size.height > 0)	{
			WindowGeometry geometry = getWindowGeometry();
			geometry.setDimension(size);
		}
		componentMoved(e);
	}
	public void componentMoved(ComponentEvent e)	{
		Point location = window.getLocation();
		if (location != null && location.x >= 0 && location.y >= 0)	{
			WindowGeometry geometry = getWindowGeometry();
			geometry.setPoint(location);
		}
	}
	public void componentShown(ComponentEvent e)	{}
	public void componentHidden(ComponentEvent e)	{}


	// interface WindowListener

	public void windowClosing(WindowEvent e)	{
		close();
	}
	public void windowClosed(WindowEvent e) {
		close();
	}

}
