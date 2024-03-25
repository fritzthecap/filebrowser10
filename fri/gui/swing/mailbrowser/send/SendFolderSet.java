package fri.gui.swing.mailbrowser.send;

import fri.util.mail.ObservableReceiveMail;

/**
	Send folders are: drafts, outbox, sent-mail. These are needed to edit or send a mail.
*/

public class SendFolderSet
{
	public final ObservableReceiveMail current, drafts, outbox, sent;
	
	/**
		Create a send argument collection with all necessary folders for sending a mail.
		This is (ordered): current, drafts, outbox, sent.
	*/
	public SendFolderSet(
		ObservableReceiveMail current,
		ObservableReceiveMail drafts,
		ObservableReceiveMail outbox,
		ObservableReceiveMail sent)
	{
		this.current = current;
		this.drafts = drafts;
		this.outbox = outbox;
		this.sent = sent;
	}

}
