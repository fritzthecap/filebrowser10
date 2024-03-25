package fri.patterns.interpreter.expressions;

/**
	Base class for Number comparison. Returns a Boolean from evaluation.
	Numbers can be compared by "==", "!=", "<", ">", ">=", "<=".
	
	@author Fritz Ritzberger, 2003
*/

public class NumberComparison extends AbstractComparison
{
	public static final NumberOperator EQUAL = new NumberOperator("==");
	public static final NumberOperator NOT_EQUAL = new NumberOperator("!=");
	public static final NumberOperator LESS = new NumberOperator("<");
	public static final NumberOperator LESS_EQUAL = new NumberOperator("<=");
	public static final NumberOperator GREATER = new NumberOperator(">");
	public static final NumberOperator GREATER_EQUAL = new NumberOperator(">=");
	public static final NumberOperator [] operators = new NumberOperator []	{
			EQUAL,
			NOT_EQUAL,
			LESS,
			LESS_EQUAL,
			GREATER,
			GREATER_EQUAL,
	};

	public NumberComparison(Value value1, NumberOperator operator, Value value2)	{
		super(value1, operator, value2);
	}
	
	protected boolean associate(Object o1, Object o2)	{
		return associateNumbers((Number)o1, (Number)o2);
	}
	
	private boolean associateNumbers(Number n1, Number n2)	{
		if (operator == EQUAL)	{
			return n1 == null && n2 == null || n1 != null && n2 != null && n1.doubleValue() == n2.doubleValue();
		}
		else
		if (operator == NOT_EQUAL)	{
			return n1 != null && n2 == null || n1 == null && n2 != null || n1 != null && n2 != null && n1.doubleValue() != n2.doubleValue();
		}
		else
		if (operator == LESS)	{
			return n1 != null && n2 != null && n1.doubleValue() < n2.doubleValue();
		}
		else
		if (operator == GREATER)	{
			return n1 != null && n2 != null && n1.doubleValue() > n2.doubleValue();
		}
		else
		if (operator == LESS_EQUAL)	{
			return n1 != null && n2 != null && n1.doubleValue() <= n2.doubleValue();
		}
		else
		if (operator == GREATER_EQUAL)	{
			return n1 != null && n2 != null && n1.doubleValue() >= n2.doubleValue();
		}

		throw new IllegalArgumentException("Wrong operator symbol: >"+operator+"<");
	}


	public void setOperator(NumberOperator operator)	{
		uncheckedSetOperator(operator);
	}

	public Operator [] getOperators()	{
		return operators;
	}
	
	public Object clone()	{
		return new NumberComparison((Value)getLeftValue().clone(), (NumberOperator)operator, (Value)getRightValue().clone());
	}
	


	public static class NumberOperator extends Operator
	{
		NumberOperator(String symbol)	{
			super(symbol);
		}
	}
	
}