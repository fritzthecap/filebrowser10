package fri.gui.swing.mailbrowser;

import fri.util.error.Err;
import fri.util.managers.InstanceManager;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.clipboard.DefaultClipboard;

/**
	@author  Ritzberger Fritz
*/

public class MailClipboard extends DefaultClipboard
{
	private static InstanceManager clipboardManager = new InstanceManager();

	public static MailClipboard getMailClipboard()	{
		return (MailClipboard)clipboardManager.getInstance("MailClipboard", new MailClipboard());
	}

	public static Object freeMailClipboard()	{
		MailClipboard last = (MailClipboard)clipboardManager.freeInstance("MailClipboard");
		if (last != null)	// is last instance
			last.clear();	// garbage collect
		return last;
	}

	protected void errorHandling(ModelItem source, ModelItem target, boolean isMove)	{
		Err.warning(Language.get("Action_Failed_On_Item")+" "+source);
	}

}