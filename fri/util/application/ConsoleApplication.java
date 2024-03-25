package fri.util.application;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
	An abstract console application. Methods to invoke must be implemented
	and get called by reflection. If no method by the name of the first
	word of the commandline is found, a method "passthrough" gets tried
	(for directly launching some protocol).
	
	@author Fritz Ritzberger, 2003
*/

public abstract class ConsoleApplication
{
	private String name;
	protected String prompt;
	private boolean exit;
	protected String workingDirectory = System.getProperty("user.dir");

	
	/**
		Start a console application by looping for input until "exit" or "quit".
	*/
	public ConsoleApplication(String name)	{
		this(name, "");
	}
	
	/**
		Start a console application by looping for input until "exit" or "quit".
	*/
	public ConsoleApplication(String name, String prompt)	{
		this.name = name;
		this.prompt = prompt+"> ";
	}
	
	/** Returns the prompt+"> " that was passed with constructor. */
	public String getPrompt()	{
		return prompt;
	}
	
	/** Starts user input loop. */
	public void start()	{
		getOut().println("This is "+name+", on Java "+System.getProperty("java.version"));
		getOut().println("Local path is "+workingDirectory);
		
		init();
		
		do	{
			try	{
				String [] args;
				String cmd;
				
				getOut().print(prompt);
				String line = readLine();
				
				if (line.length() <= 0)	// do not accept empty line
					continue;
				
				// split commandline
				int sep = line.indexOf(" ");
				if (sep > 0)	{
					cmd = line.substring(0, sep);
					String arg = line.substring(sep + 1);
					
					args = tokenize(arg);
				}
				else	{
					cmd = line;
					args = new String [0];
				}

				// check command
				if (checkCommand(cmd, args))	{
					Method m;
					Class [] argClasses = new Class [] { String[].class };
					Object [] arguments = new Object [] { args };
					
					try	{
						m = getClass().getMethod(cmd, argClasses);
					}
					catch (NoSuchMethodException ex)	{
						m = getClass().getMethod("passthrough", argClasses);
						
						String [] newArgs = new String [args.length + 1];
						newArgs[0] = cmd;
						System.arraycopy(args, 0, newArgs, 1, args.length);
						arguments = new Object [] { newArgs };
					}
					
					m.setAccessible(true);
					m.invoke(this, arguments);
				}
			}
			catch (Throwable e)	{
				e = e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException() : e;
				handleException(e);
			}
		}
		while (exit == false);
	}


	protected String [] tokenize(String arg)	{
		StringTokenizer stok = new StringTokenizer(arg);
		int len = stok.countTokens();
		String [] args = new String[len];
		for (int i = 0; stok.hasMoreTokens(); i++)
			args[i] = stok.nextToken();
			
		return args;
	}
	
	

	/** Called at start after printing application information (working directory, Java version, ...). */
	protected void init()	{
	}

	/** Override to deny the execution of an entered command. Called at every commandline dispatch. */
	protected boolean checkCommand(String cmd, String [] args)	{
		return true;
	}

	/** Any Throwable from commandline execution is passed to this method, which prints a stack trace. */
	protected void handleException(Throwable e)	{
		e.printStackTrace();
	}


	/** Returns a line from console input, removes leading and trailing spaces. */
	protected String readLine()
		throws IOException
	{
		return getIn().readLine().trim();
	}


	protected PrintStream getOut()	{
		return System.out;
	}

	protected PrintStream getErr()	{
		return System.err;
	}

	protected BufferedReader getIn()	{
		return new BufferedReader(new InputStreamReader(System.in));
	}


	/** Simply prints 'command not implemented: "+args[0]' to out-PrintStream. */
	public void passthrough(String [] args) throws Exception	{
		getOut().println("Command not implemented: "+args[0]);
	}



	/** Called by "quit". Override and return true if some remote connection is active. */
	protected boolean needsCleanup()	{
		return false;
	}
	
	/** Called by "quit" when <i>needsCleanUp()</i> returned true. Override to disconnect some remote connection. */
	protected void cleanup() throws Exception	{
	}
	
	/**
		Default "quit" command verb implementation. Calls <i>needsCleanup()</i> and <i>cleanup()</i> if was true.
		Ends execution if was not connected or was connected and user confirms termination.
		Does NOT perform <i>System.exit()</i>!
	*/
	public void quit(String [] args) throws Exception	{
		boolean connected = needsCleanup();
		
		if (connected)
			cleanup();
		
		exit = true;
		
		if (connected && args != null)	{	// was connected and called internally, confirm exit
			getOut().print("Exit? (y/n, y) ");
			String s = readLine();
			if (s.toLowerCase().equals("n"))
				exit = false;
		}
	}

	/** Default command verb "exit", calls "quit". */
	public void exit(String [] args) throws Exception	{
		quit(null);
	}

}