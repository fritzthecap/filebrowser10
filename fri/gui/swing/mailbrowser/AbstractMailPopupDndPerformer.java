package fri.gui.swing.mailbrowser;

import java.awt.Component;
import javax.swing.JScrollPane;
import fri.gui.mvc.controller.swing.dnd.AbstractModelItemPopupDndPerformer;

/**
	Drag and Drop handler that provides a multi-language drop menu and a default cancelCallback() to free used lists.
*/

public abstract class AbstractMailPopupDndPerformer extends AbstractModelItemPopupDndPerformer
{
	public AbstractMailPopupDndPerformer(Component sensor, JScrollPane scrollPane)	{
		super(sensor, scrollPane);
	}


	/** Override to internationalize the "Copy" action label. */
	protected String getCopyLabel()	{
		return Language.get("Copy");
	}
	/** Override to internationalize the "Move" action label. */
	protected String getMoveLabel()	{
		return Language.get("Move");
	}
	/** Override to internationalize the "Cancel" action label. */
	protected String getCancelLabel()	{
		return Language.get("Cancel");
	}


	/** Provide clearing member variables lists. */
	protected void cancelCallback()	{
		dropTargetList = null;
		droppedNodes = null;
	}

}
