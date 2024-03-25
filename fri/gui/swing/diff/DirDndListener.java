package fri.gui.swing.diff;

import java.io.File;
import java.awt.Component;

public class DirDndListener extends FileDndListener
{
	public DirDndListener(FileObjectLoader loader, Component tree)	{
		super(loader, tree);
	}

	/** Check type of received file. */
	protected boolean checkFileType(File file)	{
		return file.isDirectory();
	}

}