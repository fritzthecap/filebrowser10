package fri.gui.swing.filebrowser;

import fri.util.file.archive.ArchiveFactory;
import fri.util.FileUtil;
import fri.util.os.OS;

/**
	Map filenames to patterns and internal (builtin) commands.
*/

public abstract class FileExtensions
{
	public static String makeFilePattern(String name)	{
		String pattern = FileUtil.getExtension(name);
		if (OS.supportsCaseSensitiveFiles() == false)
			pattern = pattern.toLowerCase();
		return "*."+pattern;
	}

	public static String [] isArchive(String lpatt)	{
		boolean isArchive = ArchiveFactory.isArchive(lpatt);
		if (isArchive == false)
			return null;
		return new String [] { "*.zip|*.jar|*.tgz|*.tar.gz|*.sar|*.ear|*.war|*.rar", "ARCHIVE $FILE" };
	}

	public static String [] isHTML(String lpatt)	{
		boolean isHTML =
				lpatt.endsWith(".html") ||
				lpatt.endsWith(".htm") ||
				lpatt.endsWith(".shtml");
		if (isHTML == false)
			return null;
		return new String [] { "*.html|*.htm|*.shtml", "HTML $FILE" };
	}

	public static String [] isXML(String lpatt)	{
		boolean isXML =
				lpatt.endsWith(".xml") ||
				lpatt.endsWith(".xsd") ||
				lpatt.endsWith(".xsl") ||
				lpatt.endsWith(".xfd");
		if (isXML == false)
			return null;
		return new String [] { "*.xml|*.xsd|*.xsl|*.xfd", "XML $FILE" };
	}

	public static String [] isJavaClass(String lpatt)	{
		boolean isClass =
				lpatt.endsWith(".jar") ||
				lpatt.endsWith(".class");
		if (isClass == false)
			return null;
		return new String [] { "*.class|*.jar", "JAVA $FILE" };
	}

	public static String [] isText(String lpatt)	{
		boolean isText =
				lpatt.equals("*.txt") ||
				lpatt.equals("*.java") ||
				lpatt.equals("*.properties") ||
				lpatt.equals("*.c") ||
				lpatt.equals("*.cpp") ||
				lpatt.equals("*.h");
		if (isText == false)
			return null;
		return new String [] { "*.txt|*.java|*.properties|*.c|*.cpp|*.h", "VIEW $FILE" };
	}

	public static String [] isImage(String lpatt)	{
		boolean isImage =
				lpatt.endsWith(".gif") ||
				lpatt.endsWith(".jpeg") ||
				lpatt.endsWith(".jpg") ||
				lpatt.endsWith(".jpe") ||
				lpatt.endsWith(".xbm") ||
				lpatt.endsWith(".xpm") ||
				lpatt.endsWith(".xwd") ||
				lpatt.endsWith(".apf") ||
				lpatt.endsWith(".bmp") ||
				lpatt.endsWith(".dib") ||
				lpatt.endsWith(".ico") ||
				lpatt.endsWith(".ief") ||
				lpatt.endsWith(".pcx") ||
				lpatt.endsWith(".pct") ||
				lpatt.endsWith(".pic") ||
				lpatt.endsWith(".pict") ||
				lpatt.endsWith(".png") ||
				lpatt.endsWith(".ras") ||
				lpatt.endsWith(".tga") ||
				lpatt.endsWith(".tif") ||
				lpatt.endsWith(".tiff");
		if (isImage == false)
			return null;
		return new String [] { "*.gif|*.jpg|*.tif|*.png|*.bmp|*.xpm|*.ico", "IMAGE $FILE" };
	}
	
}