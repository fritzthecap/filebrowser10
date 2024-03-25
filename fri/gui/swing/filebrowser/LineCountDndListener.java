package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.util.Vector;
import java.io.File;

public class LineCountDndListener extends FileViewerDndListener
{
	private Vector files = new Vector();
	
	public LineCountDndListener(Component component)	{
		super(component);
	}

	protected int loadFile(File file, int i)	{
		files.add(file);
		return 0;	// allow files unlimited
	}

	protected int loadFolder(File dir, int loaded)	{
		files.add(dir);
		return 0;	// allow directories unlimited
	}

	protected void finishLoading()	{
		if (files.size() > 0)	{
			File [] farr = new File [files.size()];
			files.copyInto(farr);
			new LineCount(farr);
			files.clear();
		}
	}

}