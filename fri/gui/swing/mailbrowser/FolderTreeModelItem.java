package fri.gui.swing.mailbrowser;

import javax.swing.tree.*;
import javax.mail.*;
import fri.util.error.Err;
import fri.util.mail.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.swing.*;
import fri.gui.mvc.controller.CommandArguments;

/**
	The mail folder ModelItem implementing controller functionality.
	
	@author Fritz Ritzberger
*/

public class FolderTreeModelItem extends AbstractMutableTreeModelItem
{
	private FolderTreeNode saveCopyParent;
	private String saveCopyName;
	private OverwriteDialog overwriteDialog;

	
	public FolderTreeModelItem(MutableTreeNode userObject)	{
		super(userObject);
	}

	public FolderTreeModelItem(MutableTreeNode userObject, OverwriteDialog overwriteDialog)	{
		super(userObject);
		setOverwriteDialog(overwriteDialog);
	}


	public boolean isMovePending()	{
		return ((FolderTreeNode)getUserObject()).getMovePending();
	}

	public void setMovePending(boolean movePending)	{
		((FolderTreeNode)getUserObject()).setMovePending(movePending);
	}


	/** Returns the installed overwrite dialog, as pasting will attach a progress dialog to it. */
	public OverwriteDialog getOverwriteDialog()	{
		return overwriteDialog;
	}

	/** Set the installed overwrite dialog. MutableModel can not create FolderTreeModelItem with that argument. */
	public void setOverwriteDialog(OverwriteDialog overwriteDialog)	{
		this.overwriteDialog = overwriteDialog;
	}


	/** Overridden to return savecopy name if is copy to self. */
	public Object getUserObject()	{
		return saveCopyParent != null ? saveCopyParent : super.getUserObject();
	}


	/**
		Returns a new ModelItem representing a mail folder created within this MutableTreeNode userObject.
		The parent node is this node.
		@return null if new item was not created.
	*/
	protected ModelItem createInMedium(CommandArguments createInfo)	{
		FolderTreeNode parent = (FolderTreeNode)getUserObject();
		String name = (String)createInfo.getCreateData();
		
		try	{	// to create a folder
			ObservableReceiveMail rm = parent.getReceiveMail();
			Folder f = rm.create(name);
	
			// create a tree node for new folder
			FolderTreeNode newFolder = new FolderTreeNode(f);
			return new FolderTreeModelItem(newFolder, overwriteDialog);
		}
		catch (Exception e)	{
			Err.error(e);
			return null;
		}
	}

	

	/** Returns true if this item could be deleted within mail folder. */
	protected boolean deleteInMedium(CommandArguments deleteInfo)	{
		FolderTreeNode fn = (FolderTreeNode)getUserObject();

		try	{
			ObservableReceiveMail rm = fn.getReceiveMail();
			rm.cdup();
			rm.delete(fn.toString(), true);
			return true;
		}
		catch (Exception e)	{
			Err.error(e);
			return false;
		}
	}

	/** Overridden to delegate to move to trash when not within trash or on remote folder. */
	public boolean doDelete(CommandArguments deleteInfo)	{
		FolderTreeNode fn = (FolderTreeNode)getUserObject();
		FolderTreeModel m = (FolderTreeModel)deleteInfo.getModel();
		
		if (isMove || m.mustBeReallyDeleted(fn))	{
			System.err.println("calling super.doDelete for treenode "+fn);
			return super.doDelete(deleteInfo);
		}
		else	{
			System.err.println("calling doMove "+fn);
			FolderTreeNode trashNode = m.getTrashNode(fn);
			CommandArguments arg = new CommandArguments.Paste(m);
			if (doMove(new FolderTreeModelItem(trashNode, overwriteDialog), arg) != null)	{
				//m.reload(trashNode);
				return true;
			}
			return false;
		}
	}


