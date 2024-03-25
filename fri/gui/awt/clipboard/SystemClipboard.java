package fri.gui.awt.clipboard;

import java.io.*;
import java.util.*;
import java.awt.Toolkit;
import java.awt.datatransfer.*;

/**
	Makes available the contents and the state of system clipboard,
	by which data can be exchanged between different processes (VM's).
*/

public abstract class SystemClipboard
{
	/**
		Returns true if the passed data flavor is in clipboard.
		Catches every exeption and ignores it.
	*/
	public static boolean isInClipboard(DataFlavor flavorToTest)	{
		try	{
			Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable t = c.getContents(SystemClipboard.class);
			if (t == null || t.isDataFlavorSupported(flavorToTest) == false)
				return false;

			return true;
		}
		catch (Exception e)	{
			return false;
		}
	}
	
	/**
		Returns an Object from system clipboard corrsponding to passed data flavor.
		If the passed flavor is not in system clipboard, this returns null.
		For <i>DataFlavor.javaFileListFlavor</i> the return would be a List
		containing File objects.
		Catches IllegalStateExeption and prints a warning.
	*/
	public static Object getFromClipboard(DataFlavor flavorToRetrieve)	{
		try	{
			Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable t = c.getContents(SystemClipboard.class);

			if (t != null || t.isDataFlavorSupported(flavorToRetrieve))
				return t.getTransferData(flavorToRetrieve);
		}
		catch (IllegalStateException e)	{
			System.err.println("WARNING: clipboard is not available: "+e);
		}
		catch (UnsupportedFlavorException e2)	{
			//System.err.println("WARNING: unsupported flavor: "+flavorToRetrieve+", exception was "+e2);
		}
		catch (IOException e3)	{
			System.err.println("WARNING: IOException when retrieving data: "+e3);
		}
		catch (NullPointerException e)	{	// on LINUX ... and WINDOWS
			//System.err.println("ERROR: "+e);
		}
		return null;
	}
	
	
	/**
		Sets the passed Transferable to system clipboard.
		Catches IllegalStateExeption and prints a stack trace.
	*/
	public static void setToClipboard(Transferable data)	{
		try	{
			Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
			c.setContents(data, new DummyClipboardOwner());
		}
		catch (IllegalStateException e)	{
			e.printStackTrace();	// do not interrupt control flow
		}
	}


	/**
	 * Checks for DataFlavor.javaFileList and String that is a file URL.
	 * Mind that this can last long on certain operating systems,
	 * so it would be best to call it from background.
	 * @return a List of File or null.
	 */
	public static List getFilesFromClipboard()	{
		Object files = getFromClipboard(DataFlavor.javaFileListFlavor);
		
		if (files != null)	{
			return (List)files;
		}
		else	{
			String s = getStringFromClipboard();
			
			if (s != null && s.startsWith("file:"))	{
				s = s.substring("file:".length());
				if (s.startsWith("//localhost/"))
					s = s.substring("//localhost".length());
				else
				if (s.startsWith("/localhost/"))
					s = s.substring("/localhost".length());

				File f = new File(s);
				if (f.exists()) {
  				List l = new Vector();
  				l.add(f);
  				return l;
				}
			}
		}
		return null;
	}

	/** Checks for DataFlavor.javaFileList and String that is a file URL. Returns the String or null. */
	public static String getStringFromClipboard()	{
		return (String)getFromClipboard(DataFlavor.stringFlavor);
	}



	private static class DummyClipboardOwner implements ClipboardOwner
	{
		public void lostOwnership(Clipboard clipboard, Transferable contents) {
		}
	}
	
	private SystemClipboard()	{}
}
