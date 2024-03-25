package fri.gui.swing.mailbrowser;

import java.awt.event.WindowEvent;
import fri.util.os.OS;
import fri.util.error.Err;
import fri.util.activation.*;
import fri.gui.swing.error.GUIErrorHandler;
import fri.gui.swing.install.InstallLibraries;
import fri.gui.swing.application.GuiApplication;

/**
	Manages the global static CommandMap in javax.activation.
	Manages the GUI-errorhandler per instance.
*/
public class CommandMapAwareFrame extends GuiApplication
{
	private GUIErrorHandler eh;

	public CommandMapAwareFrame(String title)	{
		super(title);

		eh = new GUIErrorHandler(this);
		Err.setHandler(eh);
		
		if (OS.isWindows)	{
			try	{
				// ensure that DLL's are there
				InstallLibraries.ensure(new String [] {
						Win32Shell.getDdeDLLBaseName(),
						Win32Shell.getRegistryDLLBaseName()
					});
			}
			catch (Error err)	{
				err.printStackTrace();
			}
		}
		
		GenericCommandLauncher.installCommandMap();
	}

	public void windowActivated(WindowEvent e)	{
		Err.setHandler(eh);
		super.windowActivated(e);
	}
	
}