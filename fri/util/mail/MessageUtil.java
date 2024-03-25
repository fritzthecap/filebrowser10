package fri.util.mail;

import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;
import javax.mail.*;
import javax.mail.internet.*;

/**
	@author Fritz Ritzberger, 2003
*/

public abstract class MessageUtil
{
	/** Defines the term "new message" for this mail application. This is coded in FmMaildirFolder, too. */
	public static boolean isNewMessage(Message msg)
		throws MessagingException
	{
		return msg.isSet(Flags.Flag.RECENT) && msg.isSet(Flags.Flag.SEEN) == false;
	}

	/** Defines the term "new message" by setting SEEN and RECENT flag. This is coded in FmMaildirFolder, too. */
	public static void setMessageNew(Message msg, boolean markAsNew)
		throws MessagingException
	{
		try	{ msg.setFlag(Flags.Flag.RECENT, markAsNew); } catch (MessagingException e) { System.err.println("Set message NEW, can not set RECENT flag to "+markAsNew+": "+e.toString()); }	// exception happens on IMAP
		msg.setFlag(Flags.Flag.SEEN, !markAsNew);
	}

	/** Sets the DELETED flag to passed message. */
	public static void setMessageDeleted(Message msg)
		throws MessagingException
	{
		try	{ msg.setFlag(Flags.Flag.RECENT, false); } catch (MessagingException e) { System.err.println("Set message DELETED, can not set RECENT flag to false: "+e.toString()); }
		msg.setFlag(Flags.Flag.SEEN, true);
		// TODO: are these necessary to delete message?
		
		msg.setFlag(Flags.Flag.DELETED, true);
		//System.err.println("setting message flag DELETED, is flag set? -> "+msg.isSet(Flags.Flag.DELETED));
	}


	/** Returns the message id from header field "Message-Id", or null if not found or -1 or exception occured. */
	public static String getMessageId(Message msg)	{
		try	{
			String [] headers = msg.getHeader("Message-Id");
			if (headers != null && headers.length > 0 && headers[0] != null)	{
				String messageId = headers[0];
				if (messageId.equals("-1") == false && messageId.equals("0") == false)
					return messageId;
			}
		}
		catch (MessagingException e)	{
		}
		return null;
	}


	/** Returns the text substituted with "> " at each line start. */
	public static String replyText(String text, String lineMarker)	{
		if (lineMarker == null)
			lineMarker = "> ";
			
		StringBuffer sb = new StringBuffer(text.length() + 40);
		for (StringTokenizer stok = new StringTokenizer(text, "\n"); stok.hasMoreTokens(); )	{
			sb.append(lineMarker);
			sb.append(stok.nextToken());
			sb.append("\n");
		}
		return sb.toString();
	}


	/** Remove trailing "; ..." from content-type. */
	public static String baseContentType(Part part)
		throws MessagingException
	{
		return baseContentType(part.getContentType());
	}

	/** Remove trailing "; ..." from content-type. */
	public static String baseContentType(String contentType)
		throws MessagingException
	{
		return new ContentType(contentType).getBaseType();
	}
	

	/** Turns Addresses to a string where they are separated by ", " */
	public static String addressesToString(Address [] addresses)	{
		String toAddresses = "";
		for (int i = 0; addresses != null && i < addresses.length; i++)	{
			String address = getDecodedText(addresses[i].toString());
			toAddresses = toAddresses+(toAddresses.length() > 0 ? ", " : "")+address;
		}
		return toAddresses;
	}
	

	/** Turns mail header strings to readable Strings. */
	public static String getDecodedText(String text)	{
		try	{
			return MimeUtility.decodeText(text);
		}
		catch (UnsupportedEncodingException e)	{
			return text;
		}
	}


	/** Returns the plain text of the message, or empty string if no "text/plain" part found. */
	public static String getText(Part part)
		throws Exception
	{
		if (part.getContentType().toLowerCase().startsWith("multipart/*"))	{
			Multipart mp = (Multipart)part.getContent();
			if (mp.getCount() > 0)
				return getText(mp.getBodyPart(0));
		}
		else
		if (part.getContentType().toLowerCase().startsWith("message/rfc822"))	{
			return getText((Part)part.getContent());
		}
		else
		if (part.getContentType().toLowerCase().startsWith("text/plain"))	{
			return part.getContent().toString();
		}
		return "";
	}

}
