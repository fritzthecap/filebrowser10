package fri.gui.awt.resourcemanager.dialog;

import java.util.*;
import fri.util.Equals;
import fri.util.i18n.MultiLanguage;
import fri.gui.awt.resourcemanager.ResourceManager;
import fri.gui.awt.resourcemanager.resourceset.ResourceSet;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

/**
	The base class for all customizer dialog launchers. It manages the results
	of customizing session and activates or deactivates GUI resources.
	<p>
	This class exposes a System Property named "fri.gui.awt.resourcemanager.showRestricted".
	If this is set to an (arbitrary) value, the dialog will not let customize
	texts, tooltips, mnemonics, accelerators or AWT shortcuts.
*/

public abstract class CustomizerGUI implements
	ResourceSetEditor
{
	public static final boolean showRestricted = System.getProperty("fri.gui.awt.resourcemanager.showRestricted") != null;
	/** The ResourceManager is needed to call ComponentChoice dialog. */
	protected ResourceManager resourceManager;
	private ResourceSet resourceSet;	// the obtained ResourceSet
	protected List chooserList = new ArrayList();	// contains all ResourceChoosers allocated by a subclass
	private String currentLanguage = MultiLanguage.getLanguage();	// store language state to reset on cancel
	
	public CustomizerGUI(ResourceSet resourceSet, ResourceManager resourceManager)	{
		this.resourceSet = resourceSet;
		this.resourceManager = resourceManager;
		resourceSet.startCustomizing();	// start the customize session
	}

	/** This method controls which resource types are made available on the dialog. */
	protected String [] getResourceTypes()	{
		String [] types = getResourceSet().getResourceTypes();
		if (showRestricted == false)
			return types;
		
		ArrayList list = new ArrayList();
		for (int i = 0; i < types.length; i++)
			if (isRestrictedType(types[i]) == false)
				list.add(types[i]);
		
		types = new String[list.size()];
		list.toArray(types);
		return types;
	}
	
	protected boolean isRestrictedType(String type)	{
		return type.equals(ResourceFactory.TEXT) || type.equals(ResourceFactory.SHORTCUT);
	}
	
	/** Add one ResourceChooser to list of active editors. This list gets requested when setting or resetting resources. */
	protected void addResourceChooser(ResourceChooser chooser)	{
		chooserList.add(chooser);
	}
	
	/** Interface ResourceSetEditor. Returns the valid ResourceSet or null if canceled. */
	public ResourceSet getResourceSet()	{
		return resourceSet;
	}


	private void putEditorSettingsToResourceSet()	{
		for (int i = 0; i < chooserList.size(); i++)	{
			ResourceChooser chooser = (ResourceChooser) chooserList.get(i);
			Object value = chooser.getValue();	// retrieve current value from chooser
			String type = chooser.getResourceTypeName();	// retrieve type of Resource from chooser
			Resource resource = resourceSet.getResourceByType(type);	// look for this Resource type in ResourceSet
			
			if (resource != null)	{
				Object oldValue = resource.getVisibleValue();
				if (Equals.equals(oldValue, value) == false)	{
					System.err.println("Changed resource type "+type+", old value is "+oldValue+", "+(oldValue != null ? ""+oldValue.getClass() : "null")+", new value is "+value+", "+(value != null ? ""+value.getClass() : "null"));
					resource.setUserValue(value);
				}
				
				if (chooser.isComponentTypeBound() != resource.isComponentTypeBound())
					resource.setComponentTypeBound(chooser.isComponentTypeBound());
			}
			else	{
				if (type.equals(ResourceChooser.LANGUAGE))	{
					if (value != null)
						MultiLanguage.setLanguage(value.toString());	// notifies language listeners only when changed
				}
				else
					throw new IllegalArgumentException("Putting editor value to resource, could not find resource type of chooser: "+type);
			}
		}
	}
	
	/** Subclasses use this method to set all resources to the customized Component. Dialog finishes after calling this method. */
	protected void set()	{
		putEditorSettingsToResourceSet();	// resourceSet.set() will be done by ResourceManager refresh
	}

	/** Subclasses use this method to remove all resources from the customized Component. Dialog finishes after calling this method. */
	protected void reset()	{
		resourceSet.reset();
	}

	/** Subclasses use this method to visualize all resources on the customized Component, without disposing the dialog. */
	protected void test()	{
		putEditorSettingsToResourceSet();
		resourceSet.set();
	}

	/** Subclasses use this method to reset all visualized test resources from the customized GUI.  Dialog finishes after calling this method. */
	protected void cancel()	{
		resourceSet.rollbackCustomizing();	// cancel the customize session, restore old values in Resources
		if (currentLanguage.equals(MultiLanguage.getLanguage()) == false)
			MultiLanguage.setLanguage(currentLanguage);
	}

}
