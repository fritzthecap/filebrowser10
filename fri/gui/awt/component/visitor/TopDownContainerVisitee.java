package fri.gui.awt.component.visitor;

import java.lang.reflect.Method;
import java.awt.*;
import fri.util.reflect.ReflectUtil;

/**
	Visit all Child-Components of a Component, top down, depth first.
	A user-defined object can be received and returned in every callback.
*/

public class TopDownContainerVisitee
{
	/**
		Traverse the tree of passed Component (first callback passes the visited Container itself).
		@param c Container to loop recursively
		@param visitor the visiting object
	*/
	public TopDownContainerVisitee(Object c, ContainerVisitor visitor)	{
		this(c, visitor, null);
	}

	/**
		Additional user object for traversal.
		@param userObject some arbitrary client data
		@param rest see above
	*/
	public TopDownContainerVisitee(Object c, ContainerVisitor visitor, Object userObject)	{
		traverse(c, visitor, userObject);
	}

	/** Loop Child-Components recursively. */
	protected void traverse(Object c, ContainerVisitor visitor, Object userObject)	{
		userObject = visitor.visit(c, userObject);
		traverseWithoutVisit(c, visitor, userObject);
	}
	
	/** Used by subclasses to loop without calling visitor (when additional components got inserted). */
	protected void traverseWithoutVisit(Object c, ContainerVisitor visitor, Object userObject)	{
		if (c instanceof Container)	{
			Component [] comps = null;
			
			Method m = ReflectUtil.getMethod(c, "getMenuComponents");	// JMenu
			if (m == null)	{
				comps = ((Container)c).getComponents();
			}
			else	{	// is a JMenu
				try	{ comps = (Component[]) m.invoke(c, new Object[0]); }
				catch (Exception e)	{ e.printStackTrace(); }
			}
			
			for (int i = 0; comps != null && i < comps.length; i++)	{
				traverse(comps[i], visitor, userObject);
			}
		}
		else
		if (c instanceof MenuBar)	{	// Loop AWT MenuBar
			MenuBar mb = (MenuBar)c;
			int cnt = mb.getMenuCount();
			for (int i = 0; i < cnt; i++)	{
				Menu menu = mb.getMenu(i);
				traverse(menu, visitor, userObject);
			}
		}
		else
		if (c instanceof Menu)	{	// Loop AWT Menu-Items
			Menu menu = (Menu)c;
			int cnt = menu.getItemCount();
			
			for (int i = 0; i < cnt; i++)	{
				MenuItem item = menu.getItem(i);
				traverse(item, visitor, userObject);
			}
		}

		// Loop AWT Frame MenuBar
		if (c instanceof Frame)	{
			Frame f = (Frame)c;
			MenuBar mb = f.getMenuBar();
			if (mb != null)	{	// Swing JFrame will return null
				traverse(mb, visitor, userObject);
			}
		}
	}

}
