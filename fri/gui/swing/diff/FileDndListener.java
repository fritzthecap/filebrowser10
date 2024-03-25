package fri.gui.swing.diff;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.dnd.*;


public class FileDndListener implements
	DndPerformer
{
	private Component view;
	private FileObjectLoader loader;


	public FileDndListener(FileObjectLoader loader, Component view)	{
		this.view = view;
		this.loader = loader;
		new DndListener(this, view);
	}

	// interface DndPerformer

	/** Do not send anything: always returns null. */
	public Transferable sendTransferable()	{
		return null;
	}

	/** Check type of received file. */
	protected boolean checkFileType(File file)	{
		return file.isFile();
	}

	/** Check type of received file. */
	protected void setFiles(File file1, File file2)	{
		loader.setFiles(file1, file2);
	}

	/** Check type of received file. */
	protected void setFile(File file)	{
		loader.load(file, view);
	}

	/** Receive one or two files and load them in one or both textareas. */
	public boolean receiveTransferable(Object data, int action, Point p)	{
		List fileList = (List)data;
		Iterator iterator = fileList.iterator();
		File [] farr = new File[2];
		int loaded = 0;
		
		for (int i = 0; iterator.hasNext() && loaded < farr.length; i++) {
			File file = (File)iterator.next();
			
			if (checkFileType(file))	{
				farr[loaded] = file;
				loaded++;
			}
		}
		
		if (loaded == 1)	{
			setFile(farr[0]);
		}
		else
		if (loaded == 2)	{
			setFiles(farr[0], farr[1]);
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