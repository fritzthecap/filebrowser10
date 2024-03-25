package fri.gui.swing.mailbrowser.send;

import javax.mail.Message;
import javax.mail.Address;
import fri.util.mail.SendProperties;
import fri.gui.swing.mailbrowser.*;

/**
	Mail sending frame, reply and forward.
	It keeps in touch with mail command map.
*/

public class SendFrame extends CommandMapAwareFrame
{
	private SendPanel panel;
	
	/**
		Empty send frame.
	*/
	public SendFrame(SendProperties props, SendFolderSet sendFolders)	{
		this(props, sendFolders, null, null);
	}
	
	/**
		Send, reply to or forward a message.
		@param reply true for reply, false for forward, null for none
		@param props send host, personal name, reply address
		@param msg optional Message to raply to or to forward, can be null
	*/
	public SendFrame(SendProperties props, SendFolderSet sendFolders, Message msg, Boolean reply)	{
		super(
			reply == null
				? Language.get("Send_Message")
				: reply.equals(Boolean.TRUE)
					? Language.get("Reply_To_Message")
					: Language.get("Forward_Message"));

		panel = new SendPanel(this, props, sendFolders, msg, reply);
		getContentPane().add(panel);

		init();	// show the frame
		
		if (props == null || props.isValid() == false)	{
			new ConfigureDialog(this, props);
		}
	}


	/** Set the recipients, needed by AddressController. */
	public void setTo(Address [] addresses)	{
		panel.setTo(addresses);
	}
	

	/** Overridden to ensure saving the message. When panel returns false, false is returned (frame remains open). */
	public boolean close()	{
		if (panel.close() == false)
			return false;
		return super.close();
	}



	/** Send mail application main. */
	public static void main(String [] args)	{
		new SendFrame(ConnectionSingletons.getSendInstance(), null);
	}

}
