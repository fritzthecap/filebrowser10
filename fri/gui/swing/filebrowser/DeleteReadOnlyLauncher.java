package fri.gui.swing.filebrowser;

import java.awt.Component;
import fri.gui.swing.yestoalldialog.*;

/**
	Show a modal overwrite dialog, optional from a background thread.
	Return the dialog result synchronously, by EventQueue.invokeAndWait().
*/

public class DeleteReadOnlyLauncher extends YesToAllLauncher
{
	private String tgt, tinf;
	private Component parent;

	
	/** Called from background thread to show dialog. */
	public int show(Component parent, String tgt, String tinf)
		throws UserCancelException
	{
		this.parent = parent;
		this.tgt = tgt;
		this.tinf = tinf;

		return super.show();
	}
	
	/** Called from event: create and init dialog. */
	protected void startDialog()	{
		if (dialog == null)	{
			dialog = new DeleteReadOnlyDialog(parent);
		}
		
		((DeleteReadOnlyDialog)dialog).setInfo(tgt, tinf);

		super.startDialog();
	}
	
}