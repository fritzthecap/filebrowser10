package fri.patterns.interpreter.expressions;

/**
	Base class for Object comparison. Returns a Boolean from evaluation.
	Objects can be compared by EQUAL (method equals)and IDENTICAL (operator ==).
	
	@author Fritz Ritzberger, 2003
*/

public class ObjectComparison extends AbstractComparison
{
	public static final ObjectOperator EQUAL = new ObjectOperator("equal to");
	public static final ObjectOperator IDENTICAL = new ObjectOperator("identical with");
	public static final ObjectOperator NOT_EQUAL = new ObjectOperator(NOT+" "+EQUAL);
	public static final ObjectOperator NOT_IDENTICAL = new ObjectOperator(NOT+" "+IDENTICAL);
	public static final ObjectOperator [] operators = new ObjectOperator []	{
			EQUAL,
			IDENTICAL,
			NOT_EQUAL,
			NOT_IDENTICAL,
	};

	public ObjectComparison(Value value1, ObjectOperator operator, Value value2)	{
		super(value1, operator, value2);
	}
	
	protected boolean associate(Object o1, Object o2)	{
		if (operator == EQUAL)	{
			return o1 == null && o2 == null || o1 != null && o2 != null && o1.equals(o2);
		}
		else
		if (operator == IDENTICAL)	{
			return o1 != null && o2 != null && o1 == o2;
		}
		else
		if (operator == NOT_EQUAL)	{
			return o1 == null && o2 != null || o1 != null && o2 == null || o1 != null && o2 != null && o1.equals(o2) == false;
		}
		else
		if (operator == NOT_IDENTICAL)	{
			return o1 == null || o2 == null || o1 != o2;
		}

		throw new IllegalArgumentException("Wrong operator symbol: >"+operator+"<");
	}


	public void setOperator(ObjectOperator operator)	{
		uncheckedSetOperator(operator);
	}

	public Operator [] getOperators()	{
		return operators;
	}
	
	public Object clone()	{
		return new ObjectComparison((Value)getLeftValue().clone(), (ObjectOperator)operator, (Value)getRightValue().clone());
	}
	


	public static class ObjectOperator extends Operator
	{
		ObjectOperator(String symbol)	{
			super(symbol);
		}
	}

}