package fri.util.ftp;

import java.io.*;
import java.net.SocketException;
import java.net.UnknownHostException;
import fri.util.observer.CancelProgressObservable;
import fri.util.observer.CancelProgressObserver;

/**
	FTP client that can be monitored/canceled (transfers, connect) by a listener.
	
	@author Fritz Ritzberger, 2003
*/

public class ObservableFtpClient extends FtpClient implements
	CancelProgressObservable
{
	private CancelProgressObserver lsnr;

	/**
		FTP client on specified host and port with user and password.
	 */
	public ObservableFtpClient(CancelProgressObserver lsnr, String host, int port, String user, byte [] password, PrintStream log) {
		super(host, port, user, password, log);	// does not yet create the connection!
		this.lsnr = lsnr;
	}

	
	/** Overridden to create a ObservableFtpConnection. */
	protected FtpConnection createFtpConnection(String host, int port, String user, byte [] password, PrintStream log, int timeoutSeconds)
		throws SocketException, UnknownHostException, IOException, FtpResponseException
	{
		return new ObservableFtpConnection(host, port, user, password, log, timeoutSeconds, lsnr);
	}
	
	
	/** Implements CancelProgressObservable: Sets a CancelProgressObserver to the FTP connection. */
	public void setObserver(CancelProgressObserver lsnr)	{
		this.lsnr = lsnr;
		
		if (connection != null)
			((ObservableFtpConnection)connection).setObserver(lsnr);
	}


	/**
		Retrieve the size of a directory (optional recursive) that is about to be downloaded.
	 */
	public long getDownloadDirectorySize(String remoteDir, boolean recursive)
		throws IOException, FtpResponseException
	{
		String old = pwd();
		chdir(remoteDir);	// will fail if file
		long sum = getDownloadDirectorySize(recursive);
		chdir(old);
		log.println("Download directory size is "+sum+", directory "+remoteDir);
		return sum;
	}

	private long getDownloadSize(String remoteFileOrDir, boolean recursive)
		throws IOException, FtpResponseException
	{
		long sum = 0L;
		String old = pwd();
		
		try {
			chdir(remoteFileOrDir);	// will fail if file
			
			if (recursive) {
				sum = getDownloadDirectorySize(recursive);
			}
			
			chdir(old);
		}
		catch (FtpResponseException e) {
			if (e.response.isActionNotTaken()) {
				sum = length(remoteFileOrDir);
			}
			else {
				throw e;
			}
		}
		
		return sum;
	}
	
	private long getDownloadDirectorySize(boolean recursive)
		throws IOException, FtpResponseException
	{
		long sum = 0L;
		String[] names = listNames();
		
		for (int i = 0; i < names.length; i++) {
			if (lsnr != null && lsnr.canceled())
				throw new IOException("Directory size counting canceled!");

			if (FtpStringUtil.isValidFileName(names[i])) {
				sum += getDownloadSize(names[i], recursive);
			}
		}
		
		return sum;
	}


	/** Returns an unconnected clone of this client. */
	public Object clone()	{
		return new ObservableFtpClient(lsnr, getHost(), getPort(), getUser(), getPassword(), getLog());
	}


	/**
	 * This overriding ensures that no progress is written during download of file,
	 * as client might not have requested the size.
	 */
	public String cat(String remoteFile)
		throws IOException, FtpResponseException
	{
		((ObservableFtpConnection)ensureConnection()).setShowProgress(false);

		String s = null;
		try	{
			s = super.cat(remoteFile);
		}
		finally	{
			((ObservableFtpConnection)ensureConnection()).setShowProgress(true);
		}

		return s;
	}


	/**
	 * Overridden to set an observer note by <i>observer.setNote(filename)</i>.
	 */
	public void deleteFile(String remoteFile)
		throws IOException, FtpResponseException
	{
		if (lsnr != null)
			lsnr.setNote(makeFileProgressNote(remoteFile));
	
		super.deleteFile(remoteFile);
	}


	/** Overridden to set a note to dialog which file is about to be copied. */
	protected void downloadFile(String remoteFile, String localFile, Object dummy)
		throws IOException, FtpResponseException
	{
		if (lsnr != null)
			lsnr.setNote(makeFileProgressNote(remoteFile));

		super.downloadFile(remoteFile, localFile, dummy);
	}


	/** Overridden to set a note to dialog which file is about to be copied. */
	protected void uploadFile(String localFile, String remoteFile, Object dummy)
		throws IOException, FtpResponseException
	{
		if (lsnr != null)
			lsnr.setNote(makeFileProgressNote(localFile));
			
		super.uploadFile(localFile, remoteFile, dummy);
	}

	private String makeFileProgressNote(String filename)	{
		if (filename == null)
			return filename;
		
		// FTP shows a lot of different filenames ...
		int i = filename.lastIndexOf('/');
		if (i <= 0)
			i = filename.lastIndexOf('\\');
		if (i <= 0)
			i = filename.lastIndexOf(File.separatorChar);

		if (i <= 0 || i >= filename.length() - 1)	// separator not found or at end
			return filename;
		
		return filename.substring(i + 1)+" in "+filename.substring(0, i);
	}

}
