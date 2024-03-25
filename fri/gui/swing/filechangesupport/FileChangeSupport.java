package fri.gui.swing.filechangesupport;

import java.io.File;
import java.awt.Component;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import javax.swing.*;
import fri.gui.swing.ComponentUtil;

/**
	Watch a file for modified time and ask for reload by a JDialog,
	if the stored time is not identical with new modification time.
	The check is triggered everytime the passed Component is focussed.
	This is NOT a background thread, but a FocusListener.
	<p>
	Responsibilities of client objects:
	<ul>
		<li>call setFileIsDirty() after the file contents were changed
			by the client object (editor).
			This might not be called after reload().
			</li>
		<li>call setFile(file) after the file was saved, or was saved under another name.
			</li>
		<li>call setActive(false) when the editor closes.
			</li>
	</ul>
*/

public class FileChangeSupport implements FocusListener
{
	private long modified;
	private File file;
	private Component sensorComponent;
	private JDialog dlg;
	private boolean fileIsDirty;
	private boolean active;
	private Reloader reloader;


	/**
		The implementer is able to reload a file that was chosen to be reloaded
		when checking for file modified time took place.
	*/
	public interface Reloader
	{
		/** Reloads the file from disk. */
		public void reload();
		
		/** Gets called when user chooses NOT to reload file. */
		public void fileWasNotReloaded();
	}


	/**
		Create a FileChangeSupport watcher for passed file.
		The support is set to active and a FocusListener is added.
	*/
	public FileChangeSupport(File file, Component sensorComponent, Reloader reloader)	{
		this.sensorComponent = sensorComponent;
		this.reloader = reloader;

		setFile(file);
		setActive(true);
	}


	/**
		A warning will be appended to dialog when fileIsDirty is true,
		so that the user gets informed that he would dismiss changes
		when reload is chosen.
		It is not necessary to provide a boolean argument, as when "Save"
		takes place, setFile(file) must be called, and this resets the flag.
	*/
	public void setFileIsDirty()	{
		this.fileIsDirty = true;
	}

	/**
		Set a new File to watch for changes.
		This must be called after a "Save" or "Save As" action.
	*/
	public void setFile(File file)	{
		this.file = file;
		modified = file.lastModified();
		fileIsDirty = false;
	}

	/** Returns the file obtained by this watcher. */
	public File getFile()	{
		return file;
	}

	/**
		Set the change support inactive or active.
		The FocusListener gets installed or uninstalled.
	*/
	public void setActive(boolean active)	{
		this.active = active;
		sensorComponent.removeFocusListener(this);
		if (active == false)	{
			if (dlg != null)	{
				dlg.dispose();
				dlg = null;
			}
		}
		else	{
			sensorComponent.addFocusListener(this);
		}
	}


	// interface FocusListener: check file time for modified
	
	public void focusGained(FocusEvent e)	{
		if (active)	{
			check();
		}
	}
	
	public void focusLost(FocusEvent e)	{}


	/**
		Compares file time with stored time and shows a dialog if not identical.
		Sets the stored time to new file time at end.
		This gets called when active is true and the FocusListener receives a FocusEvent.
	*/
	protected void check()	{
		// check if already showing or no changes
		if (dlg != null || modified <= 0L || modified == file.lastModified())	{
			return;
		}

		setFile(file);	// set new file time as load time

		// Problem: focus event kommt vor close event, wenn man den InternalFrame
		// schliesst und die file-time veraendert wurde, kommt der Warn-Dialog erst,
		// wenn das Fenster geschlossen wurde!
		// Man muesste also hier wissen, ob ein Close-Event in der EventQueue steht!
		// Workaround: setActive(false) in close event will close any upcoming dialog
		
		SwingUtilities.invokeLater(new Runnable()	{	// must go to event queue as JDK 1.4 does dialog deadlock
			public void run()	{
				if (active == false)
					return;
					
				String msg =
						"\""+file.getName()+"\" "+
						(file.getParent() != null ? " in \""+file.getParent()+"\"" : "")+
						"\" Has Changed."+
						"\nDo You Want To Reload It?"+
						(fileIsDirty ? "\n\nCAUTION: There Were Unsaved Changes!" : "");
		
				Component parent = ComponentUtil.getWindowForComponent(sensorComponent);
				if (parent.isVisible() == false)
					return;
					
				JOptionPane pane = new JOptionPane(
						msg,
						JOptionPane.QUESTION_MESSAGE,
						JOptionPane.YES_NO_OPTION);
				dlg = pane.createDialog(
						parent,
						"File Has Changed");
		
				dlg.setVisible(true);
		
				Object o = pane.getValue();
				if (o instanceof Integer && ((Integer)o).intValue() == JOptionPane.YES_OPTION)	{
					reloader.reload();
				}
				else	{	// not reloaded means file needs saving
					reloader.fileWasNotReloaded();
					setFileIsDirty();
				}
		   
				dlg = null;
			}
		});
	}

}