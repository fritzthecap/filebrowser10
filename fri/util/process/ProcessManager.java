package fri.util.process;

import java.io.*;
import fri.util.ArrayUtil;

/**
 * Starts a Process executing passed commandline (which can not contain pipes!).
 * Interacts with caller by passed interfaces. The running Process can be aborted.
 * The speed of processing in, out and err can be controlled by <i>setMillis()</i>.
 * This manager avoids blocking the running Process by handling input after every
 * written output byte.
 */
public class ProcessManager implements Runnable
{
	/**
	 * Maximum value for <i>setMillis()</i>. When exceeded, the ProcessManager
	 * will suspend and must be resumed by calling <i>setMillis()</i> with a lower value.
	 * This constant should be used for a speed slider's maximum value.
	 */
	public static final int SLEEPMAX = 1000;

	/**
	 * Default delay value is 10 (execution speed).
	 */
	public static final int MILLIS = 10;

	/**
	 * Read buffer size.
	 */
	private int BUFSIZE = 512;

	private int delayMillis = MILLIS;	// current delay value
	private boolean userStop = false;
	private Process process;
	private ProcessWriter pwriter;
	private ProcessReader preader;
	private String text;	// currently read text from Process
	private byte [] buffer = new byte [BUFSIZE];
	private boolean outOpen, inOpen, errOpen;
	private InputStream in;
	private InputStream err;
	private OutputStream out;


