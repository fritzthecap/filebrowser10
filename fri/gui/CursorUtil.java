package fri.gui;

import java.io.*;
import java.util.*;
import java.awt.*;

import fri.gui.mvc.util.swing.EventUtil;
import fri.gui.swing.ComponentUtil;

/**
 * Methods for setting wait- and default-cursor on the top-level Window parent
 * of a Component, or a hierarchy of Components that have no Window parent.
 * These methods are thread-safe and respect existing cursors in Component tree.
 * It should work even when the Component tree changes between wait-cursor and
 * default-cursor call (as it is with TableCellEditor in Swing).
 * 
 * @author Fritz Ritzberger
 */
public class CursorUtil
{
	// static implementation

	// helper class to maintain top-level Windows, action source Components and their CursorUtil
	private static class CursorUtilList extends ArrayList
	{
		private static class Element
		{
			final Component topLevelWindow;	// looped up from actionSource
			final Component actionSource;	// button, for the case that it gets removed from parent
			final CursorUtil cursorUtil;
			
			Element(Component topLevelWindow, Component actionSource, CursorUtil cursorUtil)	{
				this.topLevelWindow = topLevelWindow;
				this.actionSource = actionSource;
				this.cursorUtil = cursorUtil;
			}
		}
		
		void add(Component topLevelWindow, Component actionSource, CursorUtil cursorUtil)	{
			add(new Element(topLevelWindow, actionSource, cursorUtil));
		}
		
		Element find(Component topLevelWindow, Component actionSource)	{
			for (int i = 0; i < size(); i++)	{
				Element e = (Element) get(i);
				if (matches(e, topLevelWindow, actionSource))
					return e;
			}
			return null;
		}
		
		private boolean matches(Element e, Component topLevelWindow, Component actionSource)	{
			return e.actionSource == actionSource || topLevelWindow != null && e.topLevelWindow == topLevelWindow;
		}
	}


	private static final CursorUtilList cursorUtilList = new CursorUtilList();
	private static Component lastActionSource;
	
