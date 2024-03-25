package fri.util.application;

public interface Closeable
{
	/**
		Close this frame window belonging to the current process.
		@return true if close was successful, false if user interrupts.
	*/
	public boolean close();
}
