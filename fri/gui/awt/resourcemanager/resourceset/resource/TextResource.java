package fri.gui.awt.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.ResourceUtil;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.TextConverter;

/**
	Encapsulates methods to set and reset a text resource.
*/

public class TextResource extends Resource
{
	private String methodBaseName;

	protected TextResource()	{
	}
	
	/** Constructor with a persistence value retrieved from properties. */
	public TextResource(String spec)	{
		super(spec);
	}

	/** Constructor with a original value retrieved from some GUI-component. */
	public TextResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	protected String getMethodBaseName(Object component)	{
		if (methodBaseName == null)	// can happen when persistent and inited by resourceSet.addComponent()
			methodBaseName = ResourceUtil.getTextOrTitleMethodBaseName(component);
		return methodBaseName;
	}

	protected Converter createConverter()	{
		return new TextConverter();
	}

	public String getTypeName()	{
		return ResourceFactory.TEXT;
	}

}
