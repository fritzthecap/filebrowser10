package fri.util.ftp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import fri.util.NetUtil;

/**
	Holds control socket with in- and output streams to server,
	creates data socket dynamically when needed for data transfers.
	<p>
	This class provides FTP commands that transfer data,
	like STOR, RETR and LIST. It requires an In- or OutputStream
	as argument for that purpose.
	<p>
	This class wraps dependent commands like RNFR+RNTO (move file)
	and offers methods to execute them with one call.
	
	@author Fritz Ritzberger, 2003
*/

public class FtpConnection
{
	protected Socket socket;	// the FTP control connection
	protected PrintStream log;
	protected OutputStream out;
	protected BufferedReader in;
	protected int timeout;	// in milliseconds
	private String transferType;
	private boolean active;	// will be initially false
	

	/**
		Connects to the given FTP server and logs in with the passed user data.
		The socket connect blocks, but a good timeout will cancel that when the
		server blocks the connect in some way.
	*/
	public FtpConnection(
		String host,
		int port,
		String user,
		byte [] password,
		PrintStream log,
		int timeoutSeconds)
	throws
		IOException, UnknownHostException, FtpResponseException, SocketException
	{
		init(host, port, user, password, log, timeoutSeconds);
	}

	/** Need a no-arg constructor for overriding derivations. */
	protected FtpConnection()	{
	}
	

	/** Do all constructor work: allocate a Socket, connect, log in. To be overridden by ObservableFtpConnection. */
	protected void init(
		String host,
		int port,
		String user,
		byte [] password,
		PrintStream log,
		int timeoutSeconds)
	throws
		IOException, UnknownHostException, FtpResponseException, SocketException
	{
		this.log = log;
		
		connect(host, port, timeoutSeconds);	// connect, now we may be blocked ...
		setTimeout(timeoutSeconds);	// set the timeout before connecting
		
		// get communication streams when connected
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new BufferedOutputStream(socket.getOutputStream());

		// test if server is there
		try	{
			checkSuccess("(when connecting)");
		}
		catch (FtpResponseException e)	{
			close();
			throw e;
		}
		
		// log in
		try	{
			execute(FtpCommand.USERNAME, user);
			execute(FtpCommand.PASSWORD, password);
		}
		catch (FtpResponseException e)	{	// close connection if user or password was wrong
			execute(FtpCommand.QUIT, null);
			throw e;
		}
	}


	/** To be overridden when background connect (threaded) needed. */
	protected void connect(String host, int port, int timeoutSeconds)
		throws IOException, UnknownHostException
	{
		if (canTimeoutSocket())	{
			try	{
				// load socket compatible with JDK 1.3 (where no-arg constructor and connect() did not exist)
				Class socketCls = Class.forName("java.net.Socket");
				Constructor constr = socketCls.getConstructor(new Class[0]);
				socket = (Socket)constr.newInstance(new Object[0]);
				socket.setSoTimeout(timeoutSeconds * 1000);
				
				Class addrCls = Class.forName("java.net.InetSocketAddress");
				constr = addrCls.getConstructor(new Class[] { String.class, int.class });
				Object addr = constr.newInstance(new Object [] { host, new Integer(port) });
				Class socketAddrClass = Class.forName("java.net.SocketAddress");
				Method m = socketCls.getMethod("connect", new Class [] { socketAddrClass });
				m.invoke(socket, new Object [] { addr });
				
				// JDK 1.4 version of this is:
				// socket = new Socket();
				// socket.setSoTimeout(timeoutSeconds * 1000);
				// socket.connect(new InetSocketAddress(host, port));
			}
			catch (Throwable e)	{
				e.printStackTrace();
			}
		}
		else	{
			socket = new Socket(host, port);	// blocking connect
		}
	}

	protected boolean canTimeoutSocket()	{
		return System.getProperty("java.version").startsWith("1.4");
	}
	

	/** Sets the timeout for data transfer sockets. */
	public void setTimeout(int timeoutSeconds)
		throws SocketException
	{
		socket.setSoTimeout(this.timeout = timeoutSeconds * 1000);
	}

	/** Sets the passed transfer type (ASCII or binary) by calling the protocol. */
	public void setTransferType(String transferType)
		throws FtpResponseException, IOException
	{
		execute(FtpCommand.TRANSFER_TYPE, transferType);
		this.transferType = transferType;
	}

	/**
	 * Sets the FTP mode to active (true) or passive (false) data transfer
	 * (passive is default: more secure, good supported).
	 */
	public void setActiveFtp(boolean active)	{
	  this.active = active;
	}

	/** Returns true when currently active FTP mode, else false for passive default. */
	public boolean isActiveFtp()	{
	  return active;
	}


	/** Closes the connection. */
	public void close()
		throws IOException
	{
		in.close();
		out.close();
		socket.close();
	}


	/** The raw command interface for FTP clients. */
	public Object execute(FtpCommand command, Object arg)
		throws FtpResponseException, IOException
	{
		return command.execute(arg, in, out, log);
	}


