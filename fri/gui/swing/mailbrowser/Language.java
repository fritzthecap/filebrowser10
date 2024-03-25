package fri.gui.swing.mailbrowser;

import java.util.*;

public class Language
{
	private static ResourceBundle languageBundle;

	static	{
		String cls = Language.class.getName();
		String pkg = cls.substring(0, cls.lastIndexOf("."));
		try	{
			languageBundle = ResourceBundle.getBundle(pkg+".strings");
		}
		catch (MissingResourceException e)	{
			e.printStackTrace();
		}
		
		if (languageBundle == null)
			languageBundle = ResourceBundle.getBundle(pkg+".strings", Locale.ENGLISH);
	}

	
	public static String get(String text)	{
		text = text.replace(' ', '_');
		text = text.replace('.', '_');
		text = text.replace('-', '_');
		text = text.replace(',', '_');
		return languageBundle.getString(text);
	}

	private Language()	{}
}
