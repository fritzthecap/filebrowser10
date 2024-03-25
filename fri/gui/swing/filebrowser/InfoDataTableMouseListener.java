package fri.gui.swing.filebrowser;

import java.awt.event.*;
import javax.swing.*;

import fri.gui.CursorUtil;

/**
	Handle mouse events for InfoTable, delegate to tree edit controller
*/

public class InfoDataTableMouseListener implements
	MouseListener,
	MouseMotionListener
{	
	private final TreeEditController tc;
	private final JTable table;
	
	
	public InfoDataTableMouseListener(TreeEditController tc, JTable table)	{
		this.tc = tc;
		this.table = table;
	}
	
	// interface MouseListener

	public void mousePressed(MouseEvent e)	{
		if (e.isPopupTrigger())	{
			//System.err.println("popup trigger, mouse pressed");
			doPopup(e);
		}
	}
	
	public void mouseClicked(MouseEvent e)	{
		if (e.getClickCount() >= 2)	{
			CursorUtil.setWaitCursor(table);
			try	{
				tc.openNode(table);
			}
			finally	{
				CursorUtil.resetWaitCursor(table);
			}
		}
	}
	
	public void mouseReleased(MouseEvent e)	{
		if (e.isPopupTrigger())	{
			//System.err.println("popup trigger, mouse released");
			doPopup(e);
		}
	}

	private void doPopup(MouseEvent e)	{
		CursorUtil.setWaitCursor(table);
		try	{
			tc.setEnabledActions();
			tc.showActionPopup(e, table);
		}
		finally	{
			CursorUtil.resetWaitCursor(table);
		}
	}
	
	public void mouseEntered(MouseEvent e)	{
	}
	public void mouseExited(MouseEvent e)	{
	}
	
	
	// interface MouseMotionListener

	public void mouseMoved(MouseEvent e)	{
		tc.setMousePoint(e.getPoint(), table);
	}
	
	public void mouseDragged(MouseEvent e)	{
	}
}