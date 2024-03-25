package fri.gui.awt.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.ShortcutConverter;

/**
	Encapsulates methods to set and reset and convert (String - Object) a menu shortcut resource.
*/

public class ShortcutResource extends Resource
{
	/** Constructor with a persistence value retrieved from properties. */
	public ShortcutResource(String spec)	{
		super(spec);
	}
	
	/** Constructor with a GUI-component. @exception ResourceNotContainedException when this resource is not gettable from passed component. */
	public ShortcutResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	protected Converter createConverter()	{
		return new ShortcutConverter();
	}
	
	public String getTypeName()	{
		return ResourceFactory.SHORTCUT;
	}

}
