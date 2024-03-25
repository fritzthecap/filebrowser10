package fri.gui.swing.filebrowser;

import java.io.File;

/**
	Adds a popup to the LineCount table for viewing and editing files.
*/

public class LineCount extends fri.gui.swing.linecount.LineCount implements
	ViewEditPopup.FileSelection
{
	public LineCount(File [] files)	{
		super(files);
	}
	
	protected void build()	{
		super.build();
		table.addMouseListener(new ViewEditPopup(this));
	}

	/** Implements ViewEditPopup.FileSelection. */
	public File [] getFiles()	{
		return getSelectedFiles();
	}

}
