package fri.gui.swing.dnd;

import java.io.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

/**
 * Makes avaliable both sending drags and receiving drops through interface
 * DndPerformer. Mention that the Component will receive its own drag-action (be
 * its own DragSource). Default DnD action is ACTION_COPY_OR_MOVE.
 * 
 * @author Fritz Ritzberger, 1999
 */

public class DndListener implements DragGestureListener, DragSourceListener, DropTargetListener
{
	private DragSource dragSource;
	private DropTarget dropTarget;
	private DragGestureRecognizer recognizer;
	private DndPerformer performer;
	private int allowedSourceAction;
	private int allowedTargetAction;

	/**
	 * Bind together a DnD Performer and its Component in this Listener with
	 * default allowed action copy or move for sending and receiving.
	 * @param performer object that can send drags and receive drops.
	 * @param sensorComponent that is parameter to
	 *            createDefaultDragGestureRecognizer() and new DropTarget().
	 */
	public DndListener(DndPerformer performer, Component sensorComponent) {
		this(performer, sensorComponent, DnDConstants.ACTION_COPY_OR_MOVE, DnDConstants.ACTION_COPY_OR_MOVE);
	}

	/**
	 * Bind together a DnD Performer and a Component in this Listener with a
	 * given allowed action for sending and receiving.
	 * @param performer object that can send drags and receive drops.
	 * @param sensorComponent that is parameter to
	 *            createDefaultDragGestureRecognizer() and new DropTarget().
	 * @param allowedAction any other allowed action than COPY_OR_MOVE for both
	 *            drag sender and drop receiver roles, constant value from
	 *            DndPerformer
	 */
	public DndListener(DndPerformer performer, Component sensorComponent, int allowedAction) {
		this(performer, sensorComponent, allowedAction, allowedAction);
	}

	/**
	 * Bind together a DnD Performer and a Component in this Listener with two
	 * different allowed actions for sending and receiving.
	 * @param performer object that can send drags and receive drops.
	 * @param sensorComponent that is parameter to
	 *            createDefaultDragGestureRecognizer() and new DropTarget().
	 * @param allowedSourceAction any other allowed action than COPY_OR_MOVE for
	 *            drag sender role, constant value from DndPerformer
	 * @param allowedTargetAction any other allowed action than COPY_OR_MOVE for
	 *            drop receiver role, constant value from DndPerformer
	 */
	public DndListener(DndPerformer performer, Component sensorComponent, int allowedSourceAction, int allowedTargetAction) {
		this.allowedSourceAction = getDnDConstant(allowedSourceAction);
		this.allowedTargetAction = getDnDConstant(allowedTargetAction);
		
		this.performer = performer;
		
		dragSource = DragSource.getDefaultDragSource();
		// dragSrc = new DragSource();
		
		dropTarget = new DropTarget(
				sensorComponent, // Component to which is dropped
				allowedTargetAction, // flag
				this); // DropTargetListener
		
		recognizer = dragSource.createDefaultDragGestureRecognizer(
				sensorComponent, // Component from which is dragged
				allowedSourceAction, // flag
				this); // DragGestureListener
	}

	/**
	 * Remove this drag and drop handler.
	 * It will be no more usable after this call.
	 */
	public void release()	{
		if (recognizer != null)
			recognizer.removeDragGestureListener(this);
		if (dragSource != null)
			dragSource.removeDragSourceListener(this);
		dropTarget.removeDropTargetListener(this);
		recognizer = null;
		dragSource = null;
		dropTarget = null;
		performer = null;
	}
	
	// interface DragGestureListener

	public void dragGestureRecognized(DragGestureEvent e) {
		Transferable t = performer.sendTransferable();
		if (t == null)
			return;

		try {
			System.err.println("calling startDrag with " + t);
			e.startDrag(DragSource.DefaultCopyDrop, // cursor-flag
					t, // data
					this); // DragSourceListener
		}
		catch (InvalidDnDOperationException ex) { // happens on Linux, deadly
			ex.printStackTrace();
		}
	}

	// interface DragSourceListener

	public void dragDropEnd(DragSourceDropEvent e) {
		performer.stopAutoscrolling();

		if (e.getDropSuccess() == false) {
			performer.actionCanceled();
		} else {
			if (e.getDropAction() == DnDConstants.ACTION_MOVE) {
				// notify drag source so that it can remove data
				performer.dataMoved();
			} else {
				// notify drag source so that it can release data
				performer.dataCopied();
			}
		}
	}

	public void dragEnter(DragSourceDragEvent e) {
		DragSourceContext context = e.getDragSourceContext();
		// intersection of the users selected action and the source and target actions
		int da = e.getDropAction();
		if ((da & allowedSourceAction) != 0)
			context.setCursor(DragSource.DefaultCopyDrop);
		else
			context.setCursor(DragSource.DefaultCopyNoDrop);
	}

	public void dragExit(DragSourceEvent e) {
	}

	public void dragOver(DragSourceDragEvent e) {
	}

	public void dropActionChanged(DragSourceDragEvent e) {
	}

	// interface DropTargetListener

