package fri.gui.swing.fileloader;

import java.io.*;
import javax.swing.JComponent;

/**
	Loading a file bytewise in a background thread.
	The resulting data structure is an array of bytes.
*/

public class ByteFileLoader extends GuiFileLoader
{
	private byte [] bytes;


	/**
		Create new FileLoader loading file bytes.
		@param file file to load from disk
		@param panel panel with BorderLayout where to add progressbar at SOUTH, nullable
		@param loadObserver listener which is interested in status of loading.
			At start setLoading(true) is called, at end setLoading(false). Can be null.
		@param waiter object to notify() when finished loading, nullable
	*/
	public ByteFileLoader(
		File file,
		byte [] bytes,
		JComponent panel,
		LoadObserver loadObserver,
		Object waiter)
	{
		super(file, 			panel,
			loadObserver,
			waiter);
		this.bytes = bytes;
	}


	/** Uses a FileInputStream to load the file (does NOT convert to UNICODE!). */
	protected void work(int len)
		throws Exception
	{
		if (len > 0)	{
			BufferedInputStream in = null;

			try	{
				in = new BufferedInputStream(new FileInputStream(file));
				int bsize = len > 8192 ? 8192 : len;
				int offs = 0;
				int cnt;
				
				while (false == interrupted && bsize > 0 && (cnt = in.read(bytes, offs, bsize)) != -1) {
					// report progress
					reportProgressAndError(null, cnt, null);	// call event thread
					
					offs += cnt;
					if (offs + bsize > len)
						bsize = len - offs;
				}
			}
			finally	{
				try	{ in.close(); }	catch (IOException e)	{}
			}
		}
	}

}