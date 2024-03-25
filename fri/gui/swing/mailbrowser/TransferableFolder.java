package fri.gui.swing.mailbrowser;

import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.tree.TreePath;

/**
	Implementation of interface Transferable for Drag 'n Drop of folders between mail windows.

	@author Ritzberger Fritz
*/

public class TransferableFolder implements
	Transferable,
	ClipboardOwner
{
	public static final DataFlavor mailFolderFlavor = new DataFlavor(SerialObject.class, "MailFolder");  		
	public static final DataFlavor[] flavors = {
		mailFolderFlavor,
	};
	private static final List flavorList = Arrays.asList(flavors);
	private List data;


	public TransferableFolder(List data) {
		this.data = data;
	}

	public synchronized DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor ) {
		return flavorList.contains(flavor);
	}
	
	public synchronized Object getTransferData(DataFlavor flavor)
		throws UnsupportedFlavorException, IOException
	{
		if (flavor.equals(mailFolderFlavor))	{
			return this.data;
		}
		else	{
			throw new UnsupportedFlavorException (flavor);
		}
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}
	
	public String toString() {
		return "TransferableFolder";
	}




	/**
		Class to transfer one mail folder.
	*/
	public static class SerialObject implements
		Serializable
	{
		public final String [] path;

		public SerialObject(FolderTreeNode ftn)	{
			TreePath tp = new TreePath(ftn.getPath());
			path = new String[tp.getPathCount() - 1];	// path contains root
			for (int i = 1; i < tp.getPathCount(); i++)	{	// 1: do not store root
				path[i - 1] = tp.getPathComponent(i).toString();
			}
		}
	}

}
