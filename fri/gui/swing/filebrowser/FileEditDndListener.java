package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.io.File;
import fri.gui.swing.editor.EditorFrame;

public class FileEditDndListener extends FileViewerDndListener
{
	public FileEditDndListener(Component component)	{
		super(component);
	}

	protected int loadFile(File file, int i)	{
		EditorFrame.singleton(file);
		return 1;
	}

}