package fri.gui.swing.editor;

import fri.util.props.PropertyUtil;
import fri.util.props.ClassProperties;

/** Global configuration variables. */

public abstract class Config
{
	/** Possible value for getNewline() and setNewline(). */
	public static final String UNIX_NEWLINE = "UNIX";
	/** Possible value for getNewline() and setNewline(). */
	public static final String MAC_NEWLINE = "MAC";
	/** Possible value for getNewline() and setNewline(). */
	public static final String WINDOWS_NEWLINE = "WINDOWS";
	
	public static boolean getAutoIndent()	{
		return PropertyUtil.checkClassProperty("autoIndent", Config.class, true);
	}

	public static boolean getWrapLines()	{
		return PropertyUtil.checkClassProperty("wrapLines", Config.class, false);
	}

	public static boolean getWarnDirty()	{
		return PropertyUtil.checkClassProperty("warnDirty", Config.class, true);
	}
	
	public static int getTabSize()	{
		return PropertyUtil.getClassInteger("tabSize", Config.class, 2);
	}
	
	public static String getNewline()	{
		return ClassProperties.get(Config.class, "newline");
	}

	public static boolean getIsDesktopView()	{
		return PropertyUtil.checkClassProperty("desktopView", Config.class, false);
	}
	
	public static String getEncoding()	{
		return ClassProperties.get(Config.class, "encoding");
	}

	public static boolean getDetectEncodingFromByteOrderMark()	{
		return PropertyUtil.checkClassProperty("detectEncodingFromByteOrderMark", Config.class, true);
	}

	public static boolean getDetectXmlOrHtmlHeaderEncoding()	{
		return PropertyUtil.checkClassProperty("detectXmlOrHtmlHeaderEncoding", Config.class, true);
	}

	public static boolean getCreateByteOrderMark() {
		return PropertyUtil.checkClassProperty("createByteOrderMark", Config.class, false);
	}
	

	private static String bool2String(boolean b)	{
		return b ? "true" : "false";
	}
	
	
	public static void setAutoIndent(boolean b)	{
		ClassProperties.put(Config.class, "autoIndent", bool2String(b));
	}

	public static void setWrapLines(boolean b)	{
		ClassProperties.put(Config.class, "wrapLines", bool2String(b));
	}

	public static void setWarnDirty(boolean b)	{
		ClassProperties.put(Config.class, "warnDirty", bool2String(b));
	}
	
	public static void setTabSize(int i)	{
		ClassProperties.put(Config.class, "tabSize", ""+i);
	}
	
	public static void setNewline(String newline)	{
		ClassProperties.put(Config.class, "newline", newline);
	}
	
	public static void setIsDesktopView(boolean b)	{
		ClassProperties.put(Config.class, "desktopView", bool2String(b));
	}
	
	public static void setEncoding(String encoding)	{
		ClassProperties.put(Config.class, "encoding", encoding);
	}
	
	public static void setDetectEncodingFromByteOrderMark(boolean detectEncodingFromByteOrderMark)	{
		ClassProperties.put(Config.class, "detectEncodingFromByteOrderMark", bool2String(detectEncodingFromByteOrderMark));
	}

	public static void setDetectXmlOrHtmlHeaderEncoding(boolean detectXmlOrHtmlHeaderEncoding)	{
		ClassProperties.put(Config.class, "detectXmlOrHtmlHeaderEncoding", bool2String(detectXmlOrHtmlHeaderEncoding));
	}

	public static void setCreateByteOrderMark(boolean createByteOrderMark) {
		ClassProperties.put(Config.class, "createByteOrderMark", bool2String(createByteOrderMark));
	}
	


	public static void store()	{
		ClassProperties.store(Config.class);
	}

	private Config()	{}	// do not instantiate

}