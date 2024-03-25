package fri.gui;

import java.awt.*;

/**
	Window location utilities, Swing independent.

	@auhor Ritzberger Fritz
*/

public abstract class LocationUtil
{
	/**
		Center window over its parent, ensuring that its origin is not negative.
		@param window the window to center
		@param parent its parent window, or null to center on screen
	*/
	public static void centerOverParent(Window window, Window parent)	{
		Dimension d = window.getSize();	// child size
		if (d == null || d.equals(new Dimension(0, 0)))
			d = window.getPreferredSize();	// child size

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		Dimension parentSize = parent != null ? parent.getSize() : screenSize;	// parent size
		parentSize.height -= parent != null ? 25 : 0;	// titlebar of parent

		Point parentLoc = parent != null ? parent.getLocation() : new Point(0, 0);
		parentLoc.y += parent != null ? 25 : 0;	// parents titlebar height

		Point p = new Point(
				Math.max(0, parentLoc.x + (parentSize.width - d.width) / 2),
				Math.max(0, parentLoc.y + (parentSize.height - d.height) / 2));
		
		window.setLocation(p);
	}


	/**
	 * Ensure that the window is not bigger than screen, calculated from its
	 * current location. The setSize() method gets called.
	 */
	public static void ensureSizeWithinScreen(Window window)	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d = window.getSize();	// predefined size by some setSize() call
		if (d == null || d.equals(new Dimension(0, 0)))
			d = window.getPreferredSize();	// size defined by contained components

		Point p = window.getLocation();
		boolean setSize = false;
		if (p.x + d.width > screenSize.width)	{
			setSize = true;
			d.width = screenSize.width - p.x;
		}
		if (p.y + d.height > screenSize.height)	{
			setSize = true;
			d.height = screenSize.height - p.y;
		}

		if (setSize)
			window.setSize(d.width, d.height);
	}
	
	
	/**
	 * Ensure that the window is located within screen, calculated from its
	 * current location. The setLocation() method gets called.
	 */
	public static void ensureLocationWithinScreen(Window window)	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d = window.getSize();	// predefined size by some setSize() call
		if (d == null || d.equals(new Dimension(0, 0)))
			d = window.getPreferredSize();	// size defined by contained components

		Point p = window.getLocation();
		boolean setLocation = false;
		if (p.x + d.width > screenSize.width)	{
			setLocation = true;
			p.x = Math.max(0, screenSize.width - d.width);
		}
		if (p.y + d.height > screenSize.height)	{
			setLocation = true;
			p.y = Math.max(0, screenSize.height - d.height);
		}

		if (setLocation)
			window.setLocation(p.x, p.y);
	}
	
	
	/** Locate the window to minimal overlapping with its parent. */
	public static void setFreeViewLocation(Window window, Window parent)	{
		Dimension d = window.getSize();
		
		if (d.width > 0 && d.height > 0)	{
			// find a good place for search window
			Dimension screenSize = 	Toolkit.getDefaultToolkit().getScreenSize();
			Dimension parentSize = parent.getSize();
			parentSize.height -= 25;	// titlebar height
			Point parentLoc = parent.getLocation();
			parentLoc.y += 25;	// titlebar height
			
			// calculate free place on all four sides
			int left = parentLoc.x;
			int right = screenSize.width - (parentLoc.x + parentSize.width);
			int upper = parentLoc.y;
			int lower = screenSize.height - (parentLoc.y + parentSize.height);
			
			if (left < 40 && right < 40 && upper < 40 && lower < 40)
				return;	// seems to be maximized
			
			// calculate maximum free place
			int horizMax = Math.max(left, right);
			int vertMax = Math.max(upper, lower);

			// decide if positioning to upper/lower or to left/right, by proportion
			float horizProportion = (float)horizMax / (float)d.width;
			float vertProportion = (float)vertMax / (float)d.height;
			boolean toHoriz = horizProportion > vertProportion; 
			
			// decide if positioning to upper or lower, to left or right
			boolean toLeft = toHoriz && left - 100 >= right;	// prefer right side as text is mostly on the left
			boolean toUpper = !toHoriz && upper >= lower;
			boolean toRight = toHoriz && left - 100 < right;
			boolean toLower = !toHoriz && upper < lower;
			//System.err.println("toLeft "+toLeft+", toRight "+toRight+", toUpper "+toUpper+", toLower "+toLower);
			
			Point loc = new Point();
			if (toRight)	{
				loc.x = parentLoc.x + parentSize.width;
				int max = screenSize.width - d.width; 
				if (loc.x > max)
					loc.x = max; 
				loc.y = parentLoc.y + (parentSize.height - d.height) / 2;
			}
			else
			if (toLeft)	{
				loc.x = left - d.width;
				if (loc.x < 0)
					loc.x = 0; 
				loc.y = parentLoc.y + (parentSize.height - d.height) / 2;
			}
			else
			if (toLower)	{
				loc.y = parentLoc.y + parentSize.height;
				int max = screenSize.height - d.height;
				if (loc.y > max)
					loc.y = max;
				loc.x = parentLoc.x + (parentSize.width - d.width) / 2;
			}
			else
			if (toUpper)	{
				loc.y = upper - d.height;
				if (loc.y < 0)
					loc.y = 0; 
				loc.x = parentLoc.x + (parentSize.width - d.width) / 2;
			}
			window.setLocation(loc);
		}
	}

}