	/** Renames the specified file on the FTP server with the specified name. */
	public void renameTo(String oldRemoteName, String newRemoteName)
		throws IOException, FtpResponseException
	{
		execute(FtpCommand.RENAME_FROM, oldRemoteName);
		execute(FtpCommand.RENAME_TO, newRemoteName);
	}


	/** Returns all names (file and folders) within current remote directory. */
	public String [] listNames(String remoteDirOrNull)
		throws FtpResponseException, IOException, UnknownHostException
	{
    try  {
      return list(FtpCommand.LIST_NAMES, remoteDirOrNull);
    }
    catch (FtpResponseException e) {
      // fri_2014-02-17: fri.util.ftp.FtpResponseException: Sent command >nlst<, received reply >450 /: No such file or directory<
      if (e.response.code != 450)
        throw e;
      
      String [] longListingLines = listFiles(remoteDirOrNull);
      return FtpStringUtil.getFileAndDirectoryNames(longListingLines);
    }
	}
	
	/** Returns a full (remote platform dependent) directory listing of the current remote directory. */
	public String [] listFiles(String remoteDirOrNull)
		throws FtpResponseException, IOException, UnknownHostException
	{
	  return list(FtpCommand.LIST_FILES, remoteDirOrNull);
	}
	
	private String [] list(FtpCommand cmd, String remoteDirOrNull)
		throws FtpResponseException, IOException, UnknownHostException
	{
		// RFC 959 says that ASCII type must be used for LIST commands
		if (transferType.equals(FtpCommand.BINARY_TYPE))
			execute(FtpCommand.TRANSFER_TYPE, FtpCommand.ASCII_TYPE);

		OutputStream out = new ByteArrayOutputStream();
		transfer(cmd, remoteDirOrNull, out);
		
		// reset current setting for transfers
		if (transferType.equals(FtpCommand.BINARY_TYPE))
			execute(FtpCommand.TRANSFER_TYPE, FtpCommand.BINARY_TYPE);
		
		StringTokenizer stok = new StringTokenizer(out.toString(), "\r\n");
		List names = new ArrayList(stok.countTokens());
		while (stok.hasMoreTokens())	{
			String name = stok.nextToken();
			if (FtpStringUtil.isValidFileName(name))
				names.add(name);
		}
		return (String []) names.toArray(new String[names.size()]);
	}
	
	
	/** Retrieves the passed file from FTP server to the passed output stream. */
	public void download(String remoteFile, OutputStream out)
		throws FtpResponseException, IOException
	{
		transfer(FtpCommand.RETRIEVE, remoteFile, out);
	}
	
	/** Uploads passed input stream to the passed remote file on FTP server. Does not create directories! */
	public void upload(String remoteFile, InputStream in)
		throws FtpResponseException, IOException
	{
		transfer(FtpCommand.STORE, remoteFile, in);
	}


	/**
		Transfers a remote or local file from or to FTP server.
		If stream is an <i>OutputStream</i>, this method works as download,
		if stream is an <i>InputStream</i>, this method works as upload.
		In both cases <i>fileName</i> is the remote name.
	*/
	private void transfer(FtpCommand command, String fileName, Object stream)
		throws FtpResponseException, IOException, UnknownHostException
	{
		if (stream == null)
			throw new IllegalArgumentException("Null stream not allowed: "+stream);
			
		InputStream inStream = null;
		OutputStream outStream = null;
		try	{
			if (stream instanceof OutputStream)	{	// is download
				outStream = (OutputStream) stream;
				inStream = new TransferInputStream(command, fileName);
			}
			else	{	// InputStream, is upload
				inStream = (InputStream) stream;
				outStream = new TransferOutputStream(command, fileName);
			}
			copy(inStream, outStream);	// work on streams
		}
		finally	{	// close streams
			try	{
				if (inStream != null)
					inStream.close();
			}
			finally	{
				if (outStream != null)
					outStream.close();
			}
		}
	}

	private DataSocket createDataSocket()
		throws FtpResponseException, IOException, UnknownHostException
	{
		if (active)
			return createActiveDataSocket();
		else
			return createPassiveDataSocket();
	}
	
	/*
		Execute a "PASV" command and receive a address/port from response.
		Construct a Socket and return it. Caller is responsible for closing
		socket and its streams.
	*/
	private DataSocket createPassiveDataSocket()
		throws FtpResponseException, IOException, UnknownHostException
	{
		Object o = execute(FtpCommand.PASSIVE, null);

		Object [] dataConnection = (Object []) o;
		String host = (String) dataConnection[0];
		int port = ((Integer) dataConnection[1]).intValue();

		Socket data = new Socket(host, port);
		data.setSoTimeout(timeout);
		return new DataSocket(data);
	}

