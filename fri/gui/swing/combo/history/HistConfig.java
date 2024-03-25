package fri.gui.swing.combo.history;

import fri.gui.GuiConfig;

public abstract class HistConfig
{
	private static String dir = null;
	
	public static String dir()	{
		if (dir == null)	{
			dir = GuiConfig.dir()+"histories/";
		}
		return dir;
	}
	
	private HistConfig()	{}
}