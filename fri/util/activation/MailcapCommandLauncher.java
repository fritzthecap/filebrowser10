package fri.util.activation;

import java.io.*;
import java.util.*;
import javax.activation.*;

/**
	Rudimentary implementation of mailcap logic.
	<ul>
		<li>platform command lines are executed asynchronously
			</li>
		<li>"test"-commands are executed to find the right command
			</li>
		<li>Options "needsterminal" and "copiousoutput" are considered
			and when set, command is executed in an Java Window.
			The Java class of that terminal window can be configured by
			System-Property "fri.util.activation.TerminalClass".
			It must provide a main-method that takes the first argument
			as the "commandline to execute".
			If no terminal is configured, "fri.gui.swing.commandmonitor.CommandMonitor"
			is loaded.
			</li>
		<li>No variable substitution than "%s" (absolute file path)is implemented
			</li>
		<li>Pipe commands are not interpreted by Java, and not by UNIX exec() call.
			Command `substitutions` are not interpreted by Java, and not by UNIX exec() call.
			So the exitcode will be FALSE for such commands, or they will not be executed.
			E.g. "test `echo %{charset} | tr [A-Z] [a-z]` = iso-8859-1" will fail for sure,
			and "cat %s | more", too.
			</li>
	</ul>

	This class is referenced in MailcapFile.java to set the Java Bean
	in relation with the "open" command (hardcoded). It can execute mailcap
	commandlines that hold no "|" (pipe).
	<p />
	Set the terminal command using '-Dfri.util.activation.Terminal=xterm' in Java commandline.
	Default this is 'xterm -hold -e commandline'.
*/

