package fri.gui.swing.mailbrowser.attachment;

import javax.swing.JComponent;
import javax.swing.AbstractButton;
import fri.gui.swing.mailbrowser.viewers.AbstractPartOpenController;
import fri.gui.swing.mailbrowser.viewers.PartSaveController;

/**
	Controller that contains the "Save As" and "Open" action for attachments, with its callbacks.
*/

public class PartOpenSaveController extends AbstractPartOpenController
{
	private PartSaveController saveController;
	
	/** Create a "Save As" cntroller for passed PartView. */
	public PartOpenSaveController(AttachmentButton opener, PartSaveController saveController)	{
		super(opener);
		this.saveController = saveController;
		//saveController.insertSaveAction(this);	// get the save action from passed controller into this one
	}

	/**
		Overridden to cdelegate action installation to PartSaveController if actionName is ACTION_SAVE.
		This override must match the prototype used in AbstratcPartController to install a popup!
	*/
	public AbstractButton visualizeAction(String actionName, JComponent popup, boolean showIcon, int index)	{
		if (actionName.equals(PartSaveController.ACTION_SAVE))	{
			return saveController.visualizeAction(actionName, popup, showIcon, index);
		}
		else	{
			return super.visualizeAction(actionName, popup, showIcon, index);
		}
	}


	/** Install a popup with "Save As" and "Open" item on the passed PartView. */
	public static void installOpenSavePopup(AttachmentButton partOpener)	{
		installOpenPopup(
			new PartOpenSaveController(
				partOpener,
				new PartSaveController(partOpener)
			),
			partOpener,
			new String [] { ACTION_OPEN, PartSaveController.ACTION_SAVE }
		);
	}

}