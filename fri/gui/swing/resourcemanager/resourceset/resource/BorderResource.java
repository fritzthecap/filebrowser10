package fri.gui.swing.resourcemanager.resourceset.resource;

import javax.swing.JViewport;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceNotContainedException;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.BorderConverter;

/**
	Encapsulates methods to set and reset and convert (String - Object) a Border resource.
	This class holds a inner class to store a multilanguage title resource.
*/

public class BorderResource extends Resource
{
	/** Constructor with a persistence value retrieved from properties. */
	public BorderResource(String spec)	{
		super(spec);
	}
	
	/** Constructor with a original value retrieved from some GUI-component. */
	public BorderResource(Object component)
		throws ResourceNotContainedException
	{
		if (component instanceof JViewport)
			throw new ResourceNotContainedException("JViewport does not support Border");
		
		defaultResourceDetection(component);
	}

	protected Converter createConverter()	{
		return new BorderConverter();
	}

	public String getTypeName()	{
		return JResourceFactory.BORDER;
	}

}
