package fri.util.mail;

import javax.mail.URLName;
import java.util.Properties;

/**
	This is the data struct for mail configuration, holding all basic parameters to send
	and receive mail, ready to be passed to some <i>javax.mail.Session</i>.
	Its URLName could be stored to some config file, removing the password before saving.
	<p>
	Translates URLName to mail properties as documented by the Sun Mail API and vice versa.
	The constructor URL is something like
	<pre>
		pop3://myName:myPassword@my.mail.host:port/
		localstore:/home/me/Mail
		localstore:C:\myMail
	</pre>
*/

public class MailProperties extends Properties
{
	private URLName urlName;


	/** Empty configuration constructor. */
	public MailProperties()	{
	}
	
	/** Configuration constructor with basic parameters for a mail connection. */
	public MailProperties(String protocol, String host, String port, String user, String password)	{
		// try to get numeric port for URLName constructor
		int portNr = -1;
		try	{
			portNr = port != null ? Integer.parseInt(port) : -1;
			if (portNr < 0)	{	// try to split from hostname:port
				int i = host.indexOf(":");
				if (i > 0)	{
					host = host.substring(0, i);
					String p = host.substring(i + 1);
					portNr = Integer.parseInt(p);
				}
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		
		this.urlName = new URLName(protocol == null ? "pop3" : protocol, host, portNr, null, user, password);
		
		init();
	}
	
	public MailProperties(String url)	{
		this.urlName = new URLName(url);
		
		init();
	}

	private void init()	{
		String protocol = urlName.getProtocol();
		if (protocol != null)
			setProperty("mail.store.protocol", protocol);
		
		if (protocol != null && urlName.getHost() != null)
			setProperty("mail."+protocol+".host", urlName.getHost());

		if (protocol != null && urlName.getPort() > 0)
			setProperty("mail."+protocol+".port", ""+urlName.getPort());

		if (protocol != null && urlName.getUsername() != null)
			setProperty("mail."+protocol+".user", urlName.getUsername());

		extendedInit();
	}
	
	/** Override for extended init(). */
	protected void extendedInit()	{
	}
	

	public boolean isValid()	{
		return getHost() != null && getUser() != null;
	}


	public boolean isLocal()	{
		return getHost() == null;
	}



	public String getProtocol()	{
		return getURLName().getProtocol();
	}

	public String getHost()	{
		return getURLName().getHost();
	}

	public String getPort()	{
		return ""+getURLName().getPort();
	}

	public String getUser()	{
		return getURLName().getUsername();
	}

	public String getPassword()	{
		return getURLName().getPassword();
	}

	public boolean getRememberPassword()	{
		String s = getProperty("rememberPassword");
		return s != null && s.equalsIgnoreCase("true");
	}



	public void setProtocol(String protocol)	{
		setProperty("mail.store.protocol", protocol);
		this.urlName = null;
	}

	public void setHost(String host)	{
		if (getProtocol() == null)
			throw new IllegalStateException("Can not set host when protocol is not yet defined!");
		
		setOrUnsetProperty("mail."+getProtocol()+".host", host);
	}

	public void setPort(String port)	{
		if (getProtocol() == null)
			throw new IllegalStateException("Can not set port when protocol is not yet defined!");

		setOrUnsetProperty("mail."+getProtocol()+".port", port);
	}

	public void setUser(String user)	{
		if (getProtocol() == null)
			throw new IllegalStateException("Can not set user when protocol is not yet defined!");

		setOrUnsetProperty("mail."+getProtocol()+".user", user);
	}

	public void setPassword(String password)	{
		setOrUnsetProperty("mail."+getProtocol()+".password", password);
	}

	public void setRememberPassword(boolean rememberPassword)	{
		setProperty("rememberPassword", rememberPassword ? "true" : "false");
	}


	public URLName getURLName()	{
		if (urlName == null)	{
			String protocol = getProperty("mail.store.protocol");
			String host = getProperty("mail."+protocol+".host");
			String port = getProperty("mail."+protocol+".port");
			String user = getProperty("mail."+protocol+".user");
			String password = getProperty("mail."+protocol+".password");
			urlName = new URLName(
					protocol,
					host,
					(port != null && port.length() > 0 ? Integer.parseInt(port) : -1),
					null,
					user,
					password);
		}
		return urlName;
	}


	private void setOrUnsetProperty(String name, String value)	{
		manageProperty(name, value);
		this.urlName = null;	// refresh URLName
	}
	
	protected void manageProperty(String name, String value)	{
		name = name.trim();
		if (value != null)
			value = value.trim();
		
		if (value != null && value.length() > 0)
			setProperty(name, value);
		else
			remove(name);
	}
	

	/*
	public static void main(String [] args)	{
		System.err.println(new MailProperties().getURLName());
		MailProperties mp = new MailProperties("pop3://myname:mypassword@mailhost:110/");
		System.err.println(mp.getURLName()+" is local: "+mp.isLocal());
		mp.list(System.err);
		mp = new ReceiveProperties("localstore:E:\\Mail");
		System.err.println(mp.getURLName()+" is local: "+mp.isLocal());
		System.err.println(mp.getURLName()+" file: "+mp.getURLName().getFile());
		mp.list(System.err);
	}
	*/

}
