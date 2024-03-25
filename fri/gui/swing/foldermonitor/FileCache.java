package fri.gui.swing.foldermonitor;

import java.io.File;
import java.util.*;

/**
	The Model:
	Buffer file and folders and their dynamical retrieved info (lastModified, type).
*/

class FileCache extends Hashtable
{
	private EventRenderer renderer;
	private WatchThread homeThread;
	
	FileCache(EventRenderer renderer, WatchThread homeThread)	{
		this.renderer = renderer;
		this.homeThread = homeThread;
	}
	
	public void putFile(File f)	{
		super.put(f, new Info(f));
	}
	
	public void checkFiles()	{
		// collect deleted files sorted by path (length)
		TreeMap deleted = new TreeMap();
		Set set = entrySet();
		Iterator it = set.iterator();
		
		while (it.hasNext())	{
			if (homeThread.isInterrupted())
				return;
				
			Map.Entry entry = (Map.Entry)it.next();
			File f = (File)entry.getKey();
			if (f.exists() == false)	{
				deleted.put(f.getPath(), f);
			}
		}

		// loop deleted files sorted by path length, avoid indicating deletion of folder children
		Date date = new Date();
		set = deleted.entrySet();
		it = set.iterator();
		String prevPath = null;
		
		while (it.hasNext())	{	// directories will be first
			if (homeThread.isInterrupted())
				return;
				
			Map.Entry entry = (Map.Entry)it.next();
			String path = (String)entry.getKey();
			File f = (File)entry.getValue();
			
			Info info = (Info)get(f);	// get file info before removing from cache
			remove(f);	// remove deleted file from cache
			
			if (prevPath == null || path.startsWith(prevPath) == false)	{
				renderer.event(date, Constants.EVENT_DELETED, f.getName(), f.getParent(), info.type, info.size);
				
				prevPath = path;
				if (prevPath.endsWith(File.separator) == false)	// ensure that startsWith() works correctly
					prevPath = prevPath+File.separator;
			}
		}
	}



	static class Info
	{
		public final String type;
		public final long lastModified;
		public final long size;

		Info(File f)	{
			type = Constants.toTypeString(f);
			lastModified = f.lastModified();
			size = f.length();
		}
	
	}

}