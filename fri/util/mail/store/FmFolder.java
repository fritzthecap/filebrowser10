package fri.util.mail.store;

import java.io.*;
import java.util.*;
import java.net.InetAddress;
import javax.mail.*;
import javax.mail.event.*;

/**
	Folder implementation, able to attach to already existing mail folders.
	@author Fritz Ritzberger
*/

public class FmFolder extends Folder
{
	public static final char SEPARATOR = '/';
	private boolean isOpen = false;
	private int type = Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES;
	private String name = null;
	private String path = null;
	private String [] fileCache = null;
	private String [] dirCache = null;
	private Vector msgCache = null;
	
	private static String localHost;	// needed to build message name
	static	{	// init
		try	{	
			localHost = InetAddress.getLocalHost().getHostName();
		}
		catch (Exception e)	{
			localHost = "localhost";
		}
	}


	public FmFolder(Store store, String path)	{
		super(store);

		if (path.endsWith(""+SEPARATOR))
			path = path.substring(0, path.length() - 1);

		if (path.startsWith(""+SEPARATOR))
			path = path.substring(1);

		int index = path.lastIndexOf(SEPARATOR);

		if (index < 0)	{
			this.name = path;
			this.path = "";
		}
		else	{
			this.name = path.substring(index + 1);
			this.path = path.substring(0, index);
		}
	}

	public String getName()
	{
		return name;
	}

	public String getFullName()
	{
		if (path.length() == 0)
			return name;
		else
			return path + SEPARATOR + name;
	}

	private String getFullPath()
	{
		return ((FmStore)store).getPath() + SEPARATOR + getFullName();
	}

	public int getType()
		throws MessagingException
	{
		return type;
	}

	public boolean exists()
		throws MessagingException
	{
		File file = new File(this.getFullPath());
		return file.isDirectory();
	}

	public boolean hasNewMessages()
		throws MessagingException
	{
		return false;
	}

	public void open(int mode)
		throws MessagingException
	{
		checkClosed();
		this.isOpen = true;
		ensureCache();
	}


	public Message [] expunge()
		throws MessagingException
	{
		checkOpen();
		
		if (exists() == false)
			throw new FolderNotFoundException(this);
		
		if (fileCache == null)
			return new Message[0];	// nothing to expunge
		
		Vector toDelete = new Vector(this.fileCache.length);
		Vector messages = new Vector(this.fileCache.length);
		Vector files = new Vector(this.fileCache.length);

		for (int i = 0; i < this.fileCache.length ; i++)	{
			Message msg = (Message) this.msgCache.elementAt(i);
			if (msg != null && msg.isSet(Flags.Flag.DELETED))	{
				//System.err.println("FmFolder.deleted message from "+getName()+": "+i);
				toDelete.addElement(Integer.valueOf(i));
			}
			else	{
				messages.addElement(msg);
				files.addElement(this.fileCache[i]);
			}
		}

		// delete
		Message [] deleted = new Message[toDelete.size()];
		
		for (int i = 0 ; i < toDelete.size(); i++)	{
			int index = ((Integer)toDelete.get(i)).intValue();
			
			deleted[i] = (Message)this.msgCache.elementAt(index);
			
			String fileName = getFullPath() + SEPARATOR + fileCache[index];
			File msgFile = new File(fileName);

			if (msgFile.exists() && msgFile.isFile())
				msgFile.delete();
			else
				System.err.println("ERROR: FmFolder.expunge(), file from local cache not found: "+msgFile);
		}

		// build new cache
		String [] newCache = new String[files.size()];
		for (int i = 0 ; i < newCache.length ; i++)
			newCache[i] = (String) files.elementAt(i);

		this.fileCache = newCache;
		this.msgCache = messages;
		//System.err.println("FmFolder.expunge in "+getFullName()+", creating fileCache with length "+fileCache.length);

		if (deleted.length > 0)	{
			//Thread.dumpStack();
			notifyMessageRemovedListeners(true, deleted);
		}

		return deleted;
	}


	public boolean isOpen()	{
		return isOpen;
	}

	public Store getStore()	{
		return store;
	}

	public Flags getPermanentFlags()	{
		return new Flags();
	}
	
	public int getMessageCount()
		throws MessagingException
	{
		ensureCache();
		return fileCache.length;
	}

	private File getMessageFile(int msgnum)
		throws MessagingException
	{
		ensureCache();

		//System.err.println("getMessageFile, number "+msgnum+", fileCahce length "+fileCache.length);
		String pathName = getFullPath() + SEPARATOR + fileCache[msgnum - 1];
		return new File(pathName);
	}

