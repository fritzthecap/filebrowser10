package fri.util.install;

import java.io.File;
import java.util.StringTokenizer;

/**
	Copy a resource (from JAR or FileSystem) to first writeable
	path in System.getProperty("java.library.path"). This method
	uses getResourceAsStream().
	This method assumes that the passed resource uses "/" as
	separator character, and it is not the last character.
*/

public abstract class ResourceToLibraryPath
{
	/**
		Makes a filename for the passed resource in one of
		System.getProperty("java.library.path"), preferring
		the current directory ".".
		@return File if there is a writeable library path.
	*/
	public static File targetFile(String resourceName)	{
		// get basename of resource
		String fileName = baseName(resourceName);
		
		// try to find an writeable target path in library pathes
		File targetFile = null;
		StringTokenizer stok = getStringTokenizer();
		boolean found = false;
		
		for (; stok.hasMoreTokens() && !found; )	{
			String s = stok.nextToken();
			File dir = new File(s);
			File f = new File(s, fileName);
			//System.err.println("testing "+s);

			if (s.equals(".") && dir.canWrite())	{	// is best
				found = true;	// stop search
				targetFile = f;
			}
			else
			if (targetFile == null && dir.canWrite())	{
				targetFile = f;	// continue search for "."
			}
		}
		
		return targetFile;
	}
	

	private static String baseName(String resourceName)	{
		int last = resourceName.lastIndexOf("/");
		if (last >= 0)
			return resourceName.substring(last + 1);
		return resourceName;
	}

	private static StringTokenizer getStringTokenizer()	{
		return new StringTokenizer(System.getProperty("java.library.path"), File.pathSeparator);
	}
	
	/**
		Returns true if the resourceName exists in one of
		System.getProperty("java.library.path").
	*/		
	public static boolean existsTargetFile(String resourceName)	{
		// get basename of resource
		String fileName = baseName(resourceName);

		// try to find resourceName in library pathes
		StringTokenizer stok = getStringTokenizer();

		for (; stok.hasMoreTokens(); )	{
			String s = stok.nextToken();
			File f = new File(s, fileName);
			if (f.exists())
				return true;
		}
		
		return false;
	}




//	public static void main(String [] args)	{
//		System.err.println(targetFile("Hallo.DLL"));
//		System.err.println(existsTargetFile("Shell32.dll"));
//	}

}