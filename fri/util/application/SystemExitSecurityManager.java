package fri.util.application;

import java.security.Permission;

/**
	SecurityManager that watches java.lang.System.exit() to
	do controlled exit when other applications are still running.
	Prevent System.exit to be called by classes that are not
	registered with Application.register(Closeable c).
	<pre>
		System.setSecurityManager(new SystemExitSecurityManager());
	</pre>
*/

public class SystemExitSecurityManager extends SecurityManager
{
	/**
		Ask Application.instances() if zero, else throw SecurityException.
	*/
	public void checkExit(int status) {
		int i;
		if ((i = Application.instances()) > 0)	{
			exitCalled();
			throw new SecurityException("System.exit() was called, but still got "+i+" applications.");
		}
		super.checkExit(status);
	}


	/**
		Override this to dispose some frame that called System.exit().
		This method gets called before this SecurityManager throws
		a SecurityException because Application.instances() is not zero.
	*/
	protected void exitCalled()	{
	}
	

	/** Overridden to disallow changing of SecurityManager. */
	public void checkPermission(Permission perm) {
		if (perm != null && perm.getName().equals("setSecurityManager"))	{
			throw new SecurityException("SystemExitSecurityManager can not be replaced without loss of control over running frames!");
		}
	}
	
}