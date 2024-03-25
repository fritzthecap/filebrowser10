package fri.gui.awt.geometrymanager;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

public class WindowGeometry
{
	private static int titlebarHeight = 25;
	private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private static int cascadeX = titlebarHeight, cascadeY = titlebarHeight;
	private int x = -1, y = -1, width = -1, height = -1;
	private int windowHashCode;
	
	public WindowGeometry(int windowHashCode)	{
		this.windowHashCode = windowHashCode;
	}

	public int getWindowHashCode()	{
		return windowHashCode;
	}
	
	/** Returns the next cascading point seen from this object, without changing its own coordinates. */
	public Point getNextCascadingPoint()	{
		if (x >= 0 && y >= 0)	{
			int x = this.x + cascadeX;
			int y = this.y + cascadeY;
			return ensureWithinScreen(new Point(x, y));
		}
		return null;
	}	

	/** Returns the next tiling point seen from this object, without changing its own coordinates. */
	public Point getNextTilingPoint()	{
		if (x >= 0 && y >= 0)	{
			int x = this.x + width;	// go to right
			int y = this.y;	// stay on same level
			
			if (x > screenSize.width - width)	{	// if out of screen, go down
				x = 0;	// go left
				y = this.y + height;	// go down
				
				if (y > screenSize.height - height)	{	// if out of screen, start from upper left corner
					x = 0;
					y = 0;
				}
			}
			return ensureWithinScreen(new Point(x, y));
		}
		return null;
	}	

	private Point ensureWithinScreen(Point p)	{
		if (p.x > screenSize.width - width || p.x < 0)
			p.x = 0;
		if (p.y > screenSize.height - height || p.y < 0)
			p.y = 0;
		return p;
	}
	
	
	public Dimension getDimension()	{
		if (height > 0 && width > 0)
			return new Dimension(Math.min(width, screenSize.width), Math.min(height, screenSize.height));
		return null;
	}	

	public Point getPoint()	{
		if (x >= 0 && y >= 0)
			return new Point(x, y);
		return null;
	}	

	public void setDimension(Dimension d)	{
		if (d == null)
			return;
		width = d.width;
		height = d.height;
	}	

	public void setPoint(Point p)	{
		if (p == null)
			return;
		x = p.x;
		y = p.y;
	}	

}
