package fri.util.date;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import fri.util.text.format.FormatPatternInterpreter;
import fri.util.text.format.FormatPatternSemantic;
	
/**
	Methods in conjunction with Date data type.
*/

public abstract class DateUtil
{
	/** Returns an ISO representation of passed Calendar: "2008-05-03,09:03:04:012". */
	public static String toIsoString(Calendar c)	{
		StringBuffer sb = new StringBuffer();
		fillWithZeroAndAppend(c.get(Calendar.YEAR), sb, 4);
		sb.append('-');
		fillWithZeroAndAppend(c.get(Calendar.MONTH) + 1, sb, 2);
		sb.append('-');
		fillWithZeroAndAppend(c.get(Calendar.DAY_OF_MONTH), sb, 2);
		sb.append(',');
		fillWithZeroAndAppend(c.get(Calendar.HOUR_OF_DAY), sb, 2);
		sb.append(':');
		fillWithZeroAndAppend(c.get(Calendar.MINUTE), sb, 2);
		sb.append(':');
		fillWithZeroAndAppend(c.get(Calendar.SECOND), sb, 2);
		sb.append(':');
		fillWithZeroAndAppend(c.get(Calendar.MILLISECOND), sb, 3);
		return sb.toString();
	}
	
	private static void fillWithZeroAndAppend(int number, StringBuffer sb, int width)	{
		if (width >= 2 && number < 10)
			sb.append('0');
		if (width >= 3 && number < 100)
			sb.append('0');
		if (width >= 4 && number < 1000)
			sb.append('0');
		sb.append(number);
	}
	
	/**
		Returns the local time mask which can be used to determine
		the order of hour-, minute- and seconds-textfields with passed date type.
	*/
	public static String getTimePattern(int timeType)	{
		return getDateTimePattern(-1, timeType);
	}

	/**
		Returns the local date mask which can be used to determine
		the order of year-, month-, day-textfields with passed date type.
	*/
	public static String getDatePattern(int dateType)	{
		return getDateTimePattern(dateType, -1);
	}

	/**
		Returns the local date/time mask which can be used to determine
		the order of textfields with passed date and time types.
	*/
	public static String getDateTimePattern(int dateType, int timeType)	{
		DateFormat df = dateType < 0 ? DateFormat.getTimeInstance(timeType) :
				timeType < 0 ? DateFormat.getDateInstance(dateType) :
				DateFormat.getDateTimeInstance(dateType, timeType);
				
		try	{
			SimpleDateFormat sdf = (SimpleDateFormat)df;
			return sdf.toPattern();
		}
		catch (ClassCastException e)	{
			e.printStackTrace();
			return "yMdhms";	// default order for unknown masks
		}
	}
	
	
	/**
		Returns true if the passed year is a leap year with 29 days in february.
	*/
	public static boolean isLeapYear(int year)	{
		return((year % 4 == 0 && year % 100 != 0) || year % 400 == 0);
	}


	/**
		Returns String array of all month names in currently set Locale.
		@param shortNames if true: "Oct", if false: "October"
	*/
	public static String [] getMonthDisplayNames(boolean shortNames)	{
		DateFormatSymbols dfs = new DateFormatSymbols();
		String [] sarr;
		if (shortNames)	{
			sarr = dfs.getShortMonths();
		}
		else	{
			sarr = dfs.getMonths();
		}
		
		// These contain 13 names, last is ""
		String [] ret = new String[12];
		for (int i = 0; i < 12; i++)
			ret[i] = sarr[i];
			
		return ret;
	}


	/**
		Returns String array of all day names beginning with first day of week
		of currently set Locale.
		@param shortNames if true: "Sat", if false: "Saturday"
	*/
	public static String [] getDayDisplayNames(boolean shortNames)	{
		return getDayDisplayNames(shortNames, -1);
	}

	/**
		Returns String array of all day names, starting with given first day of week.
		@param shortNames if true: "Sat", if false: "Saturday"
		@param firstDayOfWeek 1-7, one of Calendar.SUNDAY (1), Calendar.MONDAY (2), ...
	*/
	public static String [] getDayDisplayNames(boolean shortNames, int firstDayOfWeek)	{
		DateFormatSymbols dfs = new DateFormatSymbols();
		String [] sarr;
		if (shortNames)	{
			sarr = dfs.getShortWeekdays();
		}
		else	{
			sarr = dfs.getWeekdays();
		}

		if (firstDayOfWeek < 0)	{
			firstDayOfWeek = Calendar.getInstance().getFirstDayOfWeek();
		}

		// These contain 8 names, first is ""
		String [] ret = new String [7];

		for (int i = firstDayOfWeek; i < firstDayOfWeek + 7; i++)	{
			int idx = i % 7;
			String s = idx == 0 ? sarr[7] : sarr[idx];	// day zero is invalid ","
			ret[i - firstDayOfWeek] = s;
			//System.err.println("day name "+(i - firstDayOfWeek)+" is "+ret[i - firstDayOfWeek]);
		}

		return ret;
	}

