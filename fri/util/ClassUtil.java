package fri.util;

public abstract class ClassUtil
{
	public static String getSimpleClassname(String s)	{
		if (s == null)
			return null;
		int i = s.lastIndexOf(".");
		if (i > 0)
			return s.substring(i + 1);
		return s;
	}
	
	public static String getPackageName(String s)	{
		int i = s.lastIndexOf(".");
		if (i > 0)
			return s.substring(0, i);
		return "";
	}
	
}
