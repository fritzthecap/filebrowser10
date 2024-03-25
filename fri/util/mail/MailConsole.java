package fri.util.mail;

import java.io.*;
import java.util.*;
import java.text.*;
import javax.mail.*;
import fri.util.NetUtil;
import fri.util.props.ClassProperties;
import fri.util.application.ConsoleApplication;

/**
	A console application that offers literal mail commands.
	<p>
	Syntax: java fri.util.mail.MailConsole host [port] user password
	
	@author Fritz Ritzberger, 2003
*/

public class MailConsole extends ConsoleApplication
{
	private static final String version = "1.0";
	private Properties mailProperties;
	private static DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	private String receivePassword;
	private String sendPassword;
	
	
	// instance init: read mail properties, mix in system properties
	{
		mailProperties = ClassProperties.getProperties(MailConsole.class);

		for (Enumeration e = System.getProperties().propertyNames(); e.hasMoreElements(); )	{
			String s = (String)e.nextElement();
			if (s.startsWith("mail."))
				mailProperties.setProperty(s, System.getProperties().getProperty(s));
		}
	}



	
	/** Create a mail console application. */
	public MailConsole(String sendHost, String receiveHost, String user, String password)
		throws IOException
	{
		super("fri-MAIL "+version+" by Fritz Ritzberger 2003", "mail");
		
		mailProperties.put("mail.store.protocol", "pop3");
		
		String [] args;

		args = sendHost == null ? null : new String [] { sendHost };
		parseConnectionArgs(args, true, false);

		args = receiveHost == null ? null : new String [] { receiveHost, user, password };
		parseConnectionArgs(args, false, false);
	}


	// ConsoleApplication overridings start

	/** Called only once at start. Tries to connect to an host given on commandline. */
	protected void init()	{
		try	{ help(null); }
		catch (Exception e)	{}
	}
	
	/** Used to store entered mail properties. */
	public void quit(String [] args) throws Exception	{
		ClassProperties.store(MailConsole.class);
		LocalStore.close();
		super.quit(args);
	}

	// ConsoleApplication overridings end

	
	// calback implementations

	public void help(String [] args) throws Exception	{
		getOut().println("Implemented commands are:");
		getOut().println("	send|put    [sendHost[:port]   [user [password]]] ... send new mail, input until \".\" appears on a single line");
		getOut().println("	receive|get [receiveHost[:port] user [password]]  ... receive mail");
		getOut().println("	local [urlName]                                   ... connect to local disk store");
		getOut().println("	set <propertyName> <propertyValue>                ... set any connection property to a new value");
		getOut().println("	show|list                                         ... show connection properties");
		getOut().println("	debug [on|off]                                    ... set debug output on or off");
	}

	public void list(String [] args) throws Exception	{
		show(args);
	}
	
	public void show(String [] args) throws Exception	{
		if (args != null && args.length > 0)	{
			for (int i = 0; i < args.length; i++)
				getOut().println(args[i]+": "+mailProperties.getProperty(args[i]));
		}
		else	{
			for (Enumeration e = mailProperties.propertyNames(); e.hasMoreElements(); )	{
				String key = (String)e.nextElement();
				getOut().println(key+"	=	"+mailProperties.getProperty(key));
			}
		}
	}

	public void put(String [] args) throws Exception	{
		send(args);
	}
	
	public void send(String [] args) throws Exception	{
		if (parseConnectionArgs(args, true, true) == false)	{
			getOut().println("ERROR: Insufficient connection parameters!");
			show(null);
		}
		else	{
			sendMail();
		}
	}

	public void get(String [] args) throws Exception	{
		receive(args);
	}
	
	public void receive(String [] args) throws Exception	{
		if (parseConnectionArgs(args, false, true) == false)	{
			getOut().println("ERROR: Insufficient connection parameters!");
			show(null);
		}
		else	{
			receiveMail();
		}
	}

