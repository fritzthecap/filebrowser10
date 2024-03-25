package fri.gui.swing.toolbar;

import java.awt.event.*;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Receives action events from a timer to close the toolbar
 * when the mouse is not within and the last mouse move event
 * is older than a given timeout.
 * 
 * @author Fritz Ritzberger
 */
class HideToolbarTimeout extends DefaultMouseHandler implements
	ActionListener
{
	private int mouseIdleMillis;
	private Timer timer;
	private HiddenToolbar toolbar;
	private long lastMove = System.currentTimeMillis();
	private boolean mouseOverToolbar;
	
	HideToolbarTimeout(HiddenToolbar toolbar, int mouseIdleMillis)	{
		if (mouseIdleMillis < 0)
			return;	// do nothing when no timeout was passed
			
		this.toolbar = toolbar;
		this.mouseIdleMillis = (mouseIdleMillis == 0) ? 1500 : mouseIdleMillis;

		install((JComponent) toolbar);
		
		timer = new Timer(Math.min(500, mouseIdleMillis), this);
		timer.start();
	}

	// MouseListener
	public void mouseExited(MouseEvent e) {
		mouseOverToolbar = false;
	}
	
	// MouseListener
	public void mouseEntered(MouseEvent e) {
		mouseOverToolbar = true;
	}

	// MouseMotionListener
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
		lastMove = System.currentTimeMillis();
		mouseOverToolbar = true;
	}
	
	// ActionListener to listen to timer events
	public void actionPerformed(ActionEvent e) {
		if (mouseOverToolbar == false && System.currentTimeMillis() - lastMove >= mouseIdleMillis)	{
			finish();
			toolbar.disappear();
		}
		else
		if (toolbar.isVisible() == false)	{
			finish();
		}
	}
	
	private void finish()	{
		timer.stop();
		deinstall((JComponent) toolbar);
	}

}
