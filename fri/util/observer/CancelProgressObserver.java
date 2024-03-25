package fri.util.observer;

/**
 * The responsibility of an observer that gets its action label and
 * processing size by constructor. This is normally implemented
 * by some GUI dialog that shows labels and progress in a progress bar.
 */
public interface CancelProgressObserver
{
	/**
		Used by the observed object to ask for user cancel. 
		@return true if observed object should end interrupted
	*/
	public boolean canceled();

	/**
		Used by the observed object to tell the observer about done work.
		@param portion e.g. written bytes
	*/
	public void progress(long portion);

	/**
		Used when the observed object changes.
		@param note the name of the new node. 
	*/
	public void setNote(String note);

	/**
		The implementer is required to dispose the progress dialog.
		This can be caused by both an error or the successful end of the action.
		It is recommended to use a <i>finally</i> clause for this call.
	*/
	public void endDialog();

}
