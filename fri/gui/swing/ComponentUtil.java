package fri.gui.swing;

import java.awt.*;
import javax.swing.*;

/**
 * Utilities concerning Java AWT Component classes.
 * @auhor Fritz Ritzberger
 */
public abstract class ComponentUtil
{
	/**
	 * Returns the Frame ancestor of the passed Component, or null if there is no anchestor of class Frame.
	 * @param component Component to find the Frame parent for.
	 */
	public static Frame getFrame(Component component)	{
		return (Frame) getTopClassForComponent(component, Frame.class);
	}

	/**
	 * Returns the Window ancestor of the passed Component, or null if there is no anchestor of class Window.
	 * @param component Component to find the Frame parent for.
	 */
	public static Window getWindowForComponent(Component c)	{
		return (Window) getTopClassForComponent(c, Window.class);
	}

	/**
	 * If actionSource is instanceof Component, returns <i>getWindowForComponent((Component)actionSource)</i>.
	 * If actionSource is instanceof Window, returns actionSource.
	 * Returns null in all other cases.
	 */
	public static Window getWindowForActionSource(Object actionSource)	{
		if (actionSource instanceof Window)
			return (Window) actionSource;
		if (actionSource instanceof Component)
			return getWindowForComponent((Component)actionSource);
		return null;
	}

	private static Component getTopClassForComponent(Component component, Class clazz) {
		Component child = component;
		while (component != null && (clazz == null || clazz.isAssignableFrom(component.getClass()) == false))	{
			child = component;
			component = getParentExtended(component);
		}
		Component top = (component == null) ? child : component;
		return clazz == null || top != null && clazz.isAssignableFrom(top.getClass()) ? top : null;
	}

	private static Component getParentExtended(Component c)	{
		if (c instanceof MenuElement)
			if (c instanceof JPopupMenu)
				c = ((JPopupMenu)c).getInvoker();
			else
				c = ((MenuElement)c).getComponent();

		return c != null ? c.getParent() : null;
	}

	
	/**
	 * Returns the index of the passed Component within passed Container.
	 * This is for replacing Components.
	 */
	public static int getComponentIndex(Container container, Component component)	{
		for (int i = 0; i < container.getComponentCount(); i++)	{
			Component c = container.getComponent(i);
			if (c == component)
				return i;
		}
		return -1;	// not found
	}

	
	/**
	 * Replaces the passed old Component within passed container by the new Component.
	 */
	public static void replaceComponent(Container container, Component oldComponent, Component newComponent)	{
		int i = getComponentIndex(container, oldComponent);
		container.remove(i);
		container.add(newComponent, i);
	}


	/** Calls <i>c.requestFocus()</i> via <i>EventQueue.invokeLater()</i>. */
	public static void requestFocus(final Component c)	{
		if (c != null)	{
			EventQueue.invokeLater(new Runnable()	{
				public void run()	{
					//System.err.println("ComponentUtil, requestFocus on "+c);
					c.requestFocus();
				}
			});
		}
	}

}
