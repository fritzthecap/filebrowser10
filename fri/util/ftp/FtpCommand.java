package fri.util.ftp;

import java.io.*;

/**
	FTP command wrapper, holding command-specific pre-and post-processing.
	It wraps commands, their possible reply codes, and command-specific return Objects.
	FTPCommand does not bind dependent commands like RNFR and RNTO to each other, this
	is done in FtpClient.
	<p>
	A command is specified by a FTP word like "RETR" or "STOR", an optional
	runtime argument for that command and some negative or positive reply codes.
	Furthermor some Object could be created by the command, for instance the
	response message is always returned when there is no result of more importance.
	<p>
	This class uses FtpServerResponse, which can retrieve the reply code from
	response.
	
	@author Fritz Ritzberger, 2003
*/

public class FtpCommand
{
	/** The transfer type for a client retrieving ASCII. */
	public static final String ASCII_TYPE = "A";
	/** The transfer type for a client retrieving binary data. */
	public static final String BINARY_TYPE = "I";
	/** The transfer type for a client retrieving EBCDIC. */
	public static final String EBCDIC_TYPE = "E";

	/** The mode argument when switching to stream mode (is default). */
	public static final String STREAM_MODE = "S";
	/** The mode argument when switching to block mode. */
	public static final String BLOCK_MODE = "B";
	/** The mode argument when switching to compressed mode. */
	public static final String COMPRESSED_MODE = "C";

	//public static final FtpCommand ABORT = new FtpCommand("abor");
	public static final FtpCommand CHDIR = new FtpCommand("cwd");
	public static final FtpCommand DELETE_DIR = new FtpCommand("rmd");
	public static final FtpCommand DELETE_FILE = new FtpCommand("dele");
	public static final FtpCommand FILE_SIZE = new FtpCommand("size");
	public static final FtpCommand FILE_TIME = new FtpCommand("mdtm");
	public static final FtpCommand LIST_FILES = new FtpCommand("list");
	public static final FtpCommand LIST_NAMES = new FtpCommand("nlst");
	public static final FtpCommand MODE = new FtpCommand("mode");
	public static final FtpCommand MKDIR = new FtpCommand("mkd");
	public static final FtpCommand NOOP = new FtpCommand("noop");
	public static final FtpCommand PASSIVE = new FtpCommand("pasv");
	public static final FtpCommand PASSWORD = new FtpCommand("pass");
	public static final FtpCommand PORT = new FtpCommand("port");
	public static final FtpCommand WORKINGDIR = new FtpCommand("pwd");
	public static final FtpCommand QUIT = new FtpCommand("quit");
	public static final FtpCommand STATUS = new FtpCommand("stat");
	public static final FtpCommand SYSTEM = new FtpCommand("syst");
	public static final FtpCommand RETRIEVE = new FtpCommand("retr");
	public static final FtpCommand RENAME_FROM = new FtpCommand("rnfr");
	public static final FtpCommand RENAME_TO = new FtpCommand("rnto");
	public static final FtpCommand STORE = new FtpCommand("stor");
	public static final FtpCommand TRANSFER_TYPE = new FtpCommand("type");
	public static final FtpCommand CHDIR_UP = new FtpCommand("cdup");
	public static final FtpCommand USERNAME = new FtpCommand("user");

	protected final String cmd;
	
	
	/** Create a FtpCommand holding passed command name. Public to enable pass-through of runtime-generated commands. */
	public FtpCommand(String command)	{
		if (command == null)
			throw new IllegalArgumentException("FtpCommand can not be null: "+command);
		this.cmd = command;
	}


	/** Preliminary commands are commands that precede a down- or upload: STORE, RETRIEVE, LIST_FILES, LIST_NAMES. */
	protected boolean isPreliminaryCommand()	{
		return STORE.equals(cmd) || RETRIEVE.equals(cmd) || LIST_FILES.equals(cmd) || LIST_NAMES.equals(cmd);
	}

