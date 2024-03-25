package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.dnd.*;


public class FilterTextDndListener implements
	DndPerformer
{
	private HistCombo filter;
	
	
	public FilterTextDndListener(Component component, HistCombo filter)	{
		new DndListener(this, component);
		this.filter = filter;
	}

	// interface DndPerformer

	public Transferable sendTransferable()	{
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		//System.err.println("FilterTextDndListener.receiveTransferable");
		List fileList = (List)data;
		Iterator iterator = fileList.iterator();

		String filterText = filter.getText();
		if (filterText.equals("*") || filterText.equals("*") || filterText.equals("*"))
			filterText = "";

		while (iterator.hasNext()) {
			File file = (File)iterator.next();
			if (filterText.equals("") == false)
				filterText = filterText+"|"+getPattern(file.getName());
			else
				filterText = filterText+getPattern(file.getName());
		}
		filter.setText(filterText);
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


	// @return "*.java" for "file.java"
	private String getPattern(String name)	{
		int i;
		if ((i = name.indexOf(".")) < 0)
			return name;
		return "*"+name.substring(i);
	}
	
}