package fri.gui.awt.resourcemanager.component;

import fri.util.text.TextUtil;
import fri.gui.awt.component.ComponentName;
import fri.gui.awt.resourcemanager.resourceset.ResourceUtil;
import fri.gui.awt.resourcemanager.persistence.HierarchicalName;

/**
	Eases recognition of Component (after a programmer's GUI modification)
	by adding the label text to its name (if some exists): "button_OK".
*/

public class ResourceComponentName extends ComponentName
{
	/** The separator used to append component text to component name. */
	private String identifier;
	
	public ResourceComponentName(Object component)	{
		super(component);	// build the ComponentName

		// if we can use the programmatic (language-neutral) label string of the component we use it as human readable identifier
		String textMethodBase;
		if (ResourceUtil.isTextCustomizable(component) &&	// do not retrieve text from TextField or TextArea
				(textMethodBase = ResourceUtil.getTextMethodBaseName(component)) != null)
		{
			Object o = ResourceUtil.invoke(component, "get"+textMethodBase);
			if (o instanceof String)	{
				String label = o.toString().trim();
				if (label != null && label.length() > 0 && label.indexOf('\n') < 0 && label.indexOf('\r') < 0)	{	// no newlines contained, seems to be a short text
					String s = TextUtil.makeIdentifier(label);
					String check = s.replace('_', ' ').trim();	// replace '_' by spaces and trim to check for significance
					if (check.length() > 0)	// is significant
						this.identifier = s;
				}
			}
		}
	}

	/** Adds some label identifier if it could be retrieved from Component. */
	public String toString()	{
		String name = super.toString();
		if (identifier == null)
			return name;
		return name+HierarchicalName.COMPONENT_TAG_SEPARATOR+identifier;
	}

}
