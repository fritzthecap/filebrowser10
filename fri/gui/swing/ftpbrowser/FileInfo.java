package fri.gui.swing.ftpbrowser;

import java.text.DateFormat;
import java.util.Date;
import fri.util.NumberUtil;

public abstract class FileInfo
{
	public static DateFormat dateFormater = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

	public static String getFileInfo(long length, long lastModified)	{
		return getFileInfo(NumberUtil.getFileSizeString(length), dateFormater.format(new Date(lastModified)));
	}

	public static String getFileInfo(String len, String time)	{
		return "  |  "+len+"  |  "+time;
	}
}
