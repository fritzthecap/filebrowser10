package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.io.File;
import fri.gui.swing.diff.*;

public class DiffDndListener extends FileViewerDndListener
{
	private File prevFile, prevDir;
	
	public DiffDndListener(Component component)	{
		super(component);
	}

	protected int loadFile(File file, int i)	{
		if (prevFile != null)	{
			new FileDiffFrame(prevFile, file);
			prevFile = null;
		}
		else	{
			prevFile = file;
		}
		return 0;	// allow files unlimited
	}

	protected int loadFolder(File dir, int loaded)	{
		if (prevDir != null)	{
			new DirDiffFrame(prevDir, dir);
			prevDir = null;
		}
		else	{
			prevDir = dir;
		}
		return 0;	// allow directories unlimited
	}

	protected void finishLoading()	{
		prevFile = prevDir = null;
	}

}