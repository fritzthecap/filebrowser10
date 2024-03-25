package fri.patterns.interpreter.expressions;

/**
	Marker interface for comparison of data types like String, Date, Number, Object.
	A Comparison is always a tree leaf with two sub-Expressions and a compare operator.
	
	@author Fritz Ritzberger, 2003
*/

public interface Comparison extends Condition
{
	/** Returns the left Value, normally a Variable. */
	public Value getLeftValue();

	/** Sets the left Value. */
	public void setLeftValue(Value leftValue);

	/** Returns the right Value, normally a Constant. */
	public Value getRightValue();

	/** Sets the right Value. */
	public void setRightValue(Value rightValue);

}
