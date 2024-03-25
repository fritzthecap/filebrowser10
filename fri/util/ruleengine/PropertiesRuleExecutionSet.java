package fri.util.ruleengine;

import java.io.InputStream;
import java.net.URL;
import java.util.*;
import fri.util.props.TableProperties;

/**
	Implements a simple RuleExecutionSet that reads from a sorted property file
	and builds 0-n rules as Vectors. Every position in a rule Vector has a dedicated
	semantic role as documented in the example below. The condition-fields and the
	action-names are specific to a ruleengine client application. No compound (composite)
	conditions are supported (dispatching is sequential).
	<pre>
	---------------------------------------------------------------------------------------
	Bedingungslogik  Bedingungsfeld Vergleich Bedingungswert   Aktion      Aktionsparameter
	---------------------------------------------------------------------------------------
	Wenn             Empfaenger     enthaelt  niklas@chello.at empfangen
	Wenn nicht       Absender       gleich    fri@soon.com     verschieben Personal/Unknown
	Wenn             Absender       enthaelt  ad@vertis.ment   loeschen
	---------------------------------------------------------------------------------------
	</pre>

	@see fri.util.ruleengine.DefaultRuleSession
	@author Fritz Ritzberger, 2003
*/

public class PropertiesRuleExecutionSet
{
	public static final int CONDITION_LOGIC = 0;
	public static final int CONDITION_FIELDNAME = 1;
	public static final int COMPARISON_METHOD = 2;
	public static final int CONDITION_VALUE = 3;
	public static final int ACTION_NAME = 4;
	public static final int ACTION_ARGUMENT = 5;
	public static final String entityType = "rule";
	public static final String [] attributes = new String [ACTION_ARGUMENT + 1];
	static	{
		attributes[CONDITION_LOGIC] = "conditionLogic";	// if, if not
		attributes[CONDITION_FIELDNAME] = "conditionFieldname";	// name of java field
		attributes[COMPARISON_METHOD] = "comparisonMethod";	// equals, matches
		attributes[CONDITION_VALUE] = "conditionValue";	// value to compare with
		attributes[ACTION_NAME] = "actionName";	// name of method to invoke
		attributes[ACTION_ARGUMENT] = "actionArgument";	// optional arguments to be passed to action invocation
	};

	public static final String IF_LOGIC = "if";
	public static final String IF_NOT_LOGIC = "if_not";
	public static final String [] possibleConditionLogics = new String []	{
		IF_LOGIC,	// wenn
		IF_NOT_LOGIC,	// wenn nicht
	};
	
	public static final String MATCH_COMPARISON = "matches";
	public static final String EQUAL_COMPARISON = "equals";
	public static final String [] possibleComparisonMethods = new String []	{
		MATCH_COMPARISON,	// enthaelt
		EQUAL_COMPARISON,	// identisch
	};
	
	private static Hashtable cache = new Hashtable();
	
	private String name;
	private List rules;

	
	private PropertiesRuleExecutionSet(String name, List rules)	{
		this.name = name;
		this.rules = rules;
	}
	
	public String getName()	{
		return name;
	}

	public List getRules()	{
		return rules;
	}
	

	// factory method that reads from persistence
	
	/** Returns a PropertiesRuleExecutionSet read from passed URL. */
	public static PropertiesRuleExecutionSet getRuleSet(String bindUrl)	{
		if (bindUrl == null)
			return null;
		
		Object o = cache.get(bindUrl);
		if (o != null)
			return (PropertiesRuleExecutionSet)o;
		
		PropertiesRuleExecutionSet ruleSet = null;

		// read in the ruleset and register with passed uri
		try	{
			URL url = new URL(bindUrl);
			Properties props = new Properties();
			InputStream is = url.openStream();
			props.load(url.openStream());
			is.close();
			Vector rules = TableProperties.convert(props, entityType, attributes);
			ruleSet = new PropertiesRuleExecutionSet(bindUrl, rules);

			cache.put(bindUrl, ruleSet);	// put to cache
		}
		catch (Exception e)	{	// might not be there, not an error
			//e.printStackTrace();
		}

		return ruleSet;
	}

	/** Clears the ruleset cahce of this factory (force new read from disk). */
	public static void clearCache()	{
		cache = new Hashtable();
	}

}
