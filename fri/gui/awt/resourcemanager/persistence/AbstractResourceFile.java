package fri.gui.awt.resourcemanager.persistence;

import java.util.*;
import java.awt.Window;
import fri.util.i18n.MultiLanguage;
import fri.gui.awt.resourcemanager.resourceset.ResourceSet;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

/**
	For each type of Window one ResourceFile will be created.
	Mangages the ResourceSets of a Window type as a Hashtable
	with key = hierarchicalName and value = ResourceSet.
	<p>
	This class obtains an instance of AbstractResourceFile to keep
	the component-type-bound ResourceSets separately. This additional
	resource file must be created by subclasses (else all resources
	will be component-type-bound!).
	<p>
	The save() method must be called explicitely after every change.
	The component-type-bound resource file will be stored automatically
	on every change.
*/

public abstract class AbstractResourceFile extends Hashtable implements
	MultiLanguage.ChangeListener
{
	private static String language;
	private AbstractResourceFile componentTypeResourceFile;

	protected AbstractResourceFile()	{
		this(null);
	}

	protected AbstractResourceFile(AbstractResourceFile componentTypeResourceFile)	{
		this.componentTypeResourceFile = componentTypeResourceFile;
	}


	/** Loads all name/value (String/String) pairs into this resource file. */
	protected void loadFromMap(Map map, ResourceFactory resourceFactory)	{
		for (Iterator it = map.entrySet().iterator(); it.hasNext(); )	{
			Map.Entry entry = (Map.Entry) it.next();
			
			String key = (String) entry.getKey();	// "frame0.panel1.button2.Font"
			String resourceValue = (String) entry.getValue();	// e.g. "Dialog 12"
			
			if (isSpecialKey(key, resourceValue) == false)	{
				HierarchicalName hn = new HierarchicalName(key);
				
				if (hn.getResourceTypeName() != null)	{
					ResourceSet resourceSet = (ResourceSet) get(hn.getHierarchicalName());
					if (resourceSet == null)	{
						resourceSet = createResourceSet();
						super.put(hn.getHierarchicalName(), resourceSet);	// store to this Map. Call super NOT to save() on every put()
					}
					
					Resource resource = resourceFactory.newInstance(hn.getResourceTypeName(), resourceValue);
					resource.setComponentTypeBound(isComponentTypeBound());	// when NullPointerException, XxxResource have not been compiled or packed
					resourceSet.completeResources(new Resource [] { resource });
				}
				else	{
					System.err.println("WARNING: got resource type null name for value: "+resourceValue);
				}
			}
		}

		MultiLanguage.addChangeListener(this);
	}

	/** Called once on load of resource file. */
	protected boolean isSpecialKey(String key, String value)	{
		if (isComponentTypeBound() && key.equals("language"))	{	// restore language
			if (language == null || language.equals(value) == false)
				MultiLanguage.setLanguage(language = value);
			return  true;
		}
		return false;
	}

	/** Factory method to create a resource set. To be overridden by Swing implementation. */
	protected ResourceSet createResourceSet()	{
		return new ResourceSet();
	}
	

	/** Store to persistence where this ResourceFile comes from. */
	public abstract void save();
	
	
	/** Store to persistence by filling a Map and returning it for a persistence providing subclass. */
	protected Properties saveToMap()	{
		Properties p = new Properties();
		
		for (Iterator it = entrySet().iterator(); it.hasNext(); )	{
			Map.Entry entry = (Map.Entry) it.next();
			String hierarchicalName = (String) entry.getKey();
			ResourceSet resourceSet = (ResourceSet) entry.getValue();
			
			for (int i = 0; i < resourceSet.size(); i++)	{
				Resource resource = (Resource) resourceSet.get(i);
				
				if (isComponentTypeBound() == resource.isComponentTypeBound())	{
					String persistentValue = resource.toString();
					
					if (persistentValue != null)	{
						String propertiesKey = HierarchicalName.propertiesKeyFromHierarchicalName(hierarchicalName, resource.getTypeName());
						p.setProperty(propertiesKey, persistentValue);
					}
				}
			}
		}
		
		if (isComponentTypeBound())	// save current language
			p.setProperty("language", MultiLanguage.getLanguage());

		return p;
	}


	protected boolean isComponentTypeBound()	{
		return componentTypeResourceFile == null;
	}
	

	/**
		Overridden to request componentTypeResourceFile for additional component-type-bound Resources.
	*/
	public Object get(Object key)	{
		ResourceSet resourceSet = (ResourceSet) super.get(key);
		if (isComponentTypeBound())
			return resourceSet;

		// look for component-type-bound resources
		String componentTypeName = HierarchicalName.componentTypeNameFromHierarchicalName((String) key);	// "button"
		ResourceSet componentTypeResourceSet = (ResourceSet) componentTypeResourceFile.get(componentTypeName);
		
		if (componentTypeResourceSet != null && componentTypeResourceSet.size() > 0)	{
			Resource [] typeResources = new Resource[componentTypeResourceSet.size()];
			componentTypeResourceSet.toArray(typeResources);
			if (resourceSet == null)	{
				resourceSet = createResourceSet();
				super.put(key, resourceSet);
			}
			resourceSet.completeResources(typeResources);	// takes only what is not already contained
		}
		return resourceSet;
	}

	/**
		Overridden manage component-type-bound and normal resources.
		Component-type-bound Resources get stored to a separate file.
	*/
	public Object put(Object key, Object value)	{
		ResourceSet resourceSet = (ResourceSet) value;
		
		if (isComponentTypeBound() == false)	{	// need to separate resources
			String componentTypeName = HierarchicalName.componentTypeNameFromHierarchicalName((String) key);	// "button"
			ResourceSet componentTypeResourceSet = (ResourceSet) componentTypeResourceFile.get(componentTypeName);
			if (componentTypeResourceSet != null)
				 componentTypeResourceSet.clear();
			
			for (int i = 0; i < resourceSet.size(); i++)	{
				Resource resource = (Resource) resourceSet.get(i);
				
				if (resource.isComponentTypeBound())	{
					if (componentTypeResourceSet == null)
						componentTypeResourceSet = createResourceSet();	// as it is only a "shadow" resource-set it needs no components
					componentTypeResourceSet.add(resource);
				}
			}
			
			if (componentTypeResourceSet != null && componentTypeResourceSet.size() > 0)	{
				componentTypeResourceFile.put(componentTypeName, componentTypeResourceSet);
				componentTypeResourceFile.save();
			}
			else	// could have been an existing "shadow" resource set
			if (componentTypeResourceFile.get(componentTypeName) != null)	{
				componentTypeResourceFile.remove(componentTypeName);
				componentTypeResourceFile.save();
			}
		}

		return super.put(key, resourceSet);
	}


	/** Store the new language setting to persistence when language changes. */
	public void languageChanged(String newLanguage)	{
		if (language == null || language.equals(newLanguage) == false)	{
			save();
			language = newLanguage;
		}
	}
	public void languageAdded(String newLanguage)	{}
	public void languageRemoved(String language)	{}


	/** Removes all Components that are contained within passed window from its resourceSet. */
	public void closeWindow(Window window)	{
		for (Iterator it = entrySet().iterator(); it.hasNext(); )	{
			Map.Entry entry = (Map.Entry)it.next();
			ResourceSet resourceSet = (ResourceSet)entry.getValue();
			resourceSet.closeWindow(window);
		}
	}


	protected void finalize()	{
		MultiLanguage.removeChangeListener(this);
	}

}
