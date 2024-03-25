package fri.gui.swing.filechooser;

import java.io.File;
import java.awt.Component;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import fri.gui.swing.ComponentUtil;
import fri.util.props.ClassProperties;

/**
	Default FileChooser that makes its chosen path persistent
	and obtains a singleton dialog object.
*/

public abstract class DefaultFileChooser
{
	private static JFileChooser fileChooser;
	private static String chooserPath, chooserFile;
	private static boolean openMultipleFiles = true;
	private static int mode = JFileChooser.FILES_ONLY;


	public static File [] openDialog(Component parent)
		throws CancelException
	{
		return openDialog(parent, null);
	}

	public static File [] openDialog(Component parent, Class persistenceClass)
		throws CancelException
	{
		return openDialog(null, parent, persistenceClass);
	}

	public static File [] openDialog(Component parent, Class persistenceClass, String [] extensions)
		throws CancelException
	{
		return openDialog(null, parent, persistenceClass, extensions);
	}

	public static File [] openDialog(File suggested, Component parent, Class persistenceClass)
		throws CancelException
	{
		return openDialog(suggested, parent, persistenceClass, null);
	}
	
	public static File [] openDialog(File suggested, Component parent, Class persistenceClass, String [] extensions)
		throws CancelException
	{
		ensureChooser(suggested, persistenceClass, false);

		fileChooser.setMultiSelectionEnabled(openMultipleFiles);
		fileChooser.setFileSelectionMode(mode);

		manageFilters(extensions);
		
		int ret = fileChooser.showOpenDialog(ComponentUtil.getWindowForComponent(parent));
		
		File [] tgts = null;
		
		if (ret == JFileChooser.APPROVE_OPTION) {
			File [] files = fileChooser.getSelectedFiles();
			//System.err.println("filechooser opening "+files.length+" files ...");
			
			if (files.length > 0)	{
				chooserFile = files[0].getName();
				tgts = files;
			}
			else	{
				File file = fileChooser.getSelectedFile();
				
				if (file != null)	{
					chooserFile = file.getName();
					tgts = new File [] { file };
				}
			}

			chooserPath = fileChooser.getCurrentDirectory().getAbsolutePath();
			storeDialogPath(persistenceClass);
		}
		
		fileChooser.setMultiSelectionEnabled(true);	// reset as multiple files is the default
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		fileChooser.cancelSelection();	// reuse dialog. JFileChooser stores APPROVE_OPTION !
		
		if (ret == JFileChooser.CANCEL_OPTION) {
			throw new CancelException();
		}

		return tgts;
	}




	public static File saveDialog(Component parent)
		throws CancelException
	{
		return saveDialog(parent, null);
	}

	public static File saveDialog(Component parent, Class persistenceClass)
		throws CancelException
	{
		return saveDialog(null, parent, persistenceClass);
	}

	public static File saveDialog(File suggested, Component parent, Class persistenceClass)
		throws CancelException
	{
		System.err.println("suggested file for saving is: "+suggested);
		ensureChooser(suggested, persistenceClass, true);

		fileChooser.setMultiSelectionEnabled(false);
		manageFilters(null);

		int ret = fileChooser.showSaveDialog(ComponentUtil.getWindowForComponent(parent));
		
		File tgt = null;
		
		if (ret == JFileChooser.APPROVE_OPTION) {
			tgt = fileChooser.getSelectedFile();

			if (tgt == null)	{	// JDK 1.2 ???
				tgt = new File(fileChooser.getCurrentDirectory(), fileChooser.getName());
			}

			chooserPath = fileChooser.getCurrentDirectory().getAbsolutePath();
			storeDialogPath(persistenceClass);
		}

		fileChooser.cancelSelection();	// reuse dialog. JFileChooser stores APPROVE_OPTION !
		
		if (ret == JFileChooser.CANCEL_OPTION) {
			throw new CancelException();
		}

		return tgt;
	}
	


