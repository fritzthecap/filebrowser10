package fri.gui.mvc.util.swing;

import java.awt.EventQueue;

/**
  	Events sind Methoden-Aufrufe ueber Listener, welche zur 1:n Assoziation
  	von Klassen dienen. Hier sind utility-Methoden zur Handhabung solcher
  	Aufrufe. Das JDK 1.2 empfiehlt, aus Hintergrund-Threads heraus nicht
  	aufs GUI zuzugreifen (also z.B. ein repaint() auszuloesen).
  	Die Umgehung ist hier mittel EventQueue.invokeLater() implementiert.
  	Erfolgt der Aufruf aus dem event thread, wird er direkt ausgefuehrt.
	<P>
	@version 1.1
	@author  Ritzberger Fritz
*/

public abstract class EventUtil
{
	private EventUtil()	{}	// do not construct
	
	/**
		Start the passed Runnable in the current thread, if it is the event thread,
		or in the event thread, when the current one is a background thread.
		Wait for the end of execution of the Runnable.
	*/
	public static void invokeSynchronous(Runnable runnable)	{
		if (runnable == null)
			return;
		
		if (EventQueue.isDispatchThread() == false)	{
			try	{
				EventQueue.invokeAndWait(runnable);
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
		else	{
			runnable.run();
		}
	}

	/**
		Start the passed Runnable in the current thread, if it is the event thread,
		or in the event thread, when the current one is a background thread.
		This call waits for the end of execution only if it was launched in event thread.
	*/
	public static void invokeLaterOrNow(Runnable runnable)	{
		if (runnable == null)
			return;
		
		if (EventQueue.isDispatchThread() == false)	{
			EventQueue.invokeLater(runnable);
		}
		else	{
			runnable.run();
		}
	}

}