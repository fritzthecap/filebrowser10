package fri.gui.awt.resourcemanager.resourceset.resource.convert;

import java.awt.Color;

/**
	Encapsulates methods to convert (String - Object) a color resource.
*/

public class ColorConverter extends AbstractConverter
{
	/** Turn the color into a persistence string. */
	public String objectToString(Object value)	{
		if (value == null)
			return null;

		Color c = (Color)value;
		String s = Integer.toHexString(c.getRGB() & 0x00FFFFFF);
		while (s.length() < 6)
			s = "0"+s;

		return "#"+s;
	}

	/** Turn a persistence string into a color. */
	public Object stringToObject(String spec)	{
		if (spec == null)
			return null;
			
		try	{
			return Color.decode(spec);
		}
		catch (NumberFormatException e)	{
			System.err.println("ERROR: Invalid color: "+spec);
			return null;
		}
	}

	public Class getGuiValueClass(Object component)	{
		return Color.class;
	}

}