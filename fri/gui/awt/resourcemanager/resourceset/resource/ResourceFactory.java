package fri.gui.awt.resourcemanager.resourceset.resource;

import java.util.ArrayList;
import fri.util.ClassUtil;
import fri.util.reflect.ReflectUtil;

/**
	Creates Resource instances from an persistence String or an Component/MenuComponent.
	This factory looks for the Resource subclass in its own package, whereby all superclasses
	are looped (so derivations of this class can place their resources within their own "resource"
	sub-package).
	<p>
	This factory provides only the basic AWT compatible Resource subclasses:
	Font, Foreground, Background, Shortcut, Text.
	<p>
	Following is the contract for all Resource subclasses to be usable by ResourceFactory.
	Every Resource subclass must ...
	<ul>
		<li>... provide a constructor with argument String (persistence string constructor)</li>
		<li>... provide a constructor with argument Object, that represents a GUI Component,
				this throws NoSuchResourceException if it does not find itself within the passed component</li>
		<li>... inherit from Resource and implement its abstract methods</li>
	</ul>
*/

public class ResourceFactory
{
	/** Type identifier for font Resource. */
	public static final String FONT = "Font";		// must match the method-name of Component!
	/** Type identifier for background color Resource. */
	public static final String BACKGROUND = "Background";
	/** Type identifier for foreground color Resource. */
	public static final String FOREGROUND = "Foreground";
	/** Type identifier for menu shortcut Resource. */
	public static final String SHORTCUT = "Shortcut";
	/** Type identifier for text Resource. */
	public static final String TEXT = "Text";


	/** Creates a Resource from persistence typename and string-value. */
	public Resource newInstance(String typeName, String value)	{
		String [] pkgs = getPackageNames();
		
		for (int i = 0; i < pkgs.length; i++)	{
			// "Font" expands to "fri.gui.awt.resourcemanager.resourceset.resource.FontResource"
			String className = pkgs[i]+"."+typeName+"Resource";
			Resource resource = (Resource) ReflectUtil.newInstance(className, new Object [] { value });
			if (resource != null)	{
				resource.setSortPosition(getSortPosition(typeName));
				return resource;
			}
		}
		return null;
	}

	private int getSortPosition(String typeName)	{
		String [] types = getResourceTypeNames();
		for (int i = 0; i < types.length; i++)
			if (types[i].equals(typeName))
				return i;
		throw new IllegalArgumentException("Sort position of resource type not found: "+typeName);
	}


	/** Creates a set of Resources from a Component, MenuComponent, or anything that holds possible resources. */
	public Resource [] createResources(Object c)	{
		ArrayList list = new ArrayList();
		String [] resourceTypeNames = getResourceTypeNames();
		String [] pkgs = getPackageNames();
		
		for (int i = 0; i < pkgs.length; i++)	{
			for (int j = 0; j < resourceTypeNames.length; j++)	{
				String className = pkgs[i]+"."+resourceTypeNames[j]+"Resource";
				Resource resource = (Resource) ReflectUtil.newInstance(className, new Class [] { Object.class }, new Object [] { c });
				if (resource != null)	{
					resource.setSortPosition(i);
					list.add(resource);
				}
			}
		}
		
		Resource [] resources = new Resource[list.size()];
		list.toArray(resources);
		return resources;
	}


	/** Returns all resource type names loadable by this factory. */
	protected String [] getResourceTypeNames()	{
		return new String []	{
				FONT,
				BACKGROUND,
				FOREGROUND,
				TEXT,
				SHORTCUT,
		};
	}
	
	/** Returns all package names from which Resource subclasses can be loaded by this factory. */
	private String [] getPackageNames()	{
		Class clazz = getClass();
		ArrayList list = new ArrayList(3);
		do	{
			list.add(ClassUtil.getPackageName(clazz.getName()));
			clazz = clazz.getSuperclass();
		}
		while (clazz.equals(Object.class) == false);
		
		String [] pkgs = new String[list.size()];
		list.toArray(pkgs);
		return pkgs;
	}

}
