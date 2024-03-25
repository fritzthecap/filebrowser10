package fri.gui.swing.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.ResourceUtil;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceNotContainedException;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.IntegerConverter;

/**
	Encapsulates methods to set and reset a row height resource.
*/

public class RowHeightResource extends Resource
{
	private String methodBaseName;
	
	/** Constructor with a persistence value retrieved from properties. */
	public RowHeightResource(String spec)	{
		super(spec);
	}

	/** Constructor with a original value retrieved from some GUI-component. */
	public RowHeightResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	protected String getMethodBaseName(Object component)	{
		if (methodBaseName == null)	// can happen when persistent and inited by resourceSet.addComponent()
			methodBaseName = ResourceUtil.getRowHeightMethodBaseName(component);
		return methodBaseName;
	}
	
	public String getTypeName()	{
		return JResourceFactory.ROWHEIGHT;
	}

	protected Converter createConverter()	{
		return new IntegerConverter();
	}
	
}
