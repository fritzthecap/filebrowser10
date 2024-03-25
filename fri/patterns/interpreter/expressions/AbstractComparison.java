package fri.patterns.interpreter.expressions;

/**
	Abstract implementation of Comparison that provides left and right Values (Variable, Constant).
	
	@author Fritz Ritzberger, 2003
*/

public abstract class AbstractComparison extends AbstractCondition implements
	Comparison
{
	protected AbstractComparison(Value leftValue, Operator operator, Value rightValue)	{
		super(leftValue, operator, rightValue);
	}
	
	public Value getLeftValue()	{
		return (Value)expressions[0];
	}

	public void setLeftValue(Value leftValue)	{
		expressions[0] = leftValue;
	}

	public Value getRightValue()	{
		return (Value)expressions[1];
	}

	public void setRightValue(Value rightValue)	{
		expressions[1] = rightValue;
	}

	/**
		Can be used to set an operator without operator type check.
		This makes it easier to set an operator derived from a String.
	*/
	public void uncheckedSetOperator(Operator operator)	{
		checkOperator(operator);
		this.operator = operator;
	}


	/** Default implementation for Comparisons: associate expressions as long as the return is true. */
	public Object evaluate(Object valueHolder)	{
		if (this.valueHolder == valueHolder)
			return value;
		
		this.valueHolder = valueHolder;

		boolean ret = true;
		for (int i = 0; ret && i < expressions.length - 1; i++)	{
			Object o1 = expressions[i].evaluate(valueHolder);
			Object o2 = expressions[i + 1].evaluate(valueHolder);
			ret = associate(o1, o2);
		}

		value = new Boolean(ret);
		return value;
	}
	
	/** To be implemented by subclasses: associate two expressions which each other, e.g. with "==". */
	protected abstract boolean associate(Object o1, Object o2);

}
