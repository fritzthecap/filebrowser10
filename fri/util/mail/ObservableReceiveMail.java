package fri.util.mail;

import javax.mail.Message;
import javax.mail.Authenticator;

import fri.util.observer.CancelProgressObservable;
import fri.util.observer.CancelProgressObserver;

/**
	Observable version of ReceiveMail.
	
	@author Fritz Ritzberger, 2003
*/

public class ObservableReceiveMail extends ReceiveMail implements
	CancelProgressObservable
{
	private CancelProgressObserver observer;
	private boolean localStore;
	private boolean leaveMailsOnServer;
	private String messageLabel, ofLabel;

	public ObservableReceiveMail(ReceiveProperties mailProperties, Authenticator authenticator)
		throws Exception
	{
		super(mailProperties, authenticator);
		
		leaveMailsOnServer = mailProperties.getLeaveMailsOnServer();
	}
	
	public ObservableReceiveMail(String protocol, String host, String port, String user, String password)
		throws Exception
	{
		super(protocol, host, port, user, password);
	}

	public ObservableReceiveMail(String urlName)
		throws Exception
	{
		super(urlName);
	}

	public ObservableReceiveMail(String urlName, boolean localStore)
		throws Exception
	{
		this(urlName);
		this.localStore = localStore;
	}

	protected ObservableReceiveMail()	{	// clone constructor
	}
	
	

	/** Implements CancelProgressObservable. MUST call this to get this class working! */
	public void setObserver(CancelProgressObserver observer)	{
		setObserver(observer, null, null);
	}
	
	/** Implements CancelProgressObservable. MUST call this to get this class working! */
	public void setObserver(CancelProgressObserver observer, String messageLabel, String ofLabel)	{
		this.observer = observer;
		this.messageLabel = messageLabel;
		this.ofLabel = ofLabel;
	}


	/** Overridden to provide observation when receiving mails. */
	protected boolean visitMessage(MailVisitor visitor, int count, int nr, Message msg)
		throws Exception
	{
		String m1 = messageLabel == null ? "Message" : messageLabel;
		String m2 = ofLabel == null ? "Of" : ofLabel;
		if (observer != null)
			observer.setNote(m1+" "+(nr + 1)+" "+m2+" "+count);
		
		visitor.message(count, nr, msg);
		
		if (observer != null)
			observer.progress(1);
		return observer == null ? true : observer.canceled() == false;
	}


	protected ReceiveMail newInstance()	{
		ObservableReceiveMail rm = new ObservableReceiveMail();
		rm.observer = observer;
		return rm;
	}

	/** Returns false if this is a POP connection or is a local store and foldername is a standard name like inbox, trash. */
	public boolean canRename()	{
		if (isPop)
			return false;

		try	{
			String name = pwd().getName();
			return
					name.equals(LocalStore.INBOX) == false &&
					name.equals(LocalStore.OUTBOX) == false &&
					name.equals(LocalStore.DRAFTS) == false &&
					name.equals(LocalStore.TRASH) == false &&
					name.equals(LocalStore.SENT) == false;
		}
		catch (Exception e)	{
			e.printStackTrace();
			return false;
		}
	}

	public boolean isLocalStore()	{
		return localStore;
	}

	public boolean getLeaveMailsOnServer()	{
		return leaveMailsOnServer;
	}

}
