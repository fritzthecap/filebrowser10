package fri.gui.swing.ftpbrowser;

import java.util.List;
import fri.util.managers.InstanceManager;
import fri.util.ftp.FtpClient;
import fri.gui.awt.clipboard.SystemClipboard;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.clipboard.DefaultClipboard;

/**
	Provides data exchange between different FTP client windows.

	@author  Ritzberger Fritz
*/

public class FtpClipboard extends DefaultClipboard
{
	private static InstanceManager clipboardManager = new InstanceManager();
	
	/** Returns a FTP singleton clipboard that provides data exchange between different FTP windows instances. */
	public static FtpClipboard getFtpClipboard()	{
		return (FtpClipboard)clipboardManager.getInstance("FtpClipboard", new FtpClipboard());
	}

	/** Free the FTP clipboard, instance count gets decremented and Object is freed at last. */
	public static Object freeFtpClipboard()	{
		FtpClipboard last = (FtpClipboard)clipboardManager.freeInstance("FtpClipboard");
		if (last != null)	// is last instance
			last.clear();	// garbage collect
		return last;
	}

	/** Commented out! Overridden to check for system clipboard files. */
	public boolean isEmpty()	{
    return super.isEmpty();
    
	  /* This check has been commented out as it can last
	   * very long on certain operating systems.

		boolean empty =  super.isEmpty();
		if (empty == false)
			return false;
			
		Object o = SystemClipboard.getFilesFromClipboard();
		if (o != null)	{
			this.nodes = FtpController.filesToModelItems((List)o);
			System.err.println("FtpClipboard having files: "+o);
			return false;
		}
		
		return true;
     */
	}

	/** Free the possibly copied or cutten nodes for a closed FTP client (enables garbage collection). */
	void freeFtpClient(FtpClient ftpClient)	{
		ModelItem [] items = getSourceModelItems();
		
		if (items != null && items.length > 0 && items[0] instanceof FtpServerTreeModelItem)	{
			FtpServerTreeModelItem mi = (FtpServerTreeModelItem)items[0];	// all nodes come from same model
			
			if (mi != null && mi.getFtpClient() != null && mi.getFtpClient().equals(ftpClient))	{
				// a copy or move from the released FTP server has been initiated,
				// release the items now as the FTP client was closed
				clear();
				System.err.println("FtpClipboard released cutten/copied FTP client items: "+ftpClient);
			}
		}
	}
	
}