package fri.gui.swing.mailbrowser;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import fri.util.mail.*;
import fri.gui.swing.spinnumberfield.*;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.util.LeftAlignPanel;

/**
	Present existing mail properties or edit new ones. This dialog
	works for either configuring receveive or send host only or for
	configuring both receive host and send host.
*/

public class ConfigureDialog
{
	private Component parent;

	private ReceiveProperties receiveProperties;
	private SendProperties sendProperties;

	private JComboBox receiveProtocol;
	private JTextField receiveHost;
	private WholeNumberField receivePort;
	private JTextField receiveUser;
	private JPasswordField receivePassword;
	private JCheckBox leaveMailsOnServer;
	private JCheckBox rememberReceivePassword;
	private JCheckBox checkForNewMail;
	private SpinNumberField checkNewMailMinutes;
	
	private JTextField sendHost;
	private WholeNumberField sendPort;
	private JTextField sendUser;
	private JLabel sendUserLabel;
	private JPasswordField sendPassword;
	private JLabel sendPasswordLabel;
	private JTextField fromAddress;
	private JTextField personalName;
	private boolean wasCanceled;
	private JCheckBox rememberSendPassword;

	private Color optionColor = new NormalLabel("").getForeground();


	public ConfigureDialog(Component parent, ReceiveProperties receiveProperties)	{
		this(parent, receiveProperties, null);
	}

	public ConfigureDialog(Component parent, SendProperties sendProperties)	{
		this(parent, null, sendProperties);
	}

	public ConfigureDialog(Component parent, ReceiveProperties receiveProperties, SendProperties sendProperties)	{
		this.parent = parent;
		this.receiveProperties = receiveProperties;
		this.sendProperties = sendProperties;

		if (receiveProperties == null && sendProperties == null)
			throw new IllegalArgumentException("Must have either send properties or receive properties!");

		start();
	}


	public ReceiveProperties getReceiveProperties()	{
		return receiveProperties;
	}
	
	public SendProperties getSendProperties()	{
		return sendProperties;
	}
	

	private void start()	{
		JPanel panel = build();	// build GUI
		init();	// set data into dialog

		// show modal dialog
		int result = JOptionPane.showConfirmDialog(
				parent,
				panel,
				Language.get("Mail_Configuration"),
				JOptionPane.OK_CANCEL_OPTION,
				-1);
		
		if (result == JOptionPane.OK_OPTION)	{	// retrieve result and put to properties
			commit();
		}
		else	{
			wasCanceled = true;
		}
	}


	public boolean wasCanceled()	{
		return wasCanceled;
	}
	

	private JPanel build()	{
		JPanel pReceive = (receiveProperties != null) ? buildReceivePropertiesPanel() : null;
		JPanel pSend = (sendProperties != null) ? buildSendPropertiesPanel() : null;

		// put together
		
		JPanel panel = null;
		if (pSend != null && pReceive != null)	{
			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.add(pReceive);
			panel.add(pSend);
		}
		else
		if (pSend != null)	{
			panel = pSend;
		}
		else	{	// receive panel
			panel = pReceive;
		}

		return panel;
	}


