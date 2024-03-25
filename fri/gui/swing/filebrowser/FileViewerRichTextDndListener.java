package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.io.File;

public class FileViewerRichTextDndListener extends FileViewerDndListener
{
	public FileViewerRichTextDndListener(Component component)	{
		super(component);
	}

	protected int loadFolder(File dir, int loaded)	{
		new FileViewerRichText(dir);		
		return 1;
	}
	
	protected int loadFile(File file, int i)	{
		String name = file.getName().toLowerCase();
		if (FileExtensions.isHTML(name) != null)	{
			new FileViewerRichText(file);
			return 1;
		}
		return 0;
	}

}