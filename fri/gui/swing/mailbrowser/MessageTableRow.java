package fri.gui.swing.mailbrowser;

import java.util.Date;
import javax.mail.*;
import javax.mail.internet.AddressException;
import fri.util.mail.MessageUtil;
import fri.gui.mvc.model.swing.DefaultTableRow;

public class MessageTableRow extends DefaultTableRow
{
	private Message message;
	
	public MessageTableRow(Message msg)	{
		this(msg, false);
	}
	
	public MessageTableRow(Message msg, boolean renderToAddress)	{
		super(MessageTableModel.COLUMN_COUNT);
		
		this.message = msg;
		
		try	{
			String subject = msg.getSubject();
			if (subject == null)
				subject = "";
	
			String addr = "";
			try	{
				Address [] addresses = renderToAddress ? msg.getAllRecipients() : msg.getFrom();
				if (addresses != null && addresses.length > 0)
					addr = MessageUtil.addressesToString(addresses);
			}
			catch (AddressException e)	{
				addr = "AddressException: "+e.getMessage();
			}
			
			String date = "";
			Date sentDate = msg.getSentDate();
			if (sentDate != null)
				date = MessageTableModel.dateFormat.format(sentDate);
			
			MessageTableModel.buildMessageTableRow(this, MessageUtil.getDecodedText(subject), addr, date);
		}
		catch (MessagingException e)	{
			e.printStackTrace();
		}
	}


	public Message getMessage()	{
		return message;
	}

}