	public void debug(String [] args) throws Exception	{
		mailProperties.setProperty("mail.debug",
				args == null || args.length <= 0 || args[0].equals("on") || args[0].equals("true")
				? "true"
				: "false");
		list(new String [] { "mail.debug" });
	}
	
	public void q(String [] args) throws Exception	{
		quit(args);
	}
	
	public void set(String [] args) throws Exception	{
		if (args.length > 1)	{
			String s = "";
			for (int i = 1; i < args.length; i++)
				s = s+(i > 1 ? " " : "")+args[i];
				
			mailProperties.setProperty(args[0], s);
		}
		else
		if (args.length > 0)	{
			mailProperties.remove(args[0]);
		}
		list(null);
	}

	public void local(String [] args) throws Exception	{
		String urlName = (args.length <= 0)
				? LocalStore.getUrl()
				: args[0];
		getOut().println("Connecting to local store URL >"+urlName+"<");
		receiveMail(urlName);
	}



	// utility methods
	
	private boolean parseConnectionArgs(String [] args, boolean isSend, boolean interactive)
		throws IOException
	{
		// parse commandline arguments "[host[:port] [user [password]]]"
		// or ask for input for all necessary properties

		String host = null;
		String port = null;
		String user = null;
		String password = null;
		String eMail = null;
				
		// get host
		if (args != null && args.length > 0 && args[0] != null && args[0].length() > 0)	{
			host = args[0];
		}
		else	{
			String defHost = isSend ? mailProperties.getProperty("mail.smtp.host") : mailProperties.getProperty("mail.pop3.host");
			if (defHost == null && interactive)	{
				getOut().print((isSend ? "Send " : "Receive")+" Host:port ("+defHost+"): ");
				String h = readLine();
				host = (h.length() <= 0) ? defHost : h;
			}
		}

		// retrieve optional port number
		if (host != null)	{
			int i = host.lastIndexOf(":");	// check port at end of host
			if (i > 0)	{
				host = host.substring(0, i);
				port = host.substring(i + 1);
			}
			else	{
				String defPort = isSend ? mailProperties.getProperty("mail.smtp.port") : mailProperties.getProperty("mail.pop3.port");
				if (defPort != null)	{
					port = defPort;
				}
			}
		}
		
		// if sending, get email address
		if (isSend)	{
			String defEMail = mailProperties.getProperty("mail.smtp.from");
			if (defEMail != null)	{
				eMail = defEMail;
			}
			else	{
				if (interactive)	{
					defEMail = System.getProperty("user.name")+"@"+NetUtil.getLocalHostName();
					getOut().print("Your e-mail address ("+defEMail+"): ");
					String m = readLine();
					eMail = (m.length() <= 0) ? defEMail : m;
				}
			}
		}

		String smtpAuthProp = mailProperties.getProperty("mail.smtp.auth");
		boolean auth = isSend == false || smtpAuthProp != null && smtpAuthProp.equals("true");
		
		// get optional user
		if (args != null && args.length > 1 && args[1] != null && args[1].length() > 0)	{
			user = args[1];
		}
		else	{
			String defUser = isSend ? mailProperties.getProperty("mail.smtp.user") : mailProperties.getProperty("mail.pop3.user");
			if (defUser == null && interactive && auth)	{
				getOut().print((isSend ? "Optional authenticating sending SMTP" : "Receiving")+" user ("+defUser+"): ");
				String u = readLine();
				user = (u.length() <= 0) ? defUser : u;
			}
			else
			if (auth)	{
				user = defUser;
			}
		}

		// get optional password
		if (user != null && auth)	{
			if (args != null && args.length > 2 && args[2] != null && args[2].length() > 0)	{
				password = args[2];
			}
			else	{
				String defPw = isSend ? this.sendPassword : this.receivePassword;
				if (defPw == null && interactive)	{
					getOut().print((isSend ? "Optional authenticating sending SMTP" : "Receiving")+" password: ");
					String p = readLine();
					password = (p.length() <= 0) ? defPw : p;
				}
			}
		}
		
		if (isSend)	{
			if (host != null)
				mailProperties.setProperty("mail.smtp.host", host);
			if (port != null)
				mailProperties.setProperty("mail.smtp.port", port);
			if (user != null)
				mailProperties.setProperty("mail.smtp.user", user);
			if (password != null)
				this.sendPassword = password;
			if (eMail != null)
				mailProperties.setProperty("mail.smtp.from", eMail);
		}
		else	{	// is receive
			if (host != null)
				mailProperties.setProperty("mail.pop3.host", host);
			if (port != null)
				mailProperties.setProperty("mail.pop3.port", port);
			if (user != null)
				mailProperties.setProperty("mail.pop3.user", user);
			if (password != null)
				this.receivePassword = password;
		}
		
		if (isSend)
			return mailProperties.getProperty("mail.smtp.host") != null &&
					mailProperties.getProperty("mail.smtp.from") != null;
		else
			return mailProperties.getProperty("mail.pop3.host") != null &&
					mailProperties.getProperty("mail.pop3.user") != null &&
					this.receivePassword != null;
	}




