package fri.gui.swing.ftpbrowser;

import fri.gui.swing.application.*;
import fri.util.ftp.FtpConsole;	// bind commandline utility to this application

/**
	The FTP application frame.
*/

public class FtpFrame extends GuiApplication
{
	private FtpPanel panel;
	private FtpConsole ftp;	// variable used to bind console application FtpConsole to this package
	private static final String version = "1.2";
	

	public FtpFrame()	{
		this(null, -1, null, null);
	}
	
	public FtpFrame(String theHost, int thePort, String theUser, String thePassword)	{
		super("FTP Client "+version+" (File Transfer Protocol)");
		
		panel = new FtpPanel(theHost, thePort, theUser, thePassword);
		getContentPane().add(panel);

		init();
	}

	
	public boolean close()	{
		panel.close();
		return super.close();
	}


	/** FTP browser application main. */
	public static void main(String [] args)	{
		String host = null;
		int port = -1;
		String user = null;
		String password = null;

		if (args.length < 1)	{
			System.err.println("SYNTAX: "+FtpFrame.class.getName()+" host [port [user password]]");
			System.err.println("	The default FTP port is 21.");
		}
		else	{
			host = args[0];
			port = args.length > 1 ? Integer.parseInt(args[1]) : -1;
			user = args.length > 2 ? args[2] : null;
			password = args.length > 3 ? args[3] : null;
		}
		
		new FtpFrame(host, port, user, password);
	}
		
}
