package fri.gui.swing.resourcemanager.resourceset.resource.convert;

import fri.gui.awt.resourcemanager.resourceset.resource.convert.AbstractConverter;

/**
	Encapsulates methods to convert (String - Boolean) a numeric resource.
*/

public class BooleanConverter extends AbstractConverter
{
	/** Turn the Boolean into a persistence string. */
	public String objectToString(Object theBoolean)	{
		return (theBoolean == null || ((Boolean)theBoolean).booleanValue() == false) ? null : "true";
	}

	/** Turn a persistence string into an Boolean object. */
	public Object stringToObject(String spec)	{
		return (spec == null || spec.equalsIgnoreCase("false")) ? Boolean.FALSE : Boolean.TRUE;
	}

	public Class getGuiValueClass(Object component)	{
		return boolean.class;
	}

}
