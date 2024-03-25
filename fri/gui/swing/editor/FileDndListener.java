package fri.gui.swing.editor;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.dnd.*;

/**
	Accepts File and opens a new internal edit container in editor pane.
*/

public class FileDndListener implements
	DndPerformer
{
	private EditorMdiPane mdiPane;


	public FileDndListener(Component component, EditorMdiPane mdiPane)	{
		new DndListener(this, component);
		this.mdiPane = mdiPane;
	}

	// interface DndPerformer

	public Transferable sendTransferable()	{
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		List fileList = (List)data;
		Iterator iterator = fileList.iterator();

		while (iterator.hasNext()) {
			File file = (File)iterator.next();
			if (file.isDirectory() == false && file.toString().length() > 0)	{	// bug on LINUX: empty string
				//System.err.println("FileDndListener receiving File: "+file);
				mdiPane.createMdiFrame(file);
			}
		}
		return false;
	}


	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		return DataFlavor.javaFileListFlavor;
	}

	public void dataMoved()	{}	
	public void dataCopied()	{}
	public void actionCanceled()	{}

	public boolean dragOver(Point p)	{
		return true;
	}
	public void startAutoscrolling()	{}
	public void stopAutoscrolling()	{}

}