package fri.gui.swing.toolbar;

import java.util.ArrayList;
import java.awt.Component;
import java.awt.event.*;
import javax.swing.JComponent;

/**
 * Basic mouse handler that combines MouseListener and MouseMotionListener.
 * Contains methods to install this listener recursively to a Component
 * and all its children.
 * 
 * @author Fritz Ritzberger
 * 
 * TODO: this should reside in an event-package.
 */
public class DefaultMouseHandler extends MouseAdapter implements
	MouseMotionListener
{
	protected boolean dragging;
	private ArrayList sensors = new ArrayList();

	// MouseMotionListener
	public void mouseDragged(MouseEvent e) {
		dragging = true;
	}
	public void mouseMoved(MouseEvent e) {
		dragging = false;
	}
	
	/** Installs this listener on passed component and all its descendants. */
	public void install(JComponent sensor)	{
		if (sensors.contains(sensor) == false)	{	// do not install two times
			sensors.add(sensor);
			installLoop(sensor, true);
		}
	}
	
	/** Deinstalls this listener from passed component and all its descendants. */
	public void deinstall(JComponent sensor)	{
		sensors.remove(sensor);
		installLoop(sensor, false);
	}

	private void installLoop(JComponent sensor, boolean add)	{
		if (shouldListenToComponent(sensor) == false)
			return;	// do not listen to toolbar mouse moves as it would close the toolbar
			
		installListeners(sensor, add);
		
		Component [] children = sensor.getComponents();	// install on all children
		for (int i = 0; i < children.length; i++)
			if (children[i] instanceof JComponent)
				installLoop((JComponent) children[i], add);
	}
	
	/** To be overridden by subclasses that want to filter the install loop. */
	protected boolean shouldListenToComponent(JComponent c)	{
		return true;
	}
	
	/** To be overridden by subclasses that want to install other than MouseListener and MouseMotionListener. */
	protected void installListeners(JComponent sensor, boolean add)	{
		if (add)	{
			sensor.addMouseListener(DefaultMouseHandler.this);
			sensor.addMouseMotionListener(DefaultMouseHandler.this);
		}
		else	{
			sensor.removeMouseListener(DefaultMouseHandler.this);
			sensor.removeMouseMotionListener(DefaultMouseHandler.this);
		}
	}

}
