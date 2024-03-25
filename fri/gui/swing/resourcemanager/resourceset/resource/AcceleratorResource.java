package fri.gui.swing.resourcemanager.resourceset.resource;

import javax.swing.JMenu;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceNotContainedException;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.AcceleratorConverter;

/**
	Encapsulates methods to set and reset and convert (String - Object) a accelerator resource.
*/

public class AcceleratorResource extends Resource
{
	/** Constructor with a persistence value retrieved from properties. */
	public AcceleratorResource(String spec)	{
		super(spec);
	}
	
	/** Constructor with a original value retrieved from some GUI-component. */
	public AcceleratorResource(Object component)
		throws ResourceNotContainedException
	{
		if (component instanceof JMenu)	// JMenu extends JMenuItem but setAccelerator throws exception
			throw new ResourceNotContainedException("Accelerator defined only for JMenuItem");
			
		defaultResourceDetection(component);
	}

	protected Converter createConverter()	{
		return new AcceleratorConverter();
	}

	public String getTypeName()	{
		return JResourceFactory.ACCELERATOR;
	}

}
