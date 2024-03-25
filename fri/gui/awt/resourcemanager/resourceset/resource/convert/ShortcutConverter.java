package fri.gui.awt.resourcemanager.resourceset.resource.convert;

import java.awt.MenuShortcut;
import java.awt.event.InputEvent;
import fri.gui.awt.keyboard.KeyNames;

/**
	Encapsulates methods to convert (String - Object) a shortcut resource.
	Holds a static inner class that abstracts every accelerator key.
*/

public class ShortcutConverter extends AbstractConverter
{
	public static class KeyAndModifier
	{
		public final int keyCode;
		public final int modifiers;
		
		public KeyAndModifier(int keyCode, int modifiers)	{
			this.keyCode = keyCode;
			this.modifiers = modifiers;
		}
		
		public boolean equals(Object o)	{
			KeyAndModifier other = (KeyAndModifier)o;
			return other.keyCode == keyCode && other.modifiers == modifiers;
		}
	}
	
	
	/** Turn an accelerator into a persistence string. */
	public String objectToString(Object shortcut)	{
		if (shortcut == null)
			return null;
			
		if (shortcut instanceof KeyAndModifier)	{
			KeyAndModifier km = (KeyAndModifier)shortcut;
			return KeyNames.getInstance().getKeyName(km.keyCode, km.modifiers);
		}
		else	{
			MenuShortcut sc = (MenuShortcut) shortcut;
			int modifiers = InputEvent.CTRL_MASK | (sc.usesShiftModifier() ? InputEvent.SHIFT_MASK : 0);
			return KeyNames.getInstance().getKeyName(sc.getKey(), modifiers);
		}
	}

	/** Turn a persistence string into an accelerator. */
	public Object stringToObject(String spec)	{
		if (spec == null)
			return null;
		
		int keyCode = KeyNames.getInstance().getKeyCode(spec);
		int modifiers = KeyNames.getInstance().getKeyModifiers(spec);
		return new KeyAndModifier(keyCode, modifiers);
	}

	/** Returns a GUI accelerator (Swing KeyStroke or AWT MenuShortcut) or null. */
	public Object toGuiValue(Object value, Object component)	{
		KeyAndModifier km = (KeyAndModifier)value;
		if (km == null)
			return null;
		
		return new MenuShortcut(km.keyCode, (km.modifiers & InputEvent.SHIFT_MASK) != 0);
	}

	public Class getGuiValueClass(Object component)	{
		return MenuShortcut.class;
	}

}
