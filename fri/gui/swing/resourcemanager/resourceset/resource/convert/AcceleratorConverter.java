package fri.gui.swing.resourcemanager.resourceset.resource.convert;

import javax.swing.KeyStroke;
import fri.gui.awt.keyboard.KeyNames;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.ShortcutConverter;

/**
	Encapsulates methods to convert (String - Object) an Accelerator resource.
*/

public class AcceleratorConverter extends ShortcutConverter
{
	/** Turn an accelerator into a persistence string. */
	public String objectToString(Object accelerator)	{
		if (accelerator instanceof KeyStroke)	{
			KeyStroke ks = (KeyStroke) accelerator;
			int keyCode = ks.getKeyCode();
			int modifiers = ks.getModifiers();
			return KeyNames.getInstance().getKeyName(keyCode, modifiers);
		}
		else	{
			return super.objectToString(accelerator);
		}
	}


	/** Returns a GUI accelerator (Swing KeyStroke or AWT MenuShortcut) or null. */
	public Object toGuiValue(Object value, Object component)	{
		KeyAndModifier km = (KeyAndModifier)value;
		return km == null ? null : KeyStroke.getKeyStroke(km.keyCode, km.modifiers);
	}

	public Class getGuiValueClass(Object component)	{
		return KeyStroke.class;
	}

}
