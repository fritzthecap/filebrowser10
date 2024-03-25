package fri.gui.awt.resourcemanager.resourceset;

import java.awt.*;
import fri.util.reflect.ReflectUtil;
import fri.gui.awt.component.ComponentName;
import fri.gui.awt.component.visitor.*;
import fri.gui.awt.resourcemanager.ResourceIgnoringComponent;
import fri.gui.awt.resourcemanager.ResourceIgnoringContainer;
import fri.gui.awt.resourcemanager.ResourceProvidingTextComponent;

/**
	Utilities in conjunction with generic indication of methods to get and set Component properties.
	This class is Swing-agnostic!
*/

public abstract class ResourceUtil extends ReflectUtil
{
	/** Returns the basename for getter and setter method of text if passed component is not an editable text component. */
	public static String getTextMethodBaseName(Object component)	{
		String methodBaseName = "Text";
		if (getMethod(component, "get"+methodBaseName) != null)
			return isTextCustomizable(component) ? methodBaseName : null;

		methodBaseName = "Label";
		if (getMethod(component, "get"+methodBaseName) != null)
			return methodBaseName;

		return null;
	}
	
	/** Returns the basename for getter and setter method of text or title if passed component is not an editable text component. */
	public static String getTextOrTitleMethodBaseName(Object component)	{
		String methodBaseName = getTextMethodBaseName(component);
		if (methodBaseName != null)
			return methodBaseName;

		if (isTextCustomizable(component))	{	// avoid TextAreas that have a getTitle method
			methodBaseName = "Title";
			if (getMethod(component, "get"+methodBaseName) != null)
				return methodBaseName;
		}

		return null;
	}

	/** Returns false if the passed component is an text component that must not be customized. */
	public static boolean isTextCustomizable(Object component)	{
		if (component instanceof ResourceProvidingTextComponent)
			return true;
		if (component instanceof ResourceIgnoringComponent || component instanceof ResourceIgnoringContainer)
			return false;
		String name = new ComponentName(component).toString();
		return name.startsWith("text") == false && name.startsWith("combo") == false && name.startsWith("choice") == false;	// is not TextArea, TextField or ComboBox
	}

	/** Returns "RowHeight" for JTable and JTree, "FixedCellHeight" for JList: the basename for getter and setter method of row height. */
	public static String getRowHeightMethodBaseName(Object component)	{
		String methodBaseName = "RowHeight";
		if (getMethod(component, "get"+methodBaseName) != null)
			return methodBaseName;

		methodBaseName = "FixedCellHeight";
		if (getMethod(component, "get"+methodBaseName) != null)
			return methodBaseName;

		return null;
	}
	
	/** Returns true if passed component is a Swing component. This is done by "javax.swing" detection. */
	public static boolean isSwing(Object component)	{
		Class appClass = component.getClass();
		Class superClass;
		do	{
			if (appClass.getName().startsWith("javax.swing."))
				return true;

			superClass = appClass;
			appClass = appClass.getSuperclass();
		}
		while (appClass != null && appClass.equals(superClass) == false);
		return false;
	}

	/** Returns true when component is not XXXPane or Panel or Dialog or Frame. */
	public static boolean canHaveForeground(Object o)	{
		String name = new ComponentName(o).toString();
		if (name.indexOf("pane") >= 0 ||
				name.indexOf("dialog") >= 0 ||
				name.indexOf("frame") >= 0 ||
				name.indexOf("viewport") >= 0 ||
				name.indexOf("scrollbar") >= 0)
			return false;
		return true;
	}

	/** Returns the top level Window of the passed Component or MenuComponent. */
	public static Window findWindow(Object component)	{
		BottomUpContainerVisitee visitee = new BottomUpContainerVisitee(component, new ContainerVisitor()	{
			public Object visit(Object c, Object userObject)	{
				return c;	// return the parent recursively
			}
		});
		Object o = visitee.getUserObject();
		//System.err.println("BottomUpContainerVisitor found top level window "+o);
		return o instanceof Window ? (Window)o : null;
	}

	private ResourceUtil()	{}
}
