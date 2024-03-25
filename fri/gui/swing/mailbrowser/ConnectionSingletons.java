package fri.gui.swing.mailbrowser;

import java.util.*;
import java.io.IOException;
import fri.util.crypt.Crypt;
import fri.util.dump.NumericDump;
import fri.util.props.ClassProperties;
import fri.util.mail.*;

/**
	Encapsulate singleton connections for send and receive mail.
	This represents the mail clients default connections where
	to get from and put to mails.
*/

public abstract class ConnectionSingletons
{
	private static ReceiveProperties receiveSingleton;
	private static SendProperties sendSingleton;
	private static final String key = "4uasurthe1";	// random encryption key


	/** Returns the singleton send properties instance. */
	public static SendProperties getSendInstance()	{
		if (sendSingleton != null)
			return sendSingleton;
		
		sendSingleton = new SendProperties();
		setSendData(ClassProperties.getProperties(SendProperties.class));	// loads properties from disk
		return sendSingleton;
	}
	
	/** Sets initial commandline send properties. */
	public static void setSendInstance(String host)	{
		setSendData(new SendProperties(host));	// parses host and port from parameter
	}
	
	private static void setSendData(Properties source)	{
		if (sendSingleton != null)
			sendSingleton.clear();
		else
			sendSingleton = new SendProperties();

		readProperties(source, sendSingleton);
		ClassProperties.setProperties(SendProperties.class, sendSingleton);
	}
	


	/** Returns the singleton receive properties instance. */
	public static ReceiveProperties getReceiveInstance()	{
		if (receiveSingleton != null)
			return receiveSingleton;
		
		receiveSingleton = new ReceiveProperties();
		setReceiveData(ClassProperties.getProperties(ReceiveProperties.class));	// loads properties from disk
		return receiveSingleton;
	}
	
	/** Sets initial commandline receive properties. */
	public static void setReceiveInstance(String host, String user, String password)	{
		setReceiveData(new ReceiveProperties(null, host, null, user, password));	// parses host and port from parameter
	}
	
	private static void setReceiveData(Properties source)	{
		if (receiveSingleton != null)
			receiveSingleton.clear();
		else
			receiveSingleton = new ReceiveProperties();

		readProperties(source, receiveSingleton);
		ClassProperties.setProperties(ReceiveProperties.class, receiveSingleton);
	}
	


	private static void readProperties(Properties source, MailProperties target)	{
		for (Enumeration e = source.propertyNames(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			String value = source.getProperty(name);
			
			// decrypt password when coming from disk
			if (name.endsWith(".password") && source instanceof ReceiveProperties == false)	{
				try	{
					Crypt dec = new Crypt(key, null);
					byte [] bytes = dec.getBytes(NumericDump.fromNumberString(value, 31), false);
					value = new String(bytes);
				}
				catch (IOException ex)	{
					ex.printStackTrace();
				}
			}

			target.setProperty(name, value);
		}
	}
	
	private static void encryptPassword(MailProperties props)	{
		if (props.getRememberPassword() && props.getPassword() != null)	{
			String value = props.getPassword();
			byte [] bytes = value.getBytes();
			try	{
				Crypt enc = new Crypt(key, null);
				bytes = enc.getBytes(bytes, true);
				value = NumericDump.toNumberString(bytes, 31, 0);	// 31: base, 0: do not append any newline
				props.setPassword(value);
			}
			catch (IOException e)	{
				e.printStackTrace();
			}
		}
		else
		if (props.getPassword() != null)	{
			props.setPassword(null);
		}
	}



	/** Stores send and receive properties to disk. */
	public static void store()	{
		encryptPassword(getSendInstance());
		encryptPassword(getReceiveInstance());
		ClassProperties.store(ReceiveProperties.class);
		ClassProperties.store(SendProperties.class);

		// force new decrypting load when password was encrypted
		ClassProperties.clearCache(ReceiveProperties.class);
		ClassProperties.clearCache(SendProperties.class);
		sendSingleton = null;
		receiveSingleton = null;
	}


	private ConnectionSingletons()	{}

}