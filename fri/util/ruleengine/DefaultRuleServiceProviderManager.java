package fri.util.ruleengine;

public abstract class DefaultRuleServiceProviderManager
{
	private static DefaultRuleServiceProvider provider;
	
	public static DefaultRuleServiceProvider getRuleServiceProvider()	{
		return provider != null ? provider : (provider = new DefaultRuleServiceProvider());
	}
}
