package fri.gui.swing.concordance;

import java.util.*;
import java.io.File;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.*;
import fri.gui.mvc.controller.swing.dnd.AbstractDndPerformer;

class ConcordanceDndPerformer extends AbstractDndPerformer
{
	private ConcordanceController controller;
	
	public ConcordanceDndPerformer(Component c, ConcordanceController controller)	{
		super(c);
		this.controller = controller;
	}
	
	public Transferable sendTransferable()	{
		return null;
	}
	
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		return DataFlavor.javaFileListFlavor;
	}

	/** Implements DndPerformer: delegate data to ViewDnd */
	public boolean receiveTransferable(Object data, int action, Point p)	{
		if (activated == false)
			return false;
			
		if (data instanceof List)	{
			List list = (List)data;
			Iterator it = list.iterator();
			ArrayList files = new ArrayList(list.size());
		
			for (int i = 0; it.hasNext(); i++) {
				Object o = it.next();
				
				if (o instanceof File)	{
					files.add(o);
				}
			}
			
			if (files.size() > 0)	{
				File [] farr = new File[files.size()];
				files.toArray(farr);
				controller.open(farr);
			}
		}
		return false;
	}

}
