package fri.util.application;

import java.util.*;

/**
	Manage all closeable applications for one Java VM process
	to provide a exit() that respects other applications running
	int htis VM.<br>
	The managed Objects must implement Closeable to participate
	in this management.
	<p>
	GUI example:
	<pre>
	public class MyFrame extends JFrame implements
		Closeable,
		WindowListener,
		ActionListener
	{
		public MyFrame()	{
			Application.register(this);
			...
		}
		
		// implementing Closeable
		public boolean close()	{
			return true;
		}

		...

		public void windowClosing(WindowEvent e)	{
			Application.closeExit(this);
		}

		public void actionPerformed(ActionEvent e)	{
			// receiving "close all" menuitem
			Application.closeAll();
		}
	}
	</pre>

	Terminal application example:
	<pre>
	public class MyApp implements Closeable
	{
		public MyApp()	{
			Application.register(this);
			...
		}

		...

		public void finishMyWork()	{
			Application.closeExit(this);
		}
	}
	</pre>
	
	This class is called by fri.gui.swing.application.GuiApplication,
	that provides a higher level of window management.

  @author Ritzberger Fritz
*/

public abstract class Application
{
	public static final String NAME = "FriWare";
	public static final String VERSION = "1.0";
	
	private static Vector apps = new Vector();	// running frames


	/** Returns the count of currently registered applications. */
	public static int instances()	{
		return apps.size();
	}
		
	/** Returns the count of currently registered applications of the passed class */
	public static int instances(Class c)	{
		int i = 0;
		for (Iterator it = apps.iterator(); it.hasNext(); )	{
			Object f = it.next();
			if (c.isInstance(f))
				i++;
		}
		return i;
	}

		
	/** Add a new application to instance list. */
	public static void register(Closeable app)	{
		apps.add(app);
	}

	private static void register(Closeable app, int i)	{
		apps.add(i, app);
	}

	
	/**
		Remove the passed application silently from instance list.
		Its close() method is NOT called.
		@param app the application to remove from instance list
	*/
	public static void deregister(Closeable app)	{
		apps.remove(app);
	}

	
	/**
		Removes the passed application from list and calls its close() method.
		If this call returns false, the application is added again
		to instance list. This method does <b>NOT</b> call exit(), use
		<i>closeExit()</i> for that purpose.
		@app application to close
		@return true if app.close() returns true, else false
	*/
	public static boolean close(Closeable app)	{
		int i = apps.indexOf(app);
		deregister(app);
		
		if (app.close() == false)	{
			if (i >= 0)
				register(app, i);	// was not closeable

			return false;
		}
		return true;
	}


	/**
		Tries to close the passed application and exits if this succeeds and
		it is the last one in instance list.
		@app application to close and exit if success.
	*/
	public static boolean closeExit(Closeable app)	{
		boolean b = close(app);
		if (b && instances() <= 0)
			System.exit(0);
		
		System.err.println("Application.closeExit, still got "+instances()+" instances: "+apps.get(0).getClass()+ " ...");
		return b;
	}


	/**
		Tries to close all registered applications. If one returns false
		from its close() method, the closing is not finished but continued
		up to the last registered application.
		Returns true if this succeeded, but does <b>NOT</b> exit.
		Use closeAllExit() for that purpose.
		Mind that the <u>instance numbers</u> retrieved by <i>Application.indexOf</i>
		<u>might change</u> after this call, as some applications could have been
		removed from instance list!
		@return true if all applications were closed successfully
	*/
	public static boolean closeAll()	{
		Vector v = new Vector();
		
		for (Iterator it = apps.iterator(); it.hasNext(); )	{
			Closeable app = (Closeable)it.next();
			it.remove();

			System.err.println("Application.closeAll "+app.getClass());
			if (app.close() == false)
				v.add(app);
		}
		
		if (v.size() <= 0)	// all were closed successful
			return true;
			
		apps = v;	// store still open applications to list
		return false;
	}


	/** Tries to close all frames and exits if this succeeds. */
	public static void closeAllExit()	{
		if (closeAll())
			System.exit(0);
	}

	
	/**
		Returns all currently registered applications of the passed class.
		The method <i>c.isInstance(app)</i> is used to test for passed class.
	*/
	public static Enumeration elements(Class c)	{
		Vector v = new Vector();

		for (Iterator it = apps.iterator(); it.hasNext(); )	{
			Object app = it.next();
			
			if (c.isInstance(app))
				v.add(app);
		}
		return v.elements();
	}

	
	/** Returns all currently registered applications. */
	public static Enumeration elements()	{
		return apps.elements();
	}

	
	/**
		Returns instance number of passed object if in list, else -1.
		This can be used for error messages related to a certain frame.
	*/
	public static int indexOf(Object app)	{
		return apps.indexOf(app);
	}

	
	/** Returns the first found instance of passed class or null. */
	public static Closeable getFirstInstance(Class c)	{
		for (Iterator it = apps.iterator(); it.hasNext(); )	{
			Closeable app = (Closeable)it.next();
			
			if (c.isInstance(app))
				return app;
		}
		return null;
	}
	
	
	
	private Application()	{}	// do not instantiate
}