	private boolean checkAction(DropTargetDragEvent e) {
		if ((e.getDropAction() & allowedTargetAction) == 0) {
			return false;
		}

		DataFlavor df = performer.supportsDataFlavor(e.getDropAction(), e.getLocation(), e.getCurrentDataFlavors());
		if (df == null) {
			return false;
		}

		if (e.isDataFlavorSupported(df) == false) {
			return false;
		}

		return true;
	}

	private void checkDragAction(DropTargetDragEvent e) {
		if (checkAction(e) == false) {
			e.rejectDrag();
		} else {
			e.acceptDrag(e.getDropAction());
		}
	}

	public void dragEnter(DropTargetDragEvent e) {
		performer.stopAutoscrolling();
		checkDragAction(e);
	}

	public void dragExit(DropTargetEvent e) {
		performer.startAutoscrolling();
	}

	public void dragOver(DropTargetDragEvent e) {
		if (performer.dragOver(e.getLocation()) == false) {
			//System.err.println("rejecting drag for "+e);
			e.rejectDrag();
		} else {
			checkDragAction(e);
		}
	}

	public void dropActionChanged(DropTargetDragEvent e) {
		checkDragAction(e);
	}

	public void drop(DropTargetDropEvent e) {
		performer.stopAutoscrolling();

		System.err.println("DropTargetListener.drop with event " + e);
		int sourceAction = e.getSourceActions();
		int dropAction = e.getDropAction();

		if ((sourceAction & allowedTargetAction) == 0) {
			e.rejectDrop();
			return;
		}

		int action = getAction(dropAction);

		try {
			DataFlavor f;

			if ((f = performer.supportsDataFlavor(action, e.getLocation(), e.getCurrentDataFlavors())) != null) {
				Transferable t = e.getTransferable();
				// DragSource will get the performers DndConstant in some enhanced way:
				// COPY_OR_MOVE sends MOVE to the DragSource
				e.acceptDrop(allowedTargetAction);

				// HERE EVERYTHING GETS SERIALIZED
				Object data = t.getTransferData(f); // IOException: no native data transferred
				// NOW EVERYTHING IS SERIALIZED, OR NotSerializableException WAS THROWN

				boolean ret = performer.receiveTransferable(data, action, e.getLocation());
				e.dropComplete(ret);
			} else {
				e.rejectDrop();
				Toolkit.getDefaultToolkit().beep();
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
			try {
				e.rejectDrop();
			}
			catch (InvalidDnDOperationException ex2) {
			}
			Toolkit.getDefaultToolkit().beep();
		}
		catch (UnsupportedFlavorException ex1) {
			ex1.printStackTrace();
			try {
				e.rejectDrop();
			}
			catch (InvalidDnDOperationException ex2) {
			}
			Toolkit.getDefaultToolkit().beep();
		}
	}

	// translate dnd-constant to local constant
	private int getAction(int constant) {
		if (constant == DnDConstants.ACTION_MOVE)
			return DndPerformer.MOVE;
		if (constant == DnDConstants.ACTION_COPY)
			return DndPerformer.COPY;
		if (constant == DnDConstants.ACTION_COPY_OR_MOVE)
			return DndPerformer.COPY_OR_MOVE;
		if (constant == DnDConstants.ACTION_LINK)
			return DndPerformer.LINK;
		if (constant == DnDConstants.ACTION_NONE)
			return DndPerformer.NONE;
		if (constant == DnDConstants.ACTION_REFERENCE)
			return DndPerformer.REFERENCE;
		return DndPerformer.UNKNOWN;
	}

	private int getDnDConstant(int constant) {
		if (constant == DndPerformer.MOVE)
			return DnDConstants.ACTION_MOVE;
		if (constant == DndPerformer.COPY)
			return DnDConstants.ACTION_COPY;
		if (constant == DndPerformer.COPY_OR_MOVE)
			return DnDConstants.ACTION_COPY_OR_MOVE;
		if (constant == DndPerformer.LINK)
			return DnDConstants.ACTION_LINK;
		if (constant == DndPerformer.NONE)
			return DnDConstants.ACTION_NONE;
		if (constant == DndPerformer.REFERENCE)
			return DnDConstants.ACTION_REFERENCE;
		return -1;
	}

	// dump utility function
	public static String dnDConstantToString(int constant) {
		if (constant == DnDConstants.ACTION_MOVE)
			return "DnDConstants.ACTION_MOVE";
		if (constant == DnDConstants.ACTION_COPY)
			return "DnDConstants.ACTION_COPY";
		if (constant == DnDConstants.ACTION_COPY_OR_MOVE)
			return "DnDConstants.ACTION_COPY_OR_MOVE";
		if (constant == DnDConstants.ACTION_LINK)
			return "DnDConstants.ACTION_LINK";
		if (constant == DnDConstants.ACTION_NONE)
			return "DnDConstants.ACTION_NONE";
		if (constant == DnDConstants.ACTION_REFERENCE)
			return "DnDConstants.ACTION_REFERENCE";
		return "DnDConstants.unknown: " + Integer.toString(constant);
	}

}
