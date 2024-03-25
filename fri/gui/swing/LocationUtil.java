package fri.gui.swing;

import java.awt.*;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;

/**
	Window location utilities, Swing dependent.

	@auhor Ritzberger Fritz
*/

public abstract class LocationUtil
{
	private static int titleBarHeight = -1;
	
	/** Returns the platform-specific height of the OS Window titlebar. */
	public static int getTitlebarHeight()	{
		if (titleBarHeight == -1)	{
	        JFrame f = new JFrame();
	        f.pack();
	        titleBarHeight = f.getRootPane().getLocation().y;
		}
		return titleBarHeight;
	}
	
	/**
		Locate the window under the launcher component.
		@param window the window to center
		@param parent its parent window
		@param launcher the textfield, button, ... that opens the window (dialog)
	*/
	public static void locateUnderLauncher(Window window, Window parent, Component launcher)	{
		if (parent == null)
			parent = SwingUtilities.getWindowAncestor(launcher);

		Point p = launcher.getLocation();
		p = SwingUtilities.convertPoint(launcher.getParent(), p, parent);
		p.y += launcher.getHeight();
		
		Dimension d = window.getSize();
		if (d == null || d.equals(new Dimension(0, 0)))
			d = window.getPreferredSize();	// child size

		Point parentLoc = parent.getLocation();

		p.x += parentLoc.x;
		p.y += parentLoc.y;
		
		window.setLocation(p);
		// fri.gui.LocationUtil.ensureLocationWithinScreen(window);	// does not fit into multi monitor environment
	}

	/*
	 * According to GraphicsEnvironment JavaDoc a virtual env is one where at least one
	 * GraphicsConfiguration object hat other origin coordinates than 0/0.
	 */
	public static boolean isVirtualDeviceEnvironment()	{
		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice [] monitors = environment.getScreenDevices();
		if (monitors != null)	{
			// because the primary monitor likely will have 0/0 and is the first, start testing bounds from end
			for (int i = monitors.length - 1; i >= 0; i--)	{
				GraphicsConfiguration [] configurations = monitors[i].getConfigurations();
				for (int j = 0; configurations != null && j < configurations.length; j++)	{
					Rectangle r = configurations[j].getBounds();
					if (r.x != 0 || r.y != 0)
						return true;
				}
			}
		}
		return false;
	}

}