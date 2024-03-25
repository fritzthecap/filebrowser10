package fri.gui.swing.filebrowser;

import java.awt.event.*;
import java.awt.*;
import java.util.*;
import javax.swing.undo.*;
import fri.gui.CursorUtil;
import fri.gui.swing.undo.*;

/**
	Target: Running a undo/redo action in a background thread
		handled by a CancelProgressDialog. Calculate the length
		of the action as the dialog needs this.
*/

public class ObservableDoAction extends DoAction implements
	Runnable
{
	private ActionEvent actionEvent;
	private TransactionDialog observer;
	
	
	public ObservableDoAction(String meaning)	{
		super(meaning);
	}
	
	public void actionPerformed(ActionEvent e)	{
		actionEvent = e;
		// calculate length of undo/redo action
		PreviewableUndoManager um = (PreviewableUndoManager)undoManager;
		UndoableEdit edit = null;
		
		if (meaning.equals(REDO))
			edit = um.getEditToBeRedone();
		else
			edit = um.getEditToBeUndone();
			
		//System.err.println("undoable edit is "+edit);

		if (edit != null)	{
			ListableCompoundEdit ce = (ListableCompoundEdit)edit;
			Enumeration enumeration = ce.elements();
			//System.err.println("  and enumerates with: "+enumeration);
			
			if (enumeration != null)	{
				// create progress monitor
				observer = new TransactionDialog(
						(Component)e.getSource(),
						meaning+" (Interrupting can leave inconsistent state!)  ",
						this,
						null);
						
				long sum = 0;
				for (; enumeration != null && enumeration.hasMoreElements(); )	{
					FileCommand c = (FileCommand)enumeration.nextElement();
					sum += c.getRecursiveSize(observer);
				}
				
				// prepare code to run in background
				observer.start(sum);
				return;
			}
		}
		
		System.err.println("running undoable edit in foreground ... ");
		CursorUtil.setWaitCursor((Component) e.getSource());
		try	{
			run();
		}
		finally	{
			CursorUtil.resetWaitCursor((Component) e.getSource());
		}
	}
	

	public void run()	{
		super.actionPerformed(actionEvent);
		
		if (observer != null)	{
			observer.endDialog();
			observer = null;
		}
	}
}