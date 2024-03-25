package fri.util.ftp;

import java.io.*;
import java.net.*;
import fri.util.Equals;
import fri.util.file.DeleteFile;

/**
	FTP client that contains all needed methods to list directories and
	to upload and download files  and directories (optional recursive).
	This class uses FtpConnection to launch commands. It contains no
	FTP semantics like the sequence of two commands needed to rename a file.
	
	Test sites:<br>
		WINDOWS downloads.viaarena.com <br>
		VMS rf1.cuis.edu <br>
		UNIX members.chello.at <br>
	
	@author Fritz Ritzberger, 2003
*/

public class FtpClient implements
	Serializable,
	Cloneable
{
	/** The default FTP port (21) */
	public static final int DEFAULT_PORT = 21;
	/** The default username "anonymous". */
	public static final String DEFAULT_USERNAME = "anonymous";
	/** The default password "guest". */
	public static final byte [] DEFAULT_PASSWORD = "guest".getBytes();

	/** The default socket timeout (10 seconds) */
	public static final int DEFAULT_TIMEOUT = 10;
	
	private String user;
	private byte [] password;
	private String host;
	private int port;

	private String transferType = FtpCommand.BINARY_TYPE;
	private boolean activeFtp;	// defaults to false
	private int timeoutSeconds = DEFAULT_TIMEOUT;

	protected transient PrintStream log;
	protected transient FtpConnection connection;
	
	private String system;
	private Object lastReply;


	/**
		FTP client with default port 21, user "anonymous", on specified host, logging to System.err.
	 */
	public FtpClient(String host) {
		this(host, null);
	}

	/**
		FTP client with default port 21, user "anonymous", on specified host.
	 */
	public FtpClient(String host, PrintStream log) {
		this(host, -1, log);
	}

	/**
		FTP client user "anonymous", on specified host and port.
	 */
	public FtpClient(String host, int port, PrintStream log) {
		this(host, port, null, null, log);
	}

	/**
		FTP client with default port 21, on specified host with user and password, logging to System.err.
	 */
	public FtpClient(String host, String user, byte [] password) {
		this(host, user, password, null);
	}

	/**
		FTP client with default port 21, on specified host with user and password.
	 */
	public FtpClient(String host, String user, byte [] password, PrintStream log) {
		this(host, -1, user, password, log);
	}

	/**
		FTP client on specified host and port with user and password.
	 */
	public FtpClient(String host, int port, String user, byte [] password) {
		this(host, port, user, password, null);
	}

	/**
		FTP client on specified host and port with user and password.
	 */
	public FtpClient(String host, int port, String user, byte [] password, PrintStream log) {
		this.host = host;
		this.port = port > 0 ? port : DEFAULT_PORT;
		this.user = (user == null || user.length() <= 0) ? DEFAULT_USERNAME : user;
		this.password = (password == null || password.length <= 0) ? DEFAULT_PASSWORD : password;
		this.log = (log == null) ? System.err : log;
	}


	public String getHost()	{
		return host;
	}
	public int getPort()	{
		return port;
	}
	public String getUser()	{
		return user;
	}
	public byte [] getPassword()	{
		return password;
	}


	/** Returns an unconnected clone of this client. */
	public Object clone()	{
		return new FtpClient(host, port, user, password, log);
	}
	
	
	/** Set transfer type to ASCII (FtpCommand.ASCII_TYPE) or binary/IMAGE (FtpCommand.BINARY_TYPE). */
	public void setTransferType(String transferType)
		throws FtpResponseException, IOException
	{
		this.transferType = transferType;
		if (connection != null)
			connection.setTransferType(transferType);
	}

	/** Set timeout for control socket and (dynamically created) data socket. */
	public void setTimeout(int timeoutSeconds)
		throws SocketException
	{
		this.timeoutSeconds = timeoutSeconds;
		if (connection != null)
			connection.setTimeout(timeoutSeconds);
	}
	
	/** Sets the FTP transfer mode, true=active, false=passive. Active mode is rarely used, passive is default. */
	public void setActiveFtp(boolean activeFtp)	{
		this.activeFtp = activeFtp;
		if (connection != null)
			connection.setActiveFtp(activeFtp);
	}

	
	/** Returns the log stream of this client. */
	public PrintStream getLog()	{
		return log;
	}

	/** Returns true if connection is not null. */
	public boolean isConnected()	{
		return connection != null;
	}



	/**
	 * Opens the connection specified in constructor. It is not necessary to call this
	 * method explicitely, as every command execution ensures that the client is connected.
	 */
	public void connect()
		throws SocketException, UnknownHostException, IOException, FtpResponseException
	{
		if (connection == null)	{
			try	{
				connection = createFtpConnection(host, port, user, password, log, timeoutSeconds);
				setTransferType(transferType);
				setActiveFtp(activeFtp);
			}
			catch (IOException e)	{
				connection = null;
				throw e;
			}
		}
	}
	
	/** Override this to create a FtpConnection derivation. */
	protected FtpConnection createFtpConnection(String host, int port, String user, byte [] password, PrintStream log, int timeoutSeconds)
		throws SocketException, UnknownHostException, IOException, FtpResponseException
	{
		return new FtpConnection(host, port, user, password, log, timeoutSeconds);
	}
	
	protected FtpConnection ensureConnection()
		throws SocketException, UnknownHostException, IOException, FtpResponseException
	{
		if (connection == null)
			connect();
		return connection;
	}


	/**
	 * Disconnects this client from the FTP server.
	 */
	public void disconnect()
		throws IOException, FtpResponseException
	{
		if (connection != null)	{
			try	{
				execute(FtpCommand.QUIT, null);
			}
			finally	{
				connection.close();
				connection = null;
				system = null;
			}
		}
	}



	/** Execute an arbitrary subclass (or static member) of FtpCommand with given argument. This is the "raw" way to FTP. */
	public Object execute(FtpCommand command, Object arg)
		throws FtpResponseException, IOException
	{
		try	{
			Object o = ensureConnection().execute(command, arg);
			if (o != null)
				lastReply = o;
			return o;
		}
		catch (IOException e)	{
			lastReply = e.getMessage();
			
			if (e instanceof SocketException && e.getMessage().equals("Broken pipe"))	{
				connection = null;

				if (log != null)
					log.println("Server seems to have disconnected ...");
			}

			throw e;
		}
	}
	
	/** Returns the last returned object FtpConnection. In most cases this will be a String. */
	public Object getLastReply()	{
		return lastReply;
	}
	
	
	/**
	 * Sends a NOOP to the server to see if the connection is valid.
	 */
	public void ping()
		throws IOException, FtpResponseException
	{
		execute(FtpCommand.NOOP, null);
	}

	/**
	 * Returns true if the passed path exists on the FTP server as file or directory (FTP command is "STAT").
	 */
	public boolean exists(String fileOrDir)
		throws IOException, FtpResponseException
	{
		try	{
			Object o = execute(FtpCommand.STATUS, fileOrDir);

			String s = o.toString();
			if (fileOrDir.startsWith("End") == false && s.startsWith("End"))
				return false;	// VMS answers positively with "End list"

			return true;
		}
		catch (FtpResponseException e)	{
			if (e.response.isActionNotTaken())
				return false;
			else
				throw e;
		}
	}

	/**
	 * Changes the current FTP server directory to the specified (absolute or relative) path.
	 */
	public void chdir(String dir)
		throws IOException, FtpResponseException
	{
		execute(FtpCommand.CHDIR, dir);
	}

	/**
	 * Renames the specified file on the FTP server with the specified name.
	 */
	public void renameTo(String oldRemoteName, String newRemoteName)
		throws IOException, FtpResponseException
	{
		ensureConnection().renameTo(oldRemoteName, newRemoteName);
	}

	/**
	 * Creates a directory on the FTP server with the specified name.
	 */
	public void mkdir(String remoteDir)
		throws IOException, FtpResponseException
	{
		execute(FtpCommand.MKDIR, remoteDir);
	}



	/**
		Returns an array of filenames in current FTP directory.
	*/
	public String [] listNames()
		throws IOException, FtpResponseException
	{
		return listNames(null);
	}
	
	/**
		Returns an array of filenames in passed FTP directory.
	*/
	public String [] listNames(String ftpDirectory)
		throws IOException, FtpResponseException
	{
		return ensureConnection().listNames(ftpDirectory);
	}
	
	/**
		Returns a newline separated long (server OS specific) listing of filenames in current FTP directory.
	*/
	public String listFiles()
		throws IOException, FtpResponseException
	{
		String [] files = ensureConnection().listFiles(null);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < files.length; i++)	{
			sb.append(files[i]);
			sb.append('\n');
		}
		return sb.toString();
	}


	/**
	 * Changes the current FTP server directory up one level to its parent directory.
	 */
	public void chdirUp()
		throws IOException, FtpResponseException
	{
		execute(FtpCommand.CHDIR_UP, null);
	}

	/**
	 * Returns the current working directory on the FTP server.
	 */
	public String pwd()
		throws IOException, FtpResponseException
	{
		Object o = execute(FtpCommand.WORKINGDIR, null);
		return o.toString();
	}

	/**
	 * Returns the operating system name of the FTP server.
	 */
	public String system()
		throws IOException, FtpResponseException
	{
		if (system != null)
			return system;
		Object o = execute(FtpCommand.SYSTEM, null);
		return system = o.toString();
	}

	/**
	 * Returns the last modification time of the specified file on the FTP server.
	 */
	public String lastModified(String remoteFile)
		throws IOException, FtpResponseException
	{
		Object o = execute(FtpCommand.FILE_TIME, remoteFile);
		return o.toString();
	}

	/**
	 * Returns the size of the specified file on the FTP server.
	 */
	public long length(String remoteFile)
		throws IOException, FtpResponseException
	{
	  try  {
    		Object o = execute(FtpCommand.FILE_SIZE, remoteFile);
    		return toLong(o);
	  }
	  catch (FtpResponseException e) {
      // fri_2014-02-17: when selecting styles.css, following occurs:
      //   fri.util.ftp.FtpResponseException: Sent command >size /style.css<, received reply >550 /style.css: Operation not permitted<
	    // but as everything seems to work even without size, catch exception
      String msg = e.getMessage();
	    if (e.response.code != 550 || msg == null || msg.indexOf("Operation not permitted") < 0)
	      throw e;
	    
	    System.err.println("SIZE command caused error: "+e);
	    return -1L;
	  }
	}

	private long toLong(Object o)	{
		try {
			return Long.parseLong(o.toString());
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
			return -1L;
		}
	}


	/**
	 * Downloads the specified file from the FTP server and returns it as String.
	 */
	public String cat(String remoteFile)
		throws IOException, FtpResponseException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ensureConnection().download(remoteFile, out);
		return out.toString();
	}


	/**
	 * Returns true if the passed name is a remote <b>directory</b>.
	 * This test is done by <i>chdir(remote)</i>, if it fails,
	 * it is identified as directory. This is time consuming!
	 */
	public boolean isDirectory(String remote)
		throws IOException, FtpResponseException
	{
		String old = pwd();
		try {
			chdir(remote);	// will fail if file
			chdir(old);
			return true;
		}
		catch (FtpResponseException e) {
			if (e.response.isActionNotTaken()) {
				return false;
			}
			else {
				throw e;
			}
		}
	}



	/**
	 * Deletes the specified file from the FTP server.
	 */
	public void deleteFile(String remoteFile)
		throws IOException, FtpResponseException
	{
		execute(FtpCommand.DELETE_FILE, remoteFile);
	}

	/**
	 * Removes the specified directory (including its contents and
	 * subdirectories) from the FTP server.
	 */
	public void deleteDirectory(String remoteDir)
		throws IOException, FtpResponseException
	{
		String old = pwd();
		chdir(remoteDir);	// if this is a file, exception would be thrown
		cleanDir();
		chdir(old);
		
		execute(FtpCommand.DELETE_DIR, remoteDir);
	}

	private void cleanDir()
		throws IOException, FtpResponseException
	{
		String [] names = listNames();
		
		for (int i = 0; i < names.length; i++) {
			if (FtpStringUtil.isValidFileName(names[i]))	{
				try {
					deleteDirectory(names[i]);
				}
				catch (FtpResponseException e) {
					if (e.response.isActionNotTaken()) {
						deleteFile(names[i]);
					}
					else {
						throw e;
					}
				}
			}
		}
	}



	/**
	 * Downloads the specified file from the FTP server to the current local working directory.
	 */
	public void downloadFile(String remoteFile)
		throws IOException, FtpResponseException
	{
		downloadFile(remoteFile, remoteFile);
	}

	/**
	 * Downloads the specified file from the FTP server to the specified local path.
	 */
	public void downloadFile(String remoteFile, String localFile)
		throws IOException, FtpResponseException
	{
		downloadFile(remoteFile, localFile, null);
	}
	
	/** This method is called for every file downloaded, even when downloading a directory. */
	protected void downloadFile(String remoteFile, String localFile, Object dummy)
		throws IOException, FtpResponseException
	{
		log.println("Downloading remote file "+remoteFile+" to local file "+localFile);
		
		try	{
			ensureConnection().download(remoteFile, new FileOutputStream(localFile));
		}
		catch (IOException e)	{
			new File(localFile).delete();	// was a directory, or not downloaded correctly
			throw e;
		}
	}



	/**
	 * Downloads the specified directory from the FTP server to the specified local path.
	 */
	public void downloadDirectory(String remoteDir, String localDir, boolean recursive)
		throws IOException, FtpResponseException
	{
		log.println("Downloading remote directory "+remoteDir+" to local directory "+localDir);

		String old = pwd();
		chdir(remoteDir);

		File toCreate = new File(localDir);
		boolean didNotExist = toCreate.exists();
		
		try	{
			downloadDirectory(localDir, recursive);
		}
		catch (IOException e)	{
			if (didNotExist && toCreate.exists())
				new DeleteFile(toCreate);
			throw e;
		}
		finally	{
			try	{ chdir(old); }	catch (Exception e)	{}
		}
	}

	/**
	 * Downloads the current working directory from the FTP server to the specified local path.
	 */
	private void downloadDirectory(String localDir, boolean recursive)
		throws IOException, FtpResponseException
	{
		File ld = new File(localDir);
		if (ld.isFile() || ld.exists() == false && ld.mkdirs() == false)	{
			throw new IOException("Could not create directory >"+localDir+"<");
		}
		
		String[] names = listNames();
		String old = pwd();
		
		for (int i = 0; i < names.length; i++) {
			if (FtpStringUtil.isValidFileName(names[i])) {
				String subPath = localDir + File.separator + names[i];
				
				try {
					chdir(names[i]);	// will fail if file
					
					if (recursive) {
						downloadDirectory(subPath, recursive);
					}
					
					chdir(old);
				}
				catch (FtpResponseException e) {
					if (e.response.isActionNotTaken()) {
						downloadFile(names[i], subPath, null);
					}
					else {
						throw e;
					}
				}
			}
		}
	}





	/**
	 * Uploads the specified file from the local machine to the current working directory of the FTP server.
	 */
	public void uploadFile(String localFile)
		throws IOException, FtpResponseException
	{
		uploadFile(localFile, localFile);
	}

	/**
	 * Uploads the specified file from the local machine to the specified path of the FTP server.
	 */
	public void uploadFile(String localFile, String remoteFile)
		throws IOException, FtpResponseException
	{
		uploadFile(localFile, remoteFile, null);
	}

	protected void uploadFile(String localFile, String remoteFile, Object dummy)
		throws IOException, FtpResponseException
	{
		log.println("Uploading local file "+localFile+" to remote file "+remoteFile);
		ensureConnection().upload(remoteFile, new FileInputStream(localFile));	// FileInputStream throws exception if not existing
	}



	/**
	 * Uploads the specified directory recursively from the local machine to the FTP server.
	 */
	public void uploadDirectory(String localDir)
		throws IOException, FtpResponseException
	{
		uploadDirectory(localDir, localDir, true, null);
	}

	/**
	 * Uploads the specified directory from the local machine to the specified path of the FTP server.
	 */
	public void uploadDirectory(String localDir, String remoteDir, boolean recursive)
		throws IOException, FtpResponseException
	{
		log.println("Uploading local directory "+localDir+" to remote directory "+remoteDir);
		uploadDirectory(localDir, remoteDir, recursive, null);
	}

	/**
	 * Uploads files matching the specified filename filter in the specified
	 * directory from the local machine to the specified path of the FTP server.
	 */
	public void uploadDirectory(String localDir, String remoteDir, boolean recursive, FilenameFilter filenameFilter)
		throws IOException, FtpResponseException
	{
		String old = pwd();

		File directory = new File(localDir);
		if (directory.exists() == false || directory.isDirectory() == false)
			throw new IOException("Directory does not exist or is not a directory: "+directory);
		
		try {	// create remote path, ignore error if exists
			mkdir(remoteDir);
		}
		catch (FtpResponseException e) {
			if (e.response.isDirectoryExistsError() == false && e.response.isActionNotTaken() == false) {
				throw e;
			}
		}
		
		chdir(remoteDir);	// change to remote path
		
		File [] subFiles = directory.listFiles(filenameFilter);
		
		for (int i = 0; i < subFiles.length; i++) {
			String local = subFiles[i].getPath();
			String remote = subFiles[i].getName();
			
			if (subFiles[i].isDirectory() && recursive) {
				uploadDirectory(local, remote, recursive, filenameFilter);
			}
			else
			if (subFiles[i].isFile()) {
				uploadFile(local, remote, null);
			}
		}
		
		chdir(old);
	}



	/** Returns the input stream from passed remote file. */
	public InputStream getInputStream(String filePath)
		throws IOException
	{
		return connection.getInputStream(filePath);
	}

	/** Returns the output stream on passed remote file (does not create directories!). */
	public OutputStream getOutputStream(String filePath)
		throws IOException
	{
		return connection.getOutputStream(filePath);
	}


	/**
		Returns the FtpConnection this client holds.
		This method is protected for FtpClientToClient.
	*/
	FtpConnection getConnection()
		throws IOException, FtpResponseException
	{
		return ensureConnection();
	}



	public String toString()	{
		return "host="+host+", port="+port+", user="+user;
	}
	
	public int hashCode()	{
		return (host != null ? host.hashCode() : 0) + port + (user != null ? user.hashCode() : 0);
	}

	public boolean equals(Object o)	{
		if (o instanceof FtpClient == false)
			return false;
		FtpClient f = (FtpClient)o;
		return Equals.equals(f.host, host) && f.port == port && Equals.equals(f.user, user);
	}

}
