package fri.util.process;

import java.io.*;

/**
 * The implementer can write to an external Process and receives messages.
 */
public interface ProcessWriter
{
	/**
	 * Gets called periodically by ProcessManager.
	 * Returning true closes the process output stream.
	 * @param out process output stream to write to.
	 * @return false causes the stream to get closed.
	*/
	public boolean write(OutputStream out);

	/**
	 * Called when process exited.
	 * @param i exitcode of process.
	*/
	public void exited(int i);

	/**
	 * Called periodically to signalize that the process is alive and working.
	 */
	public void progress();

	/**
	 * Called at start when the command could not be executed.
	 */
	public void notfound();

	/**
	 * Called when the process was stopped programmatically (by user).
	 */
	public void userstopped();

	/**
	 * Should return false when ProcessWriter will not accept any of exited(),
	 * progress(), write() or userstopped() calls.
	 */
	public boolean ready();
}