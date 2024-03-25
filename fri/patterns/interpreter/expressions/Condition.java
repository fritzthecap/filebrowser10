package fri.patterns.interpreter.expressions;

/**
	Base marker interface for everything that is not a Variable or Constant:
	comparison, unary / binary logical association.
	Any Condition returns a Boolean from evaluation.

	@author Fritz Ritzberger, 2003
*/

public interface Condition extends Expression
{
}