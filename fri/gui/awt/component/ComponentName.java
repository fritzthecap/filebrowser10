package fri.gui.awt.component;

import java.lang.reflect.Method;
import fri.util.reflect.ReflectUtil;
import fri.util.text.Trim;
import fri.util.text.TextUtil;

/**
	Makes a resource name from a components java.awt or javax.swing superclass: "JButton" -> "button"
	If the component is not derived from AWT or Swing class, getName() is called.
*/

public class ComponentName
{
	private String name;
	
	/** Builds e.g. "button" from a Button Component. The argument could be a MenuComponent, so Object is the type. */
	public ComponentName(Object c)	{
		name = standardizeName(makeComponentName(c));
	}

	/** Returns the built name of the object passed into constructor. */
	public String toString()	{
		return name;
	}

	/** Retrieve the Component/MenuComponent name, or return the non-qualified java superclass name. */
	private String makeComponentName(Object c)	{
		String cName = retrieveComponentName(c);	// get component name

		if (cName == null || cName.length() <= 0)	// get name from Java superclass
			cName = makeComponentNameFromJavaClass(c.getClass());

		if (cName == null || cName.length() <= 0)	// get classname
			cName = c.getClass().getName();
			
		// only take last part when dot contained
		int i = cName.lastIndexOf(".");
		if (i >= 0 && cName.length() > i + 1)
			cName = cName.substring(i + 1);
			
		return cName;
	}
			
	private String retrieveComponentName(Object c)	{
		Method m = ReflectUtil.getMethod(c, "getName");
		if (m != null)	{
			Object o = ReflectUtil.invoke(c, "getName");
			if (o instanceof String)
				return (String) o;
		}
		return null;
	}
	
	/** Drills down the class to java.awt or javax.swing superclass. Returns the full qualified classname. */
	private String makeComponentNameFromJavaClass(Class clazz)	{
		boolean java = false;
		Class superClass;

		do	{
			if (clazz.getName().startsWith("java.awt") || clazz.getName().startsWith("javax.swing"))
				java = true;
			superClass = clazz;
			clazz = clazz.getSuperclass();
		}
		while (java == false && clazz != null && clazz.equals(superClass) == false);

		return java ? superClass.getName() : null;
	}
	
	/** Remove trailing digits and any special characters, remove leading "swing_" and "j". */
	private String standardizeName(String s)	{
		s = Trim.removeTrailingDigits(s);
		s = TextUtil.makeIdentifier(s);

		if (s.startsWith("J") || s.startsWith("j"))
			s = s.substring(1);
			
		return s.toLowerCase();
	}

}
