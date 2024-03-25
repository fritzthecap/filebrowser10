package fri.patterns.interpreter.expressions;

import java.io.Serializable;

/**
	Basic interface for an interpretable piece of logic.
	Expressions must be able to evaluate on some data.
	An Expression can be a Value (Variable, Constant), a Condition, or a Comparison.
	
	@author Fritz Ritzberger, 2003
*/

public interface Expression extends Cloneable, Serializable
{
	/**
		The implementer does its specific work by means of passed data.
		@param dataHolder the object that holds values for variables.
		@return the result object of evaluation.
	*/
	public Object evaluate(Object dataHolder);
	
	/**
		Returns a shallow copy of this expression.
	*/
	public Object clone();

}
