package fri.util.ftp;

import java.io.*;
import java.net.*;
import fri.util.io.CopyStream;
import fri.util.observer.CancelProgressObserver;

/**
	FtpConnection that provides interruptable connect, and observable data transfer.
	The listener is responsible for calculating the sum of bytes the transfer will contain.
	
	@author Fritz Ritzberger, 2003
*/

public class ObservableFtpConnection extends FtpConnection implements
	Runnable	// to observe connect
{
	private CancelProgressObserver lsnr;
	private String host;
	private int port;
	private boolean connected = false;
	private boolean finished = false;
	private boolean showProgress = true;
	private Exception threadException;
	
	/**
		Connects to the given FTP server and logs in with the passed user data.
		The socket connect blocks, but a good timeout will cancel that when the
		server blocks the connect in some way.
	*/
	public ObservableFtpConnection(
		String host,
		int port,
		String user,
		byte [] password,
		PrintStream log,
		int timeoutSeconds,
		CancelProgressObserver lsnr)
	throws
		IOException, UnknownHostException, FtpResponseException, SocketException
	{
		this.lsnr = lsnr;	// store the listener
		super.init(host, port, user, password, log, timeoutSeconds);	// now connect
	}


	/*** Overridden to do nothing, as listener must be stored first, super.init() will be called after. */
	protected void init(String host, int port, String user, byte [] password, PrintStream log, int timeoutSeconds)
		throws IOException, UnknownHostException, FtpResponseException, SocketException
	{
	}


	/** Overridden to connect in an synchronized background thread. */
	protected void connect(String host, int port, int timeoutSeconds)
		throws IOException, UnknownHostException
	{
		if (lsnr == null || canTimeoutSocket())	{
			super.connect(host, port, timeoutSeconds);
		}
		else	{
			this.host = host;	// store to member variable for thread.run()
			this.port = port;
			this.timeout = timeoutSeconds * 1000;
			
			log.println("Connecting to host "+host+", port "+port+" ...");
			
			Thread thread = new Thread(this);	// we dont want to wait too long
			thread.start();
			
			int timeout = timeoutSeconds * 1000;
			int sum = 0;
			int interval = 500;	// millis
			
			synchronized(this)	{	// wait for thread to connect or fail
				while (finished == false && (lsnr == null || lsnr.canceled() == false) && sum < timeout)	{
					sum += interval;

					try	{ wait(interval); }
					catch (Exception e)	{ }

					if (lsnr != null)
						lsnr.progress(0L);
				}
			}

			if (lsnr != null)
				lsnr.endDialog();

			if (connected == false)
				if (lsnr != null && lsnr.canceled())
					throw new IOException("Not connected, user canceled");
				else
					throw new IOException("Not connected, error was: "+threadException);
		}
	}


	/** Implements Runnable to connect to server in background, interruptable by listener. */
	public void run()	{
		boolean b = false;
		
		try	{
			super.connect(host, port, timeout / 1000);
			b = true;
		}
		catch (Exception e)	{	// socket throws exception when timeout
			threadException = e;
		}
		
		synchronized(this)	{
			connected = b;
			finished = true;
			notify();
		}
	}



	/** Overridden to free listeners. */
	public void close()
		throws IOException
	{
		lsnr = null;
		super.close();
	}



	/**
		Copy an InputStream to an OutputStream.
		This method is protected to be overridden by observable connection,
		and for usage in FtpClientToClient.
	*/
	protected void copy(InputStream in, OutputStream out)
		throws IOException
	{
		if (showProgress && lsnr != null && lsnr.canceled())
			throw new IOException("User canceled FTP copy action");
			
		CopyStream.bufsize = 1024;	// to see some progress, as this is very slow
		new CopyStream(in, -1L, out, (showProgress ? lsnr : null), false, false).copy();	// close will be done in superclass
	}



	/** Overridden to NOT report progress when directory listing is transferred (as the size is not known). */
	public String [] listNames(String remoteDirOrNull)
		throws FtpResponseException, IOException, UnknownHostException
	{
		setShowProgress(false);
		String [] s = super.listNames(remoteDirOrNull);
		setShowProgress(true);
		return s;
	}
	
	/** Overridden to NOT report progress when directory listing is transferred (as the size is not known). */
	public String [] listFiles(String remoteDirOrNull)
		throws FtpResponseException, IOException, UnknownHostException
	{
		setShowProgress(false);
		String [] s = super.listFiles(remoteDirOrNull);
		setShowProgress(true);
		return s;
	}
	

	/** Sets if this connection should report progress or not. */
	protected void setShowProgress(boolean showProgress)	{
		this.showProgress = showProgress;
	}


	/** Sets the observer for this connection. */
	public void setObserver(CancelProgressObserver lsnr)	{
		this.lsnr = lsnr;
	}
	
}