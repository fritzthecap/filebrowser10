package fri.patterns.interpreter.expressions;

/**
	Base class for String comparison. Returns a Boolean from evaluation.
	Strings can be compared by "contains", "equals", "matches" (regular conditions).
	
	@author Fritz Ritzberger, 2003
*/

public class StringComparison extends AbstractComparison
{
	public static final StringOperator EQUAL = new StringOperator("equal to");
	public static final StringOperator CONTAINS = new StringOperator("contains");
	public static final StringOperator MATCHES = new StringOperator("matches");
	public static final StringOperator STARTS_WITH = new StringOperator("starts with");
	public static final StringOperator ENDS_WITH = new StringOperator("ends with");
	public static final StringOperator NOT_EQUAL = new StringOperator(NOT+" "+EQUAL);
	public static final StringOperator NOT_CONTAINS = new StringOperator(NOT+" "+CONTAINS);
	public static final StringOperator NOT_MATCHES = new StringOperator(NOT+" "+MATCHES);
	public static final StringOperator NOT_STARTS_WITH = new StringOperator(NOT+" "+STARTS_WITH);
	public static final StringOperator NOT_ENDS_WITH = new StringOperator(NOT+" "+ENDS_WITH);
	public static final StringOperator [] operators = new StringOperator []	{
			CONTAINS,
			MATCHES,
			EQUAL,
			STARTS_WITH,
			ENDS_WITH,
			NOT_CONTAINS,
			NOT_MATCHES,
			NOT_EQUAL,
			NOT_STARTS_WITH,
			NOT_ENDS_WITH,
	};

	public StringComparison(Value value1, StringOperator operator, Value value2)	{
		super(value1, operator, value2);
	}
	
	protected boolean associate(Object o1, Object o2)	{
		return associateStrings((String)o1, (String)o2);
	}
	
	private boolean associateStrings(String s1, String s2)	{
		if (operator == EQUAL)	{
			return s1 == null && s2 == null || s1 != null && s2 != null && s1.equals(s2);
		}
		else
		if (operator == CONTAINS)	{
			return s1 != null && s2 != null && s1.indexOf(s2) >= 0;
		}
		else
		if (operator == MATCHES)	{
			throw new IllegalArgumentException("Currently not supported: >"+operator+"<");
		}
		else
		if (operator == STARTS_WITH)	{
			return s1 != null && s2 != null && s1.startsWith(s2);
		}
		else
		if (operator == ENDS_WITH)	{
			return s1 != null && s2 != null && s1.endsWith(s2);
		}
		else
		if (operator == NOT_EQUAL)	{
			return s1 == null && s2 != null || s1 != null && s2 == null || s1 != null && s2 != null && s1.equals(s2) == false;
		}
		else
		if (operator == NOT_CONTAINS)	{
			return s1 != null && s2 != null && s1.indexOf(s2) < 0;
		}
		else
		if (operator == NOT_MATCHES)	{
			throw new IllegalArgumentException("Currently not supported: >"+operator+"<");
		}
		else
		if (operator == NOT_STARTS_WITH)	{
			return s1 == null || s2 == null || s1.startsWith(s2) == false;
		}
		else
		if (operator == NOT_ENDS_WITH)	{
			return s1 == null || s2 == null || s1.endsWith(s2) == false;
		}

		throw new IllegalArgumentException("Wrong operator symbol: >"+operator+"<");
	}


	public void setOperator(StringOperator operator)	{
		uncheckedSetOperator(operator);
	}

	public Operator [] getOperators()	{
		return operators;
	}
	
	public Object clone()	{
		return new StringComparison((Value)getLeftValue().clone(), (StringOperator)operator, (Value)getRightValue().clone());
	}
	



	public static class StringOperator extends Operator
	{
		StringOperator(String symbol)	{
			super(symbol);
		}
	}

}