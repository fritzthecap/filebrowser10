package fri.gui.swing.mailbrowser;

import java.awt.*;
import javax.swing.*;
import java.net.InetAddress;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import fri.gui.swing.ComponentUtil;

/**
	Authentication callback for mail session. Optionally a ready-made password
	can be provided for a session that already knows the user name and password.
	When a non-empty password was entered once, this dialog does not show anymore.
*/

public class AuthenticatorDialog extends Authenticator
{
	private Component parent;
	private String password, prevPassword;
	private boolean wasCanceled;

	
	public AuthenticatorDialog(Component parent)	{
		this(parent, null);
	}

	public AuthenticatorDialog(Component parent, String password)	{
		this.parent = parent;
		setPassword(password);
	}


	public void setPassword(String password)	{
		this.prevPassword = this.password;
		this.password = password;
	}
	
	public boolean wasCanceled()	{
		return wasCanceled;
	}


	protected PasswordAuthentication getPasswordAuthentication()	{
		if (password != null && password.length() > 0)	{
			System.err.println("returning prepared password authentication ...");
			return new PasswordAuthentication(
					getDefaultUserName(),
					this.password);
		}
		
		//Thread.dumpStack();
		
		// get all display info from session environment

		String prompt = getRequestingPrompt();
		if (prompt == null)
			prompt = Language.get("Login");
		
		String protocol = getRequestingProtocol();
		if (protocol == null)
			protocol = Language.get("Unknown_Protocol");
		
		String host;
		InetAddress inet = getRequestingSite();
		if (inet != null)
			host = inet.getHostName();
		else
			host = Language.get("Unknown_Host");
		
		String port = null;
		int portnum = getRequestingPort();
		if (portnum > -1)
			port = ""+portnum;
		
		String info = Language.get("Connecting_To")+" "+protocol+", Host "+host+(port != null ? ", Port "+port : "");
		
		
		// build the login panel

		JPanel p = new JPanel(new BorderLayout());
		JPanel left = new JPanel(new GridLayout(2, 1));
		JPanel right = new JPanel(new GridLayout(2, 1));
		p.add(left, BorderLayout.WEST);
		p.add(right, BorderLayout.CENTER);
		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(p, BorderLayout.NORTH);

		p.add(new JLabel(info), BorderLayout.NORTH);

		left.add(new JLabel(Language.get("User"), JLabel.TRAILING));
		String user = getDefaultUserName();
		JTextField username = new JTextField(user);
		right.add(username);

		left.add(new JLabel(Language.get("Password"), JLabel.TRAILING));
		JPasswordField password = new JPasswordField();
		right.add(password);
		if (prevPassword != null)
			password.setText(prevPassword);

		final JComponent focus = (user != null && user.length() > 0) ? password : username;
		EventQueue.invokeLater(new Runnable()	{	// request focus a lot later
			public void run()	{
				ComponentUtil.requestFocus(focus);
			}
		});
		
		
		wasCanceled = false;

		// display the login dialog
		int result = JOptionPane.showConfirmDialog(
				parent,
				p2,
				prompt,
				JOptionPane.OK_CANCEL_OPTION);
		
		if (result == JOptionPane.OK_OPTION)	{
			String pass = new String(password.getPassword());
			if (pass.length() > 0)
				this.password = pass;
				
			System.err.println("returning entered password authentication ...");
			return new PasswordAuthentication(username.getText(), pass);
		}
		else	{
			wasCanceled = true;
			return null;
		}
	}

}
