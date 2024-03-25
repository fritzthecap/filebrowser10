package fri.util.mail;

/**
	This is the data struct for ConfigurationDialog, holding all parameters to receive mail.
*/

public class ReceiveProperties extends MailProperties
{
	public ReceiveProperties()	{
	}
	
	public ReceiveProperties(String protocol, String host, String port, String user, String password)	{
		super(protocol, host, port, user, password);
	}
	
	public ReceiveProperties(String url)	{
		super(url);
	}


	/** Init the "mail.user" name for default authentication. */
	protected void extendedInit()	{
		if (getURLName().getUsername() != null)
			setProperty("mail.user", getURLName().getUsername());
	}


	/** Sets the "leaveMailsOnServer" property. Default is false. */
	public void setLeaveMailsOnServer(boolean leaveMailsOnServer)	{
		if (leaveMailsOnServer)
			setProperty("leaveMailsOnServer", "true");
		else
			remove("leaveMailsOnServer");
	}
	
	/** Returns the "leaveMailsOnServer" property. Default is false. */
	public boolean getLeaveMailsOnServer()	{
		String s = getProperty("leaveMailsOnServer");
		return s != null && s.equals("true");
	}


	/** Sets or removes the "checkForNewMailsInterval" property. */
	public void setCheckForNewMailsInterval(int interval)	{
		if (interval > 0)
			setProperty("checkForNewMailsInterval", ""+interval);
		else
			remove("checkForNewMailsInterval");
	}

	/** Returns the "checkForNewMailsInterval" property. Default is -1. */
	public int getCheckForNewMailsInterval()	{
		String s = getProperty("checkForNewMailsInterval");
		if (s != null)	{
			try	{
				return Integer.parseInt(s);
			}
			catch (NumberFormatException e)	{
				e.printStackTrace();
			}
		}
		return -1;
	}

}
