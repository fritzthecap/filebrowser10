package fri.util.ruleengine;

import java.util.*;

public class DefaultRuleRuntime
{
	public static final int STATELESS_SESSION_TYPE = 1;
	private Hashtable sessions = new Hashtable();
	private DefaultRuleAdministrator admin;
	
	public DefaultRuleRuntime(DefaultRuleAdministrator admin)	{
		this.admin = admin;
	}
	
	public DefaultRuleSession createRuleSession(String bindUrl, Map properties, int ruleSessionType)	{
		Object o;
		if (ruleSessionType == STATELESS_SESSION_TYPE && (o = sessions.get(bindUrl)) != null)
			return (DefaultRuleSession)o;
		
		DefaultRuleSession session = new DefaultRuleSession(admin.getRuleExecutionSet(bindUrl));
		
		if (ruleSessionType == STATELESS_SESSION_TYPE)
			sessions.put(bindUrl, session);
		
		return session;
	}

}
