package fri.gui.swing.editor;

import java.io.File;
import java.awt.Component;
import java.awt.Point;
import javax.swing.JViewport;
import javax.swing.text.JTextComponent;
import java.awt.EventQueue;
import fri.gui.CursorUtil;
import fri.gui.swing.filechooser.*;
import fri.gui.swing.fileloader.*;
import fri.gui.swing.filechangesupport.FileChangeSupport;

/**
	A support object for managing the file.
*/

public class EditorTextHolderSupport extends FileGUISaveLogicImpl implements
	FileChangeSupport.Reloader
{
	private File file;
	private boolean changed = false;
	private EditorTextHolder.ChangeListener changeLsnr;
	private final Component editor;
	private Loader loader;
	private Saver saver;
	private FileChangeSupport fileChangeSupport;
	private boolean warnDirty = true;
	private FileLoader fileLoader;
	private Point pos;
	private boolean loading = true;	// will be reset by load(), necessary for find window to wait on loading


	/** Implementers load the passed File or do nothing if IO error. */
	public interface Loader
	{
		/** Load the passed file in the editor specific way, or do nothing if it does not exist. */
		public FileLoader createFileLoader(File file);
		public void afterLoading();
	}
	
	/** Implementers save the passed File or throw an exception. */
	public interface Saver
	{
		/**
			Save the passed file in the editor specific way, or throw an exception if IO error.
			This call must be processed synchronously (not in background thread).
		*/
		public void store(File file) throws Exception;
	}

	
	/**
		Create a support object for managing a file.
	*/
	public EditorTextHolderSupport(File file, Component editor, Loader loader, Saver saver)	{
		super(editor);
		
		this.file = file;
		this.editor = editor;
		this.loader = loader;
		this.saver = saver;
	}


	/** Interrupt a possibly working file loader. */
	public void interrupt()	{
		if (fileLoader != null)
			fileLoader.interrupt();
	}


	/** A ChangeListener wants to get notified when the text was changed. */	
	public void setChangeListener(EditorTextHolder.ChangeListener cl)	{
		this.changeLsnr = cl;

		if (fileChangeSupport != null)
			fileChangeSupport.setActive(warnDirty);	// closes any pending reload-dialog
	}

	public void unsetChangeListener(EditorTextHolder.ChangeListener cl)	{
		this.changeLsnr = null;

		if (fileChangeSupport != null)
			fileChangeSupport.setActive(false);	// closes any pending reload-dialog
	}


	/** Set the changed state and notify the ChangeListener. Public for use in EditorTextHolder implementation. */
	public void setChanged(boolean changed)	{
		// keep order!
		this.changed = changed;
		if (changeLsnr != null)
			changeLsnr.changed(changed);
		
		if (fileChangeSupport != null && changed)
			fileChangeSupport.setFileIsDirty();
	}


	// interface EditorTextHolder

	/** Implements EditorTextHolder */	
	public boolean getChanged()	{
		return changed;
	}



	/** Implements EditorTextHolder. Saves the file synchronously under another name. */	
	public boolean saveAs()	{
		return save(true);
	}

	/** Implements EditorTextHolder. Saves the file synchronously. */	
	public boolean save()	{
		return save(false);
	}
	
	private boolean save(boolean isSaveAs)	{
		CursorUtil.setWaitCursor(editor);
		try	{
			return (isSaveAs ? SaveLogic.saveAs(this, getFile()) != null : SaveLogic.save(this, getFile()) != null);
		}
		finally	{
			CursorUtil.resetWaitCursor(editor);
		}
	}


	/** Implements SaveLogicImpl: delegates to Saver.save(). When success, sets the new File and unchanged state. */
	public void write(Object toWrite)
		throws Exception
	{
		File f = (File)toWrite;
		saver.store(f);	// writes to file or throws exception
		
		this.file = f;

		setChanged(false);	// not reached if exception is thrown

		watchChanges(f);	// ensure watcher
		fileChangeSupport.setFile(f);	// refresh watcher time and file
	}
	


	/** Implements EditorTextHolder. Loads the file delegating to Loader. */	
	public void load()	{
		load(false);
	}
	
	/** Implements EditorTextHolder and FileChangeSupport.Reloader. Reloads the file delegating to Loader. */	
	public void reload()	{
		load(true);
	}
	
	private void load(boolean restoreViewPos)	{
		if (getFile() != null)	{
			memorizeViewPosition(restoreViewPos);
			
			setLoading(true);
			
			fileLoader = loader.createFileLoader(getFile());
			if (fileLoader != null)	{
				fileLoader.start();
			}
			else	{
				setLoading(false);
				finishLoading();
			}
		}
	}

	/** Sets the loading state, calls loader.afterLoading() when a FileLoader was active and loading is false now. */
	public void setLoading(boolean loading)	{
		this.loading = loading;
		
		if (loading == false && fileLoader != null)	{	// finished loading
			finishLoading();
		}
	}

	private void finishLoading()	{
		fileLoader = null;
		
		loader.afterLoading();
		
		setChanged(false);
		watchChanges(getFile());	// ensure watcher

		if (restoreViewPosition() == false && editor instanceof JTextComponent)	{
			((JTextComponent)editor).setCaretPosition(0);
		}
	}
	
	/** Returns true if file is loading. */
	public boolean isLoading()	{
		return loading;
	}

	private void memorizeViewPosition(boolean restoreViewPos)	{
		if (restoreViewPos)	{
			pos = ((JViewport)editor.getParent()).getViewPosition();
		}
	}
	
	private boolean restoreViewPosition()	{
		if (pos != null)	{
			EventQueue.invokeLater(new Runnable()	{
				public void run()	{
					((JViewport)editor.getParent()).setViewPosition(pos);
					pos = null;
				}
			});
			return true;
		}
		return false;
	}
	



	
	/** Implements EditorTextHolder. @return the file that is loaded in this textarea. */
	public File getFile()	{
		return file;
	}



	/** Implements EditorTextHolder. Set the fileChangeSupport active or not. */	
	public void setWarnDirty(boolean warnDirty)	{
		this.warnDirty = warnDirty;
		if (fileChangeSupport != null)
			fileChangeSupport.setActive(warnDirty);
	}
	
	private void watchChanges(File file)	{
		if (fileChangeSupport == null && file != null)	{
			fileChangeSupport = new FileChangeSupport(file, editor, this);
			fileChangeSupport.setActive(warnDirty);
		}
	}


	/** Implements FileChangeSupport.Reloader: the file must be marked as changed. */
	public void fileWasNotReloaded()	{
		setChanged(true);
	}

}