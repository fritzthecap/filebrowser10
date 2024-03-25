package fri.gui.swing.resourcemanager.resourceset.resource.convert;

import fri.gui.awt.resourcemanager.resourceset.resource.convert.AbstractConverter;

/**
	Encapsulates methods to convert (String - Integer) a numeric resource.
*/

public class IntegerConverter extends AbstractConverter
{
	/** Turn the Integer into a persistence string. */
	public String objectToString(Object integer)	{
		return (integer != null) ? integer.toString() : null;
	}

	/** Turn a persistence string into an Integer object. */
	public Object stringToObject(String spec)	{
		return (spec != null) ? Integer.valueOf(spec) : null;
	}

	public Class getGuiValueClass(Object component)	{
		return int.class;
	}

}
