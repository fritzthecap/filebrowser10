package fri.util.observer;

/**
	Observable objects implement the method to accept an observer.
*/

public interface CancelProgressObservable
{
	/**
		Used to set the observer into observable. 
	*/
	public void setObserver(CancelProgressObserver observer);
}