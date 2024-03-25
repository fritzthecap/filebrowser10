package fri.util.i18n;

import java.util.*;

/**
	This is NOT the factory for MulitLanguageStrings!
	It is a static representation of multiple languages. It provides a sorted
	list of languages with the current as first. If no language was set explicitely,
	it will be <i>Locale.getDefault().getDisplayName()</i>.
	<p>
	A Listener mechanism is provided that notifies clients when <i>setLanguage()</i> gets called,
	or when a language was added or removed.
*/

public abstract class MultiLanguage
{
	private static String language = Locale.getDefault().getDisplayLanguage();
	private static List languages = new ArrayList();	// unique list of all languages currently registered
	private static List lsnrs;
	static	{
		languages.add(language);
	}


	/**
		To be implemented by Objects that are interested in language change events.
		A ChangeListener can register using static <i>MultiLanguage.addChangeListener()</i> method.
	*/
	public interface ChangeListener
	{
		/** Called when <i>MultiLanguage.setLanguage()</i> was launched to set a new global language. */
		public void languageChanged(String newLanguage);
		
		/** Called when a new language was added. */
		public void languageAdded(String newLanguage);
		
		/** Called when a language was removed. */
		public void languageRemoved(String removedLanguage);
	}


	/** Returns the current language that controls the toString() method of all MultiLanguageString instances. */
	public static String getLanguage()	{
		return MultiLanguage.language;
	}

	/**
		Sets the current language that controls the toString() method of all MultiLanguageString instances.
		If the language is not contained it will be added. Notifies all registered ChangeListeners.
	*/
	public static void setLanguage(String language)	{
		language = ensureLanguageValue(language);
		if (language.equals(MultiLanguage.language))
			return;
		
		MultiLanguage.language = language;
		addLanguage(MultiLanguage.language);
		
		for (int i = 0; lsnrs != null && i < lsnrs.size(); i++)	// notify listeners
			((ChangeListener)lsnrs.get(i)).languageChanged(MultiLanguage.language);
	}

	/** Returns an array of all languages currently registered by all MultiLanguageStrings. */
	public static String [] getLanguages()	{
		String [] sarr = new String [MultiLanguage.languages.size()];
		MultiLanguage.languages.toArray(sarr);
		return sarr;
	}

	/**
		Sorts the passed language array and sets it as new global list. The locale language will be first.
		Duplicate and null items get removed. When the current language is not among the new languages,
		the first language is set as the new global language.
	*/
	private static void setLanguages(String [] newLanguages)	{
		// build list of new languages, remove nulls and duplicates
		ArrayList list = new ArrayList(newLanguages.length);
		for (int i = 0; i < newLanguages.length; i++)
			if (newLanguages[i] != null && list.indexOf(newLanguages[i]) < 0)
				list.add(newLanguages[i]);

		// sort list
		String [] sarr = new String[list.size()];
		list.toArray(sarr);
		Arrays.sort(sarr);
		MultiLanguage.languages = new ArrayList(sarr.length);
		for (int i = 0; i < sarr.length; i++)
			MultiLanguage.languages.add(sarr[i]);

		// put default language to top
		if (MultiLanguage.languages.indexOf(getLanguage()) >= 0)	{
			MultiLanguage.languages.remove(getLanguage());
			MultiLanguage.languages.add(0, getLanguage());
		}
		else	{	// default language not contained, put first to top
			setLanguage(MultiLanguage.languages.size() > 0 ? (String) MultiLanguage.languages.get(0) : Locale.getDefault().getDisplayLanguage());
		}
	}
	
	/**
		Adds a new language if it is not already contained.
	*/
	public static void addLanguage(String language)	{
		if (MultiLanguage.languages.indexOf(language) < 0)	{	// merge to global language list
			MultiLanguage.languages.add(language);
			setLanguages(getLanguages());	// sort

			for (int i = 0; lsnrs != null && i < lsnrs.size(); i++)	// notify listeners
				((ChangeListener)lsnrs.get(i)).languageAdded(language);
		}
	}

	/**
		Removes a language if it is contained.
		When the removed language is the current default, the first element gets the new default language.
	*/
	public static void removeLanguage(String language)	{
		if (MultiLanguage.languages.indexOf(language) >= 0)	{
			MultiLanguage.languages.remove(language);
			if (MultiLanguage.getLanguage().equals(language))
				setLanguage(MultiLanguage.languages.size() > 0 ? (String) MultiLanguage.languages.get(0) : Locale.getDefault().getDisplayLanguage());

			for (int i = 0; lsnrs != null && i < lsnrs.size(); i++)	// notify listeners
				((ChangeListener)lsnrs.get(i)).languageRemoved(language);
		}
	}
	
	/** Returns true if the passed language is in list of global languages. */
	public static boolean isLanguage(String l)	{
		return MultiLanguage.languages.indexOf(l) >= 0;
	}


	/** Add a language change listener. */
	public static void addChangeListener(ChangeListener lsnr)	{
		if (lsnrs == null)
			lsnrs = new ArrayList();
		lsnrs.add(lsnr);
	}

	/** Remove a language switch listener. */
	public static void removeChangeListener(ChangeListener lsnr)	{
		if (lsnrs != null)
			lsnrs.remove(lsnr);
	}


	static String ensureLanguageValue(String l)	{
		return l == null ? MultiLanguage.getLanguage() : l;
	}


	private MultiLanguage()	{}
}
