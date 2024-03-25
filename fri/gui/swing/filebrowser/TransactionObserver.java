package fri.gui.swing.filebrowser;

import fri.util.observer.CancelProgressObserver;
import fri.gui.swing.yestoalldialog.UserCancelException;

/**
	Extend CancelProgressObserver to confirm deletion of readonyl files
	and to confirm overwrting of existing files.
*/

public interface TransactionObserver extends CancelProgressObserver
{
	/** Should name1 be overwritten with name2? */
	public boolean askOverwrite(String name1, String info1, String name2, String info2)
	throws UserCancelException;

	/** Should name be deleted even if it is readonly? */
	public boolean askDeleteReadOnly(String name, String info)
	throws UserCancelException;
}