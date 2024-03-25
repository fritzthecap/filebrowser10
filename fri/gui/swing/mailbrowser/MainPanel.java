package fri.gui.swing.mailbrowser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.JMenu;
import fri.util.os.OS;
import fri.gui.mvc.view.swing.PopupMouseListener;
import fri.gui.swing.splitpane.SplitPane;
import fri.gui.swing.mailbrowser.addressbook.*;

public class MainPanel extends JPanel
{
	private FolderTree folderTree;
	private MessageTable messageTable;
	private AddressTable addressTable;
	private SplitPane mainSplit, leftSplit, rightSplit;
	private FolderController folderCtl;
	
	public MainPanel(String sendHost, String receiveHost, String user, String password)	{
		super(new BorderLayout());
		
		if (sendHost != null)
			ConnectionSingletons.setSendInstance(sendHost);
		
		if (receiveHost != null)
			ConnectionSingletons.setReceiveInstance(receiveHost, user, password);
		
		folderTree = new FolderTree();
		messageTable = new MessageTable();
		addressTable = new AddressTable();
		JPanel viewer = new JPanel(new BorderLayout());
		
		folderCtl = new FolderController(folderTree, messageTable);
		MessageController messageCtl = new MessageController(messageTable, viewer);
		AddressController addressCtl = new AddressController(addressTable, messageCtl);
		
		// associate the addressController to others for collecting mail addresses
		folderCtl.setAddressController(addressCtl);
		messageCtl.setAddressController(addressCtl);
		
		JToolBar tb = new JToolBar();
		if (OS.isAboveJava13) tb.setRollover(true);
		tb.setMinimumSize(new Dimension());
		folderCtl.visualizeAction(FolderController.ACTION_RECEIVE, tb);
		folderCtl.visualizeAction(FolderController.ACTION_CONFIGURE, tb);
		messageCtl.visualizeAction(MessageController.ACTION_RULES_EDITOR, tb);
		tb.addSeparator();
		messageCtl.visualizeAction(MessageController.ACTION_NEW, tb);	// new message
		messageCtl.visualizeAction(MessageController.ACTION_REPLY, tb);
		messageCtl.visualizeAction(MessageController.ACTION_FORWARD, tb);
		tb.addSeparator();
		messageCtl.visualizeAction(MessageController.ACTION_DELETE, tb);
		messageCtl.visualizeAction(MessageController.ACTION_CUT, tb);
		messageCtl.visualizeAction(MessageController.ACTION_COPY, tb);
		folderCtl.visualizeAction(FolderController.ACTION_PASTE, tb);
		tb.addSeparator();
		messageCtl.visualizeAction(MessageController.ACTION_SET_UNREAD, tb);
		messageCtl.visualizeAction(MessageController.ACTION_SAVE, tb);
		messageCtl.visualizeAction(MessageController.ACTION_VIEW, tb);
		tb.addSeparator();
		messageCtl.visualizeAction(MessageController.ACTION_NEW_WINDOW, tb);
		tb.addSeparator();
		messageCtl.visualizeAction(MessageController.ACTION_ABOUT, tb);
		
		JToolBar tbFlds = new JToolBar(JToolBar.VERTICAL);
		if (OS.isAboveJava13) tbFlds.setRollover(true);
		tbFlds.setMinimumSize(new Dimension());
		folderCtl.visualizeAction(FolderController.ACTION_NEW_FOLDER, tbFlds);	// new folders
		folderCtl.visualizeAction(FolderController.ACTION_DELETE, tbFlds);
		folderCtl.visualizeAction(FolderController.ACTION_CUT, tbFlds);
		folderCtl.visualizeAction(FolderController.ACTION_COPY, tbFlds);
		folderCtl.visualizeAction(FolderController.ACTION_PASTE, tbFlds);
		
		JPopupMenu popup;
		PopupMouseListener popupListener;

		popup = new JPopupMenu();
		JMenu connItem = new JMenu(Language.get("New_Connection"));
		folderCtl.setConnectionMenuItem(connItem);
		folderCtl.visualizeAction(FolderController.ACTION_NEW_CONNECTION, connItem, false);
		folderCtl.visualizeAction(FolderController.ACTION_NEW_LOCAL_STORE, connItem, false);
		popup.add(connItem);
		folderCtl.visualizeAction(FolderController.ACTION_NEW_FOLDER, popup, false);
		popup.addSeparator();
		folderCtl.visualizeAction(FolderController.ACTION_DELETE, popup, false);
		popup.addSeparator();
		folderCtl.visualizeAction(FolderController.ACTION_CUT, popup, false);
		folderCtl.visualizeAction(FolderController.ACTION_COPY, popup, false);
		folderCtl.visualizeAction(FolderController.ACTION_PASTE, popup, false);
		popup.addSeparator();
		folderCtl.visualizeAction(FolderController.ACTION_TO_ADDRESSBOOK, popup, false);
		
		popupListener = new PopupMouseListener(popup);
		folderTree.getSensorComponent().addMouseListener(popupListener);
				
		popup = new JPopupMenu();
		messageCtl.visualizeAction(MessageController.ACTION_NEW, popup, false);
		messageCtl.visualizeAction(MessageController.ACTION_REPLY, popup, false);
		messageCtl.visualizeAction(MessageController.ACTION_FORWARD, popup, false);
		popup.addSeparator();
		messageCtl.visualizeAction(MessageController.ACTION_DELETE, popup, false);
		messageCtl.visualizeAction(MessageController.ACTION_CUT, popup, false);
		messageCtl.visualizeAction(MessageController.ACTION_COPY, popup, false);
		folderCtl.visualizeAction(FolderController.ACTION_PASTE, popup, false);
		popup.addSeparator();
		messageCtl.visualizeAction(MessageController.ACTION_SET_UNREAD, popup, false);
		popup.addSeparator();
		messageCtl.visualizeAction(MessageController.ACTION_SAVE, popup, false);
		messageCtl.visualizeAction(MessageController.ACTION_VIEW, popup, false);
		popup.addSeparator();
		messageCtl.visualizeAction(MessageController.ACTION_TO_ADDRESSBOOK, popup, false);
		
		popupListener = new PopupMouseListener(popup);
		messageTable.getSensorComponent().addMouseListener(popupListener);

		leftSplit = new SplitPane(FolderTree.class, SplitPane.VERTICAL_SPLIT, folderTree, addressTable);
		rightSplit = new SplitPane(MessageTable.class, SplitPane.VERTICAL_SPLIT, messageTable, viewer);
		mainSplit = new SplitPane(MainPanel.class, SplitPane.HORIZONTAL_SPLIT, leftSplit, rightSplit);
		
		JToolBar tbAddr = new JToolBar(JToolBar.VERTICAL);
		if (OS.isAboveJava13) tbAddr.setRollover(true);
		tbAddr.setMinimumSize(new Dimension());
		addressCtl.visualizeAction(AddressController.ACTION_MAIL_TO, tbAddr);
		tbAddr.addSeparator();
		addressCtl.visualizeAction(AddressController.ACTION_NEW, tbAddr);
		addressCtl.visualizeAction(AddressController.ACTION_DELETE, tbAddr);
		tbAddr.addSeparator();
		addressCtl.visualizeAction(AddressController.ACTION_CUT, tbAddr);
		addressCtl.visualizeAction(AddressController.ACTION_PASTE, tbAddr);
		tbAddr.addSeparator();
		addressCtl.visualizeAction(AddressController.ACTION_EXPORT, tbAddr);

		popup = new JPopupMenu();
		addressCtl.visualizeAction(AddressController.ACTION_MAIL_TO, popup, false);
		popup.addSeparator();
		addressCtl.visualizeAction(AddressController.ACTION_NEW, popup, false);
		addressCtl.visualizeAction(AddressController.ACTION_DELETE, popup, false);
		popup.addSeparator();
		addressCtl.visualizeAction(AddressController.ACTION_CUT, popup, false);
		addressCtl.visualizeAction(AddressController.ACTION_PASTE, popup, false);
		popup.addSeparator();
		addressCtl.visualizeAction(AddressController.ACTION_EXPORT, popup, false);

		popupListener = new PopupMouseListener(popup);
		addressTable.getSensorComponent().addMouseListener(popupListener);

		messageTable.add(tb, BorderLayout.NORTH);
		folderTree.add(tbFlds, BorderLayout.WEST);
		addressTable.add(tbAddr, BorderLayout.WEST);

		add(mainSplit, BorderLayout.CENTER);


		new FolderDndPerformer(
				folderTree.getSensorComponent(),
				folderCtl);
				
		new MessageDndPerformer(
				messageTable.getSensorComponent(),
				messageCtl,
				folderCtl);
		new MessageDndPerformer(	// the viewport of table
				messageTable.getSensorComponent().getParent(),
				messageCtl,
				folderCtl);

		new AddressDndPerformer(
				addressTable.getSensorComponent(),
				addressCtl);
		new AddressDndPerformer(	// the viewport of table
				addressTable.getSensorComponent().getParent(),
				addressCtl);
	}


	/** Called after frame was shown on screen, everything is initialized. Set selection to INBOX and get new mails. */
	public void setFirstSelection()	{
		folderCtl.setFirstSelection();
	}


	/** Called when frame is closing. */
	public void close()	{
		folderTree.close();
		messageTable.close();
		addressTable.close();

		mainSplit.close();
		leftSplit.close();	// depends on FolderTree, which clears all properties, so close after FolderTree
		rightSplit.close();
		
		folderCtl.close();
		
		// TODO: put the following to controllers!
		MailClipboard.freeMailClipboard();	// one for FolderController
		MailClipboard.freeMailClipboard();	// one for MessageController
	}

}
