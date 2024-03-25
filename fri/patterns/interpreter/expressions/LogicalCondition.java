package fri.patterns.interpreter.expressions;

/**
	Base class for AND and OR conditions.
	It can process more than two conditions, but only one operator (AND, OR) is allowed.
	It performs a short evaluation cycle, that means e.g. the second condition of two
	conditions associated by AND will not be evaluated when the first is false.
	
	@author Fritz Ritzberger, 2003
*/

public class LogicalCondition extends AbstractCondition
{
	public static final int MINIMUM_CHILD_CONDITIONS = 0;
	public static final LogicalOperator AND = new LogicalOperator("AND");
	public static final LogicalOperator OR = new LogicalOperator("OR");
	public static final LogicalOperator AND_NOT = new LogicalOperator(AND+" "+NOT);
	public static final LogicalOperator OR_NOT = new LogicalOperator(OR+" "+NOT);
	public static final LogicalOperator [] operators = new LogicalOperator []	{
		AND,
		OR,
		AND_NOT,
		OR_NOT,
	};

	public LogicalCondition(Condition condition1, LogicalOperator operator, Condition condition2)	{
		super(condition1, operator, condition2);
	}
	
	public LogicalCondition(LogicalOperator operator, Condition [] conditions)	{
		super(operator, conditions);
	}


	/** Overridden as this Condition allows less than MINIMUM_CHILD_CONDITIONS child conditions. */
	protected void checkMinimumExpressions(Expression [] expressions)	{
		if (expressions.length < MINIMUM_CHILD_CONDITIONS)
			throw new IllegalArgumentException("A logical condition must have at least "+MINIMUM_CHILD_CONDITIONS+" child condition!");
	}

	
	/** Overridden for "short condition evaluation cycle". */
	public Object evaluate(Object valueHolder)	{
		if (this.valueHolder == valueHolder)
			return value;
			
		boolean ret = (operator == AND || operator == AND_NOT);	// do not stop AND-evaluation when nothing is contained
		boolean doMore = true;
		
		for (int i = 0; doMore && i < expressions.length; i++)	{
			Expression condition = (Condition) expressions[i];
			Boolean b = (Boolean) condition.evaluate(valueHolder);
			ret = b.booleanValue();

			// revert on negation
			ret = (operator == AND_NOT || operator == OR_NOT) ? !ret : ret;

			// short cycle: when an AND-operator and positive, or a OR-operator and negative, continue
			doMore = ret == true && (operator == AND || operator == AND_NOT) || ret == false && (operator == OR || operator == OR_NOT);
		}

		this.valueHolder = valueHolder;
		value = new Boolean(ret);
		//System.err.println("evaluate >"+valueHolder+"< returns "+ret+" on "+this);
		return value;
	}
	

	/** Returns a flat copy of the contained Conditions. */
	public Condition [] getConditions()	{
		Condition [] conditions = new Condition[expressions.length];
		System.arraycopy(expressions, 0, conditions, 0, conditions.length);
		return conditions;
	}

	/** Sets new Conditions. */
	public void setConditions(Condition [] conditions)	{
		checkExpressions(conditions);
		this.expressions = conditions;
	}


	public void setOperator(LogicalOperator operator)	{
		checkOperator(operator);
		this.operator = operator;
	}

	public Operator [] getOperators()	{
		return operators;
	}
	
	public Object clone()	{
		Condition [] conditions = getConditions();
		for (int i = 0; i < conditions.length; i++)
			conditions[i] = (Condition)conditions[i].clone();
		return new LogicalCondition((LogicalOperator)operator, conditions);
	}
	


	public static class LogicalOperator extends Operator
	{
		LogicalOperator(String symbol)	{
			super(symbol);
		}
	}

}