	public Message getMessage(int msgnum)
		throws MessagingException
	{
		checkOpen();
		
		ensureCache();

		Message message = (Message) msgCache.elementAt(msgnum - 1);

		if (message != null)
			return message;

		File msgFile = this.getMessageFile(msgnum);

		if (msgFile.exists() && msgFile.isFile())	{
			try {
				//System.err.println("reading message from File, number "+msgnum);
				FileInputStream msgStream = new FileInputStream(msgFile);
				message = new FmMessage(this, msgStream, msgnum);
				msgStream.close();

				/* 2007-05-12: getting AddressException from message.getFrom()
				Address [] from = message.getFrom();	// test if it is a valid message
				if (from == null)
					message = null;
				else
				*/
					msgCache.setElementAt(message, msgnum - 1);
			}
			catch (Exception ex)	{
				ex.printStackTrace();
				message = null;
			}
		}

		return message;
	}
	
	public synchronized void appendMessages(Message [] msgs)
		throws MessagingException
	{
		if (msgs == null || msgs.length <= 0)
			return;

		ensureCache();

		String [] newCache = new String[fileCache.length + msgs.length];
		
		// copy old messages to new cache
		System.arraycopy(fileCache, 0, newCache, 0, fileCache.length);

		for (int i = 0, ni = fileCache.length; i < msgs.length; ++i)	{
			FileOutputStream outS = null;
			FileInputStream inS = null;
			
			try {
				String newName;
				String newPath;
				
				// generate the message in local store
				newName = makeUniqueName();
				newPath = getFullPath() + SEPARATOR + newName;

				// store the message to disk
				outS = new FileOutputStream(newPath);
				
				Folder f = msgs[i].getFolder();	// ensure the folder is open for retrieving message
				boolean doClose = false;
				if (f != null && f.isOpen() == false)	{
					f.open(Folder.READ_ONLY);
					doClose = true;
				}
					
				msgs[i].writeTo(outS);
				
				if (doClose)
					f.close(false);

				outS.close();

				// append new name to new cache
				newCache[ni] = newName;
				ni++;

				// read message and put it into cache
				inS = new FileInputStream(newPath);
				FmMessage newMsg = new FmMessage(this, inS, msgCache.size());
				inS.close();
				msgCache.addElement(newMsg);
				//System.err.println("... appended message to >"+getName()+"<, toString "+newMsg);
				
				// write file-mail to argument array, so listeners can identify it
				msgs[i] = newMsg;
			}
			catch (IOException ex)	{
				ex.printStackTrace();
				throw new MessagingException("while writing/reading message: "+ex.getMessage());
			}
			finally	{
				try	{ outS.close(); }	catch (Exception e)	{}
				try	{ inS.close(); }	catch (Exception e)	{}
			}
		}

		this.fileCache = newCache;
		//System.err.println("FmFolder.appendMessages in "+getFullName()+", message cache size "+msgCache.size()+" fileCache "+fileCache.length);

		notifyMessageAddedListeners(msgs);
	}

	private String makeUniqueName()	{
		String newName;
		do	{	// make new message name from current time millis and hostname
			StringBuffer sb = new StringBuffer(""+System.currentTimeMillis());

			// fill with leading zeros for better sorting, millis are 9 digits on 2003-04-20
			while (sb.length() < 12)
				sb.insert(0, '0');

			sb.append(".");	// append ".hostname"
			sb.append(localHost);
			
			newName = sb.toString();
		}
		while (new File(this.getFullPath() + SEPARATOR + newName).exists());
		
		return newName;
	}

	public void fetch(Message [] message, FetchProfile fetchprofile)
		throws MessagingException
	{
	}

	public Folder getParent()
		throws MessagingException
	{
		Folder parent = store.getFolder(path);
		//System.err.println("FmFolder.getParent returns "+parent.getFullName()+" for this folder "+getFullName());
		return parent;
	}

	public synchronized Folder[] list(String filter)
		throws MessagingException
	{
		Vector v = new Vector();
		//System.err.println("FmFolder.list for folder "+getFullName());

		ensureCache();

		for (int i = 0; i < this.dirCache.length; i++)	{
			File dir = new File(getFullPath() + SEPARATOR + dirCache[i]);

			if (dir.exists() && dir.isDirectory() && internalFolderListCondition(dirCache[i]))
				v.addElement(getFullName() + SEPARATOR + dirCache[i]);
		}

		Folder [] result = new Folder[v.size()];

		for (int i = 0; i < v.size(); i++)	{
			String dir = (String) v.elementAt(i);
			result[i] = store.getFolder(dir);
		}

		return result;
	}
	
	protected boolean internalFolderListCondition(String dirName)	{
		return true;
	}
	

