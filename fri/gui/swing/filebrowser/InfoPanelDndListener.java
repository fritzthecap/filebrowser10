package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.dnd.*;


public class InfoPanelDndListener implements
	DndPerformer
{
	private InfoFrame frame;	
	
	
	public InfoPanelDndListener(Component component, InfoFrame frame)	{
		new DndListener(this, component);
		this.frame = frame;
	}

	// interface DndPerformer

	public Transferable sendTransferable()	{
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		System.err.println("InfoPanelDndListener.receiveTransferable");
		List fileList = (List)data;
		Iterator iterator = fileList.iterator();
		File [] files = new File [fileList.size()];
		for (int i = 0; iterator.hasNext(); i++) {
			files[i] = (File)iterator.next();
		}
		frame.setFiles(files);
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