package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.io.File;
import java.util.Vector;

public class FileImageViewerDndListener extends FileViewerDndListener
{
	private Vector files = new Vector();
	 
	public FileImageViewerDndListener(Component component)	{
		super(component);
	}

	protected int loadFile(File file, int i)	{
		String name = file.getName().toLowerCase();
		if (FileExtensions.isImage(name) != null)	{
			files.add(file);
			return 1;
		}
		return 0;
	}

	protected void finishLoading()	{
		if (files.size() > 0)	{
			File [] f = new File[files.size()];
			files.copyInto(f);
			ImageViewer.showImages(f);
			files.clear();
		}
	}

}