	public char getSeparator()	{
		return SEPARATOR;
	}

	public synchronized boolean create(int type)
		throws MessagingException
	{
		//System.err.println("FmFolder.create of "+getFullName());
		this.type = type;
		
		File fDir = new File(getFullPath());
		if (fDir.isFile())
			throw new MessagingException("Folder to create exists as file: "+getFullPath());
			
		boolean ok = fDir.mkdirs();
		
		if (ok)	{
			((FmFolder)getParent()).fileCache = null;	// force refresh of parent cache
			//System.err.println("FmFolder, releasing cache of "+getParent().getFullName());
			
			notifyFolderListeners(FolderEvent.CREATED);
		}
		
		return ok;
	}

	
	public synchronized boolean delete(boolean recurse)
		throws MessagingException
	{
		deleteCheck();
		
		open(READ_WRITE);
		
		// according to specification in javax.mail.Folder delete all messages
		int msgCount = getMessageCount();	// loads cache

		for (int i = 0; i < msgCount; i++)	{
			Message msg = getMessage(i + 1);
			msg.setFlag(Flags.Flag.DELETED, true);
		}

		close(true);	// delete effectively

		// according to specification in javax.mail.Folder return if not recursive
		if (recurse == false && dirCache.length > 0)
			return true;

		boolean ret = true;
		
		Folder [] folders = list();	// there will be no folders if recurse is false
		for (int i = 0; i < folders.length; i++)	{
			if (folders[i].exists() && folders[i].delete(recurse) == false)
				ret = false;	// at least one has not been deleted
		}
		
		if (ret)	{	// if no folders were present or all folders were deleted
			((FmFolder)getParent()).fileCache = null;
			((FmStore)store).folderCache.remove(getFullName());

			File fDir = new File(getFullPath());
			ret = fDir.delete();
		}
		
		if (ret)
			notifyFolderListeners(FolderEvent.DELETED);
		
		return ret;
	}


	public boolean renameTo(Folder folder)
		throws MessagingException
	{
		deleteCheck();
		
		String oldPath = getFullName();
		
		String fPath = ((FmStore)store).getPath() + SEPARATOR + getFullName();
		String newPath = ((FmStore)store).getPath() + SEPARATOR + folder.getFullName();

		File fDir = new File(fPath);
		File nDir = new File(newPath);

		boolean ok = fDir.renameTo(nDir);
		
		if (ok)	{
			((FmFolder)getParent()).fileCache = null;
			((FmFolder)folder.getParent()).fileCache = null;
			((FmStore)store).folderCache.remove(oldPath);
		}
		
		notifyFolderRenamedListeners(this);
		
		return ok;
	}

	public Folder getFolder( String s )
		throws MessagingException
	{
		return this.store.getFolder ( this.getFullName() + SEPARATOR + s );
	}


	protected void deleteCheck()
		throws MessagingException
	{
		checkClosed();
		
		if (exists() == false)
			throw new FolderNotFoundException(this);
	}
	
	private void checkClosed()
		throws IllegalStateException
	{
		if (isOpen)
			throw new IllegalStateException("folder is open: "+getFullName());
	}

	private void checkOpen()
		throws IllegalStateException
	{
		if (isOpen == false)
			throw new IllegalStateException("folder is closed: "+getFullName());
	}



	public void close(boolean expunge)
		throws MessagingException
	{
		//System.err.println("------------> close folder "+getFullName()+" expunge "+expunge);
		//Thread.dumpStack();
		checkOpen();

		if (expunge)
			expunge();

		isOpen = false;
	}


	private void ensureCache()	{
		if (this.fileCache == null)	{
			//System.err.println("filling FmFolder cache of folder "+getFullName()+" ...");

			File dir = new File(getFullPath());
			String [] dirList = dir.list();
			if (dirList == null)
				dirList = new String[0];

			Vector dirs = new Vector(dirList.length);
			Vector files = new Vector(dirList.length);
			this.msgCache = new Vector(dirList.length);
			
			for (int i = 0; i < dirList.length; i++)	{
				File f = new File(dir, dirList[i]);
				
				if (f.exists() && f.isHidden() == false && f.getName().startsWith(".") == false)	{
					if (f.isDirectory())	{
						dirs.addElement(dirList[i]);
					}
					else
					if (f.isFile())	{
						files.addElement(dirList[i]);
						this.msgCache.addElement(null);
					}
				}
			}

			this.dirCache = new String[dirs.size()];
			dirs.copyInto(this.dirCache);

			this.fileCache = new String[files.size()];
			files.copyInto(this.fileCache);
		}
	}

}
