package fri.gui.mvc.controller.swing.dnd;

import java.awt.*;
import fri.gui.swing.dnd.DndPerformer;
import fri.gui.swing.dnd.DndListener;

/**
 <UL>
 <LI><B>Background:</B> A generic Drag and Drop Listener.
 	You MUST override sendTransferable() and supportsDataFlavor().
 </LI>
 <LI><B>Responsibilities:</B> Watches Drag and Drop gestures.
 </LI>
 <LI><B>Behaviour:</B> Implements DndListener to introduce two new methods
 for overriding: <i>receciveCopy()</i> and <i>receiveMove()</i>.
 </LI>
 </UL>
 @author  Ritzberger Fritz
*/

public abstract class AbstractDndPerformer implements
	DndPerformer
{
	protected Component sensor;
	private DndListener dndListener;
	protected boolean activated = true;	// workaround option for some dnd bugs

	/**
		Create a default DND performer with abstract functionality.
		@param sensor Component to watch
	*/
	public AbstractDndPerformer(Component sensor)	{
		this.dndListener = new DndListener(this, sensor);
		this.sensor = sensor;
	}
	
	/**
	 * Remove this drag and drop handler.
	 * It will be no more usable after this call.
	 */
	public void release()	{
		dndListener.release();
		dndListener = null;
		sensor = null;
	}

	/** Workaround for bug: gaining focus starts Drag 'n Drop. */
	public void setActivated(boolean activated)	{
		this.activated = activated;
	}

	/** Implements DndPerformer: return true to allow any location for dropping */
	public boolean dragOver(Point p)	{
		return true;	// do not reject any location
	}

	/** Implements DndPerformer: delegate data to ViewDnd */
	public boolean receiveTransferable(Object data, int action, Point p)	{
		if (action == DndPerformer.COPY)
			return receiveCopy(data, p);
		else
			return receiveMove(data, p);
	}


	/** Override this to receive a move command. */
	protected boolean receiveMove(Object data, Point p)	{
		new Exception("Please implement me!").printStackTrace();
		return true;
	}
	
	/** Override this to receive a copy command. */
	protected boolean receiveCopy(Object data, Point p)	{
		new Exception("Please implement me!").printStackTrace();
		return true;
	}
	

	/** Implements DndPerformer: do nothing. */
	public void actionCanceled()	{}
	
	/** Implements DndPerformer: do nothing. */
	public void dataCopied()	{}
	
	/** Implements DndPerformer: do nothing. */
	public void dataMoved()	{}

	/** Implements DndPerformer: do nothing. */
	public void startAutoscrolling()	{}

	/** Implements DndPerformer: do nothing. */
	public void stopAutoscrolling()	{}

}