package fri.gui.swing.iconbuilder;

import javax.swing.Icon;
import fri.gui.swing.actionmanager.connector.AbstractClipboardController;
import fri.gui.swing.actionmanager.connector.AbstractInsertDeleteController;

public abstract class IconActionMap
{
	public static Icon get(String actionName)	{
		if (actionName.equals(AbstractInsertDeleteController.ACTION_NEW))
			return Icons.get(Icons.newDocument);
		if (actionName.equals(AbstractInsertDeleteController.ACTION_DELETE))
			return Icons.get(Icons.delete);
		if (actionName.equals(AbstractClipboardController.ACTION_CUT))
			return Icons.get(Icons.cut);
		if (actionName.equals(AbstractClipboardController.ACTION_COPY))
			return Icons.get(Icons.copy);
		if (actionName.equals(AbstractClipboardController.ACTION_PASTE))
			return Icons.get(Icons.paste);
		return null;
	}

	private IconActionMap()	{}	// do not instantiate
}