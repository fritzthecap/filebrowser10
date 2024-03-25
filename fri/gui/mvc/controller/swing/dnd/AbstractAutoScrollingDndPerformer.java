package fri.gui.mvc.controller.swing.dnd;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

/**
	A Drag&Drop Listener, that scrolls its drop-target Component if necessary.
  Implements startAutoscrolling() and stopAutoscrolling() of interface DndPerformer,
  starts a timer to scroll the drop target Component.

	@author  Ritzberger Fritz
*/

public abstract class AbstractAutoScrollingDndPerformer extends AbstractDndPerformer implements
	ActionListener
{
	// drag and drop autoscroll variables
	private final static int AUTOSCROLL_PERCENT = 6;
	private Point currPoint;
	private Timer timer = null;
	private JViewport port;
	private int incX, incY;
	private int direction;	// scroll direction
	private int interval = 60;	// scroll time interval
	private int percent = AUTOSCROLL_PERCENT;		// scroll space = percent of viewport height


	/**
		Create a abstract DND listener that scrolls its Component.
		@param sensor Component to watch
		@param scrollPane ScrollPane the tree lies in, for autoscrolling
	*/
	public AbstractAutoScrollingDndPerformer(
		Component sensor,
		JScrollPane scrollPane)
	{
		super(sensor);
		this.port = scrollPane.getViewport();
		timer = new Timer(interval, this);	// for autoscrolling
	}

	public void release()	{
		super.release();
		port = null;
		stopAutoscrolling();
		timer = null;
	}

	/** Implements DndPerformer: memorizes the passed point to perform autoscrollling when necessary. */
	public boolean dragOver(Point p)	{
		currPoint = p;
		return super.dragOver(p);
	}

	

	// autoscroll view

	/** Implements DndPerformer to autoscroll the view. */
	public void startAutoscrolling()	{
		//System.err.println("startAutoscrolling at "+currPoint);
		Rectangle vr = port.getViewRect();
		//System.err.println("view rect     "+vr);
		Point pos = port.getViewPosition();
		if (pos == null)	// JDK 1.3 java.lang.NullPointerException
			return;
		
		currPoint.x -= pos.x;
		currPoint.y -= pos.y;
		//System.err.println("relative           at "+currPoint);
		
		incX = vr.width  * percent / 100;
		incY = vr.height * percent / 100;
		
		direction = 0;

		if (currPoint.y >= vr.height - 5)
			direction = 4;	// down
		else
		if (currPoint.y < 5)
			direction = 2;	// up
		else
		if (currPoint.x > vr.width - 5)
			direction = 1;	// right
		else
		if (currPoint.x < 5)
			direction = 3;	// left

		//System.err.println("direction = "+direction);
		timer.start();
	}


	/** Implements DndPerformer to stop the timer. */
	public void stopAutoscrolling()	{
		//System.err.println("stopAutoscrolling");
		timer.stop();
	}

	// end interface DndPerformer


	// interface ActionListener
	
	/** Implements ActionListener to catch the timer event when autoscrolling. */
	public void actionPerformed(ActionEvent e)	{
		//System.err.println("timer scrolls ...");
		Rectangle vr = port.getViewRect();
		Rectangle tb = sensor.getBounds();
		Point pos = port.getViewPosition();
		//System.err.println("  position        "+pos);
		//System.err.println("  view rect       "+vr);
				
		if (direction == 1)	{	// right
			int diff = tb.width - vr.width;
			if (pos.x >= diff)
				return;
			pos.x += Math.min(incX, diff);
		}
		else
		if (direction == 2)	{	// up
			if (pos.y <= 0)
				return;
			int diff = tb.height - vr.height;
			pos.y -= Math.min(incY, diff);
		}
		else
		if (direction == 3)	{	// left
			if (pos.x <= 0)
				return;
			int diff = tb.width - vr.width;
			pos.x -= Math.min(incX, diff);
		}
		else
		if (direction == 4)	{	// down
			int diff = tb.height - vr.height;
			if (pos.y >= diff)
				return;
			pos.y += Math.min(incY, diff);
		}
		else
			return;	// to fast dragged

		//System.err.println("setViewPosition "+pos);
		port.setViewPosition(pos);
	}

}