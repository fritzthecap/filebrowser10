package fri.gui.swing.ftpbrowser;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import fri.util.ftp.ObservableFtpClient;
import fri.util.managers.InstanceManager;

/**
	Ships unique Models per FTP server and one for the local Filesystem.
	
	@author Fritz Ritzberger
*/

public abstract class TreeModelFactory
{
	private static InstanceManager filesystemManager = new InstanceManager();
	private static InstanceManager ftpModelManager = new InstanceManager();
	
	
	/** Returns a singleton TreeModel for the filesystem. */
	public static FilesystemTreeModel getFilesystemTreeModel()	{
		return (FilesystemTreeModel)filesystemManager.getInstance("Filesystem", new FilesystemTreeModel());
	}
	
	/** Releases the instance count on the singleton TreeModel for the filesystem, frees it at last. */
	public static void freeFilesystemTreeModel()	{
		filesystemManager.freeInstance("Filesystem");
	}
	
	
	/** Passing null returns an empty TreModel for an unconnected FTP tree, else a new TreeModel or a cached one is delivered. */
	public static DefaultTreeModel getFtpServerTreeModel(ObservableFtpClient ftpClient)	{
		if (ftpClient == null)	{	// empty model for unconnected view
			return new DefaultTreeModel(new DefaultMutableTreeNode("(Not Connected)"));	// null root in JDK 1.3 exception
		}
		return (FtpServerTreeModel)ftpModelManager.getInstance(ftpClient, new FtpServerTreeModel(ftpClient));
	}

	/** Releases the instance count on the singleton TreeModel for the FTP client, frees it at last. */
	public static Object freeFtpServerTreeModel(ObservableFtpClient ftpClient)	{
		return ftpModelManager.freeInstance(ftpClient);
	}


	private TreeModelFactory()	{}

}