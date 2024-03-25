package fri.util.mail.store;

import java.util.*;
import javax.mail.*;

/**
 * Emulate the contained maildir folders "cur", "new", "tmp".
 *
 * @author Fritz Ritzberger
 */

public class FmMaildirFolder extends FmFolder
{
	private static String [] maildirFolders = new String []	{
		"cur",
		"new",
		"tmp"
	};
	private FmFolder cur, neu, tmp;

	

	public FmMaildirFolder(Store store, String path)
	{
		super( store, path );
		
		cur = new FmFolder(store, path + SEPARATOR + "cur");
		neu = new FmFolder(store, path + SEPARATOR + "new");
		tmp = new FmFolder(store, path + SEPARATOR + "tmp");
		
		//setType(Folder.HOLDS_MESSAGES);
	}



	/**
		Detect maildir folder: "cur", "new" and "tmp" subdirectories must exist.
	*/
	public static boolean isMaildirFolder(Store store, String path)
		throws MessagingException
	{
		boolean maildir = true;
		
		for (int i = 0; i < maildirFolders.length; i++)	{
			FmFolder sub = new FmFolder(store, path + FmFolder.SEPARATOR + maildirFolders[i]);
			
			if (sub.exists() == false)
				maildir = false;
		}
		
		return maildir;
	}



	public void open(int mode)
		throws MessagingException
	{
		//System.err.println("==========> open folder "+getFullName());
		//Thread.dumpStack();
		cur.open(mode);
		neu.open(mode);
		super.open(mode);
	}

	public boolean hasNewMessages()
		throws MessagingException
	{
		return neu.getMessageCount() > 0;
	}

	public Message[] expunge()
		throws MessagingException
	{
		Message [] msgs1 = cur.exists() ? cur.expunge() : new Message[0];

		//Message [] msgs2 = neu.exists() ? neu.expunge() : new Message[0];
		if (neu.exists())
			neu.expunge();	// messages always are moved to "cur" folder, so do not notify listeners
		Message [] msgs2 = new Message[0];

		Message [] msgs3 = super.expunge();

		Message [] msgs = new Message[msgs1.length + msgs2.length + msgs3.length];
		System.arraycopy(msgs1, 0, msgs, 0, msgs1.length);
		System.arraycopy(msgs2, 0, msgs, msgs1.length, msgs2.length);
		System.arraycopy(msgs3, 0, msgs, msgs1.length + msgs2.length, msgs3.length);
		
		if (msgs.length > 0)
			notifyMessageRemovedListeners(true, msgs);
		
		return msgs;
	}

	public int getMessageCount()
		throws MessagingException
	{
		return cur.getMessageCount() + neu.getMessageCount();
	}

	public int getNewMessageCount()
		throws MessagingException
	{
		return neu.getMessageCount();
	}

	public int getUnreadMessageCount()
		throws MessagingException
	{
		return getNewMessageCount();
	}

	public Message getMessage(int msgnum)
		throws MessagingException
	{
		// check number and get "new" or "cur" message
		int newCount = neu.getMessageCount();
		
		// msgnum is 1-n
		if (msgnum <= newCount)	{
			// ensure the RECENT flag is set
			Message msg = neu.getMessage(msgnum);
			if (msg != null)	{	// 2007-05-12: may this return null?
				msg.setFlag(Flags.Flag.RECENT, true);
				msg.setFlag(Flags.Flag.SEEN, false);
			}
			return msg;
		}
		
		return cur.getMessage(msgnum - newCount);
	}
	