	private JPanel buildReceivePropertiesPanel()	{
		JPanel p1 = new JPanel(new BorderLayout());
		p1.setBorder(BorderFactory.createTitledBorder(Language.get("Receive_Connection")));
		p1.setToolTipText(Language.get("Receive_Connection_Parameters"));
		JPanel left = new JPanel(new GridLayout(6, 1));
		JPanel right = new JPanel(new GridLayout(6, 1));
		p1.add(left, BorderLayout.WEST);
		p1.add(right, BorderLayout.CENTER);
		JPanel pReceive = new JPanel(new BorderLayout());
		pReceive.add(p1, BorderLayout.NORTH);	// do not stretch textfields vertically
		
		left.add(new NormalLabel(Language.get("Protocol")));
		receiveProtocol = new JComboBox();
		receiveProtocol.setToolTipText(Language.get("Receive_Mail_Protocol"));
		receiveProtocol.addItem("pop3");
		receiveProtocol.addItem("imap");
		receiveProtocol.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				String s = receiveProtocol.getSelectedItem().toString();
				leaveMailsOnServer.setSelected(s.equals("imap"));
			}
		});
		leaveMailsOnServer = new JCheckBox(Language.get("Leave_Mails_On_Server"));
		leaveMailsOnServer.setToolTipText(Language.get("Optional")+": "+Language.get("Do_Not_Delete_Mails_From_Server"));
		leaveMailsOnServer.setForeground(optionColor);
		right.add(new LeftAlignPanel(new JComponent [] { receiveProtocol, leaveMailsOnServer }));

		left.add(new BoldLabel(Language.get("Host")));
		receiveHost = new JTextField("", 12);
		receiveHost.setToolTipText(Language.get("Required")+": "+Language.get("Name_Or_Internet_Address_Of_Receive__POP__IMAP__Host"));
		
		receivePort = new PortField();
		receivePort.setToolTipText(Language.get("Optional")+": "+Language.get("Port_Number_On_Receive_Host__default_for_POP_is_110_"));
		right.add(new LeftAlignPanel(new JComponent [] { receiveHost, new NormalLabel(Language.get("Port")), receivePort }));
		
		left.add(new BoldLabel(Language.get("User")));
		receiveUser = new JTextField("", 20);
		receiveUser.setToolTipText(Language.get("Required")+": "+Language.get("User_On_Receive_Mail_Host__Could_Be_Your_E_Mail_Address"));
		right.add(new LeftAlignPanel(receiveUser));

		left.add(new BoldLabel(Language.get("Password")));
		receivePassword = new JPasswordField("", 20);
		receivePassword.setToolTipText(Language.get("Required")+": "+Language.get("Password_On_Receive_Mail_Host"));
		right.add(new LeftAlignPanel(receivePassword));
		
		left.add(new JLabel(" "));
		rememberReceivePassword = new JCheckBox(Language.get("Remember_Password"));
		rememberReceivePassword.setForeground(optionColor);
		rememberReceivePassword.setBorder(null);
		right.add(new LeftAlignPanel(rememberReceivePassword));

		checkForNewMail = new JCheckBox(Language.get("Every"));
		checkForNewMail.setForeground(optionColor);
		checkForNewMail.setHorizontalAlignment(SwingConstants.RIGHT);
		checkForNewMail.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				boolean b = checkForNewMail.isSelected();
				checkNewMailMinutes.setEnabled(b);
			}
		});
		left.add(checkForNewMail);
		int interval = receiveProperties.getCheckForNewMailsInterval();
		checkNewMailMinutes = new MinuteField();
		checkNewMailMinutes.setEnabled(interval > 0);
		JLabel checkNewMailsLabel = new NormalLabel(Language.get("Receive_New_Mails"));
		right.add(new LeftAlignPanel(new JComponent [] { checkNewMailMinutes, checkNewMailsLabel }));

		if (receiveHost.getText().length() <= 0)
			ComponentUtil.requestFocus(receiveHost);
		else
		if (receiveUser.getText().length() <= 0)
			ComponentUtil.requestFocus(receiveUser);
		else
			ComponentUtil.requestFocus(receivePassword);

		return pReceive;
	}


	private JPanel buildSendPropertiesPanel()	{
		JPanel p1 = new JPanel(new BorderLayout());
		p1.setBorder(BorderFactory.createTitledBorder(Language.get("Send_Connection")));
		p1.setToolTipText(Language.get("Send_Connection_Parameters"));
		JPanel left = new JPanel(new GridLayout(8, 1));
		JPanel right = new JPanel(new GridLayout(8, 1));
		p1.add(left, BorderLayout.WEST);
		p1.add(right, BorderLayout.CENTER);
		JPanel pSend = new JPanel(new BorderLayout());
		pSend.add(p1, BorderLayout.NORTH);

		left.add(new BoldLabel(Language.get("E_Mail")));
		fromAddress = new JTextField("", 20);
		fromAddress.setToolTipText(Language.get("Required")+": "+Language.get("Your_Reply_E_Mail_Address___first_lastname_mailhost_"));
		right.add(new LeftAlignPanel(fromAddress));

		left.add(new NormalLabel(Language.get("Personal")));
		personalName = new JTextField("", 20);
		personalName.setToolTipText(Language.get("Optional")+": "+Language.get("Your_Personal_Name__To_Illustrate_Your_E_Mail_Address"));
		right.add(new LeftAlignPanel(personalName));

		left.add(new JLabel(" "));	// separator
		right.add(new JLabel(" "));

		left.add(new BoldLabel(Language.get("Host")));
		sendHost = new JTextField("", 12);
		sendHost.setToolTipText(Language.get("Required")+": "+Language.get("Name_Or_Internet_Address_Of_Send__SMTP__Host"));
		
		sendPort = new PortField();
		sendPort.setToolTipText(Language.get("Optional")+": "+Language.get("Port_Number_On_Send__SMTP__Host__default_is_25_"));

		right.add(new LeftAlignPanel(new JComponent [] { sendHost, new NormalLabel(Language.get("Port")), sendPort }));

		boolean needsAuth = sendProperties.getUser() != null;
		
		left.add(new JLabel(" "));
		final JCheckBox sendAuthOption = new JCheckBox(Language.get("SMTP_Authentication"), needsAuth);
		sendAuthOption.setToolTipText(Language.get("Optional")+": "+Language.get("Define_User_On_Send_Host"));
		sendAuthOption.setForeground(optionColor);
		right.add(sendAuthOption);
		sendAuthOption.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				boolean b = sendAuthOption.isSelected();
				sendUser.setEnabled(b);
				sendUserLabel.setEnabled(b);
				sendPassword.setEnabled(b);
				sendPasswordLabel.setEnabled(b);
				rememberSendPassword.setEnabled(b);
			}
		});
		
		sendUserLabel = new BoldLabel(Language.get("User"));
		sendUserLabel.setEnabled(needsAuth);
		left.add(sendUserLabel);
		sendUser = new JTextField("", 20);
		sendUser.setToolTipText(Language.get("User_On_Send_Host"));
		sendUser.setEnabled(needsAuth);
		right.add(new LeftAlignPanel(sendUser));

		sendPasswordLabel = new BoldLabel(Language.get("Password"));
		sendPasswordLabel.setEnabled(needsAuth);
		left.add(sendPasswordLabel);
		sendPassword = new JPasswordField("", 20);
		sendPassword.setToolTipText(Language.get("Password_On_Send_Host"));
		sendPassword.setEnabled(needsAuth);
		right.add(new LeftAlignPanel(sendPassword));
		
		left.add(new JLabel(" "));
		rememberSendPassword = new JCheckBox(Language.get("Remember_Password"));
		rememberSendPassword.setForeground(optionColor);
		rememberSendPassword.setEnabled(needsAuth);
		rememberSendPassword.setBorder(null);
		right.add(new LeftAlignPanel(rememberSendPassword));

		if (receiveProperties == null)	{
			if (fromAddress.getText().length() <= 0)
				ComponentUtil.requestFocus(fromAddress);
			else
			if (sendHost.getText().length() <= 0)
				ComponentUtil.requestFocus(sendHost);
			else
			if (needsAuth)
				if (sendUser.getText().length() <= 0)
					ComponentUtil.requestFocus(sendUser);
				else
					ComponentUtil.requestFocus(sendPassword);
		}

		return pSend;
	}


	private void init()	{
		if (receiveProperties != null)	{
			if (receiveProperties.getProtocol() != null)
				receiveProtocol.setSelectedItem(receiveProperties.getProtocol());
	
			String host = receiveProperties.getHost();
			receiveHost.setText(host != null ? host : "");
	
			String port = receiveProperties.getPort();
			if (port != null)
				receivePort.setValue(Integer.parseInt(port));
	
			String user = receiveProperties.getUser();
			receiveUser.setText(user != null ? user : "");
			
			leaveMailsOnServer.setSelected(receiveProperties.getLeaveMailsOnServer());

			rememberReceivePassword.setSelected(receiveProperties.getRememberPassword());
			
			if (receiveProperties.getPassword() != null)
				receivePassword.setText(receiveProperties.getPassword());
				
			int interval = receiveProperties.getCheckForNewMailsInterval();
			checkForNewMail.setSelected(interval > 0);
			if (interval > 0)
				checkNewMailMinutes.setValue(interval);
		}
		
		if (sendProperties != null)	{
			String from = sendProperties.getFrom();
			fromAddress.setText(from != null ? from : "");
	
			String personal = sendProperties.getPersonal();
			personalName.setText(personal != null ? personal : "");
	
			String host = sendProperties.getHost();
			sendHost.setText(host != null ? host : "");
	
			String port = sendProperties.getPort();
			if (port != null)
				sendPort.setValue(Integer.parseInt(port));
	
			String user = sendProperties.getUser();
			sendUser.setText(user != null ? user : "");

			rememberSendPassword.setSelected(sendProperties.getRememberPassword());
			
			if (sendProperties.getPassword() != null)
				sendPassword.setText(sendProperties.getPassword());
		}
	}


	private void commit()	{
		if (receiveProperties != null)	{
			receiveProperties.setProtocol((String)receiveProtocol.getSelectedItem());
			receiveProperties.setHost(receiveHost.getText());
			receiveProperties.setPort(receivePort.getValue() > 0 ? ""+receivePort.getValue() : "");
			receiveProperties.setUser(receiveUser.getText());
			receiveProperties.setRememberPassword(rememberReceivePassword.isSelected());
			receiveProperties.setPassword(new String(receivePassword.getPassword()));
			receiveProperties.setLeaveMailsOnServer(leaveMailsOnServer.isSelected());
			if (checkForNewMail.isSelected() && checkNewMailMinutes.getValue() > 0)
				receiveProperties.setCheckForNewMailsInterval((int)checkNewMailMinutes.getValue());
			else
				receiveProperties.setCheckForNewMailsInterval(-1);
		}

		if (sendProperties != null)	{
			sendProperties.setProtocol("smtp");
			sendProperties.setHost(sendHost.getText());
			sendProperties.setPort(sendPort.getValue() > 0 ? ""+sendPort.getValue() : "");
			sendProperties.setFrom(fromAddress.getText());
			sendProperties.setPersonal(personalName.getText());
			sendProperties.setUser(sendUser.getText());
			sendProperties.setRememberPassword(rememberSendPassword.isSelected());
			sendProperties.setPassword(new String(sendPassword.getPassword()));
		}
	}

	


	private class NormalLabel extends JLabel
	{
		NormalLabel(String text)	{
			super(text, JLabel.TRAILING);
			setForeground(Color.darkGray);
		}
	}

	private class BoldLabel extends NormalLabel
	{
		BoldLabel(String text)	{
			super(text);
			setFont(getFont().deriveFont(Font.BOLD));
			setForeground(Color.black);
		}
	}

	private class PortField extends WholeNumberField
	{
		PortField()	{
			super((short)4);	//  column count
		}

		public Dimension getMaximumSize()	{
			Dimension d = super.getMaximumSize();
			d.width = 20;
			return d;
		}
	}

	private class MinuteField extends SpinNumberField
	{
		MinuteField()	{
			super(1, 1, 3600, (short)4);
		}
		
		public Dimension getMaximumSize()	{
			Dimension d = super.getMaximumSize();
			d.width = 20;
			return d;
		}
	}
	

	/*
	public static void main(String [] args)
		throws Exception
	{
		//UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		ConfigureDialog d = new ConfigureDialog((Component)null, new ReceiveProperties(), new SendProperties());
	}
	*/
	
}
