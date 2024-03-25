package fri.gui.swing.filebrowser;

import java.io.File;
import javax.swing.JFrame;
import java.awt.event.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.IconUtil;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.foldermonitor.FolderMonitorPanel;

public class FolderMonitorFrame extends JFrame
{
	public FolderMonitorFrame(File [] files)	{
		super("Folder Monitor");

		IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());
		
		final FolderMonitorPanel fm = new FolderMonitorPanel();
		getContentPane().add(fm);
		
		addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				fm.close();
			}
		});

		new GeometryManager(this).show();
		
		fm.setRoots(files);
	}
}