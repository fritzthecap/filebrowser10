package fri.gui.swing.calculator;

import java.util.*;
import java.io.PrintStream;
import java.text.*;
import fri.util.text.Replace;
import fri.patterns.interpreter.parsergenerator.*;
import fri.patterns.interpreter.parsergenerator.syntax.*;
import fri.patterns.interpreter.parsergenerator.syntax.builder.SyntaxSeparation;
import fri.patterns.interpreter.parsergenerator.lexer.*;
import fri.patterns.interpreter.parsergenerator.parsertables.LALRParserTables;

/**
	Syntax und Parser des Rechners. Uebergeben wird ein Text, geliefert wird ein Double Resultat.
	Der Parser, Lexer und die Semantik sind global (static) und werden wiederverwendet.
	<p>
	ACHTUNG: der Calculator ist nicht Thread-safe!
*/

public class Calculator
{
	private static Parser parser;
	private static Lexer lexer;
	private static CalculatorSemantic semantic;

	public final static char decimalSeparator = new DecimalFormatSymbols().getDecimalSeparator();

	public final static String minus = ""+new DecimalFormatSymbols().getMinusSign();
	public final static String sqrt = "SQRT";
	public final static String pow = "POW";
	public final static String plus = "+";
	public final static String mult = "*";
	public final static String div = "/";
	public final static String mod = "%";
	public final static String not = "~";
	public final static String and = "&";
	public final static String or = "|";
	public final static String xor = "^";
	public final static String lshift = "<<";
	public final static String rshift = ">>";
	public final static String urshift = ">>>";
	public final static String abs = "ABS";
	public final static String lgn = "LOGN";
	public final static String exp = "EXPN";
	public final static String sin = "SIN";
	public final static String cos = "COS";
	public final static String asin = "ASIN";
	public final static String acos = "ACOS";
	public final static String tan = "TAN";
	public final static String atan = "ATAN";
	public final static String rad = "RAD";
	public final static String deg = "DEG";
	public final static String round = "ROUND";
	
	public static final String hexPrefix = "x";
	public static final String octPrefix = "o";
	public static final String binPrefix = "b";
	
	public final static List lexerSyntax;

	static	{	// Definition der Lexer-Regeln
		String [][] syntax0 = StandardLexerRules.getHexDigitsRules();
		String [][] syntax1 = StandardLexerRules.getOctDigitsRules();
		String [][] syntax2 = StandardLexerRules.getBinDigitsRules();
		String [][] syntax3 = StandardLexerRules.getUnicodeDigitsRules();
		String [][] syntax4 = {
			{ Token.TOKEN, "taggednumber" },
			{ Token.TOKEN, "floatnumber" },
			{ Token.IGNORED, "`whitespaces`" },
			{ "taggednumber", "\""+hexPrefix+"\"", "hexdigits" },	// andere Prefixe als in StandardLexerRules
			{ "taggednumber", "\""+octPrefix+"\"", "octdigits" },
			{ "taggednumber", "\""+binPrefix+"\"", "bindigits" },
			{ "floatnumber", "digits", "fraction_opt" },
			{ "floatnumber", "fraction" },
			{ "fraction_opt", "fraction" },
			{ "fraction_opt" },
			{ "fraction", "\""+decimalSeparator+"\"", "fractionnumber" },
			{ "fractionnumber", "digits", "exponent_opt" },
			{ "exponent_opt", "exp", "exponentnumber" },
			{ "exponent_opt", },
			{ "exp", "'E'" },
			{ "exp", "'e'" },
			{ "exponentnumber", "sign", "digits" },
			{ "exponentnumber", "digits" },
			{ "sign", "'"+plus+"'" },
			{ "sign", "'"+minus+"'" },
		};
		lexerSyntax = SyntaxUtil.catenizeRules(new String [][][] { syntax0, syntax1, syntax2, syntax3, syntax4, });
	}

