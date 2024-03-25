package fri.gui.swing.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceNotContainedException;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.IconConverter;

/**
	Encapsulates methods to set and reset a icon resource.
*/

public class IconResource extends Resource
{
	/** Constructor with a persistence value retrieved from properties. */
	public IconResource(String spec)	{
		super(spec);
	}
	
	/** Constructor with a original value retrieved from some GUI-component. */
	public IconResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	protected Converter createConverter()	{
		return new IconConverter();
	}

	public String getTypeName()	{
		return JResourceFactory.ICON;
	}

}
