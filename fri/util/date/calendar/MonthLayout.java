package fri.util.date.calendar;

import java.util.*;
import fri.util.date.DateUtil;

/**
	This class represents a formatted list of days, as Strings like
	" ", " ", "1", "2", ... (if first day of week is monday and the first day
	of month is wednesday).
	It respects the first day of week and provides even week numbers.
	<p>
	Bugfixes for getMinimalDaysInFirstWeek() for Locale de_AT were added.<br>
	Bugfixes for year 1582 were added.<br>
*/

public class MonthLayout extends Vector
{
	private Calendar calendar;
	private int blanksBefore, blanksAfter;
	private boolean hasDefinedDay;
	
	
	/** Constructor with no defined day. The month is 1-12. */
	public MonthLayout(int year, int month)	{
		super(31);
		
		hasDefinedDay = false;
		
		Calendar c = Calendar.getInstance();	// current date

		c.set(Calendar.YEAR, year);	// set year and month
		c.set(Calendar.MONTH, month - 1);
		c.set(Calendar.DATE, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		init(c);
	}
	
	/** Constructor with current day as defined day. */
	public MonthLayout()	{
		this(null);
	}
	
	/** Constructor with passed defined day. */
	public MonthLayout(Calendar c)	{
		super(31);
		hasDefinedDay = true;
		init(c);
	}
	
	/**
		Calculate blank positions before begin of month and after end.
		Add all blanks and day numbers to list.
	*/
	private void init(Calendar c)	{
		if (c == null)
			c = Calendar.getInstance();	// current date

		this.calendar = c;

		//System.err.println("Locale is: "+Locale.getDefault().getLanguage()+", GERMAN is: "+Locale.GERMAN);
		Locale loc = Locale.getDefault();
		if (loc != null && loc.toString().equals("de_AT"))	{
			// Bugfix for Locale de_AT, JDK 1.3.1
			getCalendar().setMinimalDaysInFirstWeek(4);
			getCalendar().setTime(getCalendar().getTime());	// refresh
		}
		
		c = (Calendar)c.clone();	// leave input object unchanged
		c.set(Calendar.DATE, 1);	// set calendar to first day of month
		
		int firstDayOfWeek = c.getFirstDayOfWeek();

		int firstDayOfMonth = c.get(Calendar.DAY_OF_WEEK);	// week day
		//System.err.println("first day of week is "+firstDayOfWeek+", first day of month "+firstDayOfMonth);
		
		blanksBefore = firstDayOfMonth - firstDayOfWeek;
		if (blanksBefore < 0)
			blanksBefore = 7 + blanksBefore;
		
		int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);	// 28, 29, 30 or 31
		// Bugfix for year 1582 when gregorian calendar was introduced
		if (c.get(Calendar.YEAR) == 1582 && c.get(Calendar.MONTH) == 9)	// October
			max = 21;	// Gregorian Calendar starts here

		//System.err.println("actual maximum is "+max+", blanks before "+blanksBefore);
		max += blanksBefore;
		int cells = max;
		int rest = max % 7;
		if (rest > 0)
			cells += 7 - rest;

		for (int i = 0; i < cells; i++)	{
			if (i < blanksBefore || i >= max)	{
				addElement("  ");
			}
			else
			if (i < max)	{
				int day = c.get(Calendar.DAY_OF_MONTH);
				addElement(day < 10 ? " "+day : ""+day);
				c.add(Calendar.DATE, 1);
			}
		}
	}

	
	/** Returns "Mon", "Tue", ... representing day of week. */
	public String [] getDayHeaders()	{
		return DateUtil.getDayDisplayNames(true, getCalendar().getFirstDayOfWeek());
	}
	
	/** Returns "40", "41", "42", ... representing week of year. */
	public String [] getWeekHeaders()	{
		Calendar c = (Calendar)getCalendar().clone();	// leave input object unchanged
		c.set(Calendar.DATE, 1);
		//System.err.println("Minimal Days in First Week: "+c.getMinimalDaysInFirstWeek());
		String [] sarr = new String [getWeeks()];
		for (int i = 0; i < sarr.length; i++)	{
			c.set(Calendar.DATE, i * 7 + 1);	// do this for week 53, e.g. Jan 1999
			int week = c.get(Calendar.WEEK_OF_YEAR);

			// Bugfix for year 1582 when gregorian calendar was introduced
			if (c.get(Calendar.YEAR) == 1582 && c.get(Calendar.MONTH) >= 9)
				if (week == 41)
					week = 42;
				else
				if (week > 41)
					week--;

			sarr[i] = (week < 10 ? " "+week : ""+week);
		}
		return sarr;
	}
	

