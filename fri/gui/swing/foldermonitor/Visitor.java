package fri.gui.swing.foldermonitor;

import java.io.File;
import java.util.Date;
import fri.util.file.RecursiveFileVisitor;

class Visitor extends RecursiveFileVisitor
{
	private EventRenderer renderer;
	private FileCache cache;
	private boolean doCheck;
	private WatchThread homeThread;
	
	Visitor(EventRenderer renderer, WatchThread homeThread, File file, FileCache cache, boolean doCheck)	{
		this.renderer = renderer;
		this.homeThread = homeThread;
		this.cache = cache;
		this.doCheck = doCheck;
		
		try	{
			loop(file, null, true);	// visit folders before contained files
		}
		catch (RuntimeException e)	{
			System.err.println("Halting with "+e);
		}
	}
	
	protected void visit(File f)	{
		if (homeThread.isInterrupted())
			throw new RuntimeException("halting file tree recursion ...");

		if (doCheck)	{
			FileCache.Info info = (FileCache.Info)cache.get(f);
			if (info == null)	{	// was not there
				renderer.event(new Date(), Constants.EVENT_CREATED, f.getName(), f.getParent(), Constants.toTypeString(f), f.length());
				cache.putFile(f);	// refresh time
			}
			else
			if (f.isDirectory() == false && info.lastModified != f.lastModified())	{
				renderer.event(new Date(), Constants.EVENT_MODIFIED, f.getName(), f.getParent(), Constants.toTypeString(f), f.length());
				cache.putFile(f);	// refresh time
			}
		}
		else	{	// do fill
			cache.putFile(f);
			//System.err.println("Scanning file "+f);
		}
	}

}