	public synchronized void appendMessages(Message [] msgs)
		throws MessagingException
	{
		if (msgs == null || msgs.length <= 0)
			return;
		
		Vector toNew = new Vector();
		Vector toCur = new Vector();
		
		// check flags and distribute to maildir subfolders
		for (int i = 0; i < msgs.length; i++)	{
			if (isNewMessage(msgs[i]))	{
				//System.err.println("... new message appending to "+getName()+" toString "+msgs[i]);
				toNew.add(msgs[i]);
			}
			else	{
				//System.err.println("... non-new message appending to "+getName()+" toString "+msgs[i]);
				try { msgs[i].setFlag(Flags.Flag.RECENT, false); } catch (MessagingException e) { System.err.println("Can not set RECENT flag to false, ignoring: "+e.toString()); }	// exception happens on IMAP
				toCur.add(msgs[i]);
			}
		}
		
		Message [] newMessages = new Message[msgs.length];
		
		if (toNew.size() > 0)	{
			Message [] msgsNew = new Message[toNew.size()];
			toNew.copyInto(msgsNew);
			neu.appendMessages(msgsNew);
			System.arraycopy(msgsNew, 0, newMessages, 0, msgsNew.length);
			
			// set new flags
			for (int i = 0; i < msgsNew.length; i++)	{
				msgsNew[i].setFlag(Flags.Flag.RECENT, true);
				msgsNew[i].setFlag(Flags.Flag.SEEN, false);
			}
		}
		
		if (toCur.size() > 0)	{
			Message [] msgsCur = new Message[toCur.size()];
			toCur.copyInto(msgsCur);
			cur.appendMessages(msgsCur);
			System.arraycopy(msgsCur, 0, newMessages, toNew.size(), msgsCur.length);
		}
		
		System.arraycopy(newMessages, 0, msgs, 0, msgs.length);	// put them into reference parameter, caller needs this
		
		notifyMessageAddedListeners(msgs);
	}


	private boolean isNewMessage(Message msg)
		throws MessagingException
	{
		return msg.isSet(Flags.Flag.RECENT) && msg.isSet(Flags.Flag.SEEN) == false;
	}
	
	protected boolean internalFolderListCondition(String dirName)	{
		boolean contained = false;
		
		for (int j = 0; j < maildirFolders.length; j++)
			if (dirName.equals(maildirFolders[j]))
				contained = true;
				
		return contained == false;
	}

	public synchronized boolean create(int type)
		throws MessagingException
	{
		if (super.create(type | HOLDS_FOLDERS) &&
				cur.create(HOLDS_MESSAGES) &&
				neu.create(HOLDS_MESSAGES) &&
				tmp.create(HOLDS_MESSAGES))
		{
			return true;
		}

		return false;
	}

	public synchronized boolean delete(boolean recurse)
		throws MessagingException
	{
		deleteCheck();
		
		if (neu.delete(recurse) && cur.delete(recurse) && tmp.delete(recurse))	{
			return super.delete(recurse);
		}
			
		return false;
	}

	public Folder getFolder(String s)
		throws MessagingException
	{
		//if (s.equals(cur.getName()) || s.equals(neu.getName()) || s.equals(tmp.getName()))
		//	throw new MessagingException("No access to internally used folder: "+getFullName() + SEPARATOR + s);
		// FRi 2003-06-16: Cann not do that as LocalStore must create these folders at begin
		
		return super.getFolder(s);
	}


	/** Implemented to move SEEN messages from "new" folder to "cur" folder when expunge is true. */
	public void close(boolean expunge)
		throws MessagingException
	{
		//System.err.println("==========> close folder "+getFullName()+" expunge "+expunge);
		if (expunge)	{
			handleSeenMessages(true);
			handleSeenMessages(false);
		}

		super.close(expunge);	// will expunge "cur" and "neu"

		cur.close(false);	// do not expunge as the folders could exist no more when deleted,
		neu.close(false);	// but they must get closed
	}


	private boolean handleSeenMessages(boolean fromNewToCur)
		throws MessagingException
	{
		Vector toMove = new Vector();
		//System.err.println("handling new and seen messages in "+getName());

		Message [] msgs = fromNewToCur ? neu.getMessages() : cur.getMessages();

		// check for RECENT but SEEN messages in "new" folder
		// or check for "set unread" messages in "cur" folder
		for (int i = 0; msgs != null && i < msgs.length; i++)	{
			Message msg = msgs[i];
			if (msg != null)	{
				boolean move = fromNewToCur ? msg.isSet(Flags.Flag.SEEN) : isNewMessage(msg);
				boolean deleted = msg.isSet(Flags.Flag.DELETED);
	
				if (move && !deleted)	{
					toMove.add(msg);
				}
			}
		}

		if (toMove.size() > 0)	{
			Message [] moving = new Message[toMove.size()];
			toMove.copyInto(moving);
			
			if (fromNewToCur)
				cur.appendMessages(moving);
			else
				neu.appendMessages(moving);
		
			for (int i = 0; i < toMove.size(); i++)	{
				Message msg = (Message)toMove.get(i);
				msg.setFlag(Flags.Flag.DELETED, true);
			}
		}
		
		return toMove.size() > 0;
	}

}
