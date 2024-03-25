package fri.gui.awt.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.ColorConverter;

/**
	Encapsulates methods to set and reset and convert (String - Object) a background color resource.
*/

public class BackgroundResource extends Resource
{
	protected BackgroundResource()	{
	}
	
	/** Constructor with a persistence value retrieved from properties. */
	public BackgroundResource(String spec)	{
		super(spec);
	}
	
	/** Constructor with a GUI-component. @exception ResourceNotContainedException when this resource is not gettable from passed component. */
	public BackgroundResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	public String getTypeName()	{
		return ResourceFactory.BACKGROUND;
	}

	protected Converter createConverter()	{
		return new ColorConverter();
	}

}
