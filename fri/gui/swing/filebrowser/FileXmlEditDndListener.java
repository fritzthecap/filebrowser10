package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.io.File;
import fri.gui.swing.xmleditor.XmlEditor;

public class FileXmlEditDndListener extends FileViewerDndListener
{
	public FileXmlEditDndListener(Component component)	{
		super(component);
	}

	protected int loadFile(File file, int i)	{
		XmlEditor.singleton(new String [] { file.getPath() });
		return 1;
	}

}