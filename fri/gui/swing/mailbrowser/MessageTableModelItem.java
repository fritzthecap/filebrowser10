package fri.gui.swing.mailbrowser;

import java.util.Vector;
import javax.mail.*;
import fri.util.error.Err;
import fri.util.mail.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.swing.*;
import fri.gui.mvc.controller.CommandArguments;

/**
	The Message ModelItem implementing controller functionality.
	
	@author Fritz Ritzberger
*/

public class MessageTableModelItem extends AbstractMutableTableModelItem
{
	public MessageTableModelItem(DefaultTableRow userObject)	{
		super(userObject);
	}

	protected DefaultTableRow createTableRow(Vector v)	{
		throw new IllegalStateException("This method is not used here, all callers are overwritten!");
	}
	

	/** The createData contains the new message. It will be added to current folder and returned wrapped into a ModelItem. */
	protected ModelItem createInMedium(CommandArguments createInfo)	{
		Message msg = (Message)createInfo.getCreateData();
		MessageTableModel m = (MessageTableModel)createInfo.getModel();
		try	{
			ObservableReceiveMail rm = m.getReceiveMail();	// the current folder's mails
			Message [] msgs = new Message [] { msg };
			rm.append(msgs);	// insert at top into current mail list
			Message createdMessage = msgs[0];
			System.err.println("created message class is: "+createdMessage.getClass());

			Vector row = new MessageTableRow(createdMessage);
			return m.createModelItem(row);
		}
		catch (Exception e)	{
			Err.error(e);
			return null;
		}
	}

	/** This implementation always returns <i>new Integer(0)</i> for insertion at start of list. */
	protected Integer createdPositionInMedium(ModelItem createdItem)	{
		return new Integer(0);
	}


	/**
		Overridden to prevent inserting into FolderTreeModel, this would cause ClassCastException.
		Overridden to do nothing as MessageCountListener does the work of visible insert.
	*/
	protected ModelItem doInsert(CommandArguments args, ModelItem actor)	{
		return actor;
	}



	// delete without expunge
	// FRi 2003-06-27: do expunge, as remote folder needs that
	private void deleteInMedium(Message msg)
		throws MessagingException
	{
		Folder f = msg.getFolder();
		//System.err.println("opening folder "+f+" read/write to delete message");
		f.open(Folder.READ_WRITE);

		MessageUtil.setMessageDeleted(msg);

		//System.err.println("closing folder "+f+" with expunge to delete message");
		f.close(true);	// expunge
	}

	/** Returns true if this item's message flag could be set to DELETED. */
	protected boolean deleteInMedium(CommandArguments deleteInfo)	{
		MessageTableRow msgRow = (MessageTableRow)getUserObject();

		try	{
			deleteInMedium(msgRow.getMessage());	// will be expunged on leaveCurrentFolder()
			return true;
		}
		catch (Exception e)	{
			Err.error(e);
			return false;
		}
	}

	/**
		Overridden to delegate to move to trash when not within trash or on remote folder.
		Does delegate to super as MessageCountListeners would do deletion only on leaving folder (expunge).
	*/
	public boolean doDelete(CommandArguments deleteInfo)	{
		MessageTableModel messageTableModel = (MessageTableModel)deleteInfo.getModel();
		
		if (isMove || messageTableModel.mustBeReallyDeleted())	{	// if is move operation or parent is trash or remote folder
			if (deleteInfo.getModel() != null)	{	// paste of messages in folder has no model as this is no more existent
				return super.doDelete(deleteInfo);
			}

			// Else: delete silently in medium, as this is a move and the sending model is not loaded at the time.
			// The deleted item will not be found when the folder gets loaded into message table
			try	{
				MessageTableRow msgRow = (MessageTableRow)getUserObject();
				Message m = msgRow.getMessage();
				deleteInMedium(m);
				return true;
			}
			catch (MessagingException e)	{
				Err.error(e);
				return false;
			}
		}
		else	{	// move to trash
			FolderTreeModel folderTreeModel = messageTableModel.getFolderTreeModel();	// retrieve the receiving model from sending model
			ModelItem target = folderTreeModel.createModelItem(messageTableModel.getTrashNode());	// create a parent ModelItem from receiving model
			CommandArguments arg = new CommandArguments.Paste(messageTableModel, folderTreeModel);	// make a paste command

			if (doMove(target, arg) != null)	{	// move this ModelItem to trash folder in folder tree model
				return true;
			}
			return false;
		}
	}


	/** Always returns false as no clone copy is supported for messages. */
	protected boolean isActionToSelf(ModelItem targetItem, CommandArguments pasteInfo, boolean isCopy)	{
		return false;
	}


	/** Copy this node. Target is in member variable <i>target</i>. */
	public Object clone()	{
		MessageTableRow msgRow = (MessageTableRow)getUserObject();
		FolderTreeNode fnTarget = (FolderTreeNode)target.getUserObject();
		
		try	{
			ObservableReceiveMail rmTarget = fnTarget.getReceiveMail();
			rmTarget.append(new Message [] { msgRow.getMessage() });
			return this;	// can not return null, else would not be deleted by doMove()
		}
		catch (Exception e)	{
			Err.error(e);
			return null;
		}
	}

}