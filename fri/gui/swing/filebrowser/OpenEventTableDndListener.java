package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.dnd.*;


public class OpenEventTableDndListener implements
	DndPerformer
{
	private OpenEventTable table;
	
	
	public OpenEventTableDndListener(Component component, OpenEventTable table)	{
		new DndListener(this, component);
		this.table = table;
	}

	// interface DndPerformer

	public Transferable sendTransferable()	{
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		System.err.println("OpenEventTableDndListener.receiveTransferable");
		List fileList = (List)data;
		Iterator iterator = fileList.iterator();
		File file = null;
		while (iterator.hasNext()) {
			file = (File)iterator.next();
			break;	// take only first
		}
		if (file != null)	{
			int idx = table.getTable().rowAtPoint(p);
			//System.err.println("   setting "+file.getPath()+" to row "+idx);
			table.setPathInto(file.getPath(), idx);
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