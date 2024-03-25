package fri.util.mail;

/**
	Extends ReceiveProperties by adding from and personal property.
*/

public class SendProperties extends MailProperties
{
	public SendProperties()	{
	}
	
	public SendProperties(String host)	{
		super("smtp", host, null, null, null);
	}
	

	public boolean isValid()	{
		return getHost() != null;
	}


	public String getFrom()	{
		return getProperty("mail.smtp.from");
	}

	public String getPersonal()	{
		return getProperty("personalName");
	}


	public void setFrom(String from)	{
		manageProperty("mail.smtp.from", from);
	}

	public void setPersonal(String personal)	{
		manageProperty("personalName", personal);
	}

}
