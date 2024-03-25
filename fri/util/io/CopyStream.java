package fri.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import fri.util.observer.CancelProgressObserver;
import fri.util.props.PropertyUtil;

/**
	Copy an input stream to an output stream.
	<UL>
		<LI>Target: designed to run interruptable in a background thread,
			telling about progress of copy.
		<LI>Behaviour: Tells written byte portions to a progress observer and
			checks after each portion for user cancel.
		<LI>Lifecycle: clients construct object, call copy(), and finally look at public error.
		<LI>Errors: If errors occured, the String error is set to non-null.
			In this case the target file is deleted.
			At first IOException the process is terminated.
	</UL>
	MIND: you must call copy() to get this class running!
	<p>
	The copy buffer size can be set by using <i>CopyStream.bufsize = ...</i>,
	it is initialized with the value of the system property "copyBufferSize":
	<pre>
		java -DcopyBufferSize=1048576 ... // 1 MB bufsize
	</pre>
	
	@author Fritz Ritzberger
*/
public class CopyStream
{
	public static final int ONE_MB = 1048576;
	public static int bufsize = PropertyUtil.getSystemInteger("copyBufferSize", ONE_MB);	// 1 MB copy block size
	private static byte [] buffer;

	public String error;
	protected CancelProgressObserver dialog;
	protected long size;
	protected InputStream in;
	protected OutputStream out;
	protected boolean doCloseIn, doCloseOut;


	/**
		Copy from InputStream to OutputStream, optionally up to a given size, close both streams at end.
		@param in stream to read from
		@param size size to copy from input stream, copy all when size is -1
		@param out stream to write to
		@param dlg observer that can canel the process
	*/
	public CopyStream(
		InputStream in,
		long size,
		OutputStream out,
		CancelProgressObserver dlg)
	{
		this(in, size, out, dlg, true);
	}

	/**
		Copy from InputStream to OutputStream, optionally up to a given size.
		@param in stream to read from
		@param size size to copy from input stream, copy all when size is -1
		@param out stream to write to
		@param dlg observer that can canel the process
		@param doCloseOut if false the output stream will not be closed when finished
	*/
	public CopyStream(
		InputStream in,
		long size,
		OutputStream out,
		CancelProgressObserver dlg,
		boolean doCloseOut)
	{
		this(in, size, out, dlg, doCloseOut, true);
	}

	/**
		Copy from InputStream to OutputStream, optionally up to a given size.
		@param in stream to read from
		@param size size to copy from input stream, copy all when size is -1
		@param out stream to write to
		@param dlg observer that can canel the process
		@param doCloseOut if false the output stream will not be closed when finished
		@param doCloseIn if false the input stream will not be closed when finished
	*/
	public CopyStream(
		InputStream in,
		long size,
		OutputStream out,
		CancelProgressObserver dlg,
		boolean doCloseOut,
		boolean doCloseIn)
	{
		this.dialog = dlg;
		this.size = size;
		this.out = out;
		this.in = in;
		this.doCloseOut = doCloseOut;
		this.doCloseIn = doCloseIn;
	}

	/** Need do-nothing constructor for subclasses. */
	protected CopyStream()	{
	}


	public void copy()
		throws IOException
	{
		canceled();
		
		int portion = CopyStream.bufsize;	// want to read as much as possible
		long todo = size;	// size to copy, can be -1 which means undefined
		int actual = -2;
		boolean doDelete = false;	// finally do not delete created file

		try	{
			while (actual != -1 && todo != 0L)	{	// while not EOF and todo is not zero
				if (todo > 0L && todo < portion)	// if todo is smaller than current read portion
					portion = (int)todo;	// set last read portion to smaller size

				if (actual == -2 && (buffer == null || buffer.length < portion))	// at start ensure buffer
					buffer = new byte[portion];
				
				actual = in.read(buffer, 0, portion);	// read current portion
				
				if (actual != -1)	{	// not at EOF, -1 is documented EOF return code
					canceled();	// check for cancel, throws Exception
					
					out.write(buffer, 0, actual);	// write read bytes to out
					
					if (todo > 0L)
						todo -= actual;	// calculate new todo

					canceled();	// check for cancel, throws Exception
					progress(actual);	// tell the dialog about done portion
				}
				else
				if (todo > 0L)	{	// EOF but still need bytes to reach given size
					error = "Got EOF but still got to do "+todo+" bytes of given size "+size;
					throw new IOException(error);
				}
			}
		}
		catch (IOException e)	{
			doDelete = true;	// finally do delete created file on error
			throw e;
		}
		finally	{
			//System.err.println("---- finally closing out "+doCloseOut+", closing in "+doCloseIn);
			if (doCloseIn)	{
				try	{ in.close(); } catch (Exception ex)	{}
			}

			if (doCloseOut)	{
				try	{ out.close(); } catch (Exception ex)	{}
			}

			if (doDelete)
				finalActionOnError();
		}
	}


	/** To be overridden by subclasses that must perform cleanup on error. */
	protected void finalActionOnError()	{
	}
	

	private void canceled()
		throws IOException
	{
		if (dialog != null && dialog.canceled())	{
			throw new IOException("User Canceled!");
		}
	}


	private void progress(int written)	{
		if (dialog != null)	{
			dialog.progress(written);
		}
	}


	/** Release the global copy buffer. It will be allocated newly if necessary. */
	public static void releaseBuffer()	{
		buffer = null;
	}

}
