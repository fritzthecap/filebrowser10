package fri.util.mail;

import java.io.*;
import javax.mail.*;
import fri.util.props.*;

/**
	Local disk mail provider and saver.
	Ensures that an mail infrastructure exists under the
	folder that is named by the store URL. By setting a new
	store URL the local store is attached to another directory.
	
	@author Fritz Ritzberger, 2003
*/

public abstract class LocalStore
{
	/** The protocolname for the default local store. */
	public static final String LOCALSTORE_PROTOCOL = "localstore";	// must equal to driver config META-INF/javamail.providers

	/** The folder that holds received messages. */
	public static final String INBOX = "inbox";
	
	/** The folder that holds received messages. */
	public static final String OUTBOX = "outbox";
	
	/** The folder that holds received messages. */
	public static final String SENT = "sent-mail";
	
	/** The folder that holds received messages. */
	public static final String DRAFTS = "drafts";
	
	/** The folder that holds received messages. */
	public static final String TRASH = "trash";
	
	
	/** The subfolder that holds the already seen messages. */
	public static final String NEW = "new";

	/** The subfolder that holds the new messages. */
	public static final String SEEN = "cur";

	/** The subfolder that holds the temporary delivering messages. */
	public static final String DELIVER = "tmp";


	/** The existing folders of the local store, under default folder. */
	public static final String [] FOLDERS = {
		INBOX,
		OUTBOX,
		SENT,
		DRAFTS,
	};

	private static String url;
	private static String defaultUrl = LOCALSTORE_PROTOCOL+":"+ConfigDir.dir()+"FmMail"+File.separator;
	
	static	{
		String s = ClassProperties.get(LocalStore.class, "url");
		if (s == null)
			url = defaultUrl;
		else
			url = s;
	}
	
	
	/**
		Returns the default URL for storing or receiving messages to and from local disk.
		This URL can be used to construct a ReceiveMail object that provides access to
		some local disk mail store.
	*/
	public static String getUrl()	{
		return url;
	}

	/**
		Sets the default URL for storing or receiving messages to and from local disk.
		This URL can be used to construct a ReceiveMail object that provides access to
		some local disk mail store.
	*/
	public static void setUrl(String url)	{
		LocalStore.url = url;
	}
	
	/** Store the url for local store to property file if not default. */
	public static void close()	{
		if (url.equals(defaultUrl))
			ClassProperties.remove(LocalStore.class, "url");
		else
			ClassProperties.put(LocalStore.class, "url", url);
	
		ClassProperties.store(LocalStore.class);
	}


	/** Returns the local store root. */
	public synchronized static ObservableReceiveMail getRoot()
		throws Exception
	{
		return getReceiveFolder((String)null);
	}

	/**
		Returns a receiver that provides access to some local disk root folder.
		This receiver can be passed to <i>save()</i> method to store some messages.
		This method creates a local store on disk if not already existing.
		@param path path of target folder like { LocalStore.INBOX, LocalStore.SEEN, ... }
	*/
	public synchronized static ObservableReceiveMail getReceiveFolder(String [] path)
		throws Exception
	{
		ObservableReceiveMail receiveMail = ensureMaildir();
		receiveMail.cd(path);
		return receiveMail;
	}
	
	/**
		Shorthand for full path getReceiveFolder() method.
	*/
	public synchronized static ObservableReceiveMail getReceiveFolder(String folderName)
		throws Exception
	{
		return getReceiveFolder(new String [] { folderName });
	}


	/** Returns true if the local store is existing. */
	public static boolean exists()	{
		File f = new File(localStoreDirectory());
		if (f.exists() == false)
			return false;

		if (f.isDirectory() == false)
			throw new IllegalStateException("Local store folder is not a directory (supports only maildir folders): "+f);

		// check the maildir structure
		for (int i = 0; i < FOLDERS.length; i++)	{
			File maildirFolder = new File(f, FOLDERS[i]);
			if (maildirFolder.isDirectory() == false)
				return false;

			if (new File(maildirFolder, SEEN).isDirectory() == false)
				return false;
			if (new File(maildirFolder, NEW).isDirectory() == false)
				return false;
			if (new File(maildirFolder, DELIVER).isDirectory() == false)
				return false;
		}

		File maildirFolder = new File(f, TRASH);
		if (maildirFolder.isDirectory() == false)
			return false;
		
		return true;
	}
	
	/** Returns the path of the local store root directory. */
	public static String localStoreDirectory()	{
		URLName urlName = new URLName(LocalStore.getUrl());
		return urlName.getFile();
	}
	
	/** Returns the path of the local store root directory. */
	public static boolean isWriteable()	{
		File f = new File(localStoreDirectory());
		return f.canWrite();
	}
	



	private static ObservableReceiveMail ensureMaildir()
		throws Exception
	{
		ObservableReceiveMail receiveMail = new ObservableReceiveMail(LocalStore.getUrl(), true);
		if (exists() == false)	{
			Folder root = receiveMail.pwd();
			createMaildir(root);
			receiveMail.close();
			
			// recreate the folder, else FmMaildirFolder will not be created
			receiveMail = new ObservableReceiveMail(LocalStore.getUrl(), true);
		}
		return receiveMail;
	}
	
	private static void createMaildir(Folder root)
		throws Exception
	{
		for (int i = 0; i < FOLDERS.length; i++)	{
			Folder f = root.getFolder(FOLDERS[i]);
			f.create(Folder.HOLDS_FOLDERS);
			createMaildirStructure(f);
		}
		Folder f = root.getFolder(TRASH);
		f.create(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);
	}

	private static void createMaildirStructure(Folder root)
		throws Exception
	{
		Folder f;
		f = root.getFolder(SEEN);	// the inbox non-new messages
		f.create(Folder.HOLDS_MESSAGES);
		
		f = root.getFolder(NEW);	// the inbox new messages
		f.create(Folder.HOLDS_MESSAGES);
		
		f = root.getFolder(DELIVER);	// the inbox in-progress messages
		f.create(Folder.HOLDS_MESSAGES);
	}


	private LocalStore()	{}

}