package fri.patterns.interpreter.expressions;

import java.util.*;

/**
	Base class for Date comparison. Returns a Boolean from evaluation.
	Dates can be compared by "==", "!=", "<", ">", ">=", "<=", "sameYear", "sameMonth", "sameDay", "sameHour".
	
	@author Fritz Ritzberger, 2003
*/

public class DateComparison extends AbstractComparison
{
	public static final DateOperator EQUAL = new DateOperator("==");
	public static final DateOperator NOT_EQUAL = new DateOperator("!=");
	public static final DateOperator BEFORE = new DateOperator("<");
	public static final DateOperator BEFORE_EQUAL = new DateOperator("<=");
	public static final DateOperator AFTER = new DateOperator(">");
	public static final DateOperator AFTER_EQUAL = new DateOperator(">=");
	public static final DateOperator SAME_YEAR = new DateOperator("in same year as");
	public static final DateOperator SAME_MONTH = new DateOperator("in same month as");
	public static final DateOperator SAME_DAY = new DateOperator("on same day as");
	public static final DateOperator SAME_HOUR = new DateOperator("in same hour as");
	public static final DateOperator NOT_SAME_YEAR = new DateOperator(NOT+" "+SAME_YEAR);
	public static final DateOperator NOT_SAME_MONTH = new DateOperator(NOT+" "+SAME_MONTH);
	public static final DateOperator NOT_SAME_DAY = new DateOperator(NOT+" "+SAME_DAY);
	public static final DateOperator NOT_SAME_HOUR = new DateOperator(NOT+" "+SAME_HOUR);
	public static final DateOperator [] operators = new DateOperator []	{
			EQUAL,
			NOT_EQUAL,
			BEFORE,
			BEFORE_EQUAL,
			AFTER,
			AFTER_EQUAL,
			SAME_YEAR,
			SAME_MONTH,
			SAME_DAY,
			SAME_HOUR,
			NOT_SAME_YEAR,
			NOT_SAME_MONTH,
			NOT_SAME_DAY,
			NOT_SAME_HOUR,
	};

	public DateComparison(Value value1, DateOperator operator, Value value2)	{
		super(value1, operator, value2);
	}
	
	protected boolean associate(Object o1, Object o2)	{
		return associateDates((Date)o1, (Date)o2);
	}
	
	private boolean associateDates(Date d1, Date d2)	{
		if (operator == EQUAL)	{
			return d1 == null && d2 == null || d1 != null && d2 != null && d1.compareTo(d2) == 0;
		}
		else
		if (operator == NOT_EQUAL)	{
			return d1 != null && d2 == null || d1 == null && d2 != null || d1 != null && d2 != null && d1.compareTo(d2) != 0;
		}
		else
		if (operator == BEFORE)	{
			return d1 != null && d2 != null && d1.compareTo(d2) < 0;
		}
		else
		if (operator == AFTER)	{
			return d1 != null && d2 != null && d1.compareTo(d2) > 0;
		}
		else
		if (operator == BEFORE_EQUAL)	{
			return d1 != null && d2 != null && d1.compareTo(d2) <= 0;
		}
		else
		if (operator == AFTER_EQUAL)	{
			return d1 != null && d2 != null && d1.compareTo(d2) >= 0;
		}
		else
		if (operator == SAME_YEAR)	{
			return d1 != null && d2 != null && sameYear(toCalendar(d1), toCalendar(d2));
		}
		else
		if (operator == SAME_MONTH)	{
			return d1 != null && d2 != null && sameMonth(toCalendar(d1), toCalendar(d2));
		}
		else
		if (operator == SAME_DAY)	{
			return d1 != null && d2 != null && sameDay(toCalendar(d1), toCalendar(d2));
		}
		else
		if (operator == SAME_HOUR)	{
			return d1 != null && d2 != null && sameHour(toCalendar(d1), toCalendar(d2));
		}
		else
		if (operator == NOT_SAME_YEAR)	{
			return d1 == null || d2 == null || sameYear(toCalendar(d1), toCalendar(d2)) == false;
		}
		else
		if (operator == NOT_SAME_MONTH)	{
			return d1 == null || d2 == null || sameMonth(toCalendar(d1), toCalendar(d2)) == false;
		}
		else
		if (operator == NOT_SAME_DAY)	{
			return d1 == null || d2 == null || sameDay(toCalendar(d1), toCalendar(d2)) == false;
		}
		else
		if (operator == NOT_SAME_HOUR)	{
			return d1 == null || d2 == null || sameHour(toCalendar(d1), toCalendar(d2)) == false;
		}

		throw new IllegalArgumentException("Wrong operator symbol: >"+operator+"<");
	}
	
	
	private Calendar toCalendar(Date d)	{
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c;
	}
	
	private boolean sameYear(Calendar c1, Calendar c2)	{
		return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
	}

	private boolean sameMonth(Calendar c1, Calendar c2)	{
		return sameYear(c1, c2) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
	}

	private boolean sameDay(Calendar c1, Calendar c2)	{
		return sameMonth(c1, c2) && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
	}

	private boolean sameHour(Calendar c1, Calendar c2)	{
		return sameDay(c1, c2) && c1.get(Calendar.HOUR_OF_DAY) == c2.get(Calendar.HOUR_OF_DAY);
	}


	public void setOperator(DateOperator operator)	{
		uncheckedSetOperator(operator);
	}

	public Operator [] getOperators()	{
		return operators;
	}
	
	public Object clone()	{
		return new DateComparison((Value)getLeftValue().clone(), (DateOperator)operator, (Value)getRightValue().clone());
	}
	


	public static class DateOperator extends Operator
	{
		DateOperator(String symbol)	{
			super(symbol);
		}
	}

}