	/** Set a default suggested file for open or save. */
	public static void setChooserFile(File suggested)	{
		setChooserFile(suggested, false);
	}
	
	private static void setChooserFile(File suggested, boolean isSave)	{
		if (suggested == null)	{
			chooserFile = null;
		}
		else
		if (suggested.isDirectory())	{
			chooserPath = suggested.getPath();
		}
		else
		if (isSave || suggested.isFile())	{	// when saving, accept even a non-existing file
			chooserPath = suggested.getParent();
			if (chooserPath == null)
				chooserPath = ".";
			chooserFile = suggested.getName();
		}
	}

	/** Returns the current chooser directory. */
	public static String getChooserDirectory(Class persistenceClass)	{
		ensureChooser(null, persistenceClass, false);
		return chooserPath;
	}

	

	/** Enable the chooser to open multiple files at once. This flag will be reset to true after openDialog(). */
	public static void setOpenMultipleFiles(boolean openMultipleFiles)	{
		DefaultFileChooser.openMultipleFiles = openMultipleFiles;
	}
	
	/** Control file selection mode. This flag will be reset to FILES_ONLY after openDialog(). */
	public static void setFileSelectionMode(int mode)	{
		DefaultFileChooser.mode = mode;
	}


	private static void manageFilters(String [] extensions)	{
		fileChooser.resetChoosableFileFilters();
		FileFilter filter = null;
		for (int i = 0; extensions != null && i < extensions.length; i++)	{
			FileFilter f = new GenericExtensionFileFilter(extensions[i]);
			if (filter == null)
				filter = f;
			fileChooser.addChoosableFileFilter(f);
		}

		if (filter != null)	// set the first filter active
			fileChooser.setFileFilter(filter);
	}

	private static Class ensurePersistenceClass(Class persistenceClass)	{
		if (persistenceClass == null)
			persistenceClass = DefaultFileChooser.class;
		return persistenceClass;

	}


	private static void ensureChooser(File suggested, Class persistenceClass, boolean isSave)	{
		if (suggested != null)	{
			setChooserFile(suggested, isSave);
		}

		if (fileChooser == null)	{
			if (chooserPath == null)
				chooserPath = getRecentChooserPath(persistenceClass);

			if (chooserFile == null)
				chooserFile = ClassProperties.get(ensurePersistenceClass(persistenceClass), "chooserFile");

			fileChooser = new JFileChooser(chooserPath != null ? chooserPath : System.getProperty("user.dir"));
		}
		else
		if (chooserPath != null)	{
			fileChooser.setCurrentDirectory(new File(chooserPath));
		}

		if (chooserFile != null)	{
			System.err.println("chooserFile was set to "+chooserFile+", isSave "+isSave);
			File pnt = chooserPath == null ? fileChooser.getCurrentDirectory() : new File(chooserPath);

			File f = new File(pnt, chooserFile);

			if (isSave || f.exists())	{
				System.err.println("fileChooser.setSelectedFile not null: "+f);
				fileChooser.setSelectedFile(f);
			}
		}
		else	{	// must clear file name textfield for "save as"
			System.err.println("fileChooser.setSelectedFile null");
			fileChooser.setSelectedFile(new File(chooserPath, "Unknown"));
		}
	}


	private static void storeDialogPath(Class persistenceClass)	{
		persistenceClass = ensurePersistenceClass(persistenceClass);

		boolean store = false;

		if (chooserPath != null)	{
			ClassProperties.put(persistenceClass, "chooserPath", chooserPath);
			store = true;
		}

		if (chooserFile != null)	{
			ClassProperties.put(persistenceClass, "chooserFile", chooserFile);
			store = true;
		}

		if (store)
			ClassProperties.store(persistenceClass);
	}
	
	
	/** Returns the path the file chooser would open for passed class. */
	public static String getRecentChooserPath(Class persistenceClass)	{
		return ClassProperties.get(ensurePersistenceClass(persistenceClass), "chooserPath");
	}

}