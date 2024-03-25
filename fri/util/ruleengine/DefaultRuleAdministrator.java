package fri.util.ruleengine;

import java.util.*;

public class DefaultRuleAdministrator
{
	private HashMap ruleSets = new HashMap();
	
	public DefaultRuleAdministrator()	{
	}
	
	/** Registers a RuleExecutionSet and associates it with a given URI. */
	public void registerRuleExecutionSet(String bindUrl, PropertiesRuleExecutionSet ruleSet, Map properties)	{
		ruleSets.put(bindUrl, ruleSet);
	}
	
	public void unregisterRuleExecutionSet(String bindUrl, Map properties)	{
		ruleSets.remove(bindUrl);
	}

	PropertiesRuleExecutionSet getRuleExecutionSet(String bindUrl)	{
		return (PropertiesRuleExecutionSet)ruleSets.get(bindUrl);
	}

}
