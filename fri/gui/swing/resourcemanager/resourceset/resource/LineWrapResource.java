package fri.gui.swing.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceNotContainedException;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.BooleanConverter;

/**
	Encapsulates methods to set and reset a line wrap resource.
*/

public class LineWrapResource extends Resource
{
	/** Constructor with a persistence value retrieved from properties. */
	public LineWrapResource(String spec)	{
		super(spec);
	}

	/** Constructor with a original value retrieved from some GUI-component. */
	public LineWrapResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	public String getTypeName()	{
		return JResourceFactory.LINEWRAP;
	}

	protected Converter createConverter()	{
		return new BooleanConverter();
	}
	
}
