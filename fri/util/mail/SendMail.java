package fri.util.mail;

import java.io.File;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import fri.util.NetUtil;

/**
	Mail sending object, relies on passed properties.
	To use authentication for SMTP, the <i>smtpPasswordOpt</i>
	and the property ""mail.smtp.user" must be non-null.
	
	@author Fritz Ritzberger, 2003
*/

public class SendMail
{
	public static final int DEFAULT_SMTP_PORT = 25;
	private Message message;
	private Properties mailProperties;
	private String smtpPassword;
	private Session session;


	/**
		Build a MIME message from passed data. Do not already send it.
		@param fromAddress Re-Mail address of sending user
		@param addresses String list of recipient(s) mail address(es) of receiving user(s)
		@param copies String list of more "Cc" recipient(s) mail address(es) of receiving user(s), can be null
		@param subject topic of this mail, appearing in subject
		@param mailText mail body text
		@param attachments String (filename) or BodyPart list of attachments
		@param mailProperties name-value pairs according to Sun's Mail API specification, to be passed to Transport Session
		@param smtpPasswordOpt optional password for SMTP authentication, can be null
	*/
	public SendMail(
		String fromAddress,
		Vector addresses,
		Vector copies,
		String subject,
		String mailText,
		Vector attachments,
		Properties mailProperties,
		String smtpPasswordOpt)
	throws
		Exception
	{
		this.mailProperties = mailProperties;
		
		if (mailProperties.getProperty("mail.smtp.user") != null && smtpPasswordOpt != null)	{
			mailProperties.setProperty("mail.smtp.auth", "true");
			this.smtpPassword = smtpPasswordOpt;
		}
		
		Properties props = new Properties(mailProperties);	// new props backed by passed ones
		
		// following is necessary to avoid NullPointerException in msg.saveChanges()
		String mailHost = mailProperties.getProperty("mail.smtp.host");
		props.setProperty("mail.host", mailHost != null ? mailHost : "localhost");
		
		String localHost = NetUtil.getLocalHostName();
		if (NetUtil.getLocalHostError() != null)	// workaround for LINUX null host name
			props.setProperty("mail.smtp.localhost", localHost);
		
		System.err.println("Mail properties for sending are:\n");
		props.list(System.err);

		this.session = Session.getDefaultInstance(props);


		// build the message
		Message msg = new MimeMessage(session);
		
		if (fromAddress != null && fromAddress.length() > 0)
			msg.setFrom(new InternetAddress(fromAddress));
		
		if (subject != null)
			msg.setSubject(subject);
			
		msg.setSentDate(new Date());

		for (int i = 0; addresses != null && i < addresses.size(); i++)
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress((String)addresses.get(i)));

		for (int i = 0; copies != null && i < copies.size(); i++)
			msg.addRecipient(Message.RecipientType.CC, new InternetAddress((String)copies.get(i)));

		if (attachments == null || attachments.size() <= 0)	{
			if (mailText != null)
				msg.setText(mailText);
		}
		else	{
			Multipart multipart = new MimeMultipart();

			BodyPart textBody = new MimeBodyPart();	// add text
			textBody.setText(mailText);
			multipart.addBodyPart(textBody);
			
			for (int i = 0; i < attachments.size(); i++)	{	// add attachments
				BodyPart messageBodyPart = null;
				
				Object o = attachments.get(i);
				if (o instanceof String)	{
					messageBodyPart = new MimeBodyPart();
					String filename = (String)o;
					DataSource source = new FileDataSource(filename);
					messageBodyPart.setFileName(new File(filename).getName());
					messageBodyPart.setDataHandler(new DataHandler(source));
				}
				else
				if (o instanceof BodyPart)	{
					messageBodyPart = (BodyPart) o;
				}
				else
				if (o instanceof Message)	{
					Message message = (Message) o;
					try	{
						o = message.getContent();
						if (o instanceof String)	{
							messageBodyPart = new MimeBodyPart();
							messageBodyPart.setText((String) o);
						}
						else
							throw new IllegalArgumentException("Message has no text content: "+o.getClass());
					}
					catch (Exception e)	{
						System.err.println("Leaving out attached message: "+e);
					}
				}
				else	{
					System.err.println("Leaving out body part: "+o.getClass());
				}
				
				if (messageBodyPart != null)
					multipart.addBodyPart(messageBodyPart);
			}
			
			msg.setContent(multipart);
		}

		msg.saveChanges();	// would generate NullPointerException when "mail.host" was not set

		this.message = msg;
	}

	
	/** Actually sends the message. */
	public void send()
		throws Exception
	{
		Transport tr = session.getTransport("smtp");
		
		if (this.smtpPassword != null)	{
			tr.connect(
					mailProperties.getProperty("mail.smtp.host"),
					mailProperties.getProperty("mail.smtp.user"),
					smtpPassword);
		}
		else	{
			tr.connect();
		}

		tr.sendMessage(getMessage(), getMessage().getAllRecipients());	// does not call saveChanges()
		tr.close();
		
	}


	/** Returns the MIME message built by this SendMail object. */
	public Message getMessage()	{
		return message;
	}



	public static void main(String [] args)
		throws Exception
	{
		if (args.length < 3)	{
			System.err.println("SYNTAX: java "+SendMail.class.getName()+" SMTPHost fromMailAddress toMailAddress [attachedFile ...]");
			System.exit(1);
		}
		Properties props = new Properties();
		props.setProperty("mail.smtp.host", args[0]);
		
		Vector to = new Vector();
		to.add(args[2]);
		
		Vector att = new Vector();
		for (int i = 3; i < args.length; i++)	{
			att.add(args[i]);
		}
		
		SendMail sm = new SendMail(
			args[1],
			to,
			null,
			"Mail from "+SendMail.class.getName(),
			"Hello! Attachments: "+att,
			att,
			props,
			null);
			
		sm.send();
	}

}
