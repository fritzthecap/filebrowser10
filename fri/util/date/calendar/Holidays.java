package fri.util.date.calendar;

import java.util.*;
import java.lang.ref.SoftReference;
import java.io.File;
import java.text.DateFormat;
import fri.util.sort.quick.*;
import fri.util.text.Replace;

/**
	Computes all holidays for an input year.
	The resulting Hashtable holds key=Calendar, value=name of holiday.
	<p>
	This class is controlled by a ResourceBundle
	that provides month/day of each holiday in a local way:
	<b>fri/util/time/calendar/Holidays_de_AT.properties</b>
	<p>
	If easter should NOT be computed, there must be an entry
	"easter = false". All other entries are considered to be (locally) like:
	<pre>
		# example for Locale.GERMAN
		Tag_der_Arbeit = 1.5.
		
		# switch off easter calculation
		easter = false
	</pre>
	Each "_" in the holiday name will be replaced by one space.
*/

public class Holidays extends Hashtable
{
	private static Hashtable yearCache = new Hashtable();
	private static Hashtable holidayCache = new Hashtable();
	private static Hashtable easterNamesCache = new Hashtable();

	protected int year;
	private String stringRepresentation;
	
	private boolean holidayLenient = false;
	private int holidayFirstDayOfWeek = 0;
	private int holidayMinimalDaysInFirstWeek = 0;

	private String easterMonday;
	private String ascensionOfChrist;
	private String whitMonday;
	private String corpusChristiDay;
	private boolean doEaster = true;


	
	/**
		Returns a Hashtable containing key=Date, value=Holiday-Name
		with all holidays for the current year.
	*/
	public static Holidays getHolidays()	{
		return getHolidays(Calendar.getInstance().get(Calendar.YEAR));
	}
	
	/**
		Returns a Hashtable containing key=Date, value=Holiday-Name
		with all holidays for the passed year.
	*/
	public static Holidays getHolidays(int year)	{
		Integer y = new Integer(year);
		SoftReference sr = (SoftReference)yearCache.get(y);
		if (sr != null)	{
			Holidays h = (Holidays)sr.get();
			if (h != null)
				return h;
		}
			
		Holidays h = new Holidays(year);
		sr = new SoftReference(h);
		yearCache.put(y, sr);
		
		return h;
	}

	/**
		Overridden to be able to look for getTime() millis only when hashing.
		Lenient, first day of week and minimal days in first week should not
		be of importance.
	*/
	public Object put(Object key, Object value)	{
		Calendar c = (Calendar)key;
		// make key equal for retrieval
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.setLenient(holidayLenient);
		c.setFirstDayOfWeek(holidayFirstDayOfWeek);
		c.setMinimalDaysInFirstWeek(holidayMinimalDaysInFirstWeek);
		
		return super.put(c, value);
	}

	/**
		Overridden to be able to look for getTime() millis only when hashing.
		Lenient, first day of week and minimal days in first week should not
		be of importance.
	*/
	public Object get(Object key)	{
		Calendar c = (Calendar)key;
		c = (Calendar)c.clone();
		// make key equal for retrieval
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.setLenient(holidayLenient);
		c.setFirstDayOfWeek(holidayFirstDayOfWeek);
		c.setMinimalDaysInFirstWeek(holidayMinimalDaysInFirstWeek);
		
		return super.get(c);
	}


	/**
		Constructor that calls init().
	*/
	protected Holidays(int year)	{
		this.year = year;
		init();
	}

	/**
		Tries to read locale resource bundle for fixed holidays.
		If
	*/
	protected void init()	{
		try	{
			parseResourceBundle();
		}
		catch (MissingResourceException e)	{
			//e.printStackTrace();
			System.err.println("Please create "+getClass().getName().replace('.', File.separatorChar)+"_"+Locale.getDefault()+".properties somewhere in CLASSPATH.");
			System.err.println("Entries must be like (in locale order):");
			System.err.println("	Tag_der_Arbeit = 1.5.");
		}
		
		if (doEaster)	{	// default true, can be denied in holiday resource bundle
			try	{
				doEasterHolidays();
			}
			catch (IllegalArgumentException e)	{	// Year is above 2299
				System.err.println("Gauss did not agree: "+e.getMessage());
			}
		}
	}
	
	protected ResourceBundle createEasterNames()	{
		return ResourceBundle.getBundle(EasterSunday.class.getName());
	}

