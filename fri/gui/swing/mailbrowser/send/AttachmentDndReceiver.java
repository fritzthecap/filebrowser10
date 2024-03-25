package fri.gui.swing.mailbrowser.send;

import java.io.File;
import java.util.List;
import java.util.Vector;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import fri.gui.mvc.controller.swing.dnd.AbstractDndPerformer;

/**
*/

public class AttachmentDndReceiver extends AbstractDndPerformer
{
	private SendController controller;
	
	public AttachmentDndReceiver(Component sensor, SendController controller)	{
		super(sensor);
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
		if (data instanceof List == false)
			return false;
			
		List files = (List)data;
		if (files.size() <= 0)
			return false;
		
		Vector v = new Vector(files.size());
		for (int i = 0; i < files.size(); i++)	{
			Object o = files.get(i);
			if (o instanceof File)
				v.add(o);
		}
		
		if (v.size() <= 0)
			return false;
		
		File [] fileArray = new File[v.size()];
		v.copyInto(fileArray);
		
		controller.attachFiles(fileArray);
		
		return true;
	}

}
