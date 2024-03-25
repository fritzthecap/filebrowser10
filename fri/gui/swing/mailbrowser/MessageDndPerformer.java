package fri.gui.swing.mailbrowser;

import java.awt.Component;
import java.util.*;
import java.awt.Point;
import java.awt.datatransfer.*;

/**
	Drag&Drop handler that autoscrolls its table.
	It sends TransferableMessage.SerialObject list and delegates to FolderDndPerformer
	when receiving TransferableMessage (denies TransferableFolder).

	@author  Ritzberger Fritz
*/

public class MessageDndPerformer extends FolderDndPerformer
{
	private MessageController messageController;

	/**
		Create a autoscrolling drag and drop handler for mail messages.
	*/
	public MessageDndPerformer(Component table, MessageController messageController, FolderController folderController)	{
		super(table, folderController);
		this.messageController = messageController;
	}


	/** Checks for types this handler is supporting: messages. */
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)
			if (flavors[i].equals(TransferableMessage.mailMessageFlavor))
				return TransferableMessage.mailMessageFlavor;

		return null;
	}

	/** Overridden to avoid autoscrolling: no paste by position, pasted items get inserted on top. */
	public void startAutoscrolling()	{
	}


	// send methods

	protected boolean checkStartDrag()	{
		return true;
	}

	/** Returns list of selected view nodes. */
	protected List getSelected()	{
		return (List)messageController.getSelection().getSelectedObject();
	}

	/** Returns null for cancel drag, or a serializable object built from casted view node. */
	protected Object createDraggedObject(Object o)	{
		MessageTableRow row = (MessageTableRow)o;
		return new TransferableMessage.SerialObject(row, messageController.getCurrentFolderNode());
	}

	/** Produce a Transferable object from passed serializable objects. */
	protected Transferable createTransferable(List serializableObjects)	{
		return new TransferableMessage(serializableObjects);
	}


	// receive methods

	/** Locates the drop target by returning the current message table folder TreeNode. */
	protected Object locateDropTarget(Point p)	{
		return messageController.getCurrentFolderNode();
	}

}
