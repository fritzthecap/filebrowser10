package fri.gui.swing.mailbrowser.rules;

import javax.mail.*;
import fri.util.mail.MessageUtil;

/**
	Wraps a <i>javax.mail.Message</i> to be able to be requested by the rule engine
	that carries out mail rules. There are some properties that can not be requested
	by introspection, like <i>getRecipients()</i>.
	<p>
	Furthermore this class propagates behaviour for the received message to the
	FolderController, that controls deletion from server, target folder and rejection
	of the received mail. Default behavoiur is: receive (do not reject), delete from server.
*/

public class MessageRuleWrapper
{
	/** List of rendered message properties that can be used by the rule engine. */
	public static final String [] fieldNames = new String []	{
		"from",
		"recipients",
		"subject",
		"text",
	};
	/** List of rendered actions that can be used by the rule engine. */
	public static final String [] actionNames = new String []	{
		"move",
		"copy",
		"delete",
		"reject",
	};
	private Message msg;
	private boolean canDeleteFromServer = true;
	private boolean canReceive = true;
	private boolean copy = false;
	private boolean move = false;
	private String receiveFolder;


	public MessageRuleWrapper(Message msg)	{
		this.msg = msg;
	}


	// behavioural methods, needed by the mail controller
	
	public boolean canDeleteFromServer()	{
		return canDeleteFromServer;
	}

	public boolean canReceive()	{
		return canReceive;
	}

	public String receiveFolder()	{
		return receiveFolder;
	}

	public boolean isCopy()	{
		return copy;
	}

	public boolean isMove()	{
		return move;
	}


	// actions called by the rule engine, written in mailrules.properties
	
	/** Leave on server, do not receive. */
	public void reject()	{
		this.canDeleteFromServer = false;
		this.canReceive = false;
		this.copy = false;
		this.move = false;
	}

	/** Do not leave on server, do not receive. */
	public void delete()	{
		this.canDeleteFromServer = true;
		this.canReceive = false;
		this.copy = false;
		this.move = false;
	}

	/** Do not leave on server, move to passed folder. */
	public void move(String receiveFolder)	{
		this.canDeleteFromServer = true;
		this.canReceive = true;
		this.copy = false;
		this.move = true;
		this.receiveFolder = receiveFolder;
	}

	/** Do not leave on server, copy to passed folder. */
	public void copy(String receiveFolder)	{
		this.canDeleteFromServer = true;
		this.canReceive = true;
		this.copy = true;
		this.move = false;
		this.receiveFolder = receiveFolder;
	}



	// properties requested by rule engine, written in mailrules.properties
	
	/** Returns the property "recipients". */
	public String getRecipients()	{
		try	{
			String to = MessageUtil.addressesToString(msg.getRecipients(Message.RecipientType.TO));
			System.err.println("message was sent to: "+to);
			return to;
		}
		catch (MessagingException e)	{
			e.printStackTrace();
			return null;
		}
	}
	
	/** Returns the property "from". */
	public String getFrom()	{
		try	{
			String from = MessageUtil.addressesToString(msg.getFrom());
			System.err.println("message was sent by: "+from);
			return from;
		}
		catch (MessagingException e)	{
			e.printStackTrace();
			return null;
		}
	}
	
	/** Returns the property "subject". */
	public String getSubject()	{
		try	{
			return msg.getSubject();
		}
		catch (MessagingException e)	{
			e.printStackTrace();
			return null;
		}
	}
	
	/** Returns the property "text". */
	public String getText()	{
		try	{
			return MessageUtil.getText(msg);
		}
		catch (Exception e)	{
			e.printStackTrace();
			return null;
		}
	}

}
