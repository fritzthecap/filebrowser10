package fri.gui.swing.tail;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.dnd.*;


public class TailFileDndListener implements
	DndPerformer
{
	private TailPanel receiver;
	
	
	public TailFileDndListener(Component component, TailPanel receiver)	{
		new DndListener(this, component);
		this.receiver = receiver;
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
			if (file.isDirectory() == false)	{
				receiver.setFile(file);
				return false;
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
