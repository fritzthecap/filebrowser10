package fri.util.file.archive;

import java.io.*;
import java.util.*;
import fri.util.io.CopyStream;
import fri.util.observer.CancelProgressObserver;
import fri.util.tar.*;

/**
	Adds cancel and progress capability to the tar archive.
*/

public class ObserveableTarArchive extends SelectiveTarArchive
{
	private CancelProgressObserver observer;


	public ObserveableTarArchive(InputStream in)
		throws IOException
	{
		super(in);
	}
	

	/**
		Stores the observer and calls super.extractEntries().
	*/
	public Hashtable extractEntries(File f, SelectiveTarEntry [] entries, CancelProgressObserver observer)
		throws Exception
	{
		this.observer = observer;
		return extractEntries(f, entries);
	}
	

	/** Overridden to ask observer for cancel. */
	protected boolean canceled()	{
		return observer != null && observer.canceled();
	}
	
	protected void errorProgress(long size)	{
		if (observer != null)
			observer.progress(size);
	}
	
	
	/**
		Overrides copy method to do cancel and progress observation.
		Delegates to a CopyStream object.
	*/
	protected void copyInputStreamToOutputStream(
		InputStream in,
		long size,
		OutputStream out)
		throws IOException
	{
		new CopyStream(in, size, out, observer, false, false).copy();
	}
	
}