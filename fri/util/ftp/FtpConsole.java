package fri.util.ftp;

import java.io.*;
import fri.util.application.ConsoleApplication;
import fri.util.file.FileSize;
import fri.util.observer.CancelProgressObserver;

/**
	A console application that offers all literal FTP commands,
	and some convenience commands, see help() sourcecode:
	<pre>
		timeout seconds          ... sets the timeout seconds for control- and data-socket
		set ascii|bin|ebcidc     ... sets the transfer type to given type
		open someHost [somePort] ... opens previous or new host, with optional given port or FTP port 21
		pwd                      ... prints remote working directory
		lpwd                     ... prints local working directory
		ls                       ... lists names of remote working directory
		ls -l                    ... long list of files of remote working directory
		lls                      ... lists names of local working directory
		cd someDir               ... changes remote working directory
		lcd someDir              ... changes local working directory
		cdup                     ... change remote working directory to parent directory
		lcdup                    ... change local working directory to parent directory
		mkdir someDir            ... creates a remote directory with given name
		rename oldPath newPath   ... renames (moves) a remote directory or file
		rmdir someDir            ... deletes given remote directory (recursive)
		delete someFile          ... deletes given remote file
		cat someFile             ... types given remote file to stdout
		get someFile             ... download given remote file
		put someFile             ... uploads given local file
		getdir someDir           ... download given remote directory (recursive)
		putdir someDir           ... uploads given local directory (recursive)
	</pre>
	
	This class writes to System.out, whereas FtpClient prints to System.err.
	<p>
	Syntax: java fri.util.ftp.FtpConsole host [port [user password]]
	
	@author Fritz Ritzberger, 2003
*/

