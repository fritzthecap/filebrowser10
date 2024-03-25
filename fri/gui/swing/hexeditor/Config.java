package fri.gui.swing.hexeditor;

import fri.util.props.PropertyUtil;
import fri.util.props.ClassProperties;

/** Global configuration variables. */

public abstract class Config
{
	public static int getBase()	{
		return PropertyUtil.getClassInteger("base", Config.class, 0);
	}

	public static void setBase(int base)	{
		ClassProperties.put(Config.class, "base", ""+base);
	}


	public static int getColumnCount()	{
		return PropertyUtil.getClassInteger("columnCount", Config.class, 32);
	}

	public static void setColumnCount(int columnCount)	{
		ClassProperties.put(Config.class, "columnCount", ""+columnCount);
	}


	public static void store()	{
		ClassProperties.store(Config.class);
	}

	private Config()	{}	// do not instantiate
	
}