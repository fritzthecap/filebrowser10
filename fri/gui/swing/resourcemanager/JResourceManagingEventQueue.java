package fri.gui.swing.resourcemanager;

import java.awt.*;
import javax.swing.JOptionPane;
import fri.gui.awt.resourcemanager.ResourceManager;
import fri.gui.awt.resourcemanager.ResourceManagingEventQueue;

/**
	An EventQueue that installs a Swing ResourceManager to every opening window.
	Install the queue by:
	<pre>
		JResourceManagingEventQueue.install (true);	// true: exit on last window close
	</pre>
	The ResourceManager can be used statically by the parent Window as action listener
	to show a component chooser dialog:
	<pre>
		JResourceManagingEventQueue.showDialog (parentWindow);
	</pre>
	Additional popup menus can be installed by:
	<pre>
		JResourceManagingEventQueue.addComponents (parentWindow, new Object [] { popup1, popup2 });
	</pre>
*/
public class JResourceManagingEventQueue extends ResourceManagingEventQueue
{
	protected JResourceManagingEventQueue(boolean exitOnLastClose)	{
		super(exitOnLastClose);
	}
	
	/** Allocates a Swing JResourceManager. */
	protected ResourceManager createResourceManager(Window w)	{
		return new JResourceManager(w);
	}

	/** Shows a Swing dialog that warns Out-Of-Memory. */
	protected void warnOutOfMemory(OutOfMemoryError oome)	{
		JOptionPane.showMessageDialog(foregroundWindow, "Out Of Memory!", "Fatal Error", JOptionPane.ERROR_MESSAGE);
	}
	
	
	/**
		Activate an event queue that installs a JResourceManager on every opening window.
		This method does nothing if called for a second time. This queue will not exit
		when last frame closes.
	*/
	public static void install()	{
		install(false);
	}

	/**
		Activate an event queue that installs a JResourceManager on every opening window.
		This method does nothing if called for a second time.
		Do not set <i>exitOnLastClose</i> true when using GeometryManager, as exit would be performed
		before window listener can save the geometry state to persistence.
		@param exitOnLastClose if true, <i>System.exit(0)</i> will be called on last closing window.
	*/
	public static void install(boolean exitOnLastClose)	{
		if (resourceManagingQueue == null)	{
			System.err.println("Installing ResourceManagingEventQueue ...");
			resourceManagingQueue = new JResourceManagingEventQueue(exitOnLastClose);
			Toolkit.getDefaultToolkit().getSystemEventQueue().push(resourceManagingQueue);
		}
	}

}
