package fri.gui.swing.mailbrowser;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import fri.util.mail.LocalStore;

/**
	Assumes that the mail rules are in local store root folder, in a file named ".mailrules.properties".
	Returns a URL to there and provides a way to test if the file exists.
*/

public abstract class RulesUrl
{
	public static boolean exists()	{
		return new File(makeRulesFileName()).isFile();
	}
	
	public static URL getMailRulesUrl()	{
		try	{
			return new URL("file", null, makeRulesFileName());
		}
		catch (MalformedURLException e)	{
			e.printStackTrace();
			return null;
		}
	}

	private static String makeRulesFileName()	{
		String localStoreDir = LocalStore.localStoreDirectory().replace(File.separatorChar, '/');
		if (localStoreDir.endsWith("/") == false)
			localStoreDir = localStoreDir+"/";
		return localStoreDir+".mailrules.properties";
	}


	private RulesUrl()	{}

}
