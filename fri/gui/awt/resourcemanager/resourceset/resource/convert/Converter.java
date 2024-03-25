package fri.gui.awt.resourcemanager.resourceset.resource.convert;

/**
	Implementers convert between different representations of resource values.
	This allows a generic treatment of resources in the abstract Resource base class.<br>
	Three representations of Resources are needed:
	<ul>
		<li>Persistence String, written to and read from resource property file</li>
		<li>A simple GUI value like Font, Color, Border, Icon, MenuShortcut or text String</li>
		<li>A more complex structure than the GUI value, like a TitledBorder supporting multiple languages,
			or an accelerator for both AWT and Swing. This struct always contains the (simple) GUI value.
			These complex objects are implemented a inner classes of the concrete Converter class.</li>
	</ul>
*/

public interface Converter
{
	/**
		Returns a resource value for a persistence string specification.
		This will be e.g. BorderConverter.BorderAndTitle, or Font, Color, ...
	*/
	public Object stringToObject(String spec);

	/**
		Returns a persistence String specification for both a simple or complex resource value.
		@param value e.g. BorderConverter.BorderAndTitle.
	*/
	public String objectToString(Object value);

	/**
		Returns a GUI representation of the passed resource value, according to the GUI of passed component.
		@param value resource value, can be complex, contains GUI value
		@param component the Component or MenuComponent that will receive the GUI value
		@return the GUI resource value like Font, Color, Border, ...
	*/
	public Object toGuiValue(Object value, Object component);

	/**
		Returns the class of the argument for the setter method of the GUI value, as its argument could be null,
		which means that the argument class for reflection can not be constructed.
		@param component the Component or MenuComponent that will receive the GUI value
		@return the argument class for the setter method of this GUI property (Font, Color, Border, ...)
	*/
	public Class getGuiValueClass(Object component);

}
