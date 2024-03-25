package fri.gui.swing.filebrowser;

import java.awt.*;
import fri.gui.swing.progressdialog.*;
import fri.gui.swing.yestoalldialog.*;

/**
	Extends CancelProgressDialog to add two dialogs:
	Ask for overwrite, ask for delete readonly files.
*/

public class TransactionDialog extends CancelProgressDialog implements
	TransactionObserver
{
	private OverwriteLauncher overwriteDialog;
	private DeleteReadOnlyLauncher deleteReadOnlyDialog;
	private TransactionContext context;
	
	
	/**
		Create a progress/cancel dialog and run it.
	*/
	public TransactionDialog(
		Component frame,
		String label,
		Runnable runnable,
		Runnable finish,
		long length)
	{
		super(frame, label, runnable, finish, length);
	}
	
	/**
		Create a progress/cancel dialog and do not run it.
	*/
	public TransactionDialog(
		Component frame,
		String label,
		Runnable runnable,
		Runnable finish)
	{
		super(frame, label, runnable, finish);
	}
	


	public void setContext(TransactionContext context)	{
		this.context = context;
	}
	

	/** Should name1 be overwritten with name2? */
	public boolean askOverwrite(String name1, String info1, String name2, String info2)
		throws UserCancelException
	{
		if (context != null)
			context.suspendTransaction();
		
		if (overwriteDialog == null)
			overwriteDialog = new OverwriteLauncher();
			
		int ret = overwriteDialog.show(getDialog(), name1, info1, name2, info2);

		if (context != null)
			context.resumeTransaction();
			
		return ret == YesToAllDialog.YES;
	}
	

	/** Should name be deleted even if it is readonly? */
	public boolean askDeleteReadOnly(String name, String info)
		throws UserCancelException
	{
		if (context != null)
			context.suspendTransaction();

		if (deleteReadOnlyDialog == null)
			deleteReadOnlyDialog = new DeleteReadOnlyLauncher();
		
		int ret = deleteReadOnlyDialog.show(getDialog(), name, info);

		if (context != null)
			context.resumeTransaction();

		return ret == YesToAllDialog.YES;
	}

}