package fri.gui;

import java.awt.*;
import java.awt.event.*;
import fri.util.application.Application;

/**
	SecurityManager that adds GUI logic to fri.util.application.SystemExitSecurityManager.
	This class pushes a new EventQueue to Toolkit.getDefaultToolkit().getSystemEventQueue()
	to watch focus events. When a System.exit() call is caught, the focused frame is disposed
	when there are still applications in Application-list, as the Frame that calls exit
	might not have been registered there.
*/

public class SystemExitSecurityManager extends fri.util.application.SystemExitSecurityManager
{
	private Window currentWindow = null;

	protected class MyEventQueue extends EventQueue
	{
		protected void dispatchEvent(AWTEvent e)	{
			if (e instanceof FocusEvent)	{
				FocusEvent fe = (FocusEvent)e;
				
				if (fe.getSource() instanceof Window)	{
					if (fe.getID() == FocusEvent.FOCUS_GAINED)	{
						System.err.println("SystemExitSecurityManager, "+e);
						//if (currWindow == null)
						currentWindow = (Window)e.getSource();
					}
				}
			}
			super.dispatchEvent(e);
		}
	}


	/**
		Constructor that calls initSystemExitSecurityManager()
		to push System-EventQueue.
	*/
	public SystemExitSecurityManager()	{
		initSystemExitSecurityManager();
	}
	
	/**
		Pushes a new EventQueue that delegates to super.
		This is done to listen for FocusEvents an Frames,
		as the Frame in foreground will be disposed when
		System.exit() occurs.
	*/
	protected void initSystemExitSecurityManager()	{
		EventQueue myQueue = new MyEventQueue();
		Toolkit.getDefaultToolkit().getSystemEventQueue().push(myQueue);
	}
	
	
	/**
		Overridden to dispose some frame that called System.exit().
		This method gets called before this SecurityManager throws
		a SecurityException because Application.instances() is not zero.
	*/
	protected void exitCalled()	{
		System.err.println("Window focused when System.exit() was caught is: "+
				(currentWindow != null ? currentWindow.getClass().getName() : "null")+
				", isShowing: "+(currentWindow != null && currentWindow.isShowing()));
				
		if (currentWindow != null && currentWindow.isShowing())	{
			if (Application.indexOf(currentWindow) < 0)	// this must be a bad application, close it
				currentWindow.dispose();
		}
	}
	
}