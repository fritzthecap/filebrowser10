package fri.gui.swing.ftpbrowser;

import java.io.File;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import fri.util.ftp.FtpClient;
import fri.util.os.OS;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.splitpane.*;
import fri.gui.swing.spinnumberfield.*;
import fri.gui.swing.tree.TreeWaitCursorListener;
import fri.gui.swing.tree.CustomJTree;
import fri.gui.mvc.view.swing.PopupMouseListener;

/**
	The main panel, containing toolbars and splitpane.

	@author Fritz Ritzberger
*/

public class FtpPanel extends JPanel implements
	FocusListener
{
	private FtpController controller;


	public FtpPanel(String theHost, int thePort, String theUser, String thePassword)	{
		super(new BorderLayout());
		
		JToolBar tb = new JToolBar(JToolBar.HORIZONTAL);
		if (OS.isAboveJava13) tb.setRollover(true);
		JToolBar tb2 = new JToolBar(JToolBar.HORIZONTAL);
		if (OS.isAboveJava13) tb2.setRollover(true);
		
		HistCombo host = new HostCombo();
		if (theHost != null)
			host.setText(theHost);
		tb.add(new JLabel("FTP Host: "));
		tb.add(host);
		host.setToolTipText("Remote FTP Host Name Or Address");
		
		WholeNumberField port = new WholeNumberField((short)4)	{	//  column count
			public Dimension getMaximumSize()	{
				Dimension d = super.getMaximumSize();
				d.width = 20;
				return d;
			}
		};
		if (thePort > 0)
			port.setValue(thePort);
		else
			port.setValue(FtpClient.DEFAULT_PORT);
		tb.add(new JLabel(" Port: "));
		tb.add(port);
		port.setToolTipText("FTP Port Number (Defaults To "+FtpClient.DEFAULT_PORT+")");
		
		//tb.add(Box.createHorizontalGlue());
		
		HistCombo user = new UserCombo();
		if (theUser != null)
			user.setText(theUser);
		else
			if (user.getDataVector().size() <= 0)
				user.setText(FtpClient.DEFAULT_USERNAME);
		tb.add(new JLabel(" User: "));
		tb.add(user);
		user.setToolTipText("Username On Remote FTP Host (Defaults To \""+FtpClient.DEFAULT_USERNAME+"\")");

		JPasswordField password = new JPasswordField(6)	{
			public Dimension getMaximumSize()	{
				Dimension d = super.getMaximumSize();
				d.width = 60;
				return d;
			}
		};
		if (thePassword != null)
			password.setText(thePassword);
		else
			if (user.getDataVector().size() <= 0)
				password.setText(new String(FtpClient.DEFAULT_PASSWORD));
		tb.add(new JLabel(" Password: "));
		tb.add(password);
		password.setToolTipText("Password On Remote FTP Host (Defaults To \""+new String(FtpClient.DEFAULT_PASSWORD)+"\")");


		JTree fileView = new CustomJTree(TreeModelFactory.getFilesystemTreeModel())	{
			/** Do not let edit root. */
			public boolean isPathEditable(TreePath tp)	{
				return isEditable() && tp.getPathCount() > 2;
			}
		};
		fileView.setCellRenderer(new TreeCellRenderer());
		fileView.setEditable(true);
		fileView.addFocusListener(this);
		fileView.setRootVisible(false);
		fileView.setShowsRootHandles(true);
		fileView.setRowHeight(16);
		new TreeWaitCursorListener(fileView);
		
		JTree ftpServerView = new CustomJTree(TreeModelFactory.getFtpServerTreeModel(null))	{
			private DefaultTreeCellRenderer cellRenderer = new TreeCellRenderer();
			
			/** Do not let edit root. */
			public boolean isPathEditable(TreePath tp)	{
				return isEditable() && tp.getPathCount() > 1;
			}
			/** Restore cell renderer if model changes. */
			public void setModel(TreeModel m)	{
				super.setModel(m);
				setCellRenderer(cellRenderer);
			}
		};
		ftpServerView.setEditable(true);
		ftpServerView.addFocusListener(this);
		ftpServerView.setRootVisible(true);
		ftpServerView.setRowHeight(16);
		new TreeWaitCursorListener(ftpServerView);

		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(new JScrollPane(fileView));
		JLabel fileLabel;
		JTextField fileStatus;
		p1.add(fileLabel = new JLabel("Local Filesystem"), BorderLayout.NORTH);
		fileLabel.setMinimumSize(new Dimension());
		p1.add(fileStatus = new JTextField(" "), BorderLayout.SOUTH);
		fileStatus.setEditable(false);

		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(new JScrollPane(ftpServerView));
		JLabel ftpServerLabel;
		JTextField ftpServerStatus;
		p2.add(ftpServerLabel = new JLabel("Remote Filesystem (Unconnected)"), BorderLayout.NORTH);
		ftpServerLabel.setMinimumSize(new Dimension());
		p2.add(ftpServerStatus = new JTextField(" "), BorderLayout.SOUTH);
		ftpServerStatus.setEditable(false);


		LogTextArea logTextArea = new LogTextArea();
		
		
		SpinNumberField timeout = new SpinNumberField(FtpClient.DEFAULT_TIMEOUT, 0, Integer.MAX_VALUE, (short)4)	{
			public Dimension getMaximumSize()	{
				Dimension d = super.getMaximumSize();
				d.width = 30;
				return d;
			}
		};
		timeout.setValue(FtpClient.DEFAULT_TIMEOUT);
		timeout.setToolTipText("Seconds To Wait For Remote Response (Defaults To "+FtpClient.DEFAULT_TIMEOUT+")");
		tb2.add(new JLabel(" Timeout: "));
		tb2.add(timeout);
		tb2.addSeparator();
		
		controller = new FtpController(
				host,
				port,
				user,
				password,
				fileStatus,
				ftpServerLabel,
				ftpServerStatus,
				timeout,
				logTextArea);
		
		controller.visualizeCheckableAction(FtpController.MENUITEM_ACTIVE_FTP, FtpServerTreeNode.activeFtp, tb2);
		controller.visualizeCheckableAction(FtpController.MENUITEM_SLOWDIRLIST_MODE, FtpServerTreeNode.doSlowButSafeListing, tb2);
		tb2.add(new JSeparator(SwingConstants.VERTICAL));
		
		controller.visualizeAction(FtpController.MENUITEM_CONNECT, tb2);
		controller.visualizeAction(FtpController.MENUITEM_DISCONNECT, tb2);
		controller.visualizeAction(FtpController.MENUITEM_PROXY, tb2);
		tb2.add(new JSeparator(SwingConstants.VERTICAL));
		controller.visualizeAction(FtpController.MENUITEM_NEW_WINDOW, tb2);
		tb2.add(new JSeparator(SwingConstants.VERTICAL));
		controller.visualizeAction(FtpController.MENUITEM_REFRESH, tb2);
		controller.visualizeAction(FtpController.MENUITEM_VIEW, tb2);
		tb2.add(new JSeparator(SwingConstants.VERTICAL));
		controller.visualizeAction(FtpController.MENUITEM_INSERT, tb2);
		controller.visualizeAction(FtpController.MENUITEM_DELETE, tb2);
		controller.visualizeAction(FtpController.MENUITEM_RENAME, tb2);
		tb2.add(new JSeparator(SwingConstants.VERTICAL));
		controller.visualizeAction(FtpController.MENUITEM_CUT, tb2);
		controller.visualizeAction(FtpController.MENUITEM_COPY, tb2);
		controller.visualizeAction(FtpController.MENUITEM_PASTE, tb2);


		JPopupMenu popup = new JPopupMenu();
		controller.visualizeAction(FtpController.MENUITEM_REFRESH, popup, false);
		controller.visualizeAction(FtpController.MENUITEM_VIEW, popup, false);
		popup.addSeparator();
		controller.visualizeAction(FtpController.MENUITEM_INSERT, popup, false);
		controller.visualizeAction(FtpController.MENUITEM_DELETE, popup, false);
		controller.visualizeAction(FtpController.MENUITEM_RENAME, popup, false);
		popup.addSeparator();
		controller.visualizeAction(FtpController.MENUITEM_CUT, popup, false);
		controller.visualizeAction(FtpController.MENUITEM_COPY, popup, false);
		controller.visualizeAction(FtpController.MENUITEM_PASTE, popup, false);
		
		MouseListener popupListener = new PopupMouseListener(popup);
		ftpServerView.addMouseListener(popupListener);
		fileView.addMouseListener(popupListener);


		controller.setView(ftpServerView);	// if Connect is pressed before view was focused.
		controller.setView(fileView);
		
		
		SplitPane split = new SplitPane(JSplitPane.HORIZONTAL_SPLIT, p1, p2);
		new SymmetryListener(split);

		JPanel p3 = new JPanel(new GridLayout(2, 1));
		p3.add(tb);
		p3.add(tb2);

		
		SplitPane split2 = new SplitPane(JSplitPane.VERTICAL_SPLIT, split, new JScrollPane(logTextArea));
		split2.setDividerLocation(0.9f);

		add(p3, BorderLayout.NORTH);
		add(split2, BorderLayout.CENTER);
		
		if (host.getText().length() > 0 && user.getText().length() > 0)
			ComponentUtil.requestFocus(password);
		else
		if (host.getText().length() > 0)
			ComponentUtil.requestFocus(user.getTextEditor());
	}

	
	/** Implements FocusListener to set the appropriate treeview to controller. */
	public void focusGained(FocusEvent e)	{
		controller.setView((JTree)e.getComponent());
	}
	public void focusLost(FocusEvent e)	{
	}
	
	
	public void close()	{
		controller.close();
	}

}




class HostCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();
	private static File globalFile = null;

	public HostCombo()	{
		super();
		manageTypedHistory(this, new File(HistConfig.dir()+"FtpHostCombo.list"));
	}

	// interface TypedHistoryHolder
	public void setTypedHistoryData(Vector v, File f)	{
		globalHist = v;
		globalFile = f;
	}
	public Vector getTypedHistory()	{
		return globalHist;
	}
	public File getHistoryFile()	{
		return globalFile;
	}
}


class UserCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();
	private static File globalFile = null;

	public UserCombo()	{
		super();
		manageTypedHistory(this, new File(HistConfig.dir()+"FtpUserCombo.list"));
	}

	// interface TypedHistoryHolder
	public void setTypedHistoryData(Vector v, File f)	{
		globalHist = v;
		globalFile = f;
	}
	public Vector getTypedHistory()	{
		return globalHist;
	}
	public File getHistoryFile()	{
		return globalFile;
	}
}