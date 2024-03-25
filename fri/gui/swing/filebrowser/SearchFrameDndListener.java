package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.dnd.*;


public class SearchFrameDndListener implements
	DndPerformer
{
	private SearchFrame frame;
	private boolean add = false;
	
	
	public SearchFrameDndListener(Component component, SearchFrame frame)	{
		new DndListener(this, component);
		this.frame = frame;
	}

	public SearchFrameDndListener(Component component, SearchFrame frame, boolean add)	{
		this(component, frame);
		this.add = add;
	}

	// interface DndPerformer

	public Transferable sendTransferable()	{
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		System.err.println("SearchFrameDndListener.receiveTransferable");
		List fileList = (List)data;
		Iterator iterator = fileList.iterator();
		File [] files = new File[fileList.size()];

		for (int i = 0; iterator.hasNext(); i++) {
			files[i] = (File)iterator.next();
		}

		if (add)
			frame.addStartPoints(files);
		else
			frame.startSearch(files);

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