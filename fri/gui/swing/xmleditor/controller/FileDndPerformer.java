package fri.gui.swing.xmleditor.controller;

import java.io.File;
import java.util.List;
import java.awt.Point;
import java.awt.Component;
import java.awt.datatransfer.*;
import fri.gui.mvc.controller.swing.dnd.AbstractDndPerformer;

/**
	Drag&Drop handler that receives Files.

	@author  Ritzberger Fritz
*/

public class FileDndPerformer extends AbstractDndPerformer
{
	private XmlEditController controller;

	/**
		Create a autoscrolling drag and drop handler.
	*/
	public FileDndPerformer(Component comp, XmlEditController controller)	{
		super(comp);

		this.controller = controller;
	}


	/** Checks for types this handler is supporting: W3C Nodes and Files. */
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)	{
			if (flavors[i].equals(DataFlavor.javaFileListFlavor))	{
				return DataFlavor.javaFileListFlavor;
			}
		}
		return null;
	}


	/** interface DndPerformer: always returns null. */
	public Transferable sendTransferable()	{
		return null;
	}

	/** Receive a File move command: open it in editor window. */
	protected boolean receiveMove(Object data, Point p)	{
		return receive(p, (List)data);
	}

	/** Receive a File copy command: open it in editor window. */
	protected boolean receiveCopy(Object data, Point p)	{
		return receive(p, (List)data);
	}

	private boolean receive(Point p, List data)	{
		boolean ret = false;

		for (int i = 0; i < data.size(); i++)	{
			Object o = data.get(i);

			if (o instanceof File)	{
				ret = true;
				controller.openURI(((File)o).getPath());
			}
		}

		return ret;
	}

}