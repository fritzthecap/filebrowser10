package fri.gui.awt.resourcemanager.resourceset;

import java.awt.Window;
import java.util.*;
import fri.util.i18n.MultiLanguage;
import fri.gui.awt.component.ComponentName;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

/**
	A set of resources like font, color, text, border, icon, ...
	This class is responsible for visualizing these resources all together by
	<i>set(), reset() or force()</i> methods. It exposes all Resources by being
	a List. It holds a list of Components that are associated with this Resources,
	so it is the link between Components and its Resources.
	<p>
	A ResourceSet can be Window-type bound or Component-type bound.
	A Component-type bound ResourceSet would customize e.g. all Buttons,
	a non-Component-type bound ResourceSet would customize only one
	button within a certain type of Window (but the Button would have the
	same ResourceSet within all instances of that Window).
*/

public class ResourceSet extends ArrayList implements
	MultiLanguage.ChangeListener
{
	protected ArrayList components = new ArrayList(1);
	
	/** Do-nothing Constructor. */
	public ResourceSet()	{
		super(8);
	}
	
	/** Constructor used by ResourceManager before dialog. Retrieve original values from component, add it. */
	public ResourceSet(Object component, Resource [] resources)	{
		super(8);
		completeResources(resources);
		addToComponents(component);
	}

	/** Adds all Resources from passed component if not already contained. The Resources will be in same order as in array. */
	public void completeResources(Resource [] resources)	{
		for (int i = 0; i < resources.length; i++)	{
			Resource existing = getResourceByType(resources[i].getTypeName());
			if (existing == null)	// not already contained
				super.add(i, resources[i]);
		}
		Collections.sort(this);	// keep order as resources could depend on each other
	}
	
	/** Add a component, initialize contained resources if first, and activate resources on it. */
	public void addComponent(Object component)	{
		addComponentSetResourceSet(component, false);
	}
	
	/** Add a component, initialize contained resources if first, and force resources on it. */
	public void addComponentForceResourceSet(Object component)	{
		addComponentSetResourceSet(component, true);
	}
	
	private void addComponentSetResourceSet(Object component, boolean force)	{
		if (components.size() <= 0)	{	// original values were not retrieved
			for (int i = 0; i < size(); i++)	{
				Resource resource = (Resource) get(i);
				resource.initFromComponent(component);
			}
		}
		addToComponents(component);

		if (force)
			force();
		else
			set(component, true);	// activate the resources
	}
	
	private void addToComponents(Object component)	{
		if (components.indexOf(component) < 0)	{
			components.add(component);	// add only if not already contained

			if (components.size() == 1 && havingTextResource())
				MultiLanguage.addChangeListener(this);	// on first component listen to language changes
		}
	}
	
	protected boolean havingTextResource()	{
		return getResourceByType(ResourceFactory.TEXT) != null;
	}

	/** Visualize user-defined (non-null) Resources upon all obtained components. */
	public void set()	{
		set(true);
	}
	
	/** Deactivate the resourceSet upon all obtained components. This visualizes all original resources. */
	public void reset()	{
		set(false);
	}
	
	/** Visualize all Resources upon all obtained components (even if user-defined values are null). Needed on Cancel after test. */
	private void force()	{
		for (int i = 0; i < components.size(); i++)	{
			Object component = components.get(i);
			for (int j = 0; j < size(); j++)	{
				Resource resource = (Resource) get(j);
				resource.forceToComponent(component);
			}
		}
	}
	
	private void set(boolean active)	{
		for (int i = 0; i < components.size(); i++)
			set(components.get(i), active);
	}

	private void set(Object component, boolean active)	{
		for (int i = 0; i < size(); i++)	{
			Resource resource = (Resource) get(i);
			if (active == false || resource.getUserValue() != null)
				resource.setToComponent(component, active);
		}
		visualize();
	}

	/** Returns true if any contained Resource has a non-null customized value. */
	public boolean hasNoValues()	{
		for (int i = 0; i < size(); i++)
			if (((Resource)get(i)).getUserValue() != null)
				return false;
		return true;
	}

	/** Returns true if no components were registered with this resource set. */
	public boolean hasNoComponents()	{
		return components.size() <= 0;
	}
	
	private void visualize()	{
		for (Iterator it = components.iterator(); it.hasNext(); )	{
			Object component = it.next();
			Window w = findWindow(component);
			if (w != null)
				w.validate();
		}
	}

	private Window findWindow(Object component)	{
		return ResourceUtil.findWindow(component);
	}

	/** Remove all components that are anchestors of the passed window. */
	public void closeWindow(Window window)	{
		for (Iterator it = components.iterator(); it.hasNext(); )
			if (findWindow(it.next()) == window)
				it.remove();

		if (components.size() == 0 && havingTextResource())
			MultiLanguage.removeChangeListener(this);
	}

	/** Remove passed components from list of obtained components. */
	public void removeComponent(Object component)	{
		components.remove(component);
	}
	
	
	/** Returns "(empty)" when no Components wer added, or the name resource typename ("button") of first Component. */
	public String toString()	{
		if (components.size() <= 0)
			return "(anonymous)";
		
		Object o = components.get(0);
		return new ComponentName(o).toString();
	}



	/** Interface MultiLanguage.ChangeListener: update text resources when language changes. */
	public void languageChanged(String language)	{
		for (int i = 0; i < components.size(); i++)	{
			Resource resource = getResourceByType(ResourceFactory.TEXT);
			if (resource != null && resource.getUserValue() != null)
				resource.setToComponent(components.get(i), true);
		}
	}
	public void languageAdded(String newLanguage)	{}
	public void languageRemoved(String language)	{}
	
	
	// methods needed for dialog customizer
	
	/** Needed by customize dialog. Returns all contained Resource types, its sort order is that of contained Resources. */
	public String [] getResourceTypes()	{
		List list = new ArrayList();
		for (int i = 0; i < size(); i++)
			list.add(((Resource)get(i)).getTypeName());
		String [] sarr = new String[list.size()];
		list.toArray(sarr);
		return sarr;
	}

	/** Returns the contained Resource of passed type (ResourceFactory.FONT, ...). */
	public Resource getResourceByType(String type)	{
		int i = getResourceIndexByType(type);
		return i < 0 ? null : (Resource) get(i);
	}

	private int getResourceIndexByType(String type)	{
		for (int i = 0; i < size(); i++)
			if (((Resource) get(i)).getTypeName().equals(type))
				return i;
		return -1;
	}


	/** Called by customize dialog before changing this ResourceSet. */ 
	public void startCustomizing()	{
		for (int i = 0; i < size(); i++)
			((Resource)get(i)).startCustomizing();
	}

	/** Called by customize dialog to restore the values after changing this ResourceSet. */ 
	public void rollbackCustomizing()	{
		for (int i = 0; i < size(); i++)
			((Resource)get(i)).rollbackCustomizing();
	}



	/** Throws IllegalStateException. Please use completeResources(). */
	public void add(int i, Object o)	{
		throw new IllegalStateException("Please use method completeResources to grant sort integrity of ResourceSet");
	}
	/** Throws IllegalStateException. Please use completeResources(). */
	public boolean add(Object o)	{
		throw new IllegalStateException("Please use method completeResources to grant sort integrity of ResourceSet");
	}	

}