	/** Returns true if year is left of month in current set Locale. */
	public static boolean isYearLeftOfMonth()	{
		String pat = DateUtil.getDateTimePattern(DateFormat.SHORT, DateFormat.LONG);
		return DateUtil.getPatternPosition(Calendar.YEAR, pat) < DateUtil.getPatternPosition(Calendar.MONTH, pat);
	}

	/**
		Returns the localized position (0-n) of all of the fields for
		DateFormat.FULL.
		@param calendarSymbol one of DateFormat.SHORT, DateFormat.MEDIUM, ...
		@return position of the pattern within the format mask,
			usable for local order of textfields.
	*/
	public static int getPatternPosition(int calendarSymbol, int dateType, int timeType)	{
		return getPatternPosition(
				charForSymbol(calendarSymbol),
				getDateTimePattern(dateType, timeType));
	}

	/**
		Returns the localized position (0-n) of one of the fields for
		DateFormat.SHORT, DateFormat.MEDIUM and DateFormat.LONG:
		<ul>
			<li>year: Calendar.YEAR</li>
			<li>month: Calendar.MONTH</li>
			<li>day: Calendar.DAY_OF_MONTH</li>
			<li>hour: Calendar.HOUR_OF_DAY (or HOUR)</li>
			<li>minute: Calendar.MINUTE</li>
			<li>second: Calendar.SECOND</li>
		</ul>
		@param calendarSymbol one of DateFormat.SHORT, DateFormat.MEDIUM, ...
		@return position of the pattern within the format mask,
			usable for local order of textfields.
	*/
	public static int getPatternPosition(int calendarSymbol)	{
		return getPatternPosition(charForSymbol(calendarSymbol));
	}

	/** Pass a buffered pattern for performance inrease in loops. */
	private static int getPatternPosition(int calendarSymbol, String pattern)	{
		return getPatternPosition(charForSymbol(calendarSymbol), pattern);
	}
	
	private static char charForSymbol(int symbol)	{
		if (symbol == Calendar.YEAR)
			return 'y';
		if (symbol == Calendar.MONTH)
			return 'M';
		if (symbol == Calendar.DAY_OF_MONTH)
			return 'd';
		if (symbol == Calendar.DAY_OF_WEEK)
			return 'E';
		if (symbol == Calendar.HOUR_OF_DAY || symbol == Calendar.HOUR)
			return 'h';
		if (symbol == Calendar.MINUTE)
			return 'm';
		if (symbol == Calendar.SECOND)
			return 's';
		if (symbol == Calendar.AM_PM)
			return 'a';
		if (symbol == Calendar.ERA)
			return 'G';
		if (symbol == Calendar.ZONE_OFFSET)
			return 'z';
		throw new IllegalArgumentException("Cannot map Calendar constant: "+symbol);
	}
	
	static String allMaskChars()	{
		return "yMdEhHkKmsSaGz";
	}
	
	private static int getPatternPosition(char ofWhat)	{
		return getPatternPosition(ofWhat, getDateTimePattern(DateFormat.SHORT, DateFormat.LONG));
	}

	private static int getPatternPosition(char ofWhat, String pattern)	{
		//System.err.println("getPatternPosition, char "+ofWhat+", pattern: "+pattern);
		PatternPositionSemantic consumer = new PatternPositionSemantic(ofWhat);
		new FormatPatternInterpreter(pattern, consumer);
		return consumer.pos;	// 's' is the only missing in th and th_TH
	}


	/** Returns the separator for time: "23:55" -> ":" */
	public static char getTimeSeparator()	{
		return getTimeSeparator(getTimePattern(DateFormat.SHORT));
	}

	/** Pass a buffered pattern for performance increase in loops. */
	private static char getTimeSeparator(String pattern)	{
		return getSeparator(pattern, "hms");
	}
	

	/** Returns the separator for dates: "2001-10-11" -> "-" */
	public static char getDateSeparator()	{
		return getDateSeparator(getDatePattern(DateFormat.SHORT));
	}

	/** Pass a buffered pattern for performance increase in loops. */
	private static char getDateSeparator(String pattern)	{
		return getSeparator(pattern, "yMd");
	}
	
