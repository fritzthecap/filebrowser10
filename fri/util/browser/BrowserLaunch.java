package fri.util.browser;

import java.io.IOException;
import fri.util.os.OS;

/**
 * <p>Utilities for generic browser handling (platform independent).
 * </p><p>
 * For WINDOWS the command "rundll32.exe url.dll,FileProtocolHandler URL" is used.
 * For all other operating systems the System-property <i>browser</i>
 * is supported, default this is "sensible-browser" for Linux, and "firefox" for others.
 * The ULR is then opened by executing "browser URL".
 * To control the browser to launch, make a Java command line like
 * <pre
 * 		java -Dbrowser=mozilla ...
 * </pre>
 * </p>
 * 
 * TODO try GenericCommandLauncher for opening URL or File.
 * 
 * @author Fritz Ritzberger, 2008
 */
public abstract class BrowserLaunch
{
	/** Open the passed ULR in an external browser window/process. */
	public static void openUrl(String url)
		throws IOException
	{
		if (OS.isWindows)	{
			String [] command = new String [] { "rundll32.exe", "url.dll,FileProtocolHandler", url };
			Runtime.getRuntime().exec(command);
		}
		else	{
			String browserName = System.getProperty("browser.name");
			browserName = (browserName != null && browserName.length() > 0) ? browserName : null;

			IOException exception;
			
			if (browserName != null)	// first try user directive
				if ((exception = openUrl(browserName, url)) == null)
					return;
				else
					throw exception;	// user directive failed, exit
			
			// different solutions, taken from Debian-LINUX perl script "sensible-browser"
			if (OS.isLinux && (exception = openUrl("sensible-browser", url)) == null)
				return;
			
			if ((exception = openUrl("www-browser", url)) == null)	// maybe this exists on Mac and UNIX?
				return;
			
			browserName = System.getenv("BROWSER");
			if (browserName != null && browserName.length() > 0)
				if ((exception = openUrl(browserName, url)) == null)
					return;
			
			if (OS.isUnix && (exception = openUrl("x-www-browser", url)) == null)
				return;
			
			if ((exception = openUrl("firefox", url)) == null)	// is quite popular ...
				return;

			throw exception;
			
			// command = "firefox", "-remote", "\"openURL("+url+", new-tab)\"" };	// fails when no browser is open
		}
	}
	
	private static IOException openUrl(String browserName, String url)	{
		String [] command = new String [] { browserName, url };
		System.err.println("Trying browser commandline: "+browserName+" "+url);
		try	{
			Runtime.getRuntime().exec(command);
			return null;	// succeeded
		}
		catch (IOException ex)	{	// command not found
			return ex;	// failed
		}
	}
	

	private BrowserLaunch()	{}	// do not instantiate

	/** test main
	public static void main(String [] args)
		throws Exception
	{
		openUrl("http://www.google.com");
	}
	*/
}
