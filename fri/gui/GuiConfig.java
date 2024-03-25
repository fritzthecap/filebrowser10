package fri.gui;

import fri.util.props.ConfigDir;

public abstract class GuiConfig
{
	public static String dir()	{
		return ConfigDir.dir();
	}
	
	private GuiConfig()	{}
}
