package fri.util.error;

import java.awt.Component;
import java.util.List;
import java.util.Vector;
import java.io.*;
import java.lang.reflect.InvocationTargetException;

/**
		Default global implementation of ErrorHandler, that prints to stderr.
		Use setHandler() to redirect messages. This is a static implementation,
		so each call to setHandler() changes the error handling strategy for
		the whole process.
 		The logging behaviour can be controlled by a System-Property "fri.log",
 		passed in "java -Dlogging=false ..." commandline.
	<P>
 		Categorize, buffer and render error messages.
 		Let set and reset different (but global) ErrorHandlers.
 		Let define a parent-Component for GUI-ErrorHandlers.
 		Enable thread-bound turning off and on of error logging.
 		Convert Exceptions to String.
	<P>
 		When a handler was set, all methods delegate to that handler.
 		Else they print to stderr. If the handler method call returns false (not on),
 		the message is additionally printed to stderr.
 <B>Usage:</B>
 <PRE>
 		Err.error(new Exception("this goes to stderr"));
 		// ... this was printed to stderr
 		GuiErrorHanlder myErrorHandler = new GuiErrorHandler(frame);
 		Err.setHandler(myErrorHandler);
 		Err.error(new Exception("this goes to my error handler"));
 		// ... this was passed to object myErrorHandler, so a GUI dialog was opened
 		...
 		Err.resetLog();	// log buffer is prepared
 		...	// do actions with a lot of ...
 		Err.log("a log message");
 		...
 		if (success == false)
 			Err.error(Err.getLog());	// this renders all buffered log messages and clears log buffer
 </PRE>

 @author  Ritzberger Fritz
*/

public abstract class Err // implementing a Error-Handler bridge
{
  private static ErrorHandler errorHandler = null, old = null;	// current handler
  private static Component parent = null;	// to show wait cursors and dialogs
	private static Vector turnedOff = new Vector();	// list of turned off threads
	private static boolean doLogging = true;

	static	{
		String s = System.getProperty("logging");
		doLogging = (s != null && s.equalsIgnoreCase("false")) ? false : true;
	}

	public static boolean active()	{
		return doLogging;
	}
	
	

	private Err()	{}	// do not instantiate


	/**
		Converts an Exception to a (multiline) String.
	*/
	public static String exceptionToString(Throwable e)	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}


	/**
		Let another ErrorHandler render errors.
		@return old error handler
	*/
	public static ErrorHandler setHandler(ErrorHandler eh) {
		old = errorHandler;
		errorHandler = eh;
		return old;
	}

	/**
		Let save and restore error handlers
	*/
	public static ErrorHandler getHandler()	{
		return errorHandler;
	}
	
	/**
		Reset ErrorHandler to previously installed (deinstall current handler).
		@return current (after call previous) installed handler.
	*/
	public static ErrorHandler resetHandler() {
		ErrorHandler current = errorHandler;
		errorHandler = old;
		return current;
	}
	
		
	
	/** @see fri.util.error.ErrorHandler */
	public static String getLog()	{
		return errorHandler != null ? errorHandler.getLog() : null;
	}

	/** @see fri.util.error.ErrorHandler */
	public static void resetLog()	{
		if (errorHandler != null)	{
			errorHandler.resetLog();
		}
		else
			System.err.flush();
	}



	/** @see fri.util.error.ErrorHandler */
	public static void setParentComponent(Object c)	{
		if (errorHandler != null)	{
			errorHandler.setParentComponent(c);
		}
		parent = (Component)c;
	}

	public static Component getParentComponent()	{
		return parent;
	}
	
	
	/** Is error handling switched on? */
	public static boolean isOn()	{
		return turnedOff.indexOf(Thread.currentThread().getName()) < 0;
	}
	
	/** Switch on error handling */
	public static void setOn()	{
		setOn(true);
	}
	
	/**
		Switch on or off error handling only for the current thread.
		Threads are managed by their name (getName() method).
	*/
	public static void setOn(boolean on)	{
		String t = Thread.currentThread().getName();
		//Thread t = Thread.currentThread();
		System.err.println("Err.setOn("+on+") for: "+t);
		int i = turnedOff.indexOf(t);
		if (i < 0 && on == false)
			turnedOff.add(t);
		else
		if (i >= 0 && on == true)
			turnedOff.remove(t);
	}

	/**
		Switch off error handling. No messages will be printed anywhere.
		Needed for background-threads that would fill logging.files.
	*/
	public static void setOff()	{
		setOn(false);
	}
	
	
	/**
		If some thread turns logging off, other threads could loose error
		information. Thererfore logging can be turned on and off by each Thread.
		This method returns false if error logging was set to false by
		"java -Dapollo.log=false ..."
	*/
	private static boolean getAccess()	{
		if (doLogging == false)
			return false;	// generally turned off
				
		//Thread t = Thread.currentThread();
		String t = Thread.currentThread().getName();
		int i = turnedOff.indexOf(t);
		if (i < 0)	{
			return true;
		}
		return false;
	}
	

	
	
	
	
	
	/** @see fri.util.error.ErrorHandler */
	public static void debug(String e)	{
		if (getAccess() == false)
			return;
		if (errorHandler == null || errorHandler.debug(e) == false)	{
			System.err.println("DEBUG: "+e);
			//Thread.dumpStack();
		}
	}
	
	/** @see fri.util.error.ErrorHandler */
	public static void fatal(String e)	{
		if (getAccess() == false)
			return;
		if (errorHandler == null || errorHandler.fatal(e) == false)	{
			System.err.println("FATAL: "+e);
		}
		//System.exit(2);
	}
	
	/** @see fri.util.error.ErrorHandler */
	public static void error(Throwable e)	{
		if (getAccess() == false)
			return;
			
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException)e).getTargetException();
			
		if (errorHandler == null || errorHandler.error(e) == false)	{
			e.printStackTrace();
			//System.err.println("ERROR: "+e);
		}
	}
	
	/** @see fri.util.error.ErrorHandler */
	public static void warning(String e)	{
		if (getAccess() == false)
			return;
		if (errorHandler == null || errorHandler.warning(e) == false)	{
			System.err.println("WARNING: "+e);
		}
	}

	/** @see fri.util.error.ErrorHandler */
	public static void log(String e)	{
		//if (e.equals("java.lang.reflect.InvocationTargetException"))
		//	Thread.dumpStack();
			
		if (getAccess() == false)
			return;
		if (errorHandler == null || errorHandler.log(e) == false)	{
			System.err.println("LOG: "+e);
		}
	}

	/** @see fri.util.error.ErrorHandler */
	public static void logn(String e)	{
		if (getAccess() == false)
			return;
		if (errorHandler == null || errorHandler.logn(e) == false)	{
			System.err.print(e);
		}
	}
		
	/** @throws IllegalArgumentException when param o is null. */
	public static void assertion(Object o)	{
		if (getAccess() == false)
			return;
		Err.assertion(o != null);
	}

	/** @throws IllegalArgumentException when condition is false */
	public static void assertion(boolean condition)	{
		if (getAccess() == false)
			return;
		if (errorHandler == null || errorHandler.assertion(condition) == false)	{
			if (condition == false)	{
				System.err.println("ASSERT: condition is false");
				throw new IllegalArgumentException("condition is false");
			}
		}
	}

	/** @see fri.util.error.ErrorHandler */
	public static Object choose(String title, List choice) {
		if (getAccess() == false || errorHandler == null)
			return choice != null && choice.size() > 0 ? choice.get(0) : null;
		return errorHandler.choose(title, choice);
	}
	
}