public class FtpConsole extends ConsoleApplication implements
	CancelProgressObserver
{
	private static final String version = "1.1";
	private ObservableFtpClient client;
	private int timeout = FtpClient.DEFAULT_TIMEOUT;	// 10 seconds
	private long transferSize, alreadyDone, prevReported;
	private String newHost;
	private int newPort;
	private String newUser;
	private byte [] newPassword;

	
	
	/** Open a FTP console application. Needs <i>start()</i> call to begin loop for commands. */
	public FtpConsole(String host, int port, String user, byte [] password)	{
		super("fri-FTP "+version+" by Fritz Ritzberger 2003", "ftp");	// pass app name and prompt
		
		newHost = host;
		newPort = port;
		newUser = user;
		newPassword = password;
	}



	// ConsoleApplication overridings start

	/** Called only once at start. Tries to connect to an host given on commandline. */
	protected void init()	{
		getOut().println("Timeout is: "+timeout+" seconds.");
		getOut().println("Transfer type is: binary");
		try	{ help(null); } catch (Exception e)	{}

		if (newHost != null && client == null)	{
			connect(newHost, newPort, newUser, newPassword);
		}
	}
	
	/** Override to deny the execution of an entered command. */
	protected boolean checkCommand(String cmd, String [] args)	{
		if (client == null &&
				(cmd.equals("set") ||
				cmd.startsWith("ls") ||
				cmd.equals("pwd") ||
				cmd.equals("cdup") ||
				cmd.equals("cd") ||
				cmd.equals("mkdir") ||
				cmd.equals("rename") ||
				cmd.equals("rmdir") ||
				cmd.equals("delete") ||
				cmd.equals("cat") ||
				cmd.equals("get") ||
				cmd.equals("put") ||
				cmd.equals("getdir") ||
				cmd.equals("putdir")))
		{
			getOut().println("Not connected! Type \"open host [port]\" to connect to a FTP site.");
			return false;
		}
		return true;
	}

	/** Any Throwable from commandline execution is passed to this method, which prints a stack trace. */
	protected void handleException(Throwable ex)	{
		if (ex instanceof ArrayIndexOutOfBoundsException)	{
			getOut().println("Wrong number of arguments? -> "+ex);
		}
		else
		if (ex instanceof FtpResponseException)	{
			if (client != null)
				getOut().println("Error: "+ex.getMessage()+". Last reply was: >"+client.getLastReply()+"<");
			else
				getOut().println(ex);
		}
		else	{
			getOut().println(ex);
			ex.printStackTrace();
		}
	}
	
	/** Called by "quit". Returns true if remote connection is active. */
	protected boolean needsCleanup()	{
		return client != null;
	}
	
	/** Called by "quit" when <i>needsCleanUp()</i> returned true. Disconnects remote connection. */
	protected void cleanup() throws Exception	{
		disconnect();
	}

	// ConsoleApplication overridings end



	/**
		Returns the current FTP client object, or null if not connected.
	*/
	public FtpClient getFtpClient()	{
		return client;
	}
	
	/**
		Sets the current FTP client object.
	*/
	public void setFtpClient(ObservableFtpClient client)	{
		this.client = client;
		
		newHost = client.getHost();
		newPort = client.getPort();
		newUser = client.getUser();
		newPassword = client.getPassword();
	}
	
	


	// callback implementations

	public void timeout(String [] args) throws Exception	{
		this.timeout = Integer.parseInt(args[0]);
		if (client != null)
			client.setTimeout(timeout);
		getOut().println("New timeout (in seconds): "+timeout);
	}

	public void open(String [] args) throws Exception	{
		if (args.length > 0)
			newHost = args[0];
		
		if (args.length > 1)
			newPort = Integer.parseInt(args[1]);
		
		getOut().print("User ("+newUser+"): ");
		String u = readLine();
		newUser = (u.length() <= 0) ? newUser : u;
		
		getOut().print("Password: ");
		newPassword = readLine().getBytes();
		
		connect(newHost, newPort, newUser, newPassword);
	}

	public void lls(String [] args) throws Exception	{
		String old = null;
		if (args.length > 0)	{
			old = workingDirectory;
			lcd(new String [] { args[0] });
		}

		File f = createFile(workingDirectory);
		String [] names = f.list();
		for (int i = 0; names != null && i < names.length; i++)
			getOut().println(names[i]);

		if (old != null)	{
			lcd(new String [] { old });
		}
	}

	public void lpwd(String [] args) throws Exception	{
		getOut().println(workingDirectory);
	}

	public void lcdup(String [] args) throws Exception	{
		String newDir = new File(workingDirectory).getParent();
		if (newDir != null)
			workingDirectory = newDir;
		getOut().println("Local working directory is now: "+workingDirectory);
	}

	public void lcd(String [] args) throws Exception	{
		if (args.length > 0 && args[0].equals(".."))	{	// go to parent
			lcdup(null);
		}
		else	{
			if (args.length > 0)	{	// go to specified directory
				File f = new File(args[0]);
				if (f.isDirectory() == false)	{
					getOut().println("Not a directory: "+args[0]);
					return;
				}
					
				if (f.isAbsolute())
					workingDirectory = args[0];
				else
					workingDirectory = createFile(args[0]).getAbsolutePath();
			}
			else	{	// go to home
				workingDirectory = System.getProperty("user.home");
			}
			getOut().println("Local working directory is now: "+workingDirectory);
		}
	}

	public void lcat(String [] args) throws Exception	{
		BufferedInputStream in = null;
		try	{
			in = new BufferedInputStream(new FileInputStream(createFile(args[0])));
			int cnt;
			byte [] b = new byte[1024];
			while ((cnt = in.read(b)) != -1)
				getOut().write(b, 0, cnt);
			getOut().println();
		}
		finally	{
			try	{ in.close(); }	catch (Exception e)	{}
		}
	}

	public void set(String [] args) throws Exception	{
		client.setTransferType(
				args[0].toLowerCase().equals("ascii") ? FtpCommand.ASCII_TYPE
				: args[0].toLowerCase().startsWith("bin") ? FtpCommand.BINARY_TYPE
				: args[0].toLowerCase().equals("ebcdic") ? FtpCommand.EBCDIC_TYPE
				: args[0]);	// unknown type, pass through
	}

	public void ls(String [] args) throws Exception	{
		String old = null;
		boolean longList = (args.length > 0 && args[0].equals("-l"));
		String dir =
				(longList == false && args.length > 0) ? args[0]
				: (longList == true && args.length > 1) ? args[1]
				: null;
		
		if (dir != null)	{
			old = client.pwd();
			client.chdir(dir);
		}
		
		if (longList)	{
			getOut().println(client.listFiles());
		}
		else	{
			String [] names = client.listNames();
			for (int i = 0; i < names.length; i++)
				getOut().println(names[i]);
		}
		
		if (old != null)
			client.chdir(old);
	}

	public void pwd(String [] args) throws Exception	{
		getOut().println(client.pwd());
	}

	public void cdup(String [] args) throws Exception	{
		client.chdirUp();
		getOut().println(client.pwd());
	}

	public void cd(String [] args) throws Exception	{
		if (args.length > 0 && args[0].equals(".."))
			client.chdirUp();
		else
		if (args.length > 0)
			client.chdir(args[0]);
		else
			client.chdir("/");

		getOut().println(client.pwd());
	}

	public void mkdir(String [] args) throws Exception	{
		client.mkdir(args[0]);
	}

	public void rename(String [] args) throws Exception	{
		client.renameTo(args[0], args[1]);
	}

	public void rmdir(String [] args) throws Exception	{
		client.deleteDirectory(args[0]);
	}
	
	public void delete(String [] args) throws Exception	{
		client.deleteFile(args[0]);
	}

	public void cat(String [] args) throws Exception	{
		getOut().println(client.cat(args[0]));
	}

	public void get(String [] args) throws Exception	{
		initTransferSize(client.length(args[0]));
		client.downloadFile(args[0], makeRelative(args[0]).getAbsolutePath());
	}

	public void put(String [] args) throws Exception	{
		File f = createFile(args[0]);
		initTransferSize(f.length());
		client.uploadFile(f.getAbsolutePath(), f.getName());
	}

	public void getdir(String [] args) throws Exception	{
		initTransferSize(client.getDownloadDirectorySize(args[0], true));
		client.downloadDirectory(args[0], makeRelative(args[0]).getAbsolutePath(), true);
	}

	public void putdir(String [] args) throws Exception	{
		File f = createFile(args[0]);
		initTransferSize(new FileSize(f).length());
		client.uploadDirectory(f.getAbsolutePath(), args[0], true);
	}

	private void initTransferSize(long size)	{
		System.err.println("Received transfer size: "+size);
		transferSize = size;
		alreadyDone = prevReported = 0L;
	}

	private File makeRelative(String remotePath)	{
		File f = new File(remotePath);
		if (f.isAbsolute() || remotePath.startsWith("/"))
			remotePath = remotePath.substring(remotePath.indexOf(File.separator + 1));
		return createFile(remotePath);
	}
	
	public void help(String [] args) throws Exception	{
		getOut().println("Implemented commands are:");
		getOut().println("	timeout seconds          ... set the timeout seconds for control- and data-socket");
		getOut().println("	set ascii|bin|ebcidc     ... set the transfer type to given type");
		getOut().println("	open someHost [somePort] ... open previous or new host, with optional given port or FTP port 21");
		getOut().println("	pwd                      ... print remote working directory");
		getOut().println("	lpwd                     ... print local working directory");
		getOut().println("	ls                       ... list names of remote working directory");
		getOut().println("	ls -l                    ... long list of files of remote working directory");
		getOut().println("	lls                      ... list names of local working directory");
		getOut().println("	cd someDir               ... change remote working directory");
		getOut().println("	lcd someDir              ... change local working directory");
		getOut().println("	cdup                     ... change remote working directory to parent directory");
		getOut().println("	lcdup                    ... change local working directory to parent directory");
		getOut().println("	mkdir someDir            ... create a remote directory with given name");
		getOut().println("	rename oldPath newPath   ... rename (moves) a remote directory or file");
		getOut().println("	rmdir someDir            ... delete given remote directory (recursive)");
		getOut().println("	delete someFile          ... delete given remote file");
		getOut().println("	cat someFile             ... type given remote file to stdout");
		getOut().println("	get someFile             ... download given remote file");
		getOut().println("	put someFile             ... upload given local file");
		getOut().println("	getdir someDir           ... download given remote directory (recursive)");
		getOut().println("	putdir someDir           ... upload given local directory (recursive)");
	}


	/** Passes the entered command directly to FTP protocol. */
	public void passthrough(String [] args) throws Exception	{
		getOut().println(client.execute(new FtpCommand(args[0]), args.length > 1 ? args[1] : null));
	}



	// utility methods

	private void connect(String host, int port, String user, byte [] password)	{
		disconnect();
		
		try	{
			client = new ObservableFtpClient(this, host, port, user, password, System.err);
			if (timeout > 0)
				client.setTimeout(timeout);
				
			client.connect();
			
			getOut().println("Connected to '"+host+"', server operating system is '"+client.system()+"'");
		}
		catch (Exception e)	{
			getOut().println(e);
			client = null;
		}
	}

	private void disconnect()	{
		if (client != null)	{
			try	{
				client.disconnect();
			}
			catch (Exception e)	{
				getOut().println("Disconnect error: "+e);
			}
			finally	{
				client = null;
			}
		}
	}
	
	private File createFile(String dir)	{
		return createFile(dir, null);
	}
	
	private File createFile(String dir, String name)	{
		String old = System.getProperty("user.dir");	// memorize current directory
		System.setProperty("user.dir", workingDirectory);	// set new working directory
		File f = (name == null) ? new File(dir) : new File(dir, name);	// create File with working directory
		f = new File(f.getAbsolutePath());	// make file absolute for new FileInputStream
		System.setProperty("user.dir", old);	// restore current directory
		return f;
	}




	// interface CancelProgressObserver

	/** Display the amount of data already transferred. */
	public void progress(long portion)	{
		if (portion > 0L)	{
			alreadyDone += portion;
			
			if (alreadyDone >= transferSize || alreadyDone - prevReported >= transferSize / 10L)	{	// finished, or another 10% done
				prevReported = alreadyDone;
				getOut().print("="+(alreadyDone * 100 / transferSize)+"%");
			}
		}
	}
	
	/** Returns true to cancel current action (connect or transfer). */
	public boolean canceled()	{
		return false;
	}

	/** Used when observed object changes. Does nothing here, as this would break progress display. */
	public void setNote(String note)	{
	}

	/** End the progress display. */
	public void endDialog()	{
		if (transferSize > 0L && alreadyDone >= transferSize)
			getOut().println();
	}




	
	
	/** FTP console application main. */
	public static void main(String [] args)	{
		String host = null;
		int port = -1;
		String user = null;
		byte [] password = null;

		if (args.length < 1)	{
			System.err.println("SYNTAX: "+FtpConsole.class.getName()+" host [port [user [password]]]");
			System.err.println("	The default FTP port is 21.");
		}
		else	{
			host = args[0];
			port = args.length > 1 ? Integer.parseInt(args[1]) : -1;
			user = args.length > 2 ? args[2] : null;
			password = args.length > 3 ? args[3].getBytes() : null;
		}
		
		new FtpConsole(host, port, user, password).start();
	}
	
}