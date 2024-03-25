package fri.patterns.interpreter.expressions;

import fri.util.Equals;

/**
	A constant value that returns a fixed value when evaluating.
	The value holder passed in evaluate is ignored.
	
	@author Fritz Ritzberger, 2003
*/

public class Constant implements Value
{
	private Object value;
	
	public Constant(Object value)	{
		this.value = value;
	}

	public Object evaluate(Object valueHolder)	{
		return value;
	}

	public void setValue(Object value)	{
		this.value = value;
	}

	public Object getValue()	{
		return value;
	}

	public String toString()	{
		return ""+value;
	}

	/** Clones this Constant, not its value. */
	public Object clone()	{
		return new Constant(value);
	}

	/** Compares the values, considering nulls to be equal. */
	public boolean equals(Object other)	{
		if (other instanceof Constant == false)
			return false;
		return Equals.equals(((Constant)other).value, value);
	}

}
