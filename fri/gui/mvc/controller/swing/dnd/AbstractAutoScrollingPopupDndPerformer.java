package fri.gui.mvc.controller.swing.dnd;

import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.awt.Point;
import javax.swing.*;

/**
	Drag&Drop handler that provides a drop menu, containing Copy, Move and Cancel.
	It finalizes <i>receiveCopy()</i> and <i>receiveMove()</i> to avoid ambiguities with popup callbacks.
	
	First the receive must be done by overriding <i>receive(Point p, List data, boolean isCopy)</i>,
	then the popup must be opened actively by calling showPopup().

	@author Ritzberger Fritz
*/

public abstract class AbstractAutoScrollingPopupDndPerformer extends AbstractAutoScrollingDndPerformer
{
	protected JMenuItem copy, move, cancel;
	private JPopupMenu popup;
	private Point currentPoint;
	private ActionListener popupActionListener;

	/**
		Create a autoscrolling drag and drop handler for mail messages and folders.
		This can be installed on message table and folder tree.
	*/
	public AbstractAutoScrollingPopupDndPerformer(Component sensor, JScrollPane scrollPane)	{
		super(sensor, scrollPane);

		popup = new JPopupMenu();
		popup.add(copy = new JMenuItem(getCopyLabel()));
		popup.add(move = new JMenuItem(getMoveLabel()));
		popup.addSeparator();
		popup.add(cancel = new JMenuItem(getCancelLabel()));

		popupActionListener = new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				if (e.getSource() == copy || e.getSource() == move || e.getSource() == cancel)	{
					if (e.getSource() == copy)	{
						copyCallback();
					}
					else
					if (e.getSource() == move)	{
						moveCallback();
					}
					else
					if (e.getSource() == cancel)	{
						cancelCallback();
					}
				}
			}
		};

		copy.addActionListener(popupActionListener);
		move.addActionListener(popupActionListener);
		cancel.addActionListener(popupActionListener);
	}

	public void release()	{
		super.release();
		copy.removeActionListener(popupActionListener);
		move.removeActionListener(popupActionListener);
		cancel.removeActionListener(popupActionListener);
		popupActionListener = null;
		popup = null;
		copy = null;
		move = null;
		cancel = null;
	}

	/** Override to internationalize the "Copy" action label. */
	protected String getCopyLabel()	{
		return "Copy";
	}
	/** Override to internationalize the "Move" action label. */
	protected String getMoveLabel()	{
		return "Move";
	}
	/** Override to internationalize the "Cancel" action label. */
	protected String getCancelLabel()	{
		return "Cancel";
	}


	/** Subclasses MUST implement this to receive popup choice "copy". */
	protected abstract void copyCallback();

	/** Subclasses MUST implement this to receive popup choice "move". */
	protected abstract void moveCallback();

	/** Subclasses implement this to receive popup choice "cancel". */
	protected void cancelCallback()	{
	}


	/** Overridden to catch drop point. */
	public boolean receiveTransferable(Object data, int action, Point p)	{
		currentPoint = p;
		return super.receiveTransferable(data, action, p);
	}


	/** Delegates to <i>receive(p, data, false)</i>. */
	protected final boolean receiveMove(Object data, Point p)	{
		return receive(p, (List)data, false);
	}

	/** Delegates to <i>receive(p, data, true)</i>. */
	protected final boolean receiveCopy(Object data, Point p)	{
		return receive(p, (List)data, true);
	}

	/** Subclasses implement this to evaluate member-variables and call showPopup() at end. */
	protected abstract boolean receive(Point p, List data, boolean isCopy);
	

	/** Show the popup at drop point. Callback will arrive in <i>xxxCallback()</i>. */
	protected void showPopup()	{		
		popup.show(sensor, currentPoint.x, currentPoint.y);
	}
	
}