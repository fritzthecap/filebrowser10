package fri.gui.swing.resourcemanager.resourceset.resource.convert;

import fri.gui.awt.resourcemanager.resourceset.resource.convert.AbstractConverter;

/**
	Encapsulates methods to convert (String - Integer) a mnemonic resource.
*/

public class MnemonicConverter extends AbstractConverter
{
	/** Turn the Character into a persistence string. */
	public String objectToString(Object character)	{
		return (character != null && character.equals(Integer.valueOf(0)) == false) ? Character.valueOf((char) ((Integer)character).intValue()).toString() : null;
	}

	/** Turn a persistence string into an Character object. */
	public Object stringToObject(String spec)	{
		return (spec != null && spec.length() == 1) ? Integer.valueOf(spec.charAt(0)) : null;
	}

	public Class getGuiValueClass(Object component)	{
		return int.class;
	}

}