	private final static String [][] parserSyntax = 	{	// Definition der Parser-Regeln
		{ "arithmetic_expression", "inclusive_or_expression" },

		{ "primary_expression", "argument" },
		{ "primary_expression", "'('", "inclusive_or_expression", "')'" },

		{ "unary_expression_not_plus_minus", "primary_expression" },
		{ "unary_expression_not_plus_minus", "\""+sqrt+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "'"+not+"'", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+abs+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+lgn+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+exp+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+sin+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+cos+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+asin+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+acos+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+tan+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+atan+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+rad+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+deg+"\"", "unary_expression" },
		{ "unary_expression_not_plus_minus", "\""+round+"\"", "unary_expression" },

		{ "unary_expression", "unary_expression_not_plus_minus" },
		{ "unary_expression", "'"+plus+"'", "unary_expression" },
		{ "unary_expression", "'"+minus+"'", "unary_expression" },

		{ "power_expression", "unary_expression" },
		{ "power_expression", "power_expression", "\""+pow+"\"", "unary_expression" },

		{ "multiplicative_expression", "power_expression" },
		{ "multiplicative_expression", "multiplicative_expression", "'"+mult+"'", "power_expression" },
		{ "multiplicative_expression", "multiplicative_expression", "'"+div+"'", "power_expression" },
		{ "multiplicative_expression", "multiplicative_expression", "'"+mod+"'", "power_expression" },

		{ "additive_expression", "multiplicative_expression" },
		{ "additive_expression", "additive_expression", "'"+plus+"'", "multiplicative_expression" },
		{ "additive_expression", "additive_expression", "'"+minus+"'", "multiplicative_expression" },

		{ "shift_expression", "additive_expression" },
		{ "shift_expression", "shift_expression", "\""+lshift+"\"", "additive_expression" },
		{ "shift_expression", "shift_expression", "\""+rshift+"\"", "additive_expression" },
		{ "shift_expression", "shift_expression", "\""+urshift+"\"", "additive_expression" },

		{ "and_expression", "shift_expression" },
		{ "and_expression", "and_expression", "'"+and+"'", "shift_expression" },

		{ "exclusive_or_expression", "and_expression" },
		{ "exclusive_or_expression", "exclusive_or_expression", "'"+xor+"'", "and_expression" },

		{ "inclusive_or_expression", "exclusive_or_expression" },
		{ "inclusive_or_expression", "inclusive_or_expression", "'"+or+"'", "exclusive_or_expression" },

		{ "argument", "`floatnumber`" },	// e.g. "0,12E-34", with local floating point
		{ "argument", "`taggednumber`" },		// e.g. "xFFFF"
	};


	private Object result;


	/** Allokiert einen globalen Parser und Lexer, wenn noch nicht geschehen. */
	public Calculator()	{
		if (parser == null)	{
			try	{
				SyntaxSeparation separation = new SyntaxSeparation(new Syntax(Calculator.lexerSyntax));
				LexerBuilder lb = new LexerBuilder(separation.getLexerSyntax(), separation.getIgnoredSymbols());
				lexer = lb.getLexer();
				ParserTables pt = new LALRParserTables(new Syntax(Calculator.parserSyntax));
				parser = new Parser(pt);
				semantic = new CalculatorSemantic();	// holds no state -> reusable
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
	}
	
	/** Konstruiert einen Calculator und startet den Parser mit dem uebergebenen Text. */
	public Calculator(String arithmeticExpression)	{
		this(arithmeticExpression, null);
	}

	/** Konstruiert einen Calculator und startet den Parser mit dem uebergebenen Text. */
	public Calculator(String arithmeticExpression, PrintStream errStream)	{
		this();
		calculate(arithmeticExpression, errStream);
	}
	
	/** Startet den Parser mit dem uebergebenen Text. */
	public Object calculate(String arithmeticExpression)	{
		return calculate(arithmeticExpression, null);
	}
	
	/** Startet den Parser mit dem uebergebenen Text. */
	public Object calculate(String arithmeticExpression, PrintStream errStream)	{
		Object ret = null;
		arithmeticExpression = Replace.replace(arithmeticExpression, ".", ""+decimalSeparator);
		
		try	{
			parser.setPrintStream(errStream);
			lexer.setInput(arithmeticExpression);
			parser.parse(lexer, semantic);
			ret = parser.getResult();
		}
		catch (Exception e)	{
			if (errStream != null)
				errStream.println(e.getMessage());
			else
				System.err.println("Error when calculating: "+e);
		}
		
		return result = ret;
	}
	
	
	/** Liefert das Resultat des Parser-Laufs. */
	public Object getResult()	{
		return result;
	}
	
	/** Setzt das Resultat des letzten Parser-Laufs auf null. */
	public void clearResult()	{
		result = null;
	}
	
	
	
	// test main
	public static void main(String [] args)	{
		System.out.println(new Calculator("3 * (4 + 7) - 15 >> 2").getResult());	// = 4
		System.out.println(new Calculator("(4 - -7,4 >>> 1\n) * 30").getResult());	// = 150
		System.out.println(new Calculator("0").getResult());
		System.out.println(new Calculator("2 POW 6").getResult());
		System.out.println(new Calculator("SQRT 4").getResult());
		//System.out.println(new Calculator("").getResult());	// error!
	}

}
