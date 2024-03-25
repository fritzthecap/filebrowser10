package fri.util.file;

import java.io.*;
import fri.util.os.OS;

public abstract class Link
{
	private static File lastOne;
	private static String lastResult;
	
	/** Test if the passed file is a link file. */
	public static boolean isLink(File f)	{
		return getLinkTarget(f) != null;
	}
	
	/** Returns null if the passed file is not a link, else its target file. */
	public static String getLinkTarget(File f)	{
		String canonicalPath = getCanonicalPath(f);
		String absolutePath = f.getAbsolutePath();
		if (canonicalPath.equals(absolutePath))
			return null;
		return canonicalPath;
	}
	
	/** Returns the absolute path this link file points to. */
	public static synchronized String getCanonicalPath(File f)	{
		if (f == lastOne)
			return lastResult;
			
		if (lastOne != null && f.equals(lastOne))
			return lastResult;
			
		lastOne = f;
		
		try	{
			if (isWindowsLink(f))	{
				WindowsShortcut ws = new WindowsShortcut(f);
				if (ws.getTarget() != null)
					return lastResult = ws.getTarget();
			}
			return lastResult = f.getCanonicalPath();
		}
		catch (Exception e)	{	// happens at network files under WINDOWS
			return lastResult = f.getAbsolutePath();
		}
	}

	private static boolean isWindowsLink(File f)	{
		return OS.isWindows && f.getName().toUpperCase().endsWith(".LNK");
	}


	private Link()	{}	// do not instantiate

}
