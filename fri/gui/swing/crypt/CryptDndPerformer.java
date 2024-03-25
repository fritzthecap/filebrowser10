package fri.gui.swing.crypt;

import java.awt.Point;
import java.awt.datatransfer.*;
import java.io.File;
import java.util.List;
import fri.gui.mvc.controller.swing.dnd.AbstractDndPerformer;

/**
	Drag&Drop implementation for loading files into textarea.
	
	@author  Ritzberger Fritz
*/

public class CryptDndPerformer extends AbstractDndPerformer
{
	/**
		Create a default DND performer with abstract functionality.
		@param sensor Component to watch
	*/
	public CryptDndPerformer(CryptPanel.ByteHoldingTextArea sensor)	{
		super(sensor);
	}

	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		return DataFlavor.javaFileListFlavor;
	}

	public Transferable sendTransferable()	{
		return null;
	}

	protected boolean receiveMove(Object data, Point p)	{
		receive(data);
		return false;
	}
	
	protected boolean receiveCopy(Object data, Point p)	{
		receive(data);
		return false;
	}

	private void receive(Object fileList)	{
		File file = (File)((List)fileList).get(0);
		((CryptPanel.ByteHoldingTextArea)sensor).open(file);
	}

}