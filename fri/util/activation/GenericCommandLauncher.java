package fri.util.activation;

import java.io.*;
import java.net.URL;
import javax.activation.*;
import fri.util.os.OS;

/**
	A generic application launcher for UNIX and WINDOWS, implemented as CommandObject.
	Put it into command map like this:
	<pre>
		application/*;; x-java-view=fri.util.activation.GenericCommandLauncher
		image/*;; x-java-view=fri.util.activation.GenericCommandLauncher
	</pre>
	This provides the static method <i>installCommandMap()</i> to install platform-specific
	CommandMap implementations contained within this package. Be sure to call this just once.
	The classes are loaded by Class.forName(), so none of the implementations have to be
	present to use this CommandObject implementation.
*/

public class GenericCommandLauncher implements CommandObject
{
	/** Implementing CommandObject: detect platform and delegate to an appropriate application launcher. */
	public void setCommandContext(String verb, DataHandler dh)
		throws IOException
	{
		try	{
			// the launchers only accept file data sources
			// store the data into temporary file
			DataSource ds = dh.getDataSource();
			
			if (ds instanceof FileDataSource == false && ds instanceof URLDataSource == false)	{	// write content to file
				// try to create a file source for mail attachment sources
				final String mimeType = dh.getContentType();
				
				File createdFile = StreamToTempFile.create(dh.getInputStream(), decodeName(ds.getName()), mimeType);
				// this call tries to get extension from MIME type when filename is not set
				
				// create new activation arguments
				ds = new FileDataSource(createdFile);
				dh = new DataHandler(ds)	{	// the new DataSource might have a wrong MIME type
					public String getContentType() {
						return mimeType;
					}
				};
			}
			
			String className = OS.isWindows ? "fri.util.activation.Win32Shell" : "fri.util.activation.MailcapCommandLauncher";
			Class cls = Class.forName(className);
			CommandObject co = (CommandObject)cls.newInstance();
			co.setCommandContext(verb, dh);
		}
		catch (Exception e)	{
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}


	/** To be overridden by applications that need to decode the name text as it comes from a Message header. */
	protected String decodeName(String name)	{
		return name;
	}


	/**
	 * Installs a platform specific CommandMap/FileTypeMap and returns the current map that was substituted.
	 * <p />
	 * FRi 2008-01-14: this is code duplication of JAFView.
	 * The difference is that in JAFView implementations are loaded explicitely, whereby
	 * here they are loaded by Class.forName().
	 */
	public static CommandMap installCommandMap()	{
		CommandMap original = CommandMap.getDefaultCommandMap();
		
		if (OS.isWindows)	{
			try	{
				if (Win32Shell.testWin32ActivationDLLs())	{
					System.err.println("installing WINDOWS FileTypeMap and CommandMap ...");
					FileTypeMap.setDefaultFileTypeMap((FileTypeMap) Class.forName("fri.util.activation.Win32RegistryFileTypeMap").newInstance());
					CommandMap.setDefaultCommandMap((CommandMap) Class.forName("fri.util.activation.Win32RegistryCommandMap").newInstance());
				}
			}
			catch (Throwable e)	{
				e.printStackTrace();
				System.err.println("FEHLER: icejni.jar muss im CLASSPATH und ICE_JNIDDE.dll + ICE_JNIRegistry.dll im library-path stehen!");
			}
		}
		else
		if (OS.isUnix)	{
			try	{
				System.err.println("installing UNIX CommandMap ...");
				CommandMap.setDefaultCommandMap((CommandMap) Class.forName("fri.util.activation.MailcapCommandMap").newInstance());
				FileTypeMap.setDefaultFileTypeMap((FileTypeMap) Class.forName("fri.util.activation.MimetypesFileTypeMap").newInstance());
			}
			catch (Throwable e)	{
				e.printStackTrace();
			}
		}
		// else let Sun javax.activation default map in activation.jar work
		
		return original;
	}

	/**
	 * Launching "open" on passed file, when this is not supported, try "view".
	 * @return the result of getCommandObject() call, which might be instanceof Component and could be embedded.
	 * 		Returning null means the command failed.
	 */
	public static Object openFile(File file)
		throws IOException, ClassNotFoundException
	{
		FileDataSource ds = new FileDataSource(file);
		DataHandler dh = new DataHandler(ds);
		
		Object o = doVerb(dh, "open");
		if (o != null)
			if (o == dh)
				return file;
			else
				return o;

		o = doVerb(dh, "view");
		if (o == dh)
			return file;
		else
			return o;
	}
	
	/**
	 * Launching "open" on passed URL, when this is not supported, try "view".
	 * @return the result of getCommandObject() call, which might be instanceof Component and could be embedded.
	 * 		Returning null means the command failed.
	 */
	public static Object openUrl(URL url)
		throws IOException, ClassNotFoundException
	{
		URLDataSource ds = new URLDataSource(url);
		DataHandler dh = new DataHandler(ds);
		
		Object o = doVerb(dh, "open");
		if (o != null)
			if (o == dh)
				return url;
			else
				return o;
	
		o = doVerb(dh, "view");
		if (o == dh)
			return url;
		else
			return o;
	}

	private static Object doVerb(DataHandler dh, String verb)
		throws IOException, ClassNotFoundException
	{
		CommandInfo ci = dh.getCommand(verb);
		if (ci != null)	{
			Object o = ci.getCommandObject(dh, dh.getClass().getClassLoader());	//dh.getBean(ci) catches Exceptions and does not report them
			if (o != null)
				return o;	// success!
			return dh;	// success(?)
		}
		return null;	// failed
	}

	/** test main
	public static void main(String [] args)
		throws Exception
	{
		installCommandMap();
		
		String file = "images/close.gif";
		Object o = openFile(new File(file));
		System.err.println("Opening file "+file+" succeeded: "+(o != null));
		
		String url = "http://www.google.com";
		o = openUrl(new URL(url));
		System.err.println("Opening URL "+url+" succeeded: "+(o != null));
	}
	*/
}