	private void sendMail()
		throws Exception
	{
		getOut().println("__________________________________________________________________________");
		String line = ".";

		Vector addresses = new Vector();
		for (int i = 1; line.equals("") == false; i++)	{
			getOut().print(i+". TO email address (empty to finish): ");
			line = readLine();
			
			if (line.length() > 0)	{
				addresses.add(line);
			}
		}
		if (addresses.size() <= 0)
			return;

		Vector copies = new Vector();
		line = ".";
		for (int i = 1; line.equals("") == false; i++)	{
			getOut().print(i+". CC email address (empty for none): ");
			line = readLine();
			
			if (line.length() > 0)
				copies.add(line);
		}

		getOut().print("Subject: ");
		String subject = readLine();

		getOut().println("Enter mail text, terminated by \".\" on a single line:");
		getOut().println("__________________________________________________________________________");
		
		StringBuffer sb = new StringBuffer();
		while (line.equals(".") == false)	{
			line = readLine();
			
			if (line.equals(".") == false)
				sb.append(line+"\n");
		}
		getOut().println("__________________________________________________________________________");

		String mailText = sb.toString();
		Vector attachments = new Vector();
		
		while (line.equals("") == false)	{
			getOut().print("Attach file (empty for none): ");
			line = readLine();

			if (line.length() > 0)	{
				File file = new File(line);
				
				if (file.exists() == false || file.isFile() == false)	{
					getOut().print("ERROR: File not found or not normal file: "+file);
				}
				else	{
					attachments.add(file.getAbsolutePath());
					getOut().println(" - Attaching file: "+line);
				}
			}
		}

		SendMail mail = new SendMail(
				mailProperties.getProperty("mail.smtp.from"),
				addresses,
				copies,
				subject,
				mailText,
				attachments,
				mailProperties,
				this.sendPassword);
		
		getOut().print("Type ENTER for sending mail or 's' for saving to local drafts folder: ");
		line = readLine();
		if (line.length() <= 0)	{	// send message
			mail.send();

			ReceiveMail rm = LocalStore.getReceiveFolder(LocalStore.SENT);
			rm.append(new Message [] { mail.getMessage() });
		}
		else	{
			ReceiveMail rm = LocalStore.getReceiveFolder(LocalStore.DRAFTS);
			rm.append(new Message [] { mail.getMessage() });
		}
	}


	
	private void receiveMail()
		throws Exception
	{
		receiveMail(null);
	}
	
