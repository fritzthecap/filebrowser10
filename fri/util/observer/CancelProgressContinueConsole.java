package fri.util.observer;

import java.io.*;

/**
	Console dialog that implements CancelProgressContinueObserver,
	used by FileSplit and FileJoin.
	<p>
	This console dialog translates "n" to "no",
	"yy" to "yes to all" and "nn" to "no to all".
	Everything else is interpreted as "yes".
*/

public class CancelProgressContinueConsole implements
	CancelProgressContinueObserver
{
	private long progress = 0;
	private boolean doInput = true;
	private boolean foreverTrue = false, foreverFalse = false;


	/**
		Creates a console dialog that accepts input when askContinue() is called.
	*/
	public CancelProgressContinueConsole()	{
	}

	/**
		Creates a console dialog.
		@param doInput accepts on askContinue() only if doInput is true.
	*/
	public CancelProgressContinueConsole(boolean doInput)	{
		this.doInput = doInput;
	}

	
	/** Implements CancelProgressObserver and returns false. */
	public boolean canceled()	{
		return false;
	}

	/** Implements CancelProgressObserver and prints progress to System.err. */
	public void progress(long portion)	{
		progress += portion;
		System.err.println("	progress is: "+progress);
	}

	/** Implements CancelProgressObserver and prints note to System.err. */
	public void setNote(String note)	{
		System.err.println(note);
	}

	/** Implements CancelProgressObserver and prints "finished" to System.err. */
	public void endDialog()	{
		System.err.println("... finished.");
	}

	/** Implements CancelProgressContinueObserver and reads keyboard input. */
	public boolean askContinue(String msg)	{
		if (doInput && foreverTrue == false && foreverFalse == false)	{
			System.err.print(msg+" ");	// +" (y|n|yy|nn): "

			try	{
				String s = new BufferedReader(new InputStreamReader(System.in)).readLine();

				if (s != null && s.trim().equals("n"))	{
					System.err.println("no");
					return false;
				}
				
				if (s != null && s.trim().equals("yy"))	{
					foreverTrue = true;
				}
				else
				if (s != null && s.trim().equals("nn"))	{
					foreverFalse = true;
				}
			}
			catch (IOException e)	{
				return false;
			}
		}
		// TODO: implement console dialog
		boolean ret = foreverFalse ? false : true;
		System.err.println(ret ? "yes" : "no");
		return ret;
	}
	
}