	// get all easter holiday names from property file
	private void getEasterNames()	{
		try	{
			ResourceBundle easterNames;
			if ((easterNames = (ResourceBundle)easterNamesCache.get(Locale.getDefault()+"_easter")) == null)	{
				easterNames = createEasterNames();
				easterNamesCache.put(Locale.getDefault()+"_easter", easterNames);
			}
			
			easterMonday = easterNames.getString("easter_monday");	// Ostermontag
			ascensionOfChrist = easterNames.getString("ascension_of_christ");	// Christi Himmelfahrt
			whitMonday = easterNames.getString("whit_monday");	// Pfingsten
			corpusChristiDay = easterNames.getString("corpus_christi_day");	// Fronleichnam
			//System.err.println("Have got easter: "+easterMonday+", "+ascensionOfChrist+", "+whitMonday+", "+corpusChristiDay);
		}
		catch (MissingResourceException e)	{
			System.err.println("Please create "+EasterSunday.class.getName().replace('.', File.separatorChar)+"_"+Locale.getDefault()+".properties somewhere in CLASSPATH.");
			System.err.println("Following names will be picked for easter holidays:");
			System.err.println("	easter_monday = ...");	// Ostermontag
			System.err.println("	ascension_of_christ = ...");	// Christi Himmelfahrt
			System.err.println("	whit_monday = ...");	// Pfingsten
			System.err.println("	corpus_christi_day = ...");	// Fronleichnam
			
			easterNamesCache.put(Locale.getDefault()+"_easter", new EmptyResourceBundle());
		}
	}
	

	/**
		Override this to return a specific variant of ResourceBundle,
		as in Germany there are different holidays for different regions.
		This default implementation returns <i>ResourceBundle.getBundle(getClass().getName());</i>
	*/
	protected ResourceBundle createHolidayResourceBundle()	{
		return ResourceBundle.getBundle(getClass().getName());
	}


	/**
		Parses the created resource bundle and stores a Date/Name entry
		for each resource into this hashtable.
	*/
	private void parseResourceBundle()
		throws MissingResourceException
	{
		ResourceBundle days;
		if ((days = (ResourceBundle)holidayCache.get(Locale.getDefault()+"_fixed")) == null)	{
			try	{
				days = createHolidayResourceBundle();
			}
			catch (MissingResourceException e)	{
				holidayCache.put(Locale.getDefault()+"_fixed", new EmptyResourceBundle());
				throw e;
			}
			holidayCache.put(Locale.getDefault()+"_fixed", days);
		}

		Calendar c = Calendar.getInstance();

		for (Enumeration e = days.getKeys(); e.hasMoreElements(); )	{
			String key = (String)e.nextElement();
			String value = days.getString(key);
			
			if (key.toLowerCase().equals("easter"))	{
				if (value.toLowerCase().equals("false"))	{
					doEaster = false;
				}
				continue;
			}

			/* FRi 2002-04-10: As WINDOWS enables individual date formats and separators (not language bound),
			 * the date CAN NOT BE written locally into Holiday_XX.properties.
			 *
			int monthPos = DateUtil.getPatternPosition(Calendar.MONTH);
			int dayPos = DateUtil.getPatternPosition(Calendar.DAY_OF_MONTH);
			char separator = DateUtil.getDateSeparator();
			 */

			int dayPos = 0;
			int monthPos = 1;
			char separator = '.';		
			
			StringTokenizer stok = new StringTokenizer(value, " \t"+separator);
			
			int month = 0, day = 0;
			
			int i = Integer.valueOf(stok.nextToken()).intValue();
			if (monthPos < dayPos)
				month = i;
			else
				day = i;

			i = Integer.valueOf(stok.nextToken()).intValue();
			if (monthPos < dayPos)
				day = i;
			else
				month = i;
				
			c.set(year, month - 1, day, 0, 0, 0);
			
			put(c.clone(), Replace.replace(key, "_", " "));
		}
	}
	

	
	/**
		Computes and stores variable easter holidays, german names.
	*/
	protected void doEasterHolidays()	{
		getEasterNames();
		if (easterMonday == null && ascensionOfChrist == null && whitMonday == null && corpusChristiDay == null)	{
			return;
		}

		int [] arr = EasterSunday.date(year);
		arr[1] = arr[1] - 1;	// Calendar month is 0-11
		
		Calendar c = Calendar.getInstance();
		
		if (easterMonday != null)	{
			c.set(arr[0], arr[1], arr[2]);
			c.add(Calendar.DATE, +1);
			put(c.clone(), easterMonday);
		}

		if (ascensionOfChrist != null)	{
			c.set(arr[0], arr[1], arr[2]);
			c.add(Calendar.DATE, +39);
			put(c.clone(), ascensionOfChrist);
		}
		
		if (whitMonday != null)	{
			c.set(arr[0], arr[1], arr[2]);
			c.add(Calendar.DATE, +50);
			put(c.clone(), whitMonday);
		}
		
		if (corpusChristiDay != null)	{
			c.set(arr[0], arr[1], arr[2]);
			c.add(Calendar.DATE, +60);
			put(c.clone(), corpusChristiDay);
		}
	}
	

	
	/** Returns the year passed to factory or the current year. */
	public int getYear()	{
		return year;
	}
	
