package fri.gui.swing.mailbrowser;

import javax.mail.URLName;
import fri.util.application.Application;
import fri.util.mail.LocalStore;
import fri.util.mail.MailConsole;

/**
	Frame application for mail client. Ensures that there is a local store and
	sets the first selection to INBOX by calling <i>folderController.setFirstSelection()</i>.
*/

public class MailFrame extends CommandMapAwareFrame
{
	public static final String version = "1.4";
	private MainPanel panel;
	private MailConsole console;	// this is to bind the class MailConsole on JAR packing
	
	public MailFrame()	{
		this(null, null, null, null);
	}
	
	public MailFrame(String sendHost, String receiveHost, String user, String password)	{
		super(Language.get("Mail_Client")+" "+version);
		
		// ensure there is a local store
		if (ensureLocalStore() == false)	{
			Application.closeExit(this);
			return;	// can not live without local store
		}
		
		// build the mail client panel with commandline arguments
		panel = new MainPanel(sendHost, receiveHost, user, password);
		getContentPane().add(panel);

		// show the frame
		init();

		// set selection to inbox
		panel.setFirstSelection();
	}

	private boolean ensureLocalStore()	{
		if (LocalStore.exists() == false)	{
			URLName urlName = new URLName(LocalStore.getUrl());
			String path = urlName.getFile();

			LocalStoreDialog dlg = new LocalStoreDialog(this, path);	// show dialog
			path = dlg.getChosenPath();

			if (path == null || path.length() <= 0)
				return false;	// can not live without local store

			LocalStore.setUrl(LocalStore.LOCALSTORE_PROTOCOL+":"+path);
		}
		return true;
	}
	

	public boolean close()	{
		if (panel != null)
			panel.close();
		return super.close();
	}


	/** Mail browser application main. */
	public static void main(String [] args)	{
		String sendhost = null;
		String receivehost = null;
		String user = null;
		String password = null;

		if (args.length < 1)	{
			System.err.println("SYNTAX: "+MailFrame.class.getName()+" [sendHost[:port] [receiveHost[:port] [user password]]]]");
		}
		else	{
			sendhost = args[0];
			receivehost = args.length > 1 ? args[1] : null;
			user = args.length > 2 ? args[2] : null;
			password = args.length > 3 ? args[3] : null;
		}
		
		new MailFrame(sendhost, receivehost, user, password);
	}

}