	public static char getSeparator(String pattern, String tags)	{
		SeparatorSemantic consumer = new SeparatorSemantic(tags);
		new FormatPatternInterpreter(pattern, consumer);
		return consumer.separator;
	}


	public static boolean isAmPmUsed()	{
		return getPatternPosition(Calendar.AM_PM) >= 0;
	}
	
	
	/** @return the age in years between birth and given "now". */
	public static int getAge(Calendar now, Calendar birth) {
		if (now.getTime().before(birth.getTime()))
			throw new IllegalArgumentException("Given 'now' is before birth date!");
	  
		final int age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
		final int todayMonth = now.get(Calendar.MONTH);
		final int birthMonth = birth.get(Calendar.MONTH);
		
		if (todayMonth < birthMonth ||
			todayMonth == birthMonth &&
			now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))
		return age - 1; // whole year not finished
		
		return age;
	}
	
	/** Date delegation to Calendar. */
	public static int getAge(Date now, Date birth) {
		Calendar birthCal = Calendar.getInstance();
		birthCal.setTime(birth);
		Calendar todayCal = Calendar.getInstance();
		todayCal.setTime(now);
		return getAge(todayCal, birthCal);
	}

	

//	public static void main(String [] args)	{
//		Locale.setDefault(Locale.US);
//		System.err.println("pattern position of YEAR is "+getPatternPosition(Calendar.YEAR));
//		System.err.println("pattern position of HOUR is "+getPatternPosition(Calendar.HOUR));
//		System.err.println("time separator is "+getTimeSeparator());
//		System.err.println("AM/PM used is "+isAmPmUsed());
//		System.err.println(DateFormat.getTimeInstance(DateFormat.LONG).format(new java.util.Date()));
//	}

}


class PatternPositionSemantic implements FormatPatternSemantic	{
	int idx = 0, pos = -1;
	char ofWhat;

	public PatternPositionSemantic(char ofWhat)	{
		this.ofWhat = ofWhat;
	}
	
	public boolean isMaskCharacter(char c)	{
		return DateUtil.allMaskChars().indexOf(c) >= 0;
	}
	public void finishMask(StringBuffer mask)	{
		char c = mask.charAt(0);
		if (c == 'H' || c == 'k' || c == 'K') c = 'h';
		if (c == ofWhat)
			pos = idx;
		idx++;
	}
	public void finishSeparator(StringBuffer sep)	{
	}
}

class SeparatorSemantic implements FormatPatternSemantic	{
	String tags;
	boolean sharp = false;
	char separator = (char)0;

	public SeparatorSemantic(String tags)	{
		this.tags = tags;
	}

	public boolean isMaskCharacter(char c)	{
		return DateUtil.allMaskChars().indexOf(c) >= 0;
	}
	public void finishMask(StringBuffer mask)	{
		char c = mask.charAt(0);
		if (c == 'H' || c == 'k' || c == 'K') c = 'h';
		if (tags.indexOf(c) >= 0)
			sharp = true;
	}
	public void finishSeparator(StringBuffer sep)	{
		if (sharp && separator == (char)0)
			separator = sep.charAt(0);
	}
}


//	private static int getPatternPosition(char ofWhat, String pattern)	{
//		StringBuffer passed = new StringBuffer();
//		for (int i = 0; i < pattern.length(); i++)	{
//			char c = pattern.charAt(i);
//			switch (c)	{
//				case 'H':
//					c = 'h';
//				case 'y':
//				case 'M':
//				case 'd':
//				case 'h':
//				case 'm':
//				case 's':
//				case 'E':
//				case 'a':
//				case 'z':
//				case 'G':
//					if (passed.toString().indexOf(c) < 0)	{
//						if (c == ofWhat)
//							return passed.length();	// we found the character
//						passed.append(c);	// remember what we have seen
//					}
//					break;
//			}
//		}
//		return passed.length();	// as 's' is the only missing in th and th_TH
//	}

//	public static char getSeparator(String pattern, String tags)	{
//		boolean found = false;
//		for (int i = 0; i < pattern.length(); i++)	{
//			char c = pattern.charAt(i);
//			switch (c)	{
//				case 'H':
//					c = 'h';
//				case 'y':
//				case 'M':
//				case 'd':
//				case 'h':
//				case 'm':
//				case 's':
//					if (found == false)	{
//						if (tags.indexOf(c) >= 0)	{	// found first mask character
//							found = true;	// separator must come behind this
//						}
//					}
//					break;
//				default:
//					if (found)	{
//						if (Character.isWhitespace(c) == false)	{
//							return c;
//						}
//					}
//					break;
//			}
//		}
//		return (char)0;
//	}