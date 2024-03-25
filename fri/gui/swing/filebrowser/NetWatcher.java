package fri.gui.swing.filebrowser;

import java.util.*;
import fri.util.props.PropertyUtil;

/**
	Target: Observe changes in the information-net from a background
		thread. If a change happens, the NetNode-interface is used
		to refresh all listeners.
	Behaviour: Constructor calls thread.start(). The run() method
		checks all containers that are listed for updates by calling
		their checkForDirty() method. If this returns NEEDS_REFRESH,
		list(refresh = true) is called. If return is UP_TO_DATE,
		the container is followed recursively.
		For files the init() method gets called.
*/

public class NetWatcher extends Thread implements
	Runnable
{
	private NetNode root;
	private TreeEditController tc;
	// milli-seconds default refresh interval
	private static final int interval = PropertyUtil.getSystemInteger("refreshInterval", 5000);
	private boolean stopped = false;
	
	
	/**
		Create a new NetWatcher thread.
		@param root root of filesystem to watch
		@param tc TreeEditController, during transaction no refresh is done.
	*/
	public NetWatcher(NetNode root, TreeEditController tc)	{
		this.root = root;
		this.tc = tc;
		start();
	}
	
	/** Terminates Thread by breaking run loop */
	public void stopRefresh()	{
		stopped = true;
	}
	
	
	// interface Runnable
	
	public void run()	{
		System.err.println("    /// NetWatcher refresh thread started ...");
		
		setPriority(MIN_PRIORITY);
		long next = 5000;	// give time to initialize all
		
		while (stopped == false)	{
			try	{
				sleep(next);
			}
			catch (InterruptedException e)	{
				//System.err.println("    /// NetWatcher - thread was interrupted ...");
			}
						
			if (!tc.transactionsInProgress())	{
				long time1 = System.currentTimeMillis();

				updateNode(root);
				//System.err.println("============================");

				long time2 = System.currentTimeMillis();
				long diff = (time2 - time1);		

				//System.err.println("      // NetWatcher needed "+diff+" millis for update.");
				next = (long)interval - diff;
				if (next < 1000)
					next = 1000;	// short pause
			}
			else	{	// do not slow down transaction
				next = interval;
				//System.err.println("      // NetWatcher: no update, transactionsInProgress");
				continue;
			}
		}
		System.err.println("    /// NetWatcher refresh thread stopped.");
	}
	
	private void updateNode(NetNode node)	{
		if (stopped)
			return;
			
		// run through all containers and compare their list with current list
		if (node.getNetNodeListeners() != null)	{	// if there are GUI listeners
			//System.err.println("    /// updateNode "+node.getFullText());
			int ret = node.checkForDirty();
			if (ret == NetNode.NOT_EXPANDED || ret == NetNode.NOT_LISTED)
				return;
				
			//System.err.println("net watcher checking: "+node);
			Vector list = null;
			if (ret == NetNode.NEEDS_REFRESH)	{
				//System.err.println("needs refresh: "+node);
				list = node.list(true);
			}
			else
			if (ret == NetNode.UP_TO_DATE)	{
				list = node.listSilent();	// no refresh, but recursive evaluation
			}
			 	
			for (int i = 0; list != null && i < list.size(); i++)	{
				NetNode nn = (NetNode)list.elementAt(i);
				updateNode(nn);
			}
		}
	}
}