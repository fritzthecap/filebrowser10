package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.dnd.JavaFileList;
import fri.gui.swing.dnd.*;


public class InfoTableDndListener implements
	DndPerformer
{
	private InfoFrame frame = null;	
	private SearchFrame searchframe = null;	
	private boolean active = true;
	public static boolean dragging = false;
	
	
	public InfoTableDndListener(Component component, InfoFrame frame)	{
		new DndListener(this, component);
		this.frame = frame;
	}

	public InfoTableDndListener(Component component, SearchFrame searchframe)	{
		new DndListener(this, component);
		this.searchframe = searchframe;
	}


	public void setActive(boolean targetFolderExists)	{
		active = targetFolderExists;
	}

	public boolean getActive()	{
		return active;
	}

	public boolean areThereDraggedNodes()	{
		System.err.println("InfoTableDndListener, dragging is "+dragging);
		return dragging;
	}

	// interface DndPerformer
	
	public Transferable sendTransferable()	{
		System.err.println("InfoTableDndListener.sendTransferable");
		if (active == false)
			return null;
		
		File [] intransfer = frame != null ?
				frame.getSelectedFileArray() :
				searchframe.getSelectedFileArray();
				
		if (intransfer == null || intransfer.length <= 0)	{
			System.err.println("FEHLER: begin drag, no selection");
			return null;
		}
		
		dragging = true;
		return new JavaFileList(Arrays.asList(intransfer));
	}


	public boolean receiveTransferable(Object data, int action, Point p)	{
		System.err.println("InfoPanelDndListener.receiveTransferable");
		if (active == false)
			return false;
			
		List fileList = null;
		try	{
			fileList = (List)data;
			if (fileList == null || fileList.size() <= 0)
				return false;
		}
		catch (Exception e)	{
			e.printStackTrace();
			return false;
		}
		
		Iterator iterator = fileList.iterator();
		File [] files = new File [fileList.size()];
		for (int i = 0; iterator.hasNext(); i++) {
			files[i] = (File)iterator.next();
		}
		
		if (frame != null)
			return frame.receiveDroppedFiles(
					files,
					(action == DndPerformer.COPY) ? true : false);
		else
			searchframe.startSearch(files);
			
		return false;
	}

	
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		return DataFlavor.javaFileListFlavor;
	}

	public void dataMoved()	{
		dragging = false;
	}	
	public void dataCopied()	{
		dragging = false;
	}
	public void actionCanceled()	{
		dragging = false;
	}

	public boolean dragOver(Point p)	{
		if (active && frame != null)
			frame.setMousePoint(p);
		return active;
	}
	public void startAutoscrolling()	{}
	public void stopAutoscrolling()	{}

}