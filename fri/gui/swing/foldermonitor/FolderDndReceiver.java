package fri.gui.swing.foldermonitor;

import java.io.File;
import java.util.List;
import java.util.Vector;
import java.awt.Point;
import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import javax.swing.*;

import fri.gui.CursorUtil;
import fri.gui.mvc.controller.swing.dnd.AbstractAutoScrollingDndPerformer;

/**
	Drag&Drop receiver for FolderMonitor.
*/

public class FolderDndReceiver extends AbstractAutoScrollingDndPerformer
{
	private FolderMonitorController controller;
	
	public FolderDndReceiver(Component table, JScrollPane sp, FolderMonitorController controller)	{
		super(table, sp);
		this.controller = controller;
	}
	
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)
			if (flavors[i].equals(DataFlavor.javaFileListFlavor))
				return flavors[i];
		return null;
	}
	
	public Transferable sendTransferable()	{
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		if (data instanceof List == false || ((List)data).size() <= 0)
			return false;
			
		List files = (List)data;
		Vector v = new Vector(files.size());
		for (int i = 0; i < files.size(); i++)	{
			Object o = files.get(i);
			if (o instanceof File && ((File)o).isDirectory())
				v.add(o);
		}
		
		if (v.size() <= 0)
			return false;
		
		File [] fileArray = new File[v.size()];
		v.copyInto(fileArray);
		
		CursorUtil.setWaitCursor(sensor);
		try	{
			controller.setRoots(fileArray);
		}
		finally	{
			CursorUtil.resetWaitCursor(sensor);
		}
		
		return true;
	}

}