	// Returns a new Calendar instance for passed Vector position
	private Calendar getDayFromPosition(int pos)	{
		String s = (String)elementAt(pos);
		if (s != null && (s = s.trim()).length() > 0)	{
			int day = Integer.valueOf(s).intValue();
			Calendar c = (Calendar)getCalendar().clone();	// leave input object unchanged
			c.set(Calendar.DATE, day);
			return c;
		}
		return null;
	}
		
	/** Return true if the passed Vector position is saturday or sunday. */
	public boolean isSundayOrSaturday(int pos)	{
		Calendar c = getDayFromPosition(pos);
		if (c == null)
			return false;
			
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
	}
	
	/** Return true if the passed Vector position is sunday. */
	public boolean isSunday(int pos)	{
		Calendar c = getDayFromPosition(pos);
		if (c == null)
			return false;
			
		return Calendar.SUNDAY == c.get(Calendar.DAY_OF_WEEK);
	}
	
	/** Return true if the passed Vector position is the day defined in this object's Calendar. */
	public boolean isDefinedDay(int pos)	{
		if (hasDefinedDay == false)
			return false;
			
		Calendar c = getDayFromPosition(pos);
		if (c == null)
			return false;
		
		//System.err.println("Comparing for defined day: "+getCalendar().getTime()+" <---> "+c.getTime());
		return getCalendar().get(Calendar.DAY_OF_MONTH) == c.get(Calendar.DAY_OF_MONTH);
	}
	
	/** Return true if the passed Vector position is a holiday. */
	public String isHoliday(int pos)	{
		Calendar c = getDayFromPosition(pos);
		if (c == null)
			return null;
			
		int year = c.get(Calendar.YEAR);

		String holiday = getHolidayName(year, c);
		return holiday;
	}


	/**
		Asks fri.util.date.calendar.Holidays if passed day is a holiday.
		Returns null if it is not a holiday, else name of the holiday.
		Override this to use another Holiday logic.
	*/
	protected String getHolidayName(int year, Calendar day)	{
		return (String)Holidays.getHolidays(year).get(day);
	}
	
	
	/** Returns the Calendar of this MonthLayout object. */
	public Calendar getCalendar()	{
		return calendar;
	}
	
	/** Returns the count of weeks in constructed month. */
	public int getWeeks()	{
		return size() / 7;
	}

	/** Returns the count of placeholders in first week row of month. */
	public int getPlaceHoldersBefore()	{
		return blanksBefore;
	}
	
	/** Returns the count of placeholders in last week row of month. */
	public int getPlaceHoldersAfter()	{
		return blanksAfter;
	}



	/** Application main. Writes a month calendar to stdout. */
	public static final void main(String [] args)	{
		//Locale.setDefault(Locale.US);
		if (args.length > 0 && (args[0].equals("-help") || args[0].equals("-h")))	{
			System.err.println("SYNTAX: fri.util.date.calendar.MonthLayout [month [year]]");
			System.err.println("	Creates a month calendar on console.");
			System.exit(1);
		}

		int m = -1, y = -1;
		if (args.length > 0)	{
			m = Integer.valueOf(args[0]).intValue();
			if (args.length > 1)	{
				y = Integer.valueOf(args[1]).intValue();
			}
			else	{
				y = Calendar.getInstance().get(Calendar.YEAR);
			}
		}
		
		MonthLayout ml = (y >= 0) ? new MonthLayout(y, m) : new MonthLayout();
		
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MMMM yyyy");
		System.out.println(fmt.format(ml.getCalendar().getTime()));
		System.out.println("===============================");

		//int rows = ml.getWeeks() + 1;
		//int cols = 7 + 1;

		String emptyCol = "   |";
		System.out.print(emptyCol);	// matrix cross
		
		// add day names header
		String [] days = ml.getDayHeaders();
		for (int i = 0; i < days.length; i++)	{
			System.out.print(" "+days[i]+" ");
		}
		System.out.println();
		System.out.print("-------------------------------");
		
		String [] weeks = ml.getWeekHeaders();

		// add day numbers and week headers
		for (int i = 0; i < ml.size(); i++)	{
			if (i % 7 == 0)	{	// add week header
				System.out.println();
				String week = weeks[i / 7];
				System.out.print(week+" |");
			}
			
			String s = ml.elementAt(i).toString();
			if (s.length() < 2)
				s = " "+s;
			System.out.print(" "+s+" ");
		}
		System.out.println();
		System.out.println("-------------------------------");
	}

}