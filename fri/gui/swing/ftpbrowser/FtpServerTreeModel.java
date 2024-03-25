package fri.gui.swing.ftpbrowser;

import java.util.List;
import javax.swing.tree.*;
import fri.util.ftp.ObservableFtpClient;
import fri.gui.mvc.model.swing.AbstractMutableTreeModel;
import fri.gui.mvc.model.ModelItem;

/**
	A DefaultTreeModel that implements MutableModel to provide FTP functionality for the controller.
*/

public class FtpServerTreeModel extends AbstractMutableTreeModel
{
	public FtpServerTreeModel(ObservableFtpClient ftpClient)	{
		super(new FtpServerTreeNode(ftpClient), true);
	}

	public ModelItem createModelItem(MutableTreeNode parent)	{
		return new FtpServerTreeModelItem((DefaultMutableTreeNode)parent);
	}

	/** Returns a List of DefaultMutableTreeNode representing the given pathes. */
	public List locatePathes(String [] pathes)	{
		return FilesystemTreeModel.locatePathes(pathes, false, this);
	}

}
