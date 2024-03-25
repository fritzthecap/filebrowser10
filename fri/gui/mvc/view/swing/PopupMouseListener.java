package fri.gui.mvc.view.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;

/**
	MouseListener that shows the passed popup on any JComponent it gets installed to as MouseListener.
	@author Fritz Ritzberger, 2003
*/

public class PopupMouseListener extends MouseAdapter
{
	private JPopupMenu popup;
	
	/** Create a MouseListener that shows the passed popup on popup events. */
	public PopupMouseListener(JPopupMenu popup)	{
		this.popup = popup;
	}
	
	
	/** Implements MouseListener to catch popup event. */
	public void mousePressed (MouseEvent e)	{
		doPopup(e);
	}
	
	/** Implements MouseListener to catch popup event. */
	public void mouseReleased (MouseEvent e)	{
		doPopup(e);
	}
	
	/** Opens the popup menu on a popup mouse event */
	protected void doPopup(MouseEvent e)	{
		if (e.isPopupTrigger())
			popup.show(e.getComponent(), e.getX(), e.getY());
	}
}