	/**
	 * Starts a Process with passed commandline and environment.
	 * @param processWriter called for output to the started process, can be null.
	 * @param processReader called with input from the started process, can not be null.
	 * @param commandLine the command to execute, can not be null.
	 * @param environment environment for <i>Runtime.getRuntime().exec(cmd, env)</i>, can be null.
	 * @param workingDirectory the directory where the process should be started, can be null.
	 */
	public ProcessManager (
		ProcessWriter processWriter,
		ProcessReader processReader,
		String [] commandLine,
		String [] environment,
		File workingDirectory)
	{
		this.pwriter = processWriter;
		this.preader = processReader;

		try {
			System.err.println("ProcessManager execute command tokens: "+ArrayUtil.print(commandLine));
			System.err.println("... in working directory: "+workingDirectory);

			process = createProcess(commandLine, environment, workingDirectory);

			Thread thread = new Thread(this);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		catch (IOException e)	{
			System.err.println("execution error was: "+e.toString());
			processReader.printlnErr(e.toString());
			processReader.exitcode(-1);
			
			if (processWriter != null)
				processWriter.notfound();
		}
	}


	private Process createProcess(String [] cmd, String [] env, File workingDirectory)
		throws IOException
	{
		if (workingDirectory != null)
			if (cmd.length == 1)
				return Runtime.getRuntime().exec(cmd[0], env, workingDirectory);
			else
				return Runtime.getRuntime().exec(cmd, env, workingDirectory);
		else
			if (cmd.length == 1)
				return Runtime.getRuntime().exec(cmd[0], env);
			else
				return Runtime.getRuntime().exec(cmd, env);
	}



	/** Returns true when output can be written to the process. */
	public synchronized boolean isInputOpen()	{
		return outOpen;
	}

	/** Hard process termination: <i>process.destroy()</i>. */
	public synchronized void exitStop()	{
		stop();
		if (process != null)
			process.destroy();
	}

	/** Soft process termination, sets stop flag. */
	public synchronized void stop()	{
		userStop = true;
		notify();	// if waiting
	}
	
	/** Suspends stream processing. */
	public synchronized void suspend()	{
		System.err.println("ProcessManager waiting for resume ...");
		try	{ wait(); }	// for notify from this object
		catch (InterruptedException e)	{}
		System.err.println("... ProcessManager finished waiting.");
	}

	/** Resumes stream processing. */
	public synchronized void resume()	{
		notify();	// this object's thread
	}

	/**
	 * Sets the delay value (stream processing speed).
	 * Will suspend execution when passed millis are higher than SLEEPMAX.
	 * Else will resume when millis were set higher than SLEEPMAX in a previous call.
	 */
	public synchronized void setDelayMillis(int millis)	{
		System.err.println("ProcessManager.setMillis: "+millis);
		if (this.delayMillis >= SLEEPMAX && millis < SLEEPMAX)
			resume();
		this.delayMillis = millis;
	}

	/** Returns the current delay value (stream processing speed). */
	public synchronized int getDelayMillis()	{
		return delayMillis;
	}



	/** Public to implement Runnable. Do not call. Manages the started Process as thread. */
	public void run()	{
		int exitcode = -1;
		boolean exited = false;

		// get streams from process
		synchronized(this)	{
			in = process.getInputStream();	// BufferedInputStream on WINDOWS
			inOpen = in != null;

			err = process.getErrorStream();	// FileInputStream on WINDOWS
			errOpen = err != null;

			if (pwriter != null)	{	// allocate a tolerant stream for writing to process
				out = process.getOutputStream();	// BufferedOutputStream on WINDOWS
				outOpen = out != null;
				if (outOpen)
					out = new PipeOutputStream(out);	// BufferedOutputStream on WINDOWS
			}
			else	{
				outOpen = false;
			}
		}

		// process communication loop
		while (stopped() == false && (exited == false || inOpen || errOpen))	{
			callProgress();	// report being alive

			doOutInErr(exited);

			if (false == exited)	{
				try	{	// look if process exited
					exitcode = process.exitValue();
					System.err.println("got exit code "+exitcode);
					exited = true;	// not reached if exception
				}
				catch (IllegalThreadStateException e1)	{	// happens when process has not yet exited
				}
				
				if (exited == false)
					delay();	// consider execution speed
			}
		}

		if (stopped() == true)	{	// was stopped by user
			System.err.println("process was stopped by user");
			
			process.destroy();
			try	{ exitcode = process.exitValue(); } catch (Exception e)	{}
			
			closeStreams();
			
			if (pwriter != null && pwriter.ready())
				pwriter.userstopped();
		}
		else	{	// process exited regular
			System.err.println("process exited regular with "+exitcode);

			preader.exitcode(exitcode);
			if (pwriter != null && pwriter.ready())
				pwriter.exited(exitcode);
		}
	}


	private synchronized boolean stopped()	{
		return userStop;
	}
	
	private void doOutInErr(boolean exited)	{	// called by communication loop
		doOut();
		doInErr(exited);
	}

	private synchronized void doOut()	{
		if (outOpen && pwriter.ready())	{
			if (pwriter.write(out) != true)	{
				try	{
					System.err.println("closing output to process (as writer returns false)");
					out.flush();
					out.close();
				}
				catch (IOException e)	{
				}
				outOpen = false;	// schreiben beendet wegen Fehler
			}
		}
	}

	private synchronized void doInErr(boolean exited)	{
		if (inOpen)	{
			inOpen = readInput(in, exited);
			callPrint(text, false);
		}
		
		if (errOpen)	{
			errOpen = readInput(err, exited);
			callPrint(text, true);
		}
	}

	private boolean readInput(InputStream input, boolean exited)	{
		this.text = null;
		try	{
			int i = input.available();
			if (i <= 0 && exited == false)	// if process did not exit, do not force blocking read
				return true;
			
			// read input (blocking), either because bytes are available or process has exited
			int cnt = input.read(buffer, 0, buffer.length);
			if (cnt == -1)	{	// end of input reached
				input.close();	// close the input stream
				return false;
			}
			
			if (cnt > 0)	// make String from input
				this.text = new String(buffer, 0, cnt);

			return true;
		}
		catch (IOException e)	{
			e.printStackTrace();
			try	{ input.close(); }	catch (Exception e2)	{}
			return false;
		}
	}

	private void delay()	{
		Thread.yield();
		int millis = getDelayMillis();
		if (millis > 0)	{
			if (millis >= ProcessManager.SLEEPMAX)	{
				suspend();
			}
			else	{
				try	{ Thread.sleep(millis); }
				catch (InterruptedException e)	{}
			}
		}
	}

	private void callPrint(String line, boolean err)	{
		if (line != null && stopped() == false)
			if (err)
				preader.printlnErr(line);
			else
				preader.printlnOut(line);
	}

	private void callProgress()	{
		if (stopped() == false)	{
			preader.progress();
			if (pwriter != null && pwriter.ready())
				pwriter.progress();
		}
	}

	private synchronized void closeStreams()	{
		if (outOpen)	{
			try	{ out.close(); } catch (Exception e)	{}
			outOpen = false;
		}
		if (inOpen)	{
			try	{ in.close(); } catch (Exception e)	{}
			inOpen = false;
		}
		if (errOpen)	{
			try	{ err.close(); } catch (Exception e)	{}
			errOpen = false;
		}
	}




	/*
	 * Processes input after each written byte to avoid in/out blocking.
	 * Is a WINDOWS workaround to avoid blocking at byte 1026 when process output is not read.
	 */
	private class PipeOutputStream extends FilterOutputStream
	{
		private int written;
		
		PipeOutputStream(OutputStream out)	{
			super(out);
		}
		
		/** Overridden to try to process input after each output byte. */
		public synchronized void write(int b)
			throws IOException
		{
			super.write(b);	// write to the real stream
			
			doInErr(false);	// try to get input from process, else it could block
			
			written++;
			
			if (written >= BUFSIZE)	{
				written = 0;
				callProgress();
				delay();
			}
		}
	}
	
}
