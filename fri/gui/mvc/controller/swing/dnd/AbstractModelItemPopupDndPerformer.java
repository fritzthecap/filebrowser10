package fri.gui.mvc.controller.swing.dnd;

import java.util.*;
import java.awt.*;
import java.util.List;
import java.awt.datatransfer.*;
import javax.swing.JScrollPane;
import fri.gui.mvc.model.ModelItem;
import fri.gui.swing.ComponentUtil;

/**
	Drag and Drop handler that provides ModelItem support and a default drag and drop workflow.
	Override abstract methods to get rid of receiving and showing the popup.
*/
public abstract class AbstractModelItemPopupDndPerformer extends AbstractAutoScrollingPopupDndPerformer
{
	protected List dropTargetList;	// list of view nodes for cb_Paste(viewNodeList)
	protected ModelItem [] droppedNodes;	// list of ModelItems

	public AbstractModelItemPopupDndPerformer(Component sensor, JScrollPane scrollPane)	{
		super(sensor, scrollPane);
	}

	public void release()	{
		super.release();
		dropTargetList = null;
		droppedNodes = null;
	}

	// send methods
	
	/** Returns false if drag should be canceled for some reason (tree is editing). This implementation returns true. */
	protected boolean checkStartDrag()	{
		return true;
	}

	/** Returns list of selected view nodes. */
	protected abstract List getSelected();
	
	/** Returns null for cancel drag, or a serializable object built from casted view node. */
	protected abstract Object createDraggedObject(Object o);

	/** Time to clear the clipboard, set the source model to it, and do anything that must be done before drag starts. */
	protected void startingDrag()	{
	}

	/** Produce a Transferable object from passed serializable objects. */
	protected abstract Transferable createTransferable(List serializableObjects);
	
	
	/** Implements DndPerformer: sending selected items. */	
	public Transferable sendTransferable()	{
		if (activated == false || checkStartDrag() == false)
			return null;
		
		// get selected nodes
		List selection = getSelected();
		if (selection == null || selection.size() <= 0)	{
			return null;
		}
		
		List intransfer = new ArrayList();

		// check for dragability and pack selected nodes
		for (Iterator it = selection.iterator(); it.hasNext(); )	{
			Object draggedObject = createDraggedObject(it.next());
			if (draggedObject == null)
				return null;

			intransfer.add(draggedObject);
		}
		
		System.err.println("sending drag data: "+intransfer);
		startingDrag();
		
		return createTransferable(intransfer);
	}


	// receive methods

	/** Locates a received (dropped) object in some view and returns a ModelItem built from it. */
	protected abstract ModelItem locateDropped(Object serializableObject);

	/** Locates the drop target from a point in this sensor view. */
	protected abstract Object locateDropTarget(Point p);


	protected boolean receive(Point p, List data, boolean isCopy)	{
		// find the dropped objects in local view
		List dropped = new ArrayList();
		for (int i = 0; i < data.size(); i++)	{
			ModelItem item = locateDropped(data.get(i));
			if (item != null)
				dropped.add(item);
		}
		
		if (dropped.size() <= 0)
			return false;

		// create ModelItem(s) to cut/copy
		droppedNodes = (ModelItem []) dropped.toArray(new ModelItem [data.size()]);
		System.err.println("Received dropped nodes: "+dropped);
		
		// find drop target in view
		Object dropTarget = locateDropTarget(p);
		if (dropTarget == null)
			return false;
			
		System.err.println("... at drop node "+dropTarget);
		dropTargetList = new ArrayList();
		dropTargetList.add(dropTarget);

		// get frame of obtained Component to foreground
		Frame f = ComponentUtil.getFrame(sensor);
		if (f != null)
			f.setVisible(true);

		// let user choose the action taken: copy or move
		showPopup();

		return true;
	}

}