	/**
	 * Sets the wait cursor if not already set.
	 * @param actionSource button pressed, e.g. from ComponentEvent.getComponent(), or top level Frame/Dialog
	 */
	public static void setWaitCursor(final Component actionSourceParam)	{
		EventUtil.invokeLaterOrNow(new Runnable()	{
			public void run() {
				Component actionSource = actionSourceParam;
				Component topLevelWindow = ComponentUtil.getWindowForActionSource(actionSource);
				CursorUtilList.Element element = cursorUtilList.find(topLevelWindow, actionSource);
				CursorUtil savedCursors = null;
				if (element != null)	{
					savedCursors = element.cursorUtil;
					topLevelWindow = element.topLevelWindow;
					actionSource = element.actionSource;
				}
				
				if (savedCursors == null)	// must create a new Element
					cursorUtilList.add(topLevelWindow, actionSource, savedCursors = new CursorUtil());
				
				savedCursors.setCursor(
						topLevelWindow != null ? topLevelWindow : actionSource,
						Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				if (savedCursors.waitCursorCallCount == 1)	// if this really has set cursors
					lastActionSource = actionSource;	// save this for no-argument default cursor call
			}
		});
	}
	
	/**
	 * Sets the default cursor from passed component, after a wait cursor call.
	 * @param actionSource button pressed, e.g. from ComponentEvent.getComponent(), or top level Frame/Dialog
	 */
	public static void resetWaitCursor(final Component actionSourceParam)	{
		EventUtil.invokeLaterOrNow(new Runnable()	{
			public void run() {
				Component actionSource = actionSourceParam;
				Component topLevelWindow = ComponentUtil.getWindowForActionSource(actionSource);
				CursorUtilList.Element element = cursorUtilList.find(topLevelWindow, actionSource);
				if (element != null)	{
					actionSource = element.actionSource;
					topLevelWindow = element.topLevelWindow;	// assign this anyway, as new top-level window could be null or other
					CursorUtil savedCursors = element.cursorUtil;
		
					savedCursors.setCursor(
							topLevelWindow != null ? topLevelWindow : actionSource,
							Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					
					if (savedCursors.waitCursorCallCount == 0)	{
						cursorUtilList.remove(element);
						if (actionSource == lastActionSource)
							lastActionSource = null;
					}
				}
			}
		});
	}
	
	/**
	 * Sets the default cursor after a wait cursor call.
	 * This will do nothing if there was no <i>setWaitCursor()</i> call before.
	 */
	public static void resetWaitCursor()	{
		if (lastActionSource != null)
			resetWaitCursor(lastActionSource);
	}
	


	// instance implementation
	
	private HashMap cursorsToRestore;
	private int waitCursorCallCount;
	
	private CursorUtil()	{	// not for public use
	}
	
	private void setCursor(Component component, Cursor cursor)	{
		if (cursorsToRestore == null)
			cursorsToRestore = new HashMap();
		
		if (cursor == Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))	{
			waitCursorCallCount++;	// push call

			if (waitCursorCallCount > 1)
				return;	// already done

			setWaitCursorRecursive(component, true, cursor, cursorsToRestore);
		}
		else
		if (cursor == Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))	{
			if (waitCursorCallCount == 0)
				return;	// no preceding wait cursor call
			
			waitCursorCallCount--;	// pop call
			
			if (waitCursorCallCount != 0)	// still waiting
				return;
			
			// process components that might have been removed meanwhile (cell editors)
			for (Iterator it = cursorsToRestore.entrySet().iterator(); it.hasNext(); )	{
				Map.Entry e = (Map.Entry) it.next();
				Cursor savedCursor = (Cursor) e.getValue();
				Component savedComponent = (Component) e.getKey();
				log(savedCursor, savedComponent, false);
				Cursor currentCursor = savedComponent.getCursor();
				if (currentCursor == Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
					savedComponent.setCursor(savedCursor);
			}
			
			// do current Components that have wait cursor, some of them could not have been stored
			resetWaitCursorRecursive(component, cursor, cursorsToRestore);
			cursorsToRestore.clear();
			cursorsToRestore = null;
		}
		else
			throw new IllegalArgumentException("Can not handle such cursors: "+cursor);
	}
	
	private void setWaitCursorRecursive(
		Component component,
		boolean isRoot,
		Cursor cursor,
		Map savedCursors)
	{
		// loop Component tree bottom-up, else parent wait cursor would be found in subcomponents
		if (component instanceof Container)	{
			Container container = (Container)component;
			Component [] components = container.getComponents();
			for (int i = 0; i < components.length ; ++i)
				setWaitCursorRecursive(components[i], false, cursor, savedCursors);
		}

		Cursor currentCursor = component.getCursor();

		// set wait cursor only when root, or current is not null, else the parent will have wait cursor
		if (isRoot || currentCursor != null)	{
			log(cursor, component, false);
			savedCursors.put(component, currentCursor);	// remember current cursor of Component
			component.setCursor(cursor);
		}
	}
	
	private void resetWaitCursorRecursive(
		Component component,
		Cursor cursor,
		Map componentsToIgnore)
	{
		if (componentsToIgnore.get(component) == null)	{
			Cursor currentCursor = component.getCursor();
			boolean currentIsWaitCursor = (currentCursor == Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			// set cursor only when wait cursor is set
			if (currentIsWaitCursor)	{
				cursor = null;
				log(cursor, component, false);
				component.setCursor(cursor);
			}
		}
		
		if (component instanceof Container)	{
			Container container = (Container)component;
			Component [] components = container.getComponents();
			for (int i = 0; i < components.length ; ++i)
				resetWaitCursorRecursive(components[i], cursor, componentsToIgnore);
		}
	}
	
	
	
	private static final boolean isDebugEnabled = false;
	
	private static void log(Cursor cursor, Component component, boolean topOfRecursion)	{
		if (isDebugEnabled)	{
			String cursorName = cursor != null ? cursor.getName() : "null";
			if (topOfRecursion)	{
				// get caller method name from a stack trace
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				new Exception().printStackTrace(pw);
				String stack = sw.toString();
				int i = stack.lastIndexOf(".CursorUtil");
				int j = stack.indexOf("\n", i);	// line end
				if (j < 0)
					j = stack.indexOf("\r", i);
				String tail = stack.substring(j + 1);	// lines after "CursorUtil" contain the caller
				j = tail.indexOf("\n");
				if (j < 0)
					j = tail.indexOf("\r");
				String line = tail.substring(0, j);	// first line of tail
				
				System.err.println("Cursor call coming from: "+line);
				System.err.println(" - Starting to set "+cursorName+" to "+component.getClass()+" "+component.hashCode());
			}
			else	{
				//System.err.println(" ... setting "+cursorName+" to "+component.getClass()+" "+component.hashCode());
			}
		}
	}

}
