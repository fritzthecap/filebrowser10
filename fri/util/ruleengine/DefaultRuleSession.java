package fri.util.ruleengine;

import java.util.*;

/**
	Applies each rule from a sequential list to all objects. Does not stop at first match.
	<p>
	Every runtime object within the list passed to <i>executeRules()</i> is both
	<ul>
		<li>decision value provider (getter-method) and</li>
		<li>action provider (action method implementer).</li>
	</ul>
	
	@see fri.util.ruleengine.PropertiesRuleExecutionSet
	@author Fritz Ritzberger, 2003
*/

public class DefaultRuleSession
{
	private PropertiesRuleExecutionSet ruleSet;
	
	public DefaultRuleSession(PropertiesRuleExecutionSet ruleSet)	{
		this.ruleSet = ruleSet;
	}
	
	public List executeRules(List objects)	{
		for (Iterator it = ruleSet.getRules().iterator(); it.hasNext(); )	{
			Vector rule = (Vector)it.next();
			
			String conditionLogic =     (String)rule.get(PropertiesRuleExecutionSet.CONDITION_LOGIC);
			String conditionFieldname = (String)rule.get(PropertiesRuleExecutionSet.CONDITION_FIELDNAME);
			String comparisonMethod =   (String)rule.get(PropertiesRuleExecutionSet.COMPARISON_METHOD);
			String conditionValue =     (String)rule.get(PropertiesRuleExecutionSet.CONDITION_VALUE);
			String actionName =         (String)rule.get(PropertiesRuleExecutionSet.ACTION_NAME);
			String actionArgument =     (String)rule.get(PropertiesRuleExecutionSet.ACTION_ARGUMENT);
			
			System.err.println("executing mail rule: "+conditionLogic+" "+conditionFieldname+" "+comparisonMethod+" "+conditionValue+" "+actionName+" "+actionArgument);
			
			boolean positiveCondition = conditionLogic.equals(PropertiesRuleExecutionSet.IF_LOGIC);
			
			for (Iterator it2 = objects.iterator(); it2.hasNext(); )	{
				Object o = it2.next();
				ObjectIntrospector introspector = new ObjectIntrospector(o);
				Object runtimeValue = introspector.getFieldValue(conditionFieldname);
				String runtimeString = runtimeValue != null ? runtimeValue.toString() : "";
				boolean matched;
				
				if (comparisonMethod.equals(PropertiesRuleExecutionSet.EQUAL_COMPARISON))
					matched = runtimeString.equals(conditionValue);
				else
					matched = runtimeString.indexOf(conditionValue) >= 0;
				
				matched = (matched && positiveCondition || !matched && !positiveCondition);
				System.err.println("rule logic matched: "+matched);

				if (matched)	{
					introspector.invokeMethod(actionName, actionArgument.length() > 0 ? actionArgument : null);
				}
			}
		}
		return objects;
	}

	public void release()	{
	}

	public int getType()	{
		return DefaultRuleRuntime.STATELESS_SESSION_TYPE;
	}

}
