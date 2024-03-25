package fri.gui.swing.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.resource.TextResource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceNotContainedException;

/**
	Encapsulates methods to set and reset a tooltip text resource.
*/

public class ToolTipTextResource extends TextResource
{
	/** Constructor with a persistence value retrieved from properties. */
	public ToolTipTextResource(String spec)	{
		super(spec);
	}

	/** Constructor with a original value retrieved from some GUI-component. */
	public ToolTipTextResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	protected String getMethodBaseName(Object component)	{
		return getTypeName();
	}

	public String getTypeName()	{
		return JResourceFactory.TOOLTIP;
	}

}
