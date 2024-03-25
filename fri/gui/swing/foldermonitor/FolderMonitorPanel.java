package fri.gui.swing.foldermonitor;

import java.io.File;
import java.awt.BorderLayout;
import javax.swing.*;
import fri.util.os.OS;
import fri.util.application.Closeable;

/**
	Presenter panel with actions (toolbar) for folder monitor.
*/

public class FolderMonitorPanel extends JPanel implements
	Closeable
{
	private FolderMonitor monitor;
	private FolderMonitorController controller;
	
	public FolderMonitorPanel()	{
		this((File)null);
	}

	public FolderMonitorPanel(File root)	{
		this(root == null ? null : new File [] { root });
	}

	public FolderMonitorPanel(File [] roots)	{
		super(new BorderLayout());
		
		JLabel label = new JLabel(" ");
		label.setBorder(BorderFactory.createTitledBorder("Watched Folders:"));
		monitor = new FolderMonitor();
		controller = new FolderMonitorController(monitor, label);
		
		JToolBar tb = new JToolBar();
		if (OS.isAboveJava13) tb.setRollover(true);
		controller.visualizeAction(FolderMonitorController.ACTION_OPEN, tb);
		controller.visualizeAction(FolderMonitorController.ACTION_SUSPEND, tb);
		controller.visualizeAction(FolderMonitorController.ACTION_RESUME, tb);
		controller.visualizeAction(FolderMonitorController.ACTION_DELETE, tb);
		
		new FolderDndReceiver(monitor.getTable(), monitor, controller);
		new FolderDndReceiver(monitor.getTable().getParent(), monitor, controller);
		
		JPanel p = new JPanel(new BorderLayout());
		p.add(tb, BorderLayout.SOUTH);
		p.add(label, BorderLayout.CENTER);

		add(p, BorderLayout.NORTH);
		add(monitor);

		setRoots(roots);
	}

	public void setRoots(File [] roots)	{
		controller.setRoots(roots);
	}

	public boolean close()	{
		monitor.close();
		return true;
	}

	
	public static void main(String [] args)	{
		File file = args.length > 0 ? new File(args[0]) : new File(System.getProperty("user.dir"));
		javax.swing.JFrame f = new javax.swing.JFrame("Folder Monitor");
		final FolderMonitorPanel p = new FolderMonitorPanel();
		f.addWindowListener(new java.awt.event.WindowAdapter()	{
			public void windowClosing(java.awt.event.WindowEvent e)	{
				p.close();
			}
		});
		f.getContentPane().add(p);
		f.setSize(650, 500);
		f.setVisible(true);
		p.setRoots(new File [] { file });	// do this now as columns would have no width else
	}

}