package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.util.List;
import javax.swing.tree.*;
import fri.gui.swing.dnd.*;


public class WastebasketDndListener implements
	DndPerformer
{
	private TreeEditController tc;
	
	
	public WastebasketDndListener(Component component, TreeEditController tc)	{
		this.tc = tc;
		new DndListener(this, component, DndPerformer.MOVE);
	}

	// interface DndPerformer

	public Transferable sendTransferable()	{
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		System.err.println("WastebasketDndListener.receiveTransferable");
		List fileList = (List)data;
		Iterator iterator = fileList.iterator();
		DefaultMutableTreeNode [] draggedNodes = new DefaultMutableTreeNode [fileList.size()];
		int i = 0;
		NetNode n = tc.getRootNetNode();
		while (iterator.hasNext()) {
			NetNode nn = n.construct(iterator.next());
			// localize file in tree and add to draggedNodes
			DefaultMutableTreeNode d = tc.localizeNode(nn);
//			if (d == null)
//				return false;
			draggedNodes[i] = d;
			i++;
		}
		tc.removeNodes(draggedNodes);
		return tc.areThereDraggedNodes() == false;
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