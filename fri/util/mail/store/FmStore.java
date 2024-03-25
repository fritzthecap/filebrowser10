package fri.util.mail.store;

import java.io.*;
import java.util.*;
import javax.mail.*;

/**
	General purpose store for attaching to "maildir" compatible and other folders.
	@author Fritz Ritzberger
*/

public class	FmStore extends	Store
{
	private String rootPath;
	private Folder defFolder;
	Hashtable folderCache = new Hashtable();


	/** Create a local filesystem maildir-compatible store. */
	public FmStore(Session session, URLName urlname)	{
		super(session, urlname);

		this.rootPath = urlname.getFile();
		
		if (new File(rootPath).isAbsolute() == false)	{
			if (this.rootPath.startsWith("~"))
				this.rootPath = new File(System.getProperty("user.home"), rootPath.substring("~".length())).getAbsolutePath();
			else
				this.rootPath = new File(System.getProperty("user.dir"), rootPath).getAbsolutePath();
		}
		
		this.rootPath = this.rootPath.replace(File.separatorChar, FmFolder.SEPARATOR);
	}

	/** Always returns true, as hard disk is assumed to be present. */
	protected boolean protocolConnect( String host, int port, String username, String password )
		throws MessagingException
	{
		return true;
	}

	/** Creates a default folder with name "" if not exists, else returns it. */
	public Folder getDefaultFolder()
		throws MessagingException
	{
		if (defFolder == null)	{
			defFolder = getFolder("");
			
			if (defFolder.exists() == false)
				defFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
		}
		return defFolder;
	}

	/** Factory method for folders. Distinguishes maildir-compatible folders and others. */
	public Folder getFolder(String s)
		throws MessagingException
	{
		if (folderCache == null)
			return null;
		
		if (s.startsWith(""+FmFolder.SEPARATOR))	// standardize the folder path
			s = s.substring(1);
				
		Folder f = (Folder)folderCache.get(s);
		
		if (f == null)	{
			// default return a FmMaildirFolder if passed path does not exist
			if (s.length() > 0 && (FmMaildirFolder.isMaildirFolder(this, s) || new FmFolder(this, s).exists() == false))
				f = new FmMaildirFolder(this, s);
			else
				f = new FmFolder(this, s);
			
			folderCache.put(s, f);
			//System.err.println("FmStore put into cache: "+s+" - "+f.getFullName()+", now: "+folderCache);
		}
		
		return f;
	}

	/** Delegates to <i>getFolder(String)</i> with <i>urlName.getFile()</i> as argument. */
	public Folder getFolder(URLName urlname)
		throws MessagingException
	{
		return getFolder(urlname.getFile());
	}

	public void close()
		throws MessagingException
	{
		super.close();
		folderCache = null;
	}
	
	
	protected String getPath()	{
		return rootPath;
	}

}
