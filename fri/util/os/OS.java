package fri.util.os;

/**
	Erkennung des Betriebs-Systems aus <code>System.getProperty("os.name")</code>.
*/

public abstract class OS
{
	static final String osname = System.getProperty("os.name").toLowerCase();
	
	public final static boolean isWindows = 
		osname.indexOf("windows") >= 0 ||
		osname.indexOf("os/2") >= 0;
		
	public final static boolean isLinux =
		osname.indexOf("linux") >= 0;
			
	public final static boolean isUnix =
		isLinux ||
		osname.indexOf("unix") >= 0 ||
		osname.indexOf("solaris") >= 0 ||
		osname.startsWith("sun") && osname.indexOf("os") >= 0 ||
		osname.indexOf("aix") >= 0 ||
		osname.indexOf("sinix") >= 0 ||
		osname.startsWith("hp") && osname.indexOf("x") >= 0;
		
	public final static boolean isMac = osname.indexOf("mac") >= 0;

	public final static String newline = System.getProperty("line.separator");

	public final static boolean isAboveJava14;
	public final static boolean isAboveJava13;
	public final static boolean isBelowJava9;
	
	static	{
		System.err.println("Operating System: isWindows="+isWindows+", isUnix="+isUnix+", isMac="+isMac);

		String version = System.getProperty("java.version");
		boolean above13 = true;
		boolean above14 = true;
		boolean below9 = false;
		try	{
			version = version.trim();
			boolean isOneDot = (version.charAt(0) == '1' && version.charAt(1) == '.');
			if (isOneDot)	{
				above13 = isOneDot && Integer.parseInt(""+version.charAt(2)) >= 4;
				above14 = isOneDot && Integer.parseInt(""+version.charAt(2)) >= 5;
				below9 = true;
			}
			else	{
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < version.length() && Character.isDigit(version.charAt(i)); i++)
					sb.append(version.charAt(i));
				below9 = Integer.parseInt(sb.toString()) < 9;
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		isAboveJava13 = above13;
		isAboveJava14 = above14;
		isBelowJava9 = below9;
		System.err.println("Java Version: "+version+", is above 1.3 "+isAboveJava13+", is above 1.4 "+isAboveJava14+", is below 9 "+isBelowJava9);
	}
	
	
	/**
		Liefert einen symbolischen Namen fuer das Betriebssystem:
		"WINDOWS", "MAC", "UNIX", "Unknown Platform".
	*/
	public static String getName()	{
		if (isWindows)
			return "WINDOWS";
		if (isUnix)
			return "UNIX";
		if (isMac)
			return "MAC";
		return "Unknown Platform";
	}

	/**
		Liefert true wenn das Betriebssystem zwischen
		bei Dateinamen Gross- und Kleinschreibung unterscheidet.
	*/
	public static boolean supportsCaseSensitiveFiles()	{
		if (isWindows)
			return false;
		if (isUnix)
			return true;
		if (isMac)
			return false;
		return true;
	}
	
	public static String newline()	{
		return newline;
	}

}