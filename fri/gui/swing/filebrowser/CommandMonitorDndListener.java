package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.io.File;
import fri.gui.swing.commandmonitor.CommandMonitor;

public class CommandMonitorDndListener extends FileViewerDndListener
{
	public CommandMonitorDndListener(Component component)	{
		super(component);
	}

	protected int loadFolder(File dir, int loaded)	{
		new CommandMonitor(dir);		
		return 1;
	}
	
	protected int loadFile(File file, int i)	{
		new CommandMonitor(file);		
		return 1;
	}

}