	/** Intermediate commands are commands that require a follower: USERNAME, RENAME_FROM. */
	protected boolean isIntermediateCommand()	{
		return USERNAME.equals(cmd) || RENAME_FROM.equals(cmd);
	}
	
	

	/**
		Convenience method that calls <i>execute(serverOut, serverIn, cmd, null)</i>.
	*/
	public Object execute(BufferedReader serverIn, OutputStream serverOut, PrintStream log)
		throws FtpResponseException, IOException
	{
		return execute(null, serverIn, serverOut, log);
	}
	
	/**
		Write a command and its (nullable) argument (separated by space ' ') to the server.
		Receive the response and return an Object made of it, throw a FtpException if the reply was erroneous.
		@return null or an command specific return Object. A null return
			does not indicate an error! In the case of PASSIVE_MODE the host and port is returned,
			CURRENT_DIRECTORY returns the directory without leading and trailing quotes,
			in all other cases the reply message (without the leading code) is returned.
	*/
	public Object execute(Object arg, BufferedReader serverIn, OutputStream serverOut, PrintStream log)
		throws FtpResponseException, IOException
	{
		// do command specific pre-processing
		preProcess(serverIn, serverOut, log);
		
		// process command
		String sentCmd = cmd+(arg != null ? " "+arg : "");
		if (log != null) log.println("FTP SEND >"+sentCmd+"<");
		
		serverOut.write(cmd.getBytes());
		
		if (arg != null)	{
			serverOut.write((byte)' ');	// write separator
			
			if (arg instanceof byte[])	// write password
				serverOut.write((byte[])arg);
			else
				serverOut.write(arg.toString().getBytes());	// write argument.toString()
		}
		
		serverOut.write("\r\n".getBytes());
		serverOut.flush();
		
		FtpServerResponse reply = FtpServerResponse.getServerResponse(serverIn, log);

		// do command specific error handling
		handleErrors(sentCmd, reply, log);

		// return command specific post-processing
		return postProcess(reply, log);
	}


	/**
		Default preprocessing does nothing. Override if needed.
	*/
	protected void preProcess(BufferedReader serverIn, OutputStream serverOut, PrintStream log)
		throws FtpResponseException, IOException
	{
	}


	/**
		Checks the response for its reply code and maps command words to allowed reply codes.
		Throws FtpResponseException if code is not allowed.
	*/
	protected void handleErrors(String sentCmd, FtpServerResponse reply, PrintStream log)
		throws FtpResponseException
	{
		if (isPreliminaryCommand())	{
			if (reply == null || reply.isPositivePreliminary() == false)
				throw new FtpResponseException(sentCmd, reply);
		}
		else
		if (isIntermediateCommand())	{
			if (reply == null || reply.isPositiveIntermediate() == false)
				throw new FtpResponseException(sentCmd, reply);
		}
		else
		if (reply == null || reply.isPositiveComplete() == false)	{
			throw new FtpResponseException(sentCmd, reply);
		}
	}


	
	/**
		Builds an optional return Object from the response, returns Object [] { inetaddress, port }
		for PASSIVE command, strips quotes from directory name for WORKINGDIR command, else returns
		the response message without the leading reply code.
	*/
	protected Object postProcess(FtpServerResponse reply, PrintStream log)	{
		Object ret = null;
		
		if (PASSIVE.equals(cmd))	{
			ret = FtpStringUtil.parseCommaSeparatedIPAddressAndPort(reply.getMessage());
		}
		else
		if (WORKINGDIR.equals(cmd))	{	// remove leading and trailing quotes
			String text = reply.getMessage().trim();
			ret = FtpStringUtil.getTextWithinQuotes(text);
		}
		else	{
			ret = reply.getMessage();
		}
		
		return ret;
	}


	
	/** Returns the FTP command word. */
	public String toString()	{
		return cmd;
	}
	
	/** Compares with this' command word. */
	public boolean equals(Object o)	{
		return cmd.equals(o);
	}

	/** Returns this command word's hashcode. */
	public int hashCode()	{
		return cmd.hashCode();
	}

}