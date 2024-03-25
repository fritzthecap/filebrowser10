package fri.gui.swing.install;

import java.io.*;
import fri.util.install.ResourceToLibraryPath;

/**
	Install libraries from classloader root to java.library.path.
	@see fri.util.install.ResourceToLibraryPath
*/

public abstract class InstallLibraries
{
	/**
		Searches passed names in java.library.path and copies them
		from classloader root to it when not found.
		@see fri.util.install.ResourceToLibraryPath
	*/
	public static void ensure(String [] resourceNames)	{
		String targets = "", errors = "";
		
		for (int i = 0; i < resourceNames.length; i++)	{
			// test if DLL is there
			if (ResourceToLibraryPath.existsTargetFile(resourceNames[i]) == false)	{
				File f = ResourceToLibraryPath.targetFile(resourceNames[i]);
				
				if (f != null && copyResource(resourceNames[i], f))
					targets = targets+"\n  "+resourceNames[i]+"  to  "+f.getParent();
				else
					errors = errors+"\n  "+resourceNames[i]+"  to  "+(f != null ? f.getParent() : "null");
			}
		}
		
		// report copy
		if (targets.length() > 0)	{
			System.err.println("COPIED REGISTRY-LIBRARIES TO CURRENT PATH");
			/* FRi 2003-06-23: Deadlocks the GUI if a modeless dialog is up.
			JOptionPane.showMessageDialog(
					null,
					"Copied: "+targets,
					"Copied Libraries",
					JOptionPane.INFORMATION_MESSAGE);
			*/
		}
		
		if (errors.length() > 0)	{
			System.err.println("ERROR: COULD NOT COPY REGISTRY-LIBRARIES TO CURRENT PATH");
			/* FRi 2003-06-23: Deadlocks the GUI if a modeless dialog is up.
			JOptionPane.showMessageDialog(
					null,
					"Could Not Copy: "+errors,
					"Error Copying Libraries",
					JOptionPane.ERROR_MESSAGE);
			*/
		}
	}


	private static boolean copyResource(String dll, File f)	{
		System.err.println("Copying Resource "+dll+" to "+f);
		if (f == null)
			return false;
		try	{
			InputStream in = InstallLibraries.class.getResourceAsStream("/"+dll);
			OutputStream out = new FileOutputStream(f);
			
			byte[] buf = new byte[4096];
			int len;
			while ((len = in.read(buf)) != -1)	{
				out.write(buf, 0, len);
			}
			out.flush();
			out.close();
			in.close();
		}
		catch (IOException e)	{
			e.printStackTrace();
			return false;
		}
		return true;
	}

}