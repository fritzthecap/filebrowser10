package fri.gui.swing.foldermonitor;

import java.io.File;
import java.util.List;
import java.util.Vector;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import fri.util.file.*;
import fri.gui.mvc.view.swing.*;
import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.filechooser.*;

/**
	Controller for folder monitor.
*/

public class FolderMonitorController extends ActionConnector implements
	ListSelectionListener
{
	public static final String ACTION_OPEN = "Open";
	public static final String ACTION_SUSPEND = "Suspend";
	public static final String ACTION_RESUME = "Resume";
	public static final String ACTION_DELETE = "Delete";

	private FolderMonitor monitor;
	private JLabel folderDisplay;
	

	public FolderMonitorController(FolderMonitor monitor, JLabel folderDisplay)	{
		super(monitor.getTable(), new TableSelectionDnd(monitor.getTable()), null);

		this.monitor = monitor;
		this.folderDisplay = folderDisplay;
		
		registerAction(ACTION_OPEN, (String)null, "Open Folder To Watch", KeyEvent.VK_O, InputEvent.CTRL_MASK);
		registerAction(ACTION_SUSPEND, (String)null, "Suspend Folder Watching");
		registerAction(ACTION_RESUME, (String)null, "Resume Folder Watching");
		registerAction(ACTION_DELETE, (String)null, "Delete Selected Object(s) From Filesystem", KeyEvent.VK_DELETE, 0);

		setEnabled(ACTION_RESUME, false);
		setEnabled(ACTION_DELETE, false);
		
		monitor.getTable().getSelectionModel().addListSelectionListener(this);
	}


	/** Implements ListSelectionListener: set enabled delete-button. */
	public void valueChanged(ListSelectionEvent e)	{
		boolean enable = false;
		List l = (List)getSelection().getSelectedObject();
		for (int i = 0; l != null && enable == false && i < l.size(); i++)	{
			List row = (List)l.get(i);
			String change = (String)row.get(Constants.columns.indexOf(Constants.CHANGE));
			if (change.equals(Constants.EVENT_CREATED))	{
				String path = (String)row.get(Constants.columns.indexOf(Constants.PATH));
				String name = (String)row.get(Constants.columns.indexOf(Constants.NAME));
				if (new File(path, name).exists())
					enable = true;
			}
		}
		setEnabled(ACTION_DELETE, enable);
	}
	
	/** Setting new folders to monitor. Needed for drag&drop. */
	public void setRoots(File [] roots)	{
		String text = roots != null && roots.length > 0 ? "<html>" : "";
		for (int i = 0; roots != null && i < roots.length; i++)
			text = text+(i > 0 ? "<br>" : "")+roots[i].getPath();
		if (text.length() > 0)
			text = text + "</html>";
			
		folderDisplay.setText(text);

		monitor.setRoots(roots);

		toggleSuspend(false);
	}
	
		
	public void cb_Open(Object selection)	{
		DefaultFileChooser.setOpenMultipleFiles(true);
		DefaultFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		try	{
			File [] files = DefaultFileChooser.openDialog(defaultKeySensor, getClass());
			if (files != null && files.length > 0)
				setRoots(files);
		}
		catch (CancelException ex)	{
		}
	}

	public void cb_Suspend(Object selection)	{
		monitor.setSuspended(true);
		toggleSuspend(true);
	}

	public void cb_Resume(Object selection)	{
		monitor.setSuspended(false);
		toggleSuspend(false);
	}

	private void toggleSuspend(boolean suspending)	{
		setEnabled(ACTION_SUSPEND, !suspending);
		setEnabled(ACTION_RESUME, suspending);
	}
	
	public void cb_Delete(Object selection)	{
		List l = (List)getSelection().getSelectedObject();
		Vector v = new Vector();
		String text = "\n";
		
		for (int i = 0; i < l.size(); i++)	{
			List row = (List)l.get(i);
			String change = (String)row.get(Constants.columns.indexOf(Constants.CHANGE));
			
			if (change.equals(Constants.EVENT_CREATED))	{
				String path = (String)row.get(Constants.columns.indexOf(Constants.PATH));
				String name = (String)row.get(Constants.columns.indexOf(Constants.NAME));
				File toDelete = new File(path, name);
				v.add(toDelete);
				text = text+"\n        "+toDelete;
			}
		}
		
		if (v.size() > 20)
			text = ""+v.size()+" Selected Files";
		else
			text = text+"\n\n";
		
		int ret = JOptionPane.showConfirmDialog(
			defaultKeySensor,
			"Really Delete "+text+" Without Undo Option?",
			"Deleting Created Files",
			JOptionPane.YES_NO_OPTION);

		if (ret != JOptionPane.YES_OPTION)
			return;

		for (int i = 0; i < v.size(); i++)	{
			File f = (File)v.get(i);
			new DeleteFile(f);
		}
	}

}
