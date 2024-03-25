package fri.util.error;

import java.util.List;

/**

	Rendering errors independent of the environment (GUI, console, files, ...).
	Several categories of errors are described. They can be implemented by
	a application-bound object. The static Err implementation can be the
	anchor implementation for global calls, where you set your error handler
	by calling "Err.setHandler(myErrorHandler)".

	@author  Ritzberger Fritz
*/

public interface ErrorHandler
{
	/**
		Programmers fault like "this section is illegal under that condition"
		@return false if class "Err" should do the work when calling "Err.debug(msg);"
	*/
	public boolean debug(String e);

	/**
		OS System fault, terminate application!
		@return false if class "Err" should do the work when calling "Err.fatal(msg);"
	*/
	public boolean fatal(String e);

	/** 
		application fault, exceptions, show to user.
		Use this like "error(new Exception("sorry, i was mean ..."));"
		@return false if class "Err" should do the work when calling "Err.error(msg);"
	*/
	public boolean error(Throwable e);
	
	/**
		System or application abnormal, do not bother the user but log it.
		@return false if class "Err" should do the work when calling "Err.warning(msg);"
	*/
	public boolean warning(String e);
	
	/**
		Progress or change log messages. A newline gets appended.
		@return false if class "Err" should do the work when calling "Err.log(msg);"
	*/
	public boolean log(String e);
	
	/**
		Progress or change log messages. No newline is appended!
		@return false if class "Err" should do the work when calling "Err.log(msg);"
	*/
	public boolean logn(String e);
	
	/**
		Programmers fault like "this parameter value should never happen".
		A stack trace will be created!
		@return false if class "Err" should do the work when calling "Err.assertion(cond);"
	*/
	public boolean assertion(boolean condition);
	
	/**
		Clear error buffer because a new transaction is taking place
		and the log output would get too big.
	*/
	public void resetLog();

	/**
		@return the error buffer built by the passed calls to the
		ErrorHandler-interface. resetLog() must be called before this call!
	*/
	public String getLog();

	/**
		Set a parent component for ErrHandlers that show dialogs
		(must have a parent to be modal!).
		@param component Frame or Dialog subclass (JDK 1.1: only Frame)
	*/
	public void setParentComponent(Object component);
	
	/**
	 * Choose one of the passed objects.
	 * @param title the title for the dialog.
	 * @param choice the objects to choose one from.
	 * @return the chosen object, or null if canceled or closed without choice.
	 */
	public Object choose(String title, List choice);

}