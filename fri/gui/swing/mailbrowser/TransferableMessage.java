package fri.gui.swing.mailbrowser;

import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.mail.*;
import fri.util.mail.MessageUtil;

/**
	Implementation of interface Transferable for Drag 'n Drop of messages between mail windows.

	@author Ritzberger Fritz
*/

public class TransferableMessage implements
	Transferable,
	ClipboardOwner
{
	public static final DataFlavor mailMessageFlavor = new DataFlavor(SerialObject.class, "MailMessage");  		
	public static final DataFlavor[] flavors = {
		mailMessageFlavor,
	};
	private static final List flavorList = Arrays.asList(flavors);
	private List data;


	public TransferableMessage(List data) {
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
		if (flavor.equals(mailMessageFlavor))	{
			return this.data;
		}
		else	{
			throw new UnsupportedFlavorException (flavor);
		}
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}
	
	public String toString() {
		return "TransferableMessage";
	}




	/**
		Class to transfer one mail message.
	*/
	public static class SerialObject extends TransferableFolder.SerialObject
	{
		public final String messageId;
		public final int hashCode;
		
		public SerialObject(MessageTableRow msgRow, FolderTreeNode folderNode)	{
			super(folderNode);
			
			Message msg = msgRow.getMessage();

			// take process unique identifier
			hashCode = msg.hashCode();
			messageId = MessageUtil.getMessageId(msg);
		}
	}

}
