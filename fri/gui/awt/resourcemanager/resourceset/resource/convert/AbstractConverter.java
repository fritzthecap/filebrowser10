package fri.gui.awt.resourcemanager.resourceset.resource.convert;

/**
	Base class of all Converters, providing 1:1 mapping of the Resource value to a GUI-value.
*/

public abstract class AbstractConverter implements
	Converter
{
	/**
		Returns the value as the GUI-value.
		@param value resource value, can be complex, contains GUI value
		@return the GUI resource value like Font, Color, Border, ...
	*/
	public Object toGuiValue(Object value, Object component)	{
		return value;
	}

}