	/** Returns a sorted, newline-sepatarated list of holidays. */
	public String toString()	{
		if (stringRepresentation != null)	{
			return stringRepresentation;
		}
		
		Vector v = new Vector();
		
		for (Enumeration e = keys(); e.hasMoreElements(); )	{
			v.add(e.nextElement());
		}
		
		v = new QSort().sort(v);
		
		StringBuffer sb = new StringBuffer();
		DateFormat fmt = DateFormat.getDateInstance(DateFormat.FULL);
		
		for (Enumeration e = v.elements(); e.hasMoreElements(); )	{
			Calendar c = (Calendar)e.nextElement();
			String name = (String)get(c);

			boolean isSunday = (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
			
			if (isSunday)
				sb.append("( ");
				
			sb.append(name);
			sb.append(": ");
			sb.append(fmt.format(c.getTime()));
			
			if (isSunday)
				sb.append(" )");
				
			sb.append("\n");
		}
		
		return stringRepresentation = sb.toString();
	}
	

	
	class EmptyResourceBundle extends ListResourceBundle
	{
		protected Object[][] getContents()	{
			return new Object [0][0];
		}

//		public Enumeration getKeys()	{
//			return new Enumeration()	{
//				public boolean hasMoreElements()	{
//					return false;
//				}
//				public Object nextElement()	{
//					return null;
//				}
//			};
//		}
		
	}
	
	

	// test main
	
	public static void main(String [] args)	{
		//Locale.setDefault(Locale.UK);
		
		Vector v = new Vector();
		
		if (args.length <= 0)	{
			v.add(Holidays.getHolidays());
		}
		else	{
			for (int i = 0; i < args.length; i++)	{
				int y = Integer.valueOf(args[i]).intValue();
				v.add(Holidays.getHolidays(y));
			}
		}
		
		for (Enumeration e = v.elements(); e.hasMoreElements(); )	{
			Holidays holidays = (Holidays)e.nextElement();
			
			System.err.println("Holidays for "+holidays.getYear()+" in Locale "+Locale.getDefault()+", "+Locale.getDefault().getDisplayName()+" are:");
			System.err.println("===========================================================");
			System.err.println(holidays);
		}
	}

}


//	/**
//		Stores (hardcoded) fixed holidays for Austria, german names.
//	*/
//	protected void doFixedHolidays()	{
//		if (Locale.getDefault().equals("de_AT") == false)	{
//			return;
//		}
//		
//		Calendar c = Calendar.getInstance();
//		
//		c.set(year, 0, 1);	// 1. Jaenner, fix
//		put(c.clone(), "Neujahr");
//
//		c.set(year, 0, 6);	// 6. Jaenner, fix
//		put(c.clone(), "Dreikönig");
//		
//		c.set(year, 4, 1);	// 1. Mai, fix
//		put(c.clone(), "Tag der Arbeit");
//		
//		c.set(year, 7, 15);	// 15. August, fix
//		put(c.clone(), "Mariä Himmelfahrt");
//
//		c.set(year, 9, 26);	// 1. Oktober, fix
//		put(c.clone(), "Nationalfeiertag");
//
//		c.set(year, 10, 1);	// 1. November, fix
//		put(c.clone(), "Allerheiligen");
//
//		c.set(year, 11, 8);	// 8. Dezember, fix
//		put(c.clone(), "Mariä Empfängnis");
//
//		c.set(year, 11, 25);	// 25. Dezember, fix
//		put(c.clone(), "Christtag");
//
//		c.set(year, 11, 26);	// 26. Dezember, fix
//		put(c.clone(), "Stefanitag");
//	}