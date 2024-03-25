package fri.gui.awt.resourcemanager;

import java.util.Hashtable;
import java.awt.*;
import java.awt.event.*;
import fri.gui.LocationUtil;
import fri.gui.awt.resourcemanager.ResourceManager;

/**
	An EventQueue that installs an AWT ResourceManager to every opening window.
	Install the queue by:
	<pre>
		ResourceManagingEventQueue.install (true);	// true: exit on last window close
	</pre>
	The ResourceManager can be used statically by the parent Window as action listener
	to show a component chooser dialog:
	<pre>
		ResourceManagingEventQueue.showDialog (parentWindow);
	</pre>
	Additional popup menus can be installed by:
	<pre>
		ResourceManagingEventQueue.addComponents (parentWindow, new Object [] { popup1, popup2 });
	</pre>
*/
public class ResourceManagingEventQueue extends EventQueue
{
	// static impl
	
	protected static ResourceManagingEventQueue resourceManagingQueue;
	
	/**
		Activate an event queue that installs a ResourceManager on every opening window.
		This method does nothing if called for a second time. This queue will not exit
		when last frame closes.
	*/
	public static void install()	{
		install(false);
	}
	
	/**
		Activate an event queue that installs a ResourceManager on every opening window.
		This method does nothing if called for a second time.
		Do not set <i>exitOnLastClose</i> true when using GeometryManager, as exit would be performed
		before window listener can save the geometry state to persistence.
		@param exitOnLastClose if true, <i>System.exit(0)</i> will be called on last closing window.
	*/
	public static void install(boolean exitOnLastClose)	{
		if (resourceManagingQueue == null)	{
			System.err.println("Installing ResourceManagingEventQueue ...");
			resourceManagingQueue = new ResourceManagingEventQueue(exitOnLastClose);
			Toolkit.getDefaultToolkit().getSystemEventQueue().push(resourceManagingQueue);
		}
	}
	
	/**
		Registers the passed popup menu with the ResourceManager that will be installed on the passed window.
		@param window frame or dialog on which the passed popup menu will be opened.
		@param component the popup menu that should be customizable by the ResourceManager of passed window
				(this could be any component, not only a popup menu).
	*/
	public static void addComponents(final Window window, final Object [] components)	{
		window.addWindowListener(new WindowAdapter()	{
			public void windowOpened(WindowEvent e)	{
				ResourceManager rm = ResourceManagingEventQueue.getResourceManager(window);
				if (rm != null)	{
					rm.addComponents(components);
				}
				else
					System.err.println("ERROR: Adding components: ResourceManagingEventQueue not installed!");
			}
		});
	}
	
	/**
		Returns a ResourceManager that was installed on a dialog or frame window.
		@param window frame or dialog for which the ResourceManager is needed (to add popup menus).
	*/
	private static ResourceManager getResourceManager(Window window)	{
		return resourceManagingQueue != null ? (ResourceManager) resourceManagingQueue.windows.get(window) : null;
	}
	
	/**
		Shows a dialog that lets customize all components contained in window.
		@param window frame or dialog that is the parent of opened dialog.
	*/
	public static void showDialog(Window window)	{
		ResourceManager rm = ResourceManagingEventQueue.getResourceManager(window);
		if (rm != null)
			rm.showDialogForAll(window);
		else
			System.err.println("ERROR: Showing chooser dialog: ResourceManagingEventQueue not installed!");
	}

	
	// instance impl

	protected Window foregroundWindow;
	private Hashtable windows = new Hashtable();
	private boolean exitOnLastClose;
	
	protected ResourceManagingEventQueue(boolean exitOnLastClose)	{
		this.exitOnLastClose = exitOnLastClose;
	}
	
	/** Overridden to install a ResourceManager to every opening window. */
	protected void dispatchEvent(AWTEvent e)	{
		try	{
			if (e instanceof WindowEvent && (e.getSource() instanceof Frame || e.getSource() instanceof Dialog))	{
				Window w = (Window) e.getSource();
				
				if (e.getID() == WindowEvent.WINDOW_ACTIVATED)	{
					foregroundWindow = w;
				}
				
				if (e.getID() == WindowEvent.WINDOW_OPENED && windows.get(w) == null)	{
					openWindow(w);
				}
				else
				if (e.getID() == WindowEvent.WINDOW_CLOSING || e.getID() == WindowEvent.WINDOW_CLOSED)	{
					closeWindow(w);
				} 
			}
			else
			// check for frames that were set invisible, as no window opened or activated will be fired on setting visible again (LINUX)
			if (e instanceof PaintEvent || e instanceof FocusEvent && e.getID() == FocusEvent.FOCUS_GAINED)	{
				Component parent = ((ComponentEvent) e).getComponent();
				while (parent instanceof Window == false && parent.getParent() != null && parent != parent.getParent())
					parent = parent.getParent();
					
				if ((parent instanceof Frame || parent instanceof Dialog) && windows.get(parent) == null)	{
					openWindow((Window) parent);
				}
			}

			super.dispatchEvent(e);
		}
		catch (OutOfMemoryError oome)	{
			// output error reason to console
			//oome.printStackTrace();	// does not have any stack information!
			
			// try to get back some memory
			System.gc();
			System.gc();
			
			// bring up a dialog
			warnOutOfMemory(oome);
			throw oome;	// draw the consequence
		}
	}

	/** Shows a AWT dialog that warns Out-Of-Memory. To be overridden by Swing. */
	protected void warnOutOfMemory(OutOfMemoryError oome)	{
		if (foregroundWindow == null)
			return;
		
		String title = "Out Of Memory";
		final Dialog dialog = (foregroundWindow instanceof Frame)
				? new Dialog((Frame) foregroundWindow, title, true)
				: new Dialog((Dialog) foregroundWindow, title, true);
		
		dialog.addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				dialog.dispose();
			}
		});
		
		dialog.add(new Label("Out Of Memory!"));
		dialog.pack();
		LocationUtil.centerOverParent(dialog, foregroundWindow);
		dialog.show();
	}
	
	private void openWindow(Window w)	{
		Object rm =  createResourceManager(w);
		windows.put(w, rm);
	}

	/** Creates a ResourceManager for the opened window. To be overridden by Swing implementation. */
	protected ResourceManager createResourceManager(Window w)	{
		return new ResourceManager(w);
	}

	private void closeWindow(Window w)	{
		windows.remove(w);
		if (exitOnLastClose && windows.size() <= 0)
			System.exit(0);
	}
	
}
