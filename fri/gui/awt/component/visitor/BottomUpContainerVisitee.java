package fri.gui.awt.component.visitor;

import java.lang.reflect.Method;
import java.awt.*;
import fri.util.reflect.ReflectUtil;

/**
	Visit all Parent-Components of a Component.
	A user-defined object can be received and returned in every callback.
*/

public class BottomUpContainerVisitee
{
	private Object ret;
	
	/**
		Traverse the tree of passed Component (first callback passes the Component itself).
		@param c Component to loop recursively
		@param visitor the visiting object
	*/
	public BottomUpContainerVisitee(Object c, ContainerVisitor visitor)	{
		this(c, visitor, null);
	}

	/**
		Additional user object for traversal.
		@param userObject some arbitrary client data
		@param rest see above
	*/
	public BottomUpContainerVisitee(Object c, ContainerVisitor visitor, Object userObject)	{
		ret = traverse(c, visitor, userObject);
	}

	/** Returns the user object after the traversal. */
	public Object getUserObject()	{
		return ret;
	}
	
	/** Loop up Parent-Components recursively ascending. */
	protected Object traverse(Object c, ContainerVisitor visitor, Object userObject)	{
		userObject = visitor.visit(c, userObject);

		if (c instanceof Window)	// reached top of component tree
			return userObject;
		
		Object parent = null;

		if (c instanceof Component)	{
			parent = ((Component)c).getParent();
		}
		else
		if (c instanceof MenuComponent)	{
			parent = ((MenuComponent)c).getParent();
		}
		else	{
			Method m = ReflectUtil.getMethod(c, "getInvoker");	// JPopupMenu.getInvoker (do not import)
			if (m != null)	{
				try	{ parent = (Component) m.invoke(c, new Object[0]); }
				catch (Exception e)	{ e.printStackTrace(); }
			}
			else	{
				Object ret = ReflectUtil.invoke(c, "getComponent", new Object[0]);	// MenuElement.getComponent (do not import)
				if (ret != null && ret instanceof Component)
					parent = ((Component) ret).getParent();
			}
		}
		
		if (parent != null && c != parent)
			userObject = traverse(parent, visitor, userObject);
			
		return userObject;
	}

}
