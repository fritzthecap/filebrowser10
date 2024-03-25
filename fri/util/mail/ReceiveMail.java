package fri.util.mail;

import java.io.PrintStream;
import java.util.Properties;
import javax.mail.*;

/**
	POP or IMAP client that offers folders, looping of messages and folders
	by an inner interface, and ensures that folders and the store get closed
	after retrieval. In the case of POP there is only one folder called "INBOX",
	with no subfolders.
	
	@author Fritz Ritzberger, 2003
*/

public class ReceiveMail implements
	Cloneable
{
	public static final int DEFAULT_POP_PORT = 110;
	protected Store store;
	protected boolean isPop;
	private Folder folder;
	private PrintStream log = System.err;
	private URLName urlName;
	private String host, user, password;
	private int port = -1;
	private boolean connected;
	private Authenticator authenticator;


	/** Client interface to loop folders or messages. */
	public interface MailVisitor
	{
		public void message(int msgCount, int msgNr, Message m);
		public void folder(int fldCount, int fldNr, Folder f);
	}
	
	/** Client interface to loop folders or messages. */
	public interface AllMessagesVisitor
	{
		public void messages(Message [] m);
	}
	
	/** Client interface to loop the structure of one messages. */
	public interface MessagePartVisitor
	{
		public void finalPart(int absolutePartNumber, int treeLevel, Part part) throws Exception;
		public void multiPart(int absolutePartNumber, int treeLevel, Part part) throws Exception;
	}


	


	/** Create a mail receive object, driven by passed mail properties. */
	public ReceiveMail(Properties mailProperties, Authenticator authenticator)
		throws Exception
	{
		this.authenticator = authenticator;
		
		Session session = Session.getInstance(mailProperties, authenticator);
		store = session.getStore();

		String protocol = mailProperties.getProperty("mail.store.protocol");
		isPop = protocol == null || protocol.equals("pop3");

		String port = mailProperties.getProperty("mail."+protocol+".port");
		int portNr = port != null ? Integer.parseInt(port) : -1;
		urlName = new URLName(
				protocol,
				mailProperties.getProperty("mail."+protocol+".host"),
				portNr,
				null,
				mailProperties.getProperty("mail."+protocol+".user"),
				mailProperties.getProperty("mail."+protocol+".password"));	// will be null
	}
	
	/** Create a mail receive object, driven by passed explicit arguments. */
	public ReceiveMail(String protocol, String host, String port, String user, String password)
		throws Exception
	{
		Session session = Session.getInstance(new Properties());
		store = session.getStore(protocol);
		int portNr = (port != null) ? Integer.parseInt(port) : -1;

		isPop = protocol == null || protocol.equals("pop3");
		urlName = new URLName(protocol, host, portNr, null, user, password);

		this.host = host;
		this.port = portNr;
		this.user = user;
		this.password = password;
	}

	/** Create a mail receive object, driven by passed URL. */
	public ReceiveMail(String urlName)
		throws Exception
	{
		log.println("creating store with URLName "+urlName);
		
		Session session = Session.getInstance(new Properties());
		this.urlName = new URLName(urlName);
		store = session.getStore(this.urlName);

		isPop = urlName.startsWith("pop3");
	}
	
	/** Clone constructor. */
	protected ReceiveMail()	{
	}


	
	public boolean isConnected()	{
		return connected;
	}

	private void connect()
		throws Exception
	{
		log.println("Connecting to store ... host="+getURLName().getHost()+", file="+getURLName().getFile());
		//Thread.dumpStack();

		if (host != null)
			store.connect(host, port, user, password);
		else
			store.connect();
	}

	private void ensureConnect()
		throws Exception
	{
		if (connected == false)	{
			connect();
			connected = true;
		}
	}
	
	public Authenticator getAuthenticator()	{
		return authenticator;
	}

	/** Returns the store. Do not close! */
	public Store getStore()	{
		return store;
	}

	/** Returns the URLName of this mail connection. */
	public URLName getURLName()	{
		return urlName;
	}

	/**
		Returns a clone of this object. The new object's current folder will
		be the same as the cloned. This method is for copy and move within one store.
	*/
	public Object clone()	{
		ReceiveMail rm = newInstance();
		rm.store = store;
		rm.isPop = isPop;
		rm.folder = folder;
		rm.connected = connected;
		if (connected == false)	{
			rm.host = host;
			rm.port = port;
			rm.user = user;
			rm.password = password;
		}
		return rm;
	}
	
	/** Override to allocate a subclass clone instance. Fill with initial values there. */
	protected ReceiveMail newInstance()	{
		return new ReceiveMail();
	}
	
	


	
	private Folder openCurrentFolder(int flag)
		throws Exception
	{
		ensureConnect();
		
		Folder f = null;
		try	{
			f = pwd();

			if (f.isOpen() == false)	{
				log.println("Opening folder >"+f.getName()+"< with flag "+flag);
				f.open(flag);
			}
		}
		catch (Exception e)	{
			log.println("Could not open "+f);
		}
		return f;
	}

	private void checkPop(String method)
		throws MessagingException
	{
		if (isPop)
			throw new MessagingException("Not supported for POP3, as there is only INBOX: \""+method+"\"");
	}
	
	
	public Folder pwd()
		throws Exception
	{
		ensureConnect();
		
		if (this.folder == null)
			this.folder = store.getDefaultFolder();
		return this.folder;
	}
	
	public Folder cd(String [] path)
		throws Exception
	{
		Folder f = pwd();
		
		for (int i = 0; path != null && i < path.length; i++)
			f = cd(path[i]);
			
		return f;
	}
	
	public Folder cd(String folderName)
		throws Exception
	{
		if (folderName == null)	{	// change back to root
			this.folder = null;
		}
		else	{	// change to folder if it exists
			Folder f = pwd();
			Folder newFolder = f.getFolder(folderName);

			if (newFolder != null && newFolder.exists())	{
				this.folder = newFolder;
			}
			else	{
				throw new FolderNotFoundException(newFolder);
			}
		}

		return pwd();
	}
	
	public Folder cdup()
		throws Exception
	{
		checkPop("cdup");
		
		this.folder = pwd().getParent();
		
		return pwd();
	}

	

	/** Returns the count of messages, opening the folder if necessary */
	public int getMessageCount()
		throws Exception
	{
		return getMessageCount(false);
	}

	/** Returns the count of new (unread) messages, opening the folder if necessary */
	public int getNewMessageCount()
		throws Exception
	{
		return getMessageCount(true);
	}
	
	/** Returns the count of new (unread) messages, opening the folder if necessary */
	public int getMessageCount(boolean newCount)
		throws Exception
	{
		synchronized(pwd())	{
			int i = newCount ? pwd().getNewMessageCount() : pwd().getMessageCount();
			if (i == -1)	{	// specification says -1 means getMessageCount() only supported on open folders
				Folder f = openCurrentFolder(Folder.READ_ONLY);
				try	{
					i = newCount ? f.getNewMessageCount() : f.getMessageCount();
				}
				finally	{
					f.close(false);
					log.println("2: Closing folder >"+f.getFullName()+"< with false");
				}
			}
			return i;
		}
	}


	/** Deletes the named folder. */
	public void delete(String folderName, boolean recurse)
		throws Exception
	{
		checkPop("delete");

		Folder toDelete = pwd().getFolder(folderName);
		synchronized(toDelete)	{
			toDelete.delete(recurse);
		}
	}


	/** Moves the named folder to target by copy and delete. Returns the moved folder. */
	public Folder move(String folderName, ReceiveMail target)
		throws Exception
	{
		checkPop("move");

		Folder toMove = pwd().getFolder(folderName);
		synchronized(toMove)	{
			Folder newFolder = copy(folderName, target, null);
			if (newFolder != null)
				toMove.delete(true);
				
			return newFolder;
		}
	}

	/** Copies the named folder (in current directory) to target. Returns the copied (original) folder. */
	public Folder copy(String folderName, ReceiveMail target, String newName)
		throws Exception
	{
		Folder toCopy = pwd().getFolder(folderName);
		if (toCopy.exists() == false)
			throw new FolderNotFoundException(toCopy);
			
		synchronized(toCopy)	{
			if (newName == null || newName.equals(folderName))	{	// copy with same name
				target.append(new Folder [] { toCopy });
				return target.cd(folderName);
			}
			else	{	// copy to new name, must create new folder and put anything there
				target.create(newName);
				target.cd(newName);

				target.append(toCopy.list());	// can be invoked on closed folder
				
				toCopy.open(Folder.READ_ONLY);
				target.append(toCopy.getMessages());
				toCopy.close(false);
				
				return target.pwd();
			}
		}
	}


	public void append(Message [] msgs)
		throws Exception
	{
		synchronized(pwd())	{	// can be invoked on closed folder
			if (msgs != null && msgs.length > 0)	{
				pwd().appendMessages(msgs);
			}
		}
	}
	

	public void delete(Message [] msgs)
		throws Exception
	{
		synchronized(pwd())	{
			Folder f = openCurrentFolder(Folder.READ_WRITE);
			try	{
				internalDelete(msgs);
			}
			finally	{
				f.close(true);
				log.println("3: Closing folder >"+f.getFullName()+"< with true");
			}
		}
	}
	
	/** Delete messages by index 0-n. */
	public void delete(int [] msgNrs)
		throws Exception
	{
		Folder f = openCurrentFolder(Folder.READ_WRITE);
		try	{
			Message [] msgs = null;
			
			if (msgNrs == null)	{	// delete all
				msgs = f.getMessages();
			}
			else	{	// delete specified
				msgs = new Message[msgNrs.length];
				for (int i = 0; i < msgNrs.length; i++)	{
					int msgNr = msgNrs[i] + 1;	// messages are numbered 1-n
					msgs[i] = f.getMessage(msgNr);
				}
			}
			
			internalDelete(msgs);
		}
		finally	{
			f.close(true);
			log.println("4: Closing folder >"+f.getFullName()+"< with true");
		}
	}

	private void internalDelete(Message [] msgs)
		throws Exception
	{
		for (int i = 0; msgs != null && i < msgs.length; i++)	{
			msgs[i].setFlag(Flags.Flag.DELETED, true);
			log.println("set deleted flag to message "+msgs[i].getSubject());
		}
	}
	

	public void append(Folder [] folders)
		throws Exception
	{
		checkPop("append");

		for (int i = 0; folders != null && i < folders.length; i++)	{
			String name = folders[i].getName();
			System.err.println("ReceiveMail creating folder "+name+" in folder "+pwd());
			create(name);
			cd(name);
			
			synchronized(folders[i])	{
				folders[i].open(Folder.READ_ONLY);
				try	{
					Message [] msgs = folders[i].getMessages();
					if (msgs != null && msgs.length > 0)
						append(msgs);
					
					Folder [] flds = folders[i].list();
					if (flds != null && flds.length > 0)
						append(flds);
				}
				finally	{
					folders[i].close(false);
					log.println("5: Closing folder >"+folders[i].getFullName()+"< with false");
				}
			}
			
			cdup();
		}
	}

	public Folder create(String folderName)
		throws Exception
	{
		checkPop("create");

		synchronized(pwd())	{
			Folder f = openCurrentFolder(Folder.READ_WRITE);
			try	{
				Folder newFolder = f.getFolder(folderName);
	
				if (newFolder.exists() == false)	{
					newFolder.create(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);
				}
				return newFolder;
			}
			finally	{
				f.close(true);
				log.println("6: Closing folder >"+f.getFullName()+"< with true");
			}
		}
	}
	
	/** Rename the current folder. The new name MUST NOT contain a full path, it is just the new folder name. */
	public Folder rename(String newFolderName)
		throws Exception
	{
		checkPop("rename");
		
		if (newFolderName.indexOf("/") >= 0)
			throw new IllegalArgumentException("Renaming a folder with a path specification is not allowed: "+newFolderName+" - use move for that purpose.");

		synchronized(pwd())	{
			Folder folder = pwd();
			Folder parent = folder.getParent();
			Folder newFolder = parent.getFolder(newFolderName);
			
			if (folder.renameTo(newFolder))	{
				this.folder = newFolder;
			}
			else	{
				throw new MessagingException("Could not rename folder to: "+newFolderName);
			}
			return pwd();
		}
	}
	
	
	public void folders(MailVisitor visitor)
		throws Exception
	{
		checkPop("folders");

		Folder [] folders = pwd().list();
		for (int i = 0; folders != null && i < folders.length; i++)	{
			visitor.folder(folders.length, i, folders[i]);
		}
	}
	

	public void get(int msgNr, MailVisitor v)
		throws Exception
	{
		Folder f = openCurrentFolder(Folder.READ_ONLY);
		try	{
			v.message(1, msgNr, f.getMessage(msgNr + 1));	// messages are numbered 1-n
		}
		finally	{
			f.close(false);
			log.println("7: Closing folder >"+f.getFullName()+"< with false");
		}
	}


	/** Opens the current folder READ_WRITE, passes all messages to visitor, and closes the folder with expunge true. */
	public void messages(MailVisitor visitor)
		throws Exception
	{
		synchronized(pwd())	{
			//log.println("... messages(MailVisitor) got monitor for pwd");
			Folder f = openCurrentFolder(Folder.READ_WRITE);	// setting DELETE flag could happen when retrieving from server
			try	{
				int cnt = f.getMessageCount();
				log.println("Got "+cnt+" messages in folder "+this.folder);
				for (int i = 0; i < cnt; i++)	{
					Message msg = f.getMessage(i + 1);
					if (msg != null && visitMessage(visitor, cnt, i, msg) == false)
						break;
				}
			}
			finally	{
				//if (f.isOpen())	{
					f.close(true);	// expunge as DELETE could have happened when retrieving from server
					log.println("8: Closing folder >"+f.getFullName()+"< with true");
				//}
			}
		}
	}

	protected boolean visitMessage(MailVisitor visitor, int count, int nr, Message msg)
		throws Exception
	{
		visitor.message(count, nr, msg);
		return true;
	}


	public void messages(AllMessagesVisitor visitor)
		throws Exception
	{
		Folder f = openCurrentFolder(Folder.READ_ONLY);
		try	{
			visitor.messages(f.getMessages());
		}
		finally	{
			f.close(false);
			log.println("9: Closing folder >"+f.getFullName()+"< with false");
		}
	}


	
	/** Visit a single message where folder open and close is guaranteed. No expunge is performed when closing. */
	public void message(Message msg, MailVisitor visitor)
		throws Exception
	{
		Folder f = msg.getFolder();
		synchronized(f)	{
			try	{
				if (f.isOpen() == false)
					f.open(Folder.READ_ONLY);

				visitor.message(0, 0, msg);
			}
			finally	{
				if (f.isOpen())	{
					f.close(false);
					log.println("10: Closing folder >"+f.getFullName()+"< with false");
				}
			}
		}
	}
	
	/** Visit a single message where folder open and close is guaranteed. No expunge is performed when closing. */
	public void messageParts(Message msg, MessagePartVisitor visitor)
		throws Exception
	{
		Folder f = msg.getFolder();
		synchronized(f)	{
			try	{
				if (f.isOpen() == false)
					f.open(Folder.READ_ONLY);
				visitMessageParts(msg, visitor, 0, 0);
			}
			finally	{
				if (f.isOpen())	{
					f.close(false);
					log.println("11: Closing folder >"+f.getFullName()+"< with false");
				}
			}
		}
	}
	
	public void messageParts(int msgNr, MessagePartVisitor visitor)
		throws Exception
	{
		msgNr++;	// messages are numbered 1-n
		Folder f = openCurrentFolder(Folder.READ_ONLY);
		try	{
			Message m = f.getMessage(msgNr);
			visitMessageParts(m, visitor, 0, 0);
		}
		finally	{
			f.close(false);
			log.println("12: Closing folder >"+f.getFullName()+"< with false");
		}
	}
	
	
	private int visitMessageParts(Part part, MessagePartVisitor visitor, int absolutePartNumber, int treeLevel)
		throws Exception
	{
		if (part.isMimeType("multipart/*"))	{
			visitor.multiPart(absolutePartNumber, treeLevel, part);
			absolutePartNumber++;
			
			Multipart mp = (Multipart)part.getContent();
			int cnt = mp.getCount();
			for (int i = 0; i < cnt; i++)	{
				absolutePartNumber = visitMessageParts(mp.getBodyPart(i), visitor, absolutePartNumber, treeLevel + 1);
			}
			return absolutePartNumber;
		}
		else
		if (part.isMimeType("message/rfc822"))	{
			Part containedMessage = (Part)part.getContent();
			visitor.multiPart(absolutePartNumber, treeLevel, containedMessage);
			
			return visitMessageParts(containedMessage, visitor, absolutePartNumber + 1, treeLevel + 1);
		}
		else	{
			visitor.finalPart(absolutePartNumber, treeLevel, part);
			return absolutePartNumber + 1;
		}
	}
	
	
	/**
		Opens and closes the current folder (when folder is not open).
		This moves SEEN messages from "new" to "cur" qmail-folder and
		expunges DELETED messages.
	*/
	public void leaveCurrentFolder()
		throws Exception
	{
		log.println("start to leave current folder");
		synchronized(pwd())	{
			log.println("... leaveCurrentFolder got monitor for pwd");
			if (pwd().isOpen() == false)	{
				Folder f = openCurrentFolder(Folder.READ_WRITE);
				log.println("leaving folder, name "+f.getName());
				f.close(true);
				log.println("13: Closing folder >"+f.getFullName()+"< with true");
			}
		}
		// else: leave the folder management to GUI application
		// FRi 2003-06-22: is this obsolete?
	}



	/** Call this when closing mail session. Closes the open mail store. */
	public void close()
		throws Exception
	{
		store.close();
		log.println("Store closed.");
	}



	/**
		Returns true if passed object is instanceof ReceiveMail and has the same store as this.
	*/
	public boolean equals(Object o)	{
		if (o instanceof ReceiveMail == false)
			return false;
			
		return ((ReceiveMail)o).store.equals(store);
	}
	
	/**
		Returns the store's hashcode.
	*/
	public int hashCode()	{
		return store.hashCode();
	}

}


	/**
		Returns the (recursive) byte size of current folder, i.e. the byte sum of all messages.
		This could be a time-consuming method!
	public long size(boolean recurse)
		throws Exception
	{
		Folder f = openCurrentFolder(Folder.READ_ONLY);

		try	{
			long size = 0L;
			Message [] msgs = f.getMessages();
			
			for (int i = 0; msgs != null && i < msgs.length; i++)	{
				size += msgs[i].getSize();
			}
			
			if (recurse)	{
				Folder [] flds = f.list();
				
				for (int i = 0; flds != null && i < flds.length; i++)	{
					cd(flds[i].getName());
					size += size(recurse);
					cdup();
				}
			}
			
			return size;
		}
		finally	{
			f.close(false);
			log.println("1: Closing folder >"+f.getFullName()+"< with false");
		}
	}
	*/

