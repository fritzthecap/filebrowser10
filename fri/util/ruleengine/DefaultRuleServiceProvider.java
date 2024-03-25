package fri.util.ruleengine;

public class DefaultRuleServiceProvider
{
	private DefaultRuleRuntime runtime;
	private DefaultRuleAdministrator admin;
	
	public DefaultRuleServiceProvider()	{
	}
	
	public DefaultRuleRuntime getRuleRuntime()	{
		return runtime != null ? runtime : (runtime = new DefaultRuleRuntime(getRuleAdministrator()));
	}

	public DefaultRuleAdministrator getRuleAdministrator()	{
		return admin != null ? admin : (admin = new DefaultRuleAdministrator());
	}

}
