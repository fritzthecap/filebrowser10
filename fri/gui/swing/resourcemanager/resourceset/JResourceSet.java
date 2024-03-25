package fri.gui.swing.resourcemanager.resourceset;

import fri.gui.awt.resourcemanager.resourceset.ResourceSet;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.swing.resourcemanager.resourceset.resource.JResourceFactory;

/**
	Overriding Swing implementation that updates (titled) borders when language changes.
*/

public class JResourceSet extends ResourceSet
{
	public JResourceSet()	{
		super();
	}
	
	public JResourceSet(Object component, Resource [] resources)	{
		super(component, resources);
	}

	protected boolean havingTextResource()	{
		return super.havingTextResource() || getResourceByType(JResourceFactory.BORDER) != null;
	}

	/** Interface MultiLanguage.ChangeListener: update text and (titled) border resources when language changes. */
	public void languageChanged(String language)	{
		super.languageChanged(language);
		
		for (int i = 0; i < components.size(); i++)	{
			Resource resource = getResourceByType(JResourceFactory.BORDER);
			if (resource != null && resource.getUserValue() != null)	{
				resource.setToComponent(components.get(i), true);
			}
		}
	}

}
