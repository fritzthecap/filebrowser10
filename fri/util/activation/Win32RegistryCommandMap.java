package fri.util.activation;

import java.util.*;
import javax.activation.*;
import com.ice.jni.registry.*;

/**
	Extends CommandMap to provide WINDOWS Registry command verbs.
	Uses a native library for that purpose.
*/

public class Win32RegistryCommandMap extends CommandMap
{
	private static Hashtable cache = new Hashtable();
	private String commandObjectClassName = "fri.util.activation.GenericCommandLauncher";	// creates FileDataSource if necessary


	/**
		Get the command for verb corresponding to the MIME type.
	*/
	public CommandInfo getCommand(String mimeType, String verb)	{
		CommandInfo [] ci = getCommands(mimeType, verb);
		if (ci != null && ci.length >= 1)
			return ci[0];
		return null;
	}

	/**
		Get all the available commands for this type.
		This is implemented the same as getPreferredCommands().
	*/
	public CommandInfo[] getAllCommands(String mimeType)	{
		return getCommands(mimeType, null);
	}

	/**
		Get the preferred command list from a MIME Type.
		This is implemented the same as getAllCommands().
	*/
	public CommandInfo[] getPreferredCommands(String mimeType)	{
		return getCommands(mimeType, null);
	}


	// do the work
	private CommandInfo [] getCommands(String mimeType, String verb)	{
		if (mimeType == null)
			return null;
			
		CommandInfo [] carr = null;
		if ((carr = find(mimeType, verb)) != null)
			return carr;

		String defaultType = mimeType;	// assume Windows-Type is MIME-Type
		RegistryKey key = null, rkey = Registry.HKEY_CLASSES_ROOT;

		try	{
			// try to get real MIME type, its extension and the Windows-Type for it
			try	{
				// get the extension for mime-type
				key = rkey.openSubKey("MIME\\Database\\Content Type\\"+mimeType);
				String mimeExt = key.getStringValue("Extension");
				key.closeKey();

				// get the Windows-Type for extension
				key = rkey.openSubKey(mimeExt);
				String defValue;
				if (key != null && (defValue = key.getDefaultValue()) != null)
					defaultType = defValue;
			}
			catch (Exception e) {
				System.err.println("No MIME Extension in Registry for: "+mimeType);
			}
			finally	{
				try	{ key.closeKey(); }	catch (Exception e)	{}
			}

			// try to read shell verbs and store its command (application path)
			try	{
				key = rkey.openSubKey(defaultType+"\\shell");
				Vector v = new Vector();
				Enumeration e = key.keyElements();

				for (; e.hasMoreElements(); )	{
					String shellVerb = (String)e.nextElement();
					CommandInfo ci = new CommandInfo(shellVerb, commandObjectClassName);
					v.add(ci);
				}

				key.closeKey();

				if (v.size() > 0)	{
					carr = new CommandInfo[v.size()];
					v.copyInto(carr);
	
					cache.put(mimeType, carr);

					if (verb != null)	// if verb was given, search in cached CommandInfo objects
						return find(mimeType, verb);
				}
			}
			catch (Exception e) {
				System.err.println("No Command in Registry! MIME type = "+mimeType+", WINDOWS type = "+defaultType+", error: "+e.toString());
			}
		}
		finally	{
			try	{ rkey.closeKey(); } catch (Exception e)	{ }
		}

		return carr;
	}


	private CommandInfo [] find(String mimeType, String verb)	{
		CommandInfo [] carr = (CommandInfo[]) cache.get(mimeType);

		if (carr != null && verb != null)	{
			// try exact match
			for (int i = 0; i < carr.length; i++)	{
				if (carr[i].getCommandName().equals(verb))	{
					return new CommandInfo[] { carr[i] };
				}
			}
			// try case insensitive match
			verb = verb.toLowerCase();
			for (int i = 0; i < carr.length; i++)	{
				if (carr[i].getCommandName().toLowerCase().equals(verb))	{
					return new CommandInfo[] { carr[i] };
				}
			}
			return null;
		}
		return carr;
	}


	private CommandMap defaultCommandMap;

	/**
		Locate a DataContentHandler that corresponds to the MIME type.
	*/
	public DataContentHandler createDataContentHandler(String mimeType)	{
		if (defaultCommandMap == null)
			defaultCommandMap = new javax.activation.MailcapCommandMap();
		return defaultCommandMap.createDataContentHandler(mimeType);
	}



	// test main
	public static void main(String[] args)	{
		Win32RegistryCommandMap map = new Win32RegistryCommandMap();
		String type = "text/html";
		//CommandInfo [] arr = map.getAllCommands(type);
		CommandInfo [] arr = map.getPreferredCommands(type);
		System.err.println("CommandInfo for "+type+" is: "+arr);
		for (int i = 0; arr != null && i < arr.length; i++)
			System.err.println(arr[i].getCommandName());
	}

}