	protected boolean isActionToSelf(ModelItem targetItem, CommandArguments pasteInfo, boolean isCopy)	{
		if (targetItem.getClass().equals(getClass()) == false)
			return false;
			
		FolderTreeNode otherNode = (FolderTreeNode)targetItem.getUserObject();
		FolderTreeNode thisNode = (FolderTreeNode)getUserObject();
		TreePath tp1 = new TreePath(otherNode.getPath());
		TreePath tp2 = new TreePath(thisNode.getPath());
		boolean identical = tp1.equals(tp2);
		
		boolean takeParent = true;
		if (identical == false)	{	// we must check the parent of this node, as move to self can be move to same folder
			FolderTreeNode thisParent = (FolderTreeNode)thisNode.getParent();
			TreePath tp3 = new TreePath(thisParent.getPath());
			identical = tp1.equals(tp3);
			
			if (identical == false)
				return false;

			takeParent = false;	// the node was dropped at its parent
		}
			
		if (isCopy == false && identical)
			return true;
			
		// provide save copy parent as new target
		FolderTreeNode saveCopyParent = takeParent ? (FolderTreeNode)otherNode.getParent() : otherNode;
		((FolderTreeModelItem)targetItem).saveCopyParent = saveCopyParent;
		
		// make save copy name
		String name = thisNode.toString();
		int cnt = 1;
		boolean nameExists = false;
		do	{
			this.saveCopyName = "Copy"+cnt+"_"+name;
			cnt++;
			nameExists = saveCopyParent.existsChild(this.saveCopyName);
		}
		while (nameExists);
		
		return false;	// do the copy with the new name and parent arguments
	}


	/** Copy this node. Target is in member variable <i>target</i>. */
	public Object clone()	{
		FolderTreeNode fn = (FolderTreeNode)getUserObject();

		if (overwriteDialog != null)	{
			if (overwriteDialog.getCancelProgressDialog().canceled())
				return null;
			overwriteDialog.getCancelProgressDialog().setNote(fn.toString());
		}

		FolderTreeNode fnTarget = (FolderTreeNode)target.getUserObject();
		System.err.println("FolderTreeModelItem.clone of "+fn+", target folder is "+fnTarget);
		String newName = saveCopyName != null ? saveCopyName : fn.toString();

		// check if folder exists
		if (fnTarget.existsChild(newName))	{	// existing folder in target
			// confirm overwrite
			boolean overwrite = overwriteDialog.show(newName, " in "+fn.getParent().toString(), newName, " in "+fnTarget.toString());
			if (overwrite == false)	{
				setMovePending(false);
				return null;
			}

			// we must remove the overwritten folder
			FolderTreeModel m = (FolderTreeModel)this.pasteInfo.getReceivingModel();
			FolderTreeNode existingFolder = (FolderTreeNode)m.getChildByName(fnTarget, newName);
			
			boolean oldIsMove = isMove;
			isMove = false;
			ModelItem toDelete = m.createModelItem(existingFolder);	// create a ModelItem parent to remove the folder
			boolean ok = toDelete.doDelete(new CommandArguments.Paste(m));
			isMove = oldIsMove;
			
			if (ok == false)	{
				System.err.println("ERROR: could not delete "+newName+" in "+fnTarget+" when cloning "+fn);
				return null;
			}
		}

		try	{
			// keep order!
			ObservableReceiveMail rmTarget = (ObservableReceiveMail)fnTarget.getReceiveMail().clone();	// freeze current cd path
			ObservableReceiveMail rm = fn.getReceiveMail();
			System.err.println("FolderTreeModelItem.clone, receivemail is "+rm+", parent of treenode is "+fn.getParent());
		
			rm.cdup();	// change to parent folder
			Folder copy = rm.copy(fn.toString(), rmTarget, newName);
			return new FolderTreeModelItem(new FolderTreeNode(copy), overwriteDialog);
		}
		catch (Exception e)	{
			Err.error(e);
			return null;
		}
	}

}