	private void receiveMail(String urlName)
		throws Exception
	{
		ReceiveMail receiveMail;
		String line = "";
		String proto = urlName;

		if (urlName == null)	{
			String defProto = mailProperties.getProperty("mail.store.protocol");
			getOut().print("Protocol (pop3|imap, default "+defProto+"): ");
			line = readLine();
			
			proto = line.length() <= 0 ? defProto : line;
			Properties receiveProps = adaptPropertiesToProtocol((Properties)mailProperties.clone(), proto);
				
			receiveMail = new ReceiveMail(receiveProps, new SilentAuthenticator(mailProperties, receivePassword));
		}
		else	{
			receiveMail = new ReceiveMail(urlName);
			LocalStore.setUrl(urlName);
		}

		getOut().println("__________________________________________________________________________");
		getOut().println("Commands are now:");
		getOut().println("	m[essages]             list messages within current folder");
		getOut().println("	f[olders]              list folders within current folder");
		getOut().println("	g[et] <number>         display message with given list number");
		getOut().println("	s[ave] <number>        store message with given list number to a file or to local store");
		getOut().println("	d[elete] <number ...>  delete message(s) with given number(s)");
		getOut().println("	pwd                    print name of current folder");
		getOut().println("	cd <folder>            change to given folder");
		getOut().println("	cdup                   change to parent folder");
		getOut().println("	q[uit]                 finish receive session.");
		getOut().println("__________________________________________________________________________");

		getOut().println("Current folder is: "+receiveMail.pwd().getName());
		
		while (line.startsWith("q") == false)	{
			try	{
				getOut().print(proto+"> ");
				line = readLine();
				String [] tokens = tokenize(line);
				line = tokens.length > 0 ? tokens[0] : "";
				
				if (line.equals("pwd"))	{		// print current folder
					getOut().println(receiveMail.pwd().getName());
				}
				else
				if (line.equals("cd"))	{		// change folder
					String dir = null;
					if (tokens.length > 1)
						dir = tokens[1];
					if (dir != null && dir.equals(".."))
						receiveMail.cdup();
					else
						receiveMail.cd(dir);
					getOut().println(receiveMail.pwd().getName());
				}
				else
				if (line.equals("cdup"))	{		// change to parent folder
					receiveMail.cdup();
					getOut().println(receiveMail.pwd().getName());
				}
				else
				if (line.startsWith("f"))	{		// list folders
					receiveMail.folders(new OutputListVisitor());
				}
				else
				if (line.startsWith("m") || line.startsWith("l"))	{		// list messages
					receiveMail.messages(new OutputListVisitor());
				}
				else
				if (line.startsWith("g"))	{		// read message
					int i = tokens.length > 1 ? Integer.parseInt(tokens[1]) : 0;

					getOut().print("Loop message parts? (y|n, default y) ");
					String ret = readLine();
					if (ret.equals("n") == false)
						receiveMail.messageParts(i, new PartsVisitor());
					else
						receiveMail.get(i, new OutputMessageVisitor());
				}
				else
				if (line.startsWith("d"))	{		// delete message(s)
					if (tokens.length > 1)	{
						int [] iarr = new int [tokens.length - 1];
						for (int i = 1; i < tokens.length; i++)	{
							iarr[i - 1] = Integer.parseInt(tokens[i]);
						}
						getOut().print("Really delete message(s)? (y|n, default y) ");
						String ret = readLine();
						if (ret.equals("n") == false)
							receiveMail.delete(iarr);
					}
					else	{
						getOut().print("Really delete ALL messages? (y|n, default n) ");
						String ret = readLine();
						if (ret.equals("y"))
							receiveMail.delete((int[])null);
					}
				}
				else
				if (line.startsWith("s"))	{		// save message(s)
					if (tokens.length > 1)	{
						int i = Integer.parseInt(tokens[1]);
						receiveMail.get(i, new SaveMessageVisitor());
					}
					else	{
						receiveMail.messages(new SaveAllMessagesVisitor());
					}
				}
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}	// end while input
		
		receiveMail.close();
	}


	private Properties adaptPropertiesToProtocol(Properties mailProperties, String proto)	{
		String host = mailProperties.getProperty("mail.pop3.host");
		String port = mailProperties.getProperty("mail.pop3.port");
		String user = mailProperties.getProperty("mail.pop3.user");
		
		mailProperties.remove("mail.pop3.host");
		mailProperties.remove("mail.pop3.port");
		mailProperties.remove("mail.pop3.user");
		
		mailProperties.setProperty("mail.store.protocol", proto);

		mailProperties.setProperty("mail."+proto+".host", host);
		if (port != null) mailProperties.setProperty("mail."+proto+".port", port);
		mailProperties.setProperty("mail."+proto+".user", user);
		
		return mailProperties;
	}


	private String getSaveFileName(boolean defaultSaveToLocalStore)	// returns null if canceled, "" if overwrite was denied
		throws IOException
	{
		getOut().print("Save to file"+(defaultSaveToLocalStore ? " (ENTER for saving to local store)" : "")+": ");
		String file = readLine();

		if (file.length() <= 0)	{	// nothing given
			return null;
		}
			
		if (new File(file).exists())	{
			getOut().print("Overwrite "+file+" ? (y|n, default y) ");
			String ret = readLine();
			if (ret.equals("n"))	{
				return getSaveFileName(defaultSaveToLocalStore);	// try again
			}
		}
		
		return file;
	}
	
	



	private class OutputListVisitor implements ReceiveMail.MailVisitor
	{
		public void folder(int count, int nr, Folder f)	{
			getOut().println(f.getName());
		}

		public void message(int count, int nr, Message msg)	{
			try	{
				getOut().println("["+nr+"]\t"+ df.format(msg.getSentDate()) +"\t"+ msg.getFrom()[0] +"\t"+ msg.getSubject());
			}
			catch (MessagingException e)	{
				e.printStackTrace();
			}
		}
	}



	private class OutputMessageVisitor implements ReceiveMail.MailVisitor
	{
		public void folder(int count, int nr, Folder f)	{
		}

		public void message(int count, int nr, Message msg)	{
			try	{
				msg.writeTo(getOut());
				
				if (MessageUtil.isNewMessage(msg))	{
					msg.setFlag(Flags.Flag.SEEN, true);
					new SaveMessageVisitor().message(1, 1, msg);	// save if recently received (new)
				}
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
	}



	private class SaveMessageVisitor implements ReceiveMail.MailVisitor
	{
		public void folder(int count, int nr, Folder f)	{
		}

		public void message(int count, int nr, Message msg)	{
			FileOutputStream fout = null;
			try	{
				String file = getSaveFileName(true);

				if (file == null)	{	// nothing given, save to local store
					ReceiveMail rm = LocalStore.getReceiveFolder(LocalStore.INBOX);
					rm.append(new Message [] { msg });
					getOut().println("Saved message to local inbox in "+LocalStore.getUrl());
				}
				else	{
					fout = new FileOutputStream(file);
					msg.writeTo(fout);	// actually save the message
					getOut().println("Saved message to file: "+file);
				}
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
			finally	{
				try	{ fout.close(); }	catch (Exception e)	{}
			}
		}
	}



	private class SaveAllMessagesVisitor implements ReceiveMail.AllMessagesVisitor
	{
		public void messages(Message [] msgs)	{
			try	{
				ReceiveMail rm = LocalStore.getReceiveFolder(LocalStore.INBOX);
				rm.append(msgs);
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
	}


	
	private class PartsVisitor implements ReceiveMail.MessagePartVisitor
	{
		PartsVisitor()	{
			getOut().println("__________________________________________________________________________");
			getOut().println(  "Message commands are:");
			getOut().println(  "	ENTER  ... show next part");
			getOut().println(  "	s      ... save part to a file");
			getOut().println(  "	p      ... print part to screen");
			getOut().println("__________________________________________________________________________");
		}
		
		public void multiPart(int absolutePartNumber, int treeLevel, Part part)
			throws Exception
		{
			dumpPart(absolutePartNumber, treeLevel, part);
		}
		
		public void finalPart(int absolutePartNumber, int treeLevel, Part part)
			throws Exception
		{
			dumpPart(absolutePartNumber, treeLevel, part);
			savePart(part);
		}
		
		private void savePart(Part part)
			throws Exception
		{
			getOut().print("message> ");
			String ret = readLine();

			if (ret.startsWith("s"))	{
				String file = getSaveFileName(false);

				if (file == null)	// nothing given
					return;

				InputStream in;
				
				if (part.isMimeType("text/html"))	{
					String s = "<html><head></head><body>"+(String)part.getContent()+"</body></html>";
					in = new ByteArrayInputStream(s.getBytes());
				}
				else
				if (part.isMimeType("text/plain"))	{
					in = new ByteArrayInputStream(((String)part.getContent()).getBytes());
				}
				else	{
					in = part.getInputStream();
				}
				
				if (in != null)	{
					FileOutputStream fout = new FileOutputStream(file);
					byte[] buf = new byte[4096];
					int len;
					while ((len = in.read(buf)) != -1)	{
						fout.write(buf, 0, len);
					}
					fout.flush();
					fout.close();
					in.close();
				}
				else	{
					getOut().println("ERROR: no input stream available.");
				}
			}
			else
			if (ret.startsWith("p"))	{
				getOut().println("__________________________________________________________________________");
				part.writeTo(getOut());
				getOut().println();
				getOut().println("__________________________________________________________________________");
			}
			else
			if (ret.startsWith("q"))	{
				throw new Exception("User canceled.");
			}
		}
		
		private void dumpPart(int absolutePartNumber, int treeLevel, Part part)
			throws Exception
		{
			String mimeType = part.getContentType();
			int idx = mimeType.indexOf(";");
			if (idx > 0)
				mimeType = mimeType.substring(0, idx);
			
			getOut().println(spaces(treeLevel)+
					"Part "+absolutePartNumber+
					": Content-Type="+mimeType+
					", Size="+part.getSize()+
					", Filename="+part.getFileName()+
					", Disposition="+part.getDisposition()+
					", Content-ID="+part.getHeader("Content-ID")+
					", Message-ID="+part.getHeader("Message-ID")+
					", Description="+part.getDescription()+
					", Part-Class="+part.getClass().getName()+
					", Content-Class="+part.getContent().getClass().getName());
		}
		
		private String spaces(int cnt)	{
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < cnt; i++)
				sb.append("    ");
			return sb.toString();
		}
	}




	// simple authenticator object knowing mail properties and store password. */

	private class SilentAuthenticator extends Authenticator
	{
		private Properties props;
		private String password;
		
		SilentAuthenticator(Properties props, String password)	{
			this.props = props;
			this.password = password;
		}
	
		protected PasswordAuthentication getPasswordAuthentication()	{
			String user = props.getProperty("mail.pop3.user");
			return new PasswordAuthentication(user, password);
		}
	}





	/** Mail console application main. */
	public static void main(String [] args)	{
		try	{
			String sendHost = null;
			String receiveHost = null;
			String user = null;
			String password = null;
	
			if (args.length < 1)	{
				System.err.println("SYNTAX: "+MailConsole.class.getName()+" SMTP-host[:port] [POP-host[:port] POP-user [POP-password]]]");
			}
			else	{
				sendHost = args.length >= 1 ? args[0] : null;
				receiveHost = args.length >= 2 ? args[1] : null;
				user = args.length >= 3 ? args[2] : null;
				password = args.length >= 4 ? args[3] : null;
				
				if (sendHost != null && receiveHost == null)
					receiveHost = sendHost;
			}
			
			new MailConsole(sendHost, receiveHost, user, password).start();
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
	}
	
}