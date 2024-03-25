package fri.util.ftp;

import java.io.PrintStream;
import fri.util.managers.InstanceManager;
import fri.util.observer.CancelProgressObserver;

/**
	Manages observable FTP clients for usage in different GUI's.
*/

public abstract class FtpClientFactory
{
	private static InstanceManager ftpClientCache = new InstanceManager();
	

	public static ObservableFtpClient getFtpClient(CancelProgressObserver observer, String host, int port, String user, byte [] password, PrintStream log)	{
		ObservableFtpClient search = new ObservableFtpClient(observer, host, port, user, password, log);
		return (ObservableFtpClient)ftpClientCache.getInstance(search);
	}
	
	/** Releases the reference to passed client and disconnects it if this call was the last usage of the FtpClient. */
	public static void freeFtpClient(FtpClient ftpClient)	{
		if (ftpClientCache.freeInstance(ftpClient) != null)	{	// was the last reference to client
			try	{ ftpClient.disconnect(); }	catch (Exception e)	{}	// disconnect is safe
		}
	}

	
	private FtpClientFactory()	{}

}