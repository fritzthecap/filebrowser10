package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.io.File;
import fri.gui.swing.hexeditor.HexEditorFrame;

public class FileHexEditDndListener extends FileViewerDndListener
{
	public FileHexEditDndListener(Component component)	{
		super(component);
	}

	protected int loadFile(File file, int i)	{
		HexEditorFrame.singleton(file);
		return 1;
	}

}