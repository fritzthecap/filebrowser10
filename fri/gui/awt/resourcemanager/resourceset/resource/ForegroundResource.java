package fri.gui.awt.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.ResourceUtil;

/**
	Encapsulates methods to set and reset and convert (String - Object) a foreground color resource.
*/

public class ForegroundResource extends BackgroundResource
{
	/** Constructor with a persistence value retrieved from properties. */
	public ForegroundResource(String spec)	{
		super(spec);
	}
	
	/** Constructor with a GUI-component. @exception ResourceNotContainedException when this resource is not gettable from passed component. */
	public ForegroundResource(Object component)
		throws ResourceNotContainedException
	{
		if (ResourceUtil.canHaveForeground(component) == false)
			throw new ResourceNotContainedException(getTypeName());
		
		defaultResourceDetection(component);
	}

	public String getTypeName()	{
		return ResourceFactory.FOREGROUND;
	}
	
}
