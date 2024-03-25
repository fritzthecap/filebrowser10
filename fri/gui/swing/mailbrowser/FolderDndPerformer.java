package fri.gui.swing.mailbrowser;

import java.util.*;
import java.awt.Point;
import java.awt.Component;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.mail.*;
import fri.util.error.Err;
import fri.util.mail.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.swing.TreeNodeUtil;
import fri.gui.swing.scroll.ScrollPaneUtil;

public class FolderDndPerformer extends AbstractMailPopupDndPerformer
{
	private FolderController folderController;

	/**
		Create a autoscrolling drag and drop handler for mail messages and folders.
		This can be installed on message table and folder tree.
	*/
	public FolderDndPerformer(Component tree, FolderController folderController)	{
		super(tree, ScrollPaneUtil.getScrollPane(tree));
		this.folderController = folderController;
	}


	/** Checks for types this handler is supporting: folders and messages. */
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)
			if (flavors[i].equals(TransferableFolder.mailFolderFlavor) || flavors[i].equals(TransferableMessage.mailMessageFlavor))
				return flavors[i];
		return null;
	}


	// send methods

	/** Returns false if drag should be canceled for some reason (tree is editing). This implementation returns true. */
	protected boolean checkStartDrag()	{
		return ((JTree)sensor).isEditing() == false;
	}

	/** Returns list of selected view nodes. */
	protected List getSelected()	{
		return (List)folderController.getSelection().getSelectedObject();
	}
	
	/** Returns null for cancel drag, or a serializable object built from casted view node. */
	protected Object createDraggedObject(Object o)	{
		FolderTreeNode tn = (FolderTreeNode)o;
		if (tn.isFolder() == false)
			return null;
		return new TransferableFolder.SerialObject(tn);
	}

	/** Produce a Transferable object from passed serializable objects. */
	protected Transferable createTransferable(List serializableObjects)	{
		return new TransferableFolder(serializableObjects);
	}


	// receive methods

	protected boolean receive(Point p, List data, boolean isCopy)	{
		move.setEnabled(true);
		return super.receive(p, data, isCopy);
	}

	/** Locates a received object in this view. */
	protected ModelItem locateDropped(Object serializableObject)	{
		// find the target folder
		TransferableFolder.SerialObject serialFolder = (TransferableFolder.SerialObject)serializableObject;
		String [] path = serialFolder.path;
		FolderTreeModel model = folderController.getFolderTreeModel();
		TreeNode root = (TreeNode)model.getRoot();
		FolderTreeNode sourceFolder = (FolderTreeNode)TreeNodeUtil.locate(root, path);
		
		// keep inheritage order: first test if message dropped
		if (serializableObject instanceof TransferableMessage.SerialObject)	{
			TransferableMessage.SerialObject serialMessage = (TransferableMessage.SerialObject)serializableObject;
			String id = serialMessage.messageId;
			int hashCode = serialMessage.hashCode;
			MessageIdFinder finder = new MessageIdFinder(id, hashCode);
			
			try	{
				ReceiveMail rm = sourceFolder.getReceiveMail();
				rm.messages(finder);	// visit all messages and search for id
				if (finder.found == null)	{	// do it without id
					finder = new MessageIdFinder(null, hashCode);
					rm.messages(finder);
				}

				if (finder.found != null)	{
					return new MessageTableModelItem(new MessageTableRow(finder.found));	// must build object for pasting
				}
				System.err.println("WARNING: Could not find dragged message "+serialMessage);
			}
			catch (Exception e)	{
				Err.error(e);
			}
		}
		else	{	// must be folder dropped
			if (sourceFolder != null)	{
				if (folderController.canMove(sourceFolder) == false)
					move.setEnabled(false);
				
				FolderTreeModelItem item = (FolderTreeModelItem)model.createModelItem(sourceFolder);
				item.setOverwriteDialog(new OverwriteDialog(null));
				return item;
			}
		}
		return null;
	}


	/** Locates the drop target from a point in this sensor view. */
	protected Object locateDropTarget(Point p)	{
		FolderTreeNode target = (FolderTreeNode)folderController.getSelectionDnd().getObjectFromPoint(p);
		System.err.println("FolderDndPerformer.locateDropTarget, target is "+target);
		if (target == null)
			return null;

		Object userObject = droppedNodes[0].getUserObject();
		if (userObject instanceof MessageTableRow)	{
			if (folderController.canCreateMessages(target) == false)
				return null;
		}
		else	{
			if (folderController.canCreateFolder(target) == false)
				return null;
		}

		return target;
	}



	/** Receive popup choice "copy". */
	protected void copyCallback()	{
		System.err.println("FolderDndPerformer, copyCallback(), dropped nodes are: "+droppedNodes[0].getUserObject());
		folderController.getClipboard().copy(droppedNodes);
		folderController.cb_Paste(dropTargetList);
		cancelCallback();	// free lists
	}

	/** Receive popup choice "move". */
	protected void moveCallback()	{
		System.err.println("FolderDndPerformer, moveCallback(), dropped nodes are: "+droppedNodes[0].getUserObject());
		folderController.getClipboard().cut(droppedNodes);
		folderController.cb_Paste(dropTargetList);
		cancelCallback();	// free lists
	}




	private class MessageIdFinder implements ReceiveMail.MailVisitor	
	{
		private String id;
		private int hashCode;
		public Message found;
		
		MessageIdFinder(String id, int hashCode)	{
			this.id = id;
			this.hashCode = hashCode;
		}
		
		public void message(int msgCount, int msgNr, Message m)	{
			if (found != null)
				return;
				
			if (id != null)	{
				String msgId = MessageUtil.getMessageId(m);
				if (msgId != null && msgId.equals(id))
					found = m;
			}
			else	{
				if (hashCode == m.hashCode())
					found = m;
			}
		}
		
		public void folder(int fldCount, int fldNr, Folder f)	{
		}
	}
}