public class MailcapCommandLauncher implements CommandObject
{
	/** Implementing CommandObject: executing mailcap commandline. */
	public void setCommandContext(String verb, DataHandler dh)
		throws IOException
	{
		System.err.println("MailcapCommandLauncher.setCommandContext "+verb+", datahandler "+dh);
		if (dh == null)
			throw new IllegalArgumentException("DataHandler is null");

		DataSource dataSource = dh.getDataSource();
		
		String path;
		if (dataSource instanceof FileDataSource)
			path = ((FileDataSource)dh.getDataSource()).getFile().getAbsolutePath();
		else
		if (dataSource instanceof URLDataSource)
			path = ((URLDataSource)dh.getDataSource()).getURL().toExternalForm();
		else
			throw new IOException("Only FileDataSource or URLDataSource accepted to MailcapCommandLauncher.setCommandContext(): "+dataSource);
		
		// find out all commandlines
		MailcapCommandMap map = (MailcapCommandMap) CommandMap.getDefaultCommandMap();	// FRi 2004-11-09: ClassCastException on .doc file

		String mimeType = dh.getContentType();
		int idx;	// need to shorten the MIME type to get a .mailcap match
		if (mimeType != null && (idx = mimeType.indexOf(";")) > 0)
			mimeType = mimeType.substring(0, idx).trim();
		
		Vector cmds = map.getMailcapCommandList(mimeType);
		System.err.println("MailcapCommandLauncher, got commands: "+cmds);

		if (cmds == null)	{
			throw new IOException("MailcapCommandLauncher: No commands defined for "+mimeType);
		}

		// TODO: prefer commands where needsTerminal is false
		
		// loop over e.g. "text/plain" and "text/*"
		for (Enumeration e = cmds.elements(); e.hasMoreElements(); )	{
			Vector v = (Vector)e.nextElement();
			System.err.println("MailcapCommandLauncher, got command-list: "+v);

			String cmdLine = map.getMailcapCommandLine(v);	// e.g. "cat %s"
			String testCmd = map.getMailcapCommandTest(v);	// e.g. "test -n $DISPLAY"
			Vector options = map.getMailcapCommandOptions(v);	// e.g. "needsterminal", "copiousoutput"

			// first execute test command if existing
			if (testCmd != null)	{
				System.err.println("MailcapCommandLauncher, executing test \""+testCmd+"\"");
				Process p = Runtime.getRuntime().exec(testCmd);
				try	{
					p.waitFor();
				}
				catch (InterruptedException ex)	{
					ex.printStackTrace();
				}
				int exit = p.exitValue();
				if (exit != 0)	{
					System.err.println("MailcapCommandLauncher, test failed, exit: "+exit);
					continue;	// test was negative, do not take this command
				}
				System.err.println("MailcapCommandLauncher, test succeeded, exit: "+exit);
			}

			// command test was OK or not existent, execute command according to options
			// first substitute "%s" with file name
			int i;
			String [] search = new String [] { "'%s'", "%s" };	// both are possible in UNIX mailcap
			for (int j = 0; j < search.length; j++)
				while ((i = cmdLine.indexOf(search[j])) > 0)
					cmdLine = cmdLine.substring(0, i) + path + cmdLine.substring(i + search[j].length());

			// test options
			boolean needsTerminal = options != null && options.indexOf("needsterminal") >= 0;
			boolean copiousoutput = options != null && options.indexOf("copiousoutput") >= 0;

			if (needsTerminal)	{	// call a terminal window for the command
				try	{	// get a terminal
					String s = System.getProperty("fri.util.activation.Terminal");
					String cmd = (s != null && s.length() > 0 ? s : "xterm -hold -e") + " " + cmdLine;
					System.err.println("MailcapCommandLauncher, executing terminal \""+cmd+"\"");
					Runtime.getRuntime().exec(cmd);
					return;
				}
				catch (Exception ex)	{
					ex.printStackTrace();
				}
			}
			else
			if (copiousoutput)	{	// catch output and try to dispatch it to another command
				// assume it is a decompress command, make a new file for output
				// ... this might not work with URLs!
				
				// make a name for a temporary file
				String name = dataSource.getName();
				while (name.startsWith("/"))	// remove leading slashes
					name = name.substring(1);
				
				int j = name.lastIndexOf(".");	// cut ".gz" from "xxx_ps.gz"
				if (j > 0)	{
					name = name.substring(0, j);	// is now "xxx_ps"
					j = name.lastIndexOf("_");	// "." might have been replaced by "_" to match some MIME extension
					if (j > 0 && j < name.length() - 1)
						name = name.substring(0, j)+"."+name.substring(j + 1);	// is now "xxx.ps"
				}
				else	{
					name = name+".txt";
				}
				
				// execute the command and write output to new file
				System.err.println("MailcapCommandLauncher, executing copiousoutput \""+cmdLine+"\"");
				Process p = Runtime.getRuntime().exec(cmdLine);	// start converter process
				InputStream in = p.getInputStream();	// write conversion result to tmp file
				File tmpFile = new File(System.getProperty("java.io.tmpdir"), name);
				tmpFile = StreamToTempFile.createUniqueFile(tmpFile);
				OutputStream out = new FileOutputStream(tmpFile);
				try	{
					int c;
					while ((c = in.read()) != -1)
						out.write(c);
				}
				finally	{
					out.close();
				}
				tmpFile.deleteOnExit();

				// create new activation arguments and try with tmp file as source
				DataSource ds = new FileDataSource(tmpFile);
				DataHandler dhNew = new DataHandler(ds);
				setCommandContext(verb, dhNew);
				return;
			}
			else	{	// might be a a GUI application
				System.err.println("MailcapCommandLauncher, executing command \""+cmdLine+"\"");
				Runtime.getRuntime().exec(cmdLine);
				return;
			}
		}	// end for all possible commands
	}


	/** test main */
	public static void main(String [] args)
		throws Exception
	{
		/*{
			MailcapCommandMap map = new MailcapCommandMap();	//MailcapCommandMap.class.getResourceAsStream("mailcap.txt"));
			CommandMap.setDefaultCommandMap(map);
			FileDataSource fds = new FileDataSource("Test.txt"); 
			DataHandler dh = new DataHandler(fds); 			
			CommandInfo ci = dh.getCommand("open");
			if (ci != null)
				System.err.println("Bean is: "+dh.getBean(ci));
		}*/
		/*{
			CommandMap.setDefaultCommandMap(new MailcapCommandMap());
			URLDataSource ds = new URLDataSource(new java.net.URL("http://www.google.com/index.html")); 
			System.err.println("DataSource name is: "+ds.getName()+", URL "+ds.getURL());
			DataHandler dh = new DataHandler(ds); 			
			CommandInfo ci = dh.getCommand("open");
			if (ci != null)
				ci.getCommandObject(dh, null);
		}*/
	}

}