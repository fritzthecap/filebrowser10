package fri.gui.swing.diff;

import java.io.File;
import java.awt.Component;

/**
	Loads one or both file(s) into a given or both view(s).
*/

public interface FileObjectLoader
{
	/** Load one file into view. */
	public void load(File file, Component whichView);
	/** Load both files into views. */
	public void setFiles(File file1, File file2);
}