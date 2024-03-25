package fri.gui.swing.mailbrowser.viewers;

import javax.swing.JPopupMenu;
import fri.gui.mvc.view.swing.PopupMouseListener;
import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.mailbrowser.Language;

/**
	Controller that contains the "Save As" and "Open" action for attachments, with its callbacks.
*/

public abstract class AbstractPartController extends ActionConnector
{
	protected PartView partView;
	
	/** Create a "Save As" cntroller for passed PartView. */
	public AbstractPartController(PartView partView)	{
		super(partView.getSensorComponent(), null, null);
		this.partView = partView;
	}

	/** Do internationalization for action labels. */
	protected String language(String label)	{
		return Language.get(label);
	}
	

	/** Install a popup with passed items on the passed PartView. */
	protected static JPopupMenu installPopup(AbstractPartController controller, PartView partView, String [] actionNames)	{
		JPopupMenu popup = new JPopupMenu();
		for (int i = 0; i < actionNames.length; i++)	{
			controller.visualizeAction(actionNames[i], popup, false, i);
		}
		partView.getSensorComponent().addMouseListener(new PopupMouseListener(popup));
		return popup;
	}

}