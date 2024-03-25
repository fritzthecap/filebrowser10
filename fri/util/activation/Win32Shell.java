package fri.util.activation;

import java.io.*;
import javax.activation.*;
import com.ice.jni.dde.*;

public class Win32Shell implements CommandObject
{
	/** Implementing CommandObject: calling JNIDDE.shellExecute(). */
	public void setCommandContext(String verb, DataHandler dh) throws IOException
	{
		System.err.println("Win32Shell.setCommandContext "+verb+", datahandler "+dh);
		if (verb == null || dh == null)
			throw new IllegalArgumentException("Verb or DataHandler is null: verb = "+verb+", DataHandler = "+dh);

		DataSource dataSource = dh.getDataSource();
		
		String path;
		String parent;
		if (dataSource instanceof FileDataSource)	{
			File f = new File(((FileDataSource)dataSource).getFile().getAbsolutePath());	// JNIDDE must get an absolute path
			path = f.getPath();
			parent = f.getParent();
		}
		else
		if (dataSource instanceof URLDataSource)	{
			path = ((URLDataSource)dataSource).getURL().toExternalForm();
			parent = path;	// shellExecute() needs a parent, else crash
		}
		else
			throw new IOException("Only FileDataSource or URLDataSource accepted to MailcapCommandLauncher.setCommandContext(): "+dataSource);

		try	{
			shellExecute(verb, path, parent);
		}
		catch (UnsatisfiedLinkError ex)	{
			ex.printStackTrace();
			System.err.println(getNotFoundErrorText());
			throw new IOException(getNotFoundErrorText());
		}
		catch (Exception ex)	{
			try	{	// make a last try with "open"
				shellExecute("open", path, parent);
				return;
			}
			catch (Exception ex2)	{
			}
			ex.printStackTrace();
			throw new IOException(ex.toString());
		}
	}

	private void shellExecute(String verb, String path, String parent)
		throws Exception
	{
		JNIDDE dde = new JNIDDE(); // do not remove, this is to check the dll
		System.err.println("-> Calling JNIDDEshellExecute "+verb+" with "+path+", parent "+parent);
		JNIDDE.shellExecute(verb, path, null, parent, JNIDDE.SW_SHOWNORMAL);
	}


	/** Returns true when JNIDDE native library could be loaded (is present and useable). */
	public static boolean testWin32ActivationDLLs()	{
		try	{
			JNIDDE dde = new JNIDDE(); // Do this just to check the dll
			return dde != null;
		}
		catch (UnsatisfiedLinkError ex)	{
			//ex.printStackTrace();
			System.err.println(getNotFoundErrorText());
			return false;
		}
	}

	public static String getDdeDLLBaseName()	{
		return "ICE_JNIDDE.dll";
	}
	
	public static String getRegistryDLLBaseName()	{
		return "ICE_JNIRegistry.dll";
	}
	
	public static String getNotFoundErrorText()	{
		return "ICE_JNIDDE.dll and ICE_JNIRegistry.dll must be in (one of): "+System.getProperty("java.library.path");
	}
	
	
	/** test main
	public static void main(String [] args)
		throws Exception
	{
		{
			String filename = "C:\\Dokumente\\Führerscheinantrag.doc";
			
			CommandMap.setDefaultCommandMap(new Win32RegistryCommandMap());
			FileTypeMap.setDefaultFileTypeMap(new Win32RegistryFileTypeMap());
	
			File f = new File(filename);
			DataSource ds = new FileDataSource(f);
			DataHandler dh = new DataHandler(ds);
			System.err.println("Content Type: "+dh.getContentType().toLowerCase());
			CommandInfo ci = dh.getCommand("open");
			Object bean = dh.getBean(ci);
		}
		
		{
			CommandMap.setDefaultCommandMap(new Win32RegistryCommandMap());
			URLDataSource ds = new URLDataSource(new java.net.URL("http://www.google.com/index.html")); 
			System.err.println("DataSource name is: "+ds.getName()+", URL "+ds.getURL());
			DataHandler dh = new DataHandler(ds); 			
			CommandInfo ci = dh.getCommand("open");
			Object co = ci.getCommandObject(dh, null);
		}
	}
	*/
}
