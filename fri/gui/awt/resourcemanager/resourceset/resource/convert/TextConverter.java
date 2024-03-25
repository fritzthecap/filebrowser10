package fri.gui.awt.resourcemanager.resourceset.resource.convert;

import java.util.*;
import fri.util.i18n.*;
import fri.util.props.NestableProperties;

/**
	Encapsulates methods to convert (String - Object) a multilanguage text resource.
*/

public class TextConverter extends AbstractConverter
{
	/** Turn the (multilanguage or any) text into a persistence string. */
	public String objectToString(Object text)	{
		if (text == null)
			return null;
			
		Properties np = new NestableProperties();
		if (text instanceof MultiLanguageString)	{
			MultiLanguageString mls = (MultiLanguageString)text;
			for (int i = 0; i < mls.size(); i++)	{
				MultiLanguageString.Tuple t = (MultiLanguageString.Tuple) mls.get(i);
				if (t.toString() != null)
					np.setProperty(t.getLanguage(), t.toString());
			}
		}
		else	{
			String s = (String) text;
			if (s.length() > 0)
				np.setProperty(MultiLanguage.getLanguage(), s);
		}
		return np.size() > 0 ? np.toString() : null;
	}

	/** Turn a persistence string into a multilanguage text. */
	public Object stringToObject(String spec)	{
		if (spec == null || spec.length() <= 0)
			return null;
		
		MultiLanguageString mls = new MultiLanguageString();
		Properties p = new NestableProperties(spec);
		for (Enumeration e = p.propertyNames(); e.hasMoreElements(); )	{
			String l = (String) e.nextElement();
			String t = p.getProperty(l);
			mls.setTranslation(l, t);
		}
		return mls;	//mls.size() <= 0 ? null : mls;	// workaround for icon replacing text
	}

	/** Returns "" when null, or toString() of passed value. */
	public Object toGuiValue(Object value, Object component)	{
		return (value == null) ? null : value.toString();
	}

	public Class getGuiValueClass(Object component)	{
		return String.class;
	}

}
