package fri.gui.swing.calculator;

import fri.patterns.interpreter.parsergenerator.semantics.ReflectSemantic;

/**
	Implements the semantics of all arithmetic operators.
	Methods must be named like terminal symbols in syntax.
*/

public strictfp class CalculatorSemantic extends ReflectSemantic
{
	public Object primary_expression(Object argument)
	{
		try	{
			if (argument instanceof String)	{
				String s = (String)argument;
	
				if (s.startsWith(Calculator.hexPrefix))	{	// hexadecimal number
					s = s.substring(Calculator.hexPrefix.length());
					long i = Long.parseLong(s, 16);
					argument = new Double((double)i);
				}
				else
				if (s.startsWith(Calculator.octPrefix))	{	// octal number
					s = s.substring(Calculator.octPrefix.length());
					long i = Long.parseLong(s, 8);
					argument = new Double((double)i);
				}
				else
				if (s.startsWith(Calculator.binPrefix))	{	// binary number
					s = s.substring(Calculator.binPrefix.length());
					long i = Long.parseLong(s, 2);
					argument = new Double((double)i);
				}
				else	{
					s = s.replace(Calculator.decimalSeparator, '.');
					argument = Double.valueOf(s);
				}
			}
		}
		catch (NumberFormatException e)	{
			e.printStackTrace();
			throw e;
		}
		return argument;
	}

	public Object primary_expression(Object LPAREN, Object inclusive_or_expression, Object RPAREN)
	{
		return inclusive_or_expression;
	}

	public Object unary_expression_not_plus_minus(Object sym, Object unary_expression)
	{
		Double dbl = (Double)unary_expression;
		double d = dbl.doubleValue();
		if (sym.equals(Calculator.not))	{	// binary complement
			int i = (int)Math.rint(d);
			d = ~i;
		}
		else
		if (sym.equals(Calculator.sqrt))	{	// square root
			d = Math.sqrt(d);
		}
		else
		if (sym.equals(Calculator.abs))	{	// absolute amount
			d = Math.abs(d);
		}
		else
		if (sym.equals(Calculator.lgn))	{	// natural logarithm
			d = Math.log(d);
		}
		else
		if (sym.equals(Calculator.exp))	{	// exponential number
			d = Math.exp(d);
		}
		else
		if (sym.equals(Calculator.sin))	{	// sinus
			d = Math.sin(Math.toRadians(d));
		}
		else
		if (sym.equals(Calculator.cos))	{	// cosinus
			d = Math.cos(Math.toRadians(d));
		}
		else
		if (sym.equals(Calculator.asin))	{	// arcus sinus
			d = Math.toDegrees(Math.asin(d));
		}
		else
		if (sym.equals(Calculator.acos))	{	// arcus cosinus
			d = Math.toDegrees(Math.acos(d));
		}
		else
		if (sym.equals(Calculator.tan))	{	// tangens
			d = Math.tan(Math.toRadians(d));
		}
		else
		if (sym.equals(Calculator.atan))	{	// arcus tangens
			d = Math.toDegrees(Math.atan(d));
		}
		else
		if (sym.equals(Calculator.rad))	{	// to radians
			d = Math.toRadians(d);
		}
		else
		if (sym.equals(Calculator.deg))	{	// to degrees
			d = Math.toDegrees(d);
		}
		else
		if (sym.equals(Calculator.round))	{	// round to nearest int
			d = Math.round(d);
		}
		else	{
			throw new IllegalArgumentException("Unknown unary operator "+sym);
		}
		return new Double(d);
	}

	public Object unary_expression(Object PLUS_or_MINUS, Object unary_expression)
	{
		if (PLUS_or_MINUS.equals(Calculator.minus))	{	// negate number
			Double dbl = (Double)unary_expression;
			double d = dbl.doubleValue();
			unary_expression = new Double(-d);
		}
		return unary_expression;
	}

	public Object power_expression(Object power_expression, Object POWER, Object unary_expression)
	{
		Double dbl1 = (Double)power_expression;
		double d1 = dbl1.doubleValue();
		Double dbl2 = (Double)unary_expression;
		double d2 = dbl2.doubleValue();
		return new Double(Math.pow(d1, d2));
	}

	public Object multiplicative_expression(Object multiplicative_expression, Object MULT_or_DIV_or_MOD, Object power_expression)
	{
		Double dbl1 = (Double)multiplicative_expression;
		double d1 = dbl1.doubleValue();
		Double dbl2 = (Double)power_expression;
		double d2 = dbl2.doubleValue();
		if (MULT_or_DIV_or_MOD.equals(Calculator.mult))	{	// multiplication
			d1 = d1 * d2;
		}
		else
		if (MULT_or_DIV_or_MOD.equals(Calculator.div))	{	// division
			d1 = d1 / d2;
		}
		else
		if (MULT_or_DIV_or_MOD.equals(Calculator.mod))	{	// modulo
			d1 = d1 % d2;
		}
		else	{
			throw new IllegalArgumentException("Unknown multiplicative operator "+MULT_or_DIV_or_MOD);
		}
		return new Double(d1);
	}

	public Object additive_expression(Object additive_expression, Object PLUS_or_MINUS, Object multiplicative_expression)
	{
		Double dbl1 = (Double)additive_expression;
		double d1 = dbl1.doubleValue();
		Double dbl2 = (Double)multiplicative_expression;
		double d2 = dbl2.doubleValue();
		if (PLUS_or_MINUS.equals(Calculator.plus))	{	// addition
			d1 = d1 + d2;
		}
		else
		if (PLUS_or_MINUS.equals(Calculator.minus))	{	// subtraction
			d1 = d1 - d2;
		}
		else	{
			throw new IllegalArgumentException("Unknown additive operator "+PLUS_or_MINUS);
		}
		return new Double(d1);
	}

	public Object shift_expression(Object shift_expression, Object SHIFT, Object additive_expression)
	{
		Double dbl1 = (Double)shift_expression;
		double d1 = dbl1.doubleValue();
		Double dbl2 = (Double)additive_expression;
		double d2 = dbl2.doubleValue();
		int i1 = (int)Math.rint(d1);
		int i2 = (int)Math.rint(d2);
		if (SHIFT.equals(Calculator.lshift))	{	// shift left
			d1 = i1 << i2;
		}
		else
		if (SHIFT.equals(Calculator.rshift))	{	// shift right
			d1 = i1 >> i2;
		}
		else
		if (SHIFT.equals(Calculator.urshift))	{	// unsigned shift right
			d1 = i1 >>> i2;
		}
		else	{
			throw new IllegalArgumentException("Unknown shift operator "+SHIFT);
		}
		return new Double(d1);
	}

	public Object and_expression(Object and_expression, Object AND, Object shift_expression)
	{
		Double dbl1 = (Double)and_expression;
		double d1 = dbl1.doubleValue();
		Double dbl2 = (Double)shift_expression;
		double d2 = dbl2.doubleValue();
		int i1 = (int)Math.rint(d1);
		int i2 = (int)Math.rint(d2);
		return new Double(i1 & i2);	// binary AND
	}

	public Object exclusive_or_expression(Object exclusive_or_expression, Object XOR, Object and_expression)
	{
		Double dbl1 = (Double)exclusive_or_expression;
		double d1 = dbl1.doubleValue();
		Double dbl2 = (Double)and_expression;
		double d2 = dbl2.doubleValue();
		int i1 = (int)Math.rint(d1);
		int i2 = (int)Math.rint(d2);
		return new Double(i1 ^ i2);	// binary XOR
	}

	public Object inclusive_or_expression(Object inclusive_or_expression, Object OR, Object exclusive_or_expression)
	{
		Double dbl1 = (Double)inclusive_or_expression;
		double d1 = dbl1.doubleValue();
		Double dbl2 = (Double)exclusive_or_expression;
		double d2 = dbl2.doubleValue();
		int i1 = (int)Math.rint(d1);
		int i2 = (int)Math.rint(d2);
		return new Double(i1 | i2);	// binary OR
	}

}