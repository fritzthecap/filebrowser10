package fri.patterns.interpreter.expressions;

import java.io.Serializable;

/**
	Base class for logical associations and comparisons. Returns a Boolean from evaluation.
	
	@author Fritz Ritzberger, 2003
*/

public abstract class AbstractCondition implements
	Condition
{
	public static final String NOT = "NOT";
	protected Expression [] expressions;
	protected Operator operator;
	protected Object valueHolder;
	protected Object value;
	
	protected AbstractCondition(Expression expression1, Operator operator, Expression expression2)	{
		this(operator, new Expression [] { expression1, expression2 });
	}
	
	protected AbstractCondition(Operator operator, Expression [] expressions)	{
		this.expressions = expressions;
		this.operator = operator;

		checkExpressions(expressions);
		checkOperator(operator);
	}
	

	protected void checkOperator(Operator operator)	{
		if (operator == null)
			throw new IllegalArgumentException("Can not construct AbstractCondition without operator: "+operator);
	}
	
	protected void checkExpressions(Expression [] expressions)	{
		if (expressions == null)
			throw new IllegalArgumentException("Expressions array is null!");

		for (int i = 0; expressions != null && i < expressions.length; i++)
			if (expressions[i] == null)
				throw new IllegalArgumentException("Expression number "+i+" is null!");
				
		checkMinimumExpressions(expressions);
	}
	
	protected void checkMinimumExpressions(Expression [] expressions)	{
		if (expressions.length < 2)
			throw new IllegalArgumentException("Can not construct AbstractCondition with less than 2 expressions!");
	}


	/** Returns the operator. The setter method must be typed and is implemented in subclasses. */
	public Operator getOperator()	{
		return operator;
	}


	public abstract Operator [] getOperators();

	public abstract Object clone();

	
	/** This is a "deep" compare of Conditions. */
	public boolean equals(Object other)	{
		if (other instanceof AbstractCondition == false)
			return false;

		AbstractCondition ac = (AbstractCondition)other;
		if (ac.operator.equals(operator) == false)
			return false;

		if (ac.expressions.length != expressions.length)
			return false;

		for (int i = 0; i < expressions.length; i++)
			if (ac.expressions[i].equals(expressions[i]) == false)
				return false;

		return true;
	}


	public String toString()	{
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < expressions.length; i++)	{
			if (i == 0)	{
				if (this instanceof Comparison == false)
					sb.append("( ");

				if (operator.toString().endsWith(NOT))
					sb.append(NOT);
			}
			else	{
				sb.append(" "+operator+" ");
			}
			
			sb.append(expressions[i] instanceof Constant ? "'"+expressions[i]+"'" : ""+expressions[i]);
		}

		if (this instanceof Comparison == false)
			sb.append(" )");

		return sb.toString();
	}





	/** Encapsulates definitions of all comparison and association operators. */
	public static class Operator implements Serializable
	{
		private final String symbol;
		
		Operator(String symbol)	{
			this.symbol = symbol;
		}
		
		public String toString()	{
			return symbol;
		}

		public boolean equals(Object other)	{
			return symbol.equals(((Operator)other).symbol);
		}
	}

}