package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.dnd.*;
import fri.util.FileUtil;


public class FileViewerDndListener implements
	DndPerformer
{
	public static int MAX_LOAD_FILES = 10;
	private FileViewer viewer;	
	
	
	public FileViewerDndListener(Component component, FileViewer viewer)	{
		new DndListener(this, component);
		this.viewer = viewer;
	}

	public FileViewerDndListener(Component component)	{
		this(component, null);
	}
	
	// interface DndPerformer

	public Transferable sendTransferable()	{
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		//System.err.println("FileViewerDndListener.receiveTransferable");
		List fileList = (List)data;
		Iterator iterator = fileList.iterator();
		int loaded = 0;
		
		for (int i = 0; iterator.hasNext() && loaded < MAX_LOAD_FILES; i++) {
			File file = (File)iterator.next();
			if (file.isDirectory())
				loaded += loadFolder(file, loaded);
			else
				loaded += loadFile(file, loaded);
		}
		finishLoading();
		return false;
	}

	/**
		Called when receiveTransferable is finished.
	*/
	protected void finishLoading()	{
	}

	/**
		Loads the directory contents into viewer.
		@param dir directory to load
		@param loaded number of files already loaded
		@return number of loaded files.
	*/
	protected int loadFolder(File dir, int loaded)	{
		File [] files = dir.listFiles();
		for (int i = 0; i < files.length && loaded < MAX_LOAD_FILES; i++)	{
			if (files[i].isDirectory() == false)
				loaded += loadFile(files[i], loaded);
		}
		return loaded;
	}

	/**
		Loads the file into viewer,
		@param file File to load
		@param i index of file in load-list, 0-n
		@return number of loaded, 0 if not loaded, else 1.
	*/
	protected int loadFile(File file, int i)	{
		// load synchronized, if file comes from ZIP
		boolean sync = FileUtil.isSubdir(ZipInfoData.tempPath, file.getPath());
		System.err.println("loading file "+file+" at "+i+", syncLoad "+sync);
		
		if (viewer == null || viewer.isVisible() == false)
			viewer = FileViewer.construct(file, sync);
		else
		if (i == 0)
			viewer.setFile(file, sync);
		else
			FileViewer.construct(file, sync);
			
		return 1;
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