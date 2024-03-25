package fri.util.i18n;

import java.io.Serializable;
import java.util.*;
import fri.util.Equals;

/**
	Text in multiple languages. The toString() method returns the translation
	of currently set language. This List is sorted by language name (which is a
	displayable language name).
	It allows empty Strings for both language and text, but substitutes
	null language by <i>Locale.getDefault().getDisplayLanguage()</i> and null text
	by empty String, so this class will never return null for a translation or from
	its toString() method!
*/

public class MultiLanguageString implements Serializable, Cloneable
{
	private ArrayList tuples;
	
	public MultiLanguageString()	{
	}

	/** Creates a MultiLanguageString with <i>Locale.getDefault().getDisplayName()</i>. */
	public MultiLanguageString(String text)	{
		this(null, text);
	}

	/** Creates a MultiLanguageString with passed language. */
	public MultiLanguageString(String language, String text)	{
		setTranslation(language, text);
	}

	public MultiLanguageString(Map map)	{
		Set set = map.entrySet();
		for (Iterator it = set.iterator(); it.hasNext(); )	{
			Map.Entry entry = (Map.Entry) it.next();
			Object o = entry.getValue();
			setTranslation(entry.getKey().toString(), o != null ? o.toString() : "");
		}
	}


	/** Returns true if no translation is contained within this multilanguage string. */
	public boolean isEmpty()	{
		for (int i = 0; i < size(); i++)	{
			Tuple t = (Tuple) tuples.get(i);
			String s = t.toString();
			if (s.length() > 0)
				return false;
		}
		return true;
	}
	
	/** Returns the translation of the passed language (if it is a global language!). */
	public String getTranslation(String language)	{
		if (MultiLanguage.isLanguage(language))	{
			for (int i = 0; tuples != null && i < tuples.size(); i++)	{
				Tuple t = (Tuple) tuples.get(i);
				if (t.getLanguage().equals(language))
					return t.toString();
			}
		}
		// else removeTranslation(language);	// never loose a translation, this is a lot of work!
		return ensureStringValue(null);
	}

	/** Adds or updates the passed translation. The language implicitely gets added to global MultiLanguage.languages. */
	public void setTranslation(String language, String text)	{
		language = MultiLanguage.ensureLanguageValue(language);
		text = ensureStringValue(text);
		MultiLanguage.addLanguage(language);
		
		if (tuples == null)
			tuples = new ArrayList();

		boolean ok = false;
		for (int i = 0; ok == false && i < tuples.size(); i++)	{
			Tuple t = (Tuple) tuples.get(i);

			if (t.getLanguage().equals(language))	{	// update found element
				t.text = text;
				return;
			}

			if (t.getLanguage().compareTo(language) > 0)	{	// add to this position
				tuples.add(i, new Tuple(language, text));
				return;
			}
		}
		
		tuples.add(new Tuple(language, text));
	}

	/** Returns the translation Tuple of passed index. This is for iteration. */
	public Tuple get(int i)	{
		return (Tuple) tuples.get(i);
	}
	
	/** Returns the number of languages stored within this MultiLanguageString. This is for iteration. */
	public int size()	{
		return tuples != null ? tuples.size() : 0;
	}
	
	/** Removes a translation from this MultiLanguageString. */
	public void removeTranslation(String language)	{
		for (int i = 0; tuples != null && i < tuples.size(); i++)	{
			Tuple t = (Tuple) tuples.get(i);
			
			if (t.getLanguage().equals(language))	{
				tuples.remove(t);
				return;
			}
		}
	}

	/** Returns the text of currently set language (MultiLanguage.getLanguage()). */
	public String toString()	{
		return getTranslation(MultiLanguage.getLanguage());
	}

	/** Returns the toString() result from internal list of translations. */
	public String internalToString()	{
		return tuples != null ? tuples.toString() : "";
	}

	/** Returns true if the passed object is MultiLanguageStrng and has the same Tuples as this object. */
	public boolean equals(Object o)	{
		if (o instanceof MultiLanguageString == false)
			return false;
		MultiLanguageString other = (MultiLanguageString)o;
		return Equals.equals(other.tuples, tuples);
	}

	/** Returns the hashcode of contained tuples list. */
	public int hashCode()	{
		return tuples == null ? 0 : tuples.hashCode();
	}
	
	/** Returns a "deep" clone of this MultiLanguageString (all contained Tuples get copied). */
	public Object clone()	{
		MultiLanguageString mls = new MultiLanguageString();
		for (int i = 0; tuples != null && i < tuples.size(); i++)	{
			Tuple t = (Tuple) tuples.get(i);
			mls.setTranslation(t.getLanguage(), t.toString());	// a Tuple clone gets constructed
		}
		return mls;
	}	

	private static String ensureStringValue(String s)	{
		return s == null ? "" : s;
	}

	/** Print internal array for debug purposes. */
	public String dump()	{
		return tuples.toString();
	}



	/**
		A tuple of language and the String's representation according to that language.
	*/
	public static class Tuple implements Serializable
	{
		private String language, text;
		
		public Tuple(String language, String text)	{
			this.language = MultiLanguage.ensureLanguageValue(language);
			this.text = ensureStringValue(text);
		}
		
		/** Returns the language of text. */
		public String getLanguage()	{
			return language;
		}

		/** Returns the text according to language. */
		public String toString()	{
			return text;
		}

		/** Tests passed Object for equality if it is a Tuple or a String. */
		public boolean equals(Object o)	{
			if (o instanceof Tuple)	{
				Tuple t = (Tuple)o;
				return getLanguage().equals(t.getLanguage()) && toString().equals(t.toString());
			}
			else
			if (o instanceof String)	{
				return o.equals(text);
			}
			return false;
		}

		/** Returns the hashCode of contained text String. */
		public int hashCode()	{
			return toString().hashCode();
		}

	}

}
