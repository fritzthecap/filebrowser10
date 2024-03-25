package fri.gui.swing.toolbar;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Helper class that listens to mouse exit to show a hidden toolbar,
 * listens to mouse enter and move to hide toolbar,
 * listens to mouse dragging to NOT show toolbar on such events.
 * This listener installs itself to the passed component and all its
 * descendants, it then listens to ContainerEvents and installs itself
 * on every dynamically added component.
 * 
 * @author Fritz Ritzberger
 */

public class MouseKnockAppearanceTrigger extends DefaultMouseHandler implements
	ContainerListener,
	AppearanceTrigger
{
	private HiddenToolbar toolbar;
	private boolean appearEvenOnMouseDrag;
	private int mouseIdleMillis;

	public MouseKnockAppearanceTrigger(HiddenToolbar toolbar) {
		setHiddenToolbar(toolbar);
	}

	/** Sets the toolbar for this action trigger. */
	public void setHiddenToolbar(HiddenToolbar toolbar)	{
		if (toolbar != null && toolbar.getAppearanceTrigger() != this)
			toolbar.setAppearanceTrigger(this);
		this.toolbar = toolbar;
	}
	
	/** Sets the passed timeout in millis for automatically hiding the toolbar. */
	public void setHideToolbarTimeout(int mouseIdleMillis)	{
		this.mouseIdleMillis = mouseIdleMillis;
	}
	
	/** When this is set to true, mouse dragging is interpreted as knock gesture. */
	public void setAppearEvenOnMouseDrag(boolean appearEvenOnMouseDrag)	{
		this.appearEvenOnMouseDrag = appearEvenOnMouseDrag;
	}

	// determine if passed point is knocking at parent component
	private boolean knocking(JComponent sensor, Point p)	{
		if (sensor != toolbar.getParentComponent())	// convert point if source is over parent component
			p = SwingUtilities.convertPoint(sensor, p, toolbar.getParentComponent());
			
		Rectangle r = toolbar.getParentComponent().getVisibleRect();	// could be scrolled
		int alignment = toolbar.getToolbarAlignment();
		
		if (alignment == SwingConstants.LEFT)
			return p.x <= r.x && p.x >= r.x - 5;
			
		if (alignment == SwingConstants.RIGHT)
			return p.x >= r.x + r.width && p.x <= r.x + r.width + 5;
			
		if (alignment == SwingConstants.TOP)
			return p.y <= r.y && p.y >= r.y - 5;
			
		if (alignment == SwingConstants.BOTTOM)
			return p.y >= r.y + r.height && p.y <= r.y + r.height + 5;
			
		return false;	// will not be reached
	}
	
	// MouseListener
	public void mouseExited(MouseEvent e) {
		if ((dragging == false || appearEvenOnMouseDrag == true) &&	// do not appear on mouse dragging, this bothers
				toolbar.isVisible() == false &&	// appear only when not visible
				knocking((JComponent) e.getComponent(), e.getPoint()))	// is in specified knock region
		{
			toolbar.appear();
			new HideToolbarTimeout(toolbar, mouseIdleMillis);
		}
	}
	
	// MouseMotionListener
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
		if (toolbar.isVisible())	{	//  && knocking((JComponent) e.getComponent(), e.getPoint()) == false
			toolbar.disappear();
		}
	}
	
	// MouseMotionListener
	public void mouseDragged(MouseEvent e) {
		if (appearEvenOnMouseDrag == false)
			super.mouseDragged(e);
	}
	
	// ContainerListener
	public void componentAdded(ContainerEvent e) {
		//System.err.println("componentAdded "+e.getChild());
		if (e.getChild() instanceof JComponent)
			install((JComponent) e.getChild());
	}
	public void componentRemoved(ContainerEvent e) {
		//System.err.println("componentRemoved "+e.getChild());
		if (e.getChild() instanceof JComponent)
			deinstall((JComponent) e.getChild());
	}


	/** Overridden to NOT install this to toolbar (could be a contained Component). */
	protected boolean shouldListenToComponent(JComponent c)	{
		return c != toolbar;
	}
	
	/** Overridden to additionally install this as ContainerListener. */
	protected void installListeners(JComponent sensor, boolean add)	{
		super.installListeners(sensor, add);	// install mouse listeners
		
		if (add)	// install ContainerListener
			sensor.addContainerListener(MouseKnockAppearanceTrigger.this);
		else
			sensor.removeContainerListener(MouseKnockAppearanceTrigger.this);
	}

}
