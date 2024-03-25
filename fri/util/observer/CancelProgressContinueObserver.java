package fri.util.observer;

public interface CancelProgressContinueObserver extends CancelProgressObserver
{
	/**
		Prompt the user for continue confirmation. 
	*/
	public boolean askContinue(String msg);
}