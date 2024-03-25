package fri.util;

import java.util.*;

/**
 * Provides mapping from a language (Locale or ISO abbreviation)
 * to an encoding string that can be used in XML declarations.
 * 
 * Created on 19.03.2005
 * @author Fritz Ritzberger
 */
public abstract class LocaleToEncoding
{
	private static final Map languageToEncoding = new Hashtable();

	/** Returns the ISO encoding string for passed Locale. If not found, null is returned. */ 
	public static String encoding(Locale loc)	{
		return encoding(loc.getLanguage());
	}
	
	/** Returns the ISO encoding string for passed language. If not found, null is returned. */ 
	public static String encoding(String language)	{
		if (language == null || language.length() <= 0)
			throw new IllegalArgumentException("Language string is not defined: >"+language+"<");
			
		String s = (String) languageToEncoding.get(language);
		return s; 
	}
	
	/** Returns an estimated Locale from matching the passed ISO-encoding to a language, or null if not found. */
	public static Locale locale(String encoding)	{
		encoding = encoding.toUpperCase();
		for (Iterator it = languageToEncoding.entrySet().iterator(); it.hasNext(); )	{
			Map.Entry e = (Map.Entry) it.next();
			if (e.getValue().equals(encoding))
				return new Locale((String) e.getKey());
		}
		return null;
	}

	static	{
		languageToEncoding.put("ar", "ISO-8859-6");	// Arabic
		languageToEncoding.put("bu", "ISO-8859-5");	// Bulgarian
		languageToEncoding.put("bg", "ISO-8859-5");	// Bulgarian
		languageToEncoding.put("by", "ISO-8859-5");	// Byelorussian
		languageToEncoding.put("be", "ISO-8859-5");	// Byelorussian
		languageToEncoding.put("cr", "ISO-8859-2");	// Croatian
		languageToEncoding.put("hr", "ISO-8859-2");	// Croatian
		languageToEncoding.put("cy", "ISO-8859-5");	// Cyrillic
		languageToEncoding.put("cz", "ISO-8859-2");	// Czech
		languageToEncoding.put("cs", "ISO-8859-2");	// Czech
		languageToEncoding.put("de", "ISO-8859-1");	// German
		languageToEncoding.put("et", "ISO-8859-15");	// Estonian*
		languageToEncoding.put("gr", "ISO-8859-7");	// Greek
		languageToEncoding.put("el", "ISO-8859-7");	// Greek
		languageToEncoding.put("en", "ISO-8859-1");	// English
		languageToEncoding.put("fr", "ISO-8859-1");	// French
		languageToEncoding.put("kl", "ISO-8859-4");	// Greenlandic*
		languageToEncoding.put("he", "ISO-8859-8");	// Hebrew
		languageToEncoding.put("iw", "ISO-8859-8");	// Hebrew 
		languageToEncoding.put("af", "ISO-8859-2");	// Hungarian
		languageToEncoding.put("ja", "ISO-2022-jp");	// Japanese
		languageToEncoding.put("la", "ISO-8859-13");	// Latvian
		languageToEncoding.put("lv", "ISO-8859-13");	// Latvian
		languageToEncoding.put("li", "ISO-8859-13");	// Lithuanian
		languageToEncoding.put("lt", "ISO-8859-13");	// Lithuanian
		languageToEncoding.put("ma", "ISO-8859-5");	// Macedonian
		languageToEncoding.put("mk", "ISO-8859-5");	// Macedonian
		languageToEncoding.put("mt", "ISO-8859-3");	// Maltese*
		languageToEncoding.put("po", "ISO-8859-2");	// Polish
		languageToEncoding.put("pl", "ISO-8859-2");	// Polish
		languageToEncoding.put("ro", "ISO-8859-2");	// Romanian
		languageToEncoding.put("ru", "ISO-8859-5");	// Russian
		languageToEncoding.put("se", "ISO-8859-2");	// Serbian
		languageToEncoding.put("sr", "ISO-8859-2");	// Serbian
		languageToEncoding.put("sl", "ISO-8859-2");	// Slovenian
		languageToEncoding.put("sk", "ISO-8859-2");	// Slovak
		languageToEncoding.put("tu", "ISO-8859-9");	// Turkish
		languageToEncoding.put("tr", "ISO-8859-9");	// Turkish
		languageToEncoding.put("uk", "ISO-8859-5");	// Ukrainian
		languageToEncoding.put("ji", "ISO-8859-8");	// Yiddish 
		languageToEncoding.put("yi", "ISO-8859-8");	// Yiddish 
	}

}