	/*
		Create a socket and execute a "PORT" command with host and port.
		Return the accepted server socket with data socket. Caller is responsible
		for closing socket and its streams.
	*/
	private DataSocket createActiveDataSocket()
		throws FtpResponseException, IOException, UnknownHostException
	{
		ServerSocket dataServer = new ServerSocket(0);
		dataServer.setSoTimeout(timeout);
		String host = NetUtil.getLocalHostAddress();
		int port = dataServer.getLocalPort();

		String arg = FtpStringUtil.buildCommaSeparatedIPAddressAndPort(host, port); 
		execute(FtpCommand.PORT, arg);
		
		return new DataSocket(dataServer);
	}

	private void checkSuccess(String msgTag)
		throws FtpResponseException, IOException
	{
		FtpServerResponse sr = FtpServerResponse.getServerResponse(in, log);
		if (sr == null || sr.isPositiveComplete() == false)
			throw new FtpResponseException(msgTag, sr);
	}

	/**
		Copy an InputStream to an OutputStream.
		This method is protected to be overridden by observable connection,
		and for usage in FtpClientToClient.
	*/
	protected void copy(InputStream in, OutputStream out)
		throws IOException
	{
		byte b[] = new byte[1024];
		int cnt;
		
		while ((cnt = in.read(b)) != -1) {
			out.write(b, 0, cnt);
		}
	}


	/** Returns the input stream from passed remote file. */
	public InputStream getInputStream(String filePath)
		throws IOException
	{
		return new TransferInputStream(FtpCommand.RETRIEVE, filePath);
	}

	/** Returns the output stream on passed remote file. */
	public OutputStream getOutputStream(String filePath)
		throws IOException
	{
		return new TransferOutputStream(FtpCommand.STORE, filePath);
	}



	private class TransferInputStream extends InputStream
	{
		private DataSocket dataSocket;

		TransferInputStream(FtpCommand ftpCmd, String ftpArg)
			throws IOException
		{
			this.dataSocket = createDataSocket();
			execute(ftpCmd, ftpArg);
		}
		
		public void close() throws IOException, FtpResponseException	{
			dataSocket.close();
			checkSuccess("(when transferring data from input stream)");
		}
		public int available() throws IOException	{
			return dataSocket.getInputStream().available();
		}
		public int read() throws IOException	{
			return dataSocket.getInputStream().read();
		}
		public int read(byte[] b) throws IOException	{
			return dataSocket.getInputStream().read(b);
		}
		public int read(byte[] b, int off, int len) throws IOException	{
			return dataSocket.getInputStream().read(b, off, len);
		}
		public void reset() throws IOException	{
			dataSocket.getInputStream().reset();
		}
		public long skip(long n) throws IOException	{
			return dataSocket.getInputStream().skip(n);
		}
		public void mark(int readlimit)	{
			try	{ dataSocket.getInputStream().mark(readlimit); }	catch (Exception e)	{}
		}
		public boolean markSupported()	{
			try	{ return dataSocket.getInputStream().markSupported(); }	catch (Exception e)	{ return false; }
		}
	}


	private class TransferOutputStream extends OutputStream
	{
		private DataSocket dataSocket;

		TransferOutputStream(FtpCommand ftpCmd, String ftpArg)
			throws IOException
		{
			this.dataSocket = createDataSocket();
			execute(ftpCmd, ftpArg);
		}
		
		public void close() throws IOException, FtpResponseException	{
			dataSocket.close();
			checkSuccess("(when transferring data to output stream)");
		}
		public void flush() throws IOException	{
			dataSocket.getOutputStream().flush();
		}
		public void write(byte[] b) throws IOException	{
			dataSocket.getOutputStream().write(b);
		}
		public void write(byte[] b, int off, int len) throws IOException	{
			dataSocket.getOutputStream().write(b, off, len);
		}
		public void write(int b) throws IOException	{
			dataSocket.getOutputStream().write(b);
		}
	}

	
	/* Wraps active and passive FTP in/out streams in one union object to ensure their closing. */
	private static class DataSocket
	{
		private ServerSocket serverSocket;
		private Socket socket;
		private InputStream inStream;
		private OutputStream outStream;
	  
		/** The passive FTP constructor. */
		DataSocket(Socket socket)	{
			this.socket = socket;
		}
		/** The active FTP constructor. */
		DataSocket(ServerSocket serverSocket)	{
			this.serverSocket = serverSocket;
		}
		
		InputStream getInputStream()
			throws IOException
		{
			if (inStream == null)	{
				if (serverSocket != null)
					socket = serverSocket.accept();
				inStream = socket.getInputStream();
			}
			return inStream;
		}
		
		OutputStream getOutputStream()
			throws IOException
		{
			if (outStream == null)	{
				if (serverSocket != null)
					socket = serverSocket.accept();
				outStream = socket.getOutputStream();
			}
			return outStream;
		}
		
		void close()
			throws IOException
		{
			try	{
				if (inStream != null)
					inStream.close();
				
				if (outStream != null)
					outStream.close();
			}
			finally	{
				try	{
					socket.close();
				}
				finally	{
					if (serverSocket != null)
						serverSocket.close();
				}
			}
		}
	}

}
