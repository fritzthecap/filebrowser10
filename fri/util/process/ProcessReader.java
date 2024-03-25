package fri.util.process;

/**
 * The implementer receives messages from a ProcessManager.
 */
public interface ProcessReader
{
	/** Receive some stderr text from ProcessManager. */
	public void printlnErr(String line);
	
	/** Receive some stdout text from ProcessManager. */
	public void printlnOut(String line);
	
	/** Receive the exitcode after process termination from ProcessManager. */
	public void exitcode(int ec);
	
	/** Receive execution progress from ProcessManager. */
	public void progress();

}
