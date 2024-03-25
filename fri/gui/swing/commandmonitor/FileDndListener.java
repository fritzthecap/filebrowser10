package fri.gui.swing.commandmonitor;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.dnd.*;


public class FileDndListener implements
	DndPerformer
{
	private DefaultCommandMonitor receiver;
	private Component component;

	
	public FileDndListener(Component component, DefaultCommandMonitor receiver)	{
		if (component == null)	{
			throw new IllegalArgumentException("Drag & drop component is null!");
		}
		new DndListener(this, component);
		this.receiver = receiver;
		this.component = component;
	}

	
	// interface DndPerformer

	public Transferable sendTransferable()	{
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		//System.err.println("FileDndListener.receiveTransferable");
		List fileList = (List)data;
		Iterator iterator = fileList.iterator();

		if (iterator.hasNext()) {
			File file = (File)iterator.next();
			receiver.setDndFile(file, component);
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