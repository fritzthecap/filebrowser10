package fri.gui.swing.ftpbrowser;

import java.io.File;
import java.util.*;
import java.awt.Point;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import fri.util.ftp.*;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.dnd.JavaFileList;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.model.swing.AbstractMutableTreeModel;
import fri.gui.mvc.view.swing.TreeSelectionDnd;
import fri.gui.mvc.controller.swing.dnd.AbstractAutoScrollingPopupDndPerformer;

/**
	Drag&Drop handler that autoscrolls its tree. It performs
	copy and move actions within File- or FTP-view.

	@author  Ritzberger Fritz
*/

public class FtpDndPerformer extends AbstractAutoScrollingPopupDndPerformer
{
	private FtpController controller;
	private List dropTargetList;
	private ModelItem [] droppedNodes;


	/**
		Create a autoscrolling drag and drop handler for FTP and local files.
	*/
	public FtpDndPerformer(JTree tree, JScrollPane scrollPane, FtpController controller)	{
		super(tree, scrollPane);
		this.controller = controller;
	}


	private JTree getTree()	{
		return (JTree)sensor;
	}


	/** Checks for types this handler is supporting: local and FTP files. */
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)	{
			if (flavors[i].equals(DataFlavor.javaFileListFlavor))	{
				return DataFlavor.javaFileListFlavor;
			}
			else
			if (flavors[i].equals(NetworkFileTransferable.networkFileFlavor))	{
				return NetworkFileTransferable.networkFileFlavor;
			}
		}
		return null;
	}


	// send methods

	/** Implements DndPerformer: sending selected items. */	
	public Transferable sendTransferable()	{
		if (activated == false || getTree().isEditing())
			return null;
		
		// get selected nodes
		List intransfer = new Vector();
		List selection = (List)controller.getSelection().getSelectedObject();

		if (selection == null || selection.size() <= 0)	{
			return null;
		}
		
		boolean isFile = getTree().getModel() instanceof FilesystemTreeModel;

		// check for dragability and pack selected nodes
		for (Iterator it = selection.iterator(); it.hasNext(); )	{
			AbstractTreeNode treeNode = (AbstractTreeNode)it.next();

			if (treeNode.isDragable() == false)	{
				System.err.println("FtpDndPerformer: can not drag "+treeNode);
				return null;
			}

			Object o;
			if (isFile)	{
				FilesystemTreeNode fstn = (FilesystemTreeNode)treeNode;
				o = (File)fstn.getUserObject();
			}
			else	{
				o = new NetworkFileTransferable.NetworkFile((FtpServerTreeNode)treeNode);
			}
			
			intransfer.add(o);
		}
		
		System.err.println("sending drag data: "+intransfer);
		controller.getClipboard().clear();	// clear clipboard to avoid ambiguities
		controller.getClipboard().setSourceModel((MutableModel)getTree().getModel());

		if (isFile)	{
			return new JavaFileList(intransfer);
		}
		else	{
			return new NetworkFileTransferable(intransfer);
		}
	}



	// receive methods

	protected boolean receive(Point p, List data, boolean isCopy)	{
		Object o = data.get(0);	// look what has arrived
		
		// received data can come from this process or from another Java VM.
		// As we can not rely on having the model in this VM, we must request
		// it from TreeModelFactory (the same for FtpClient).
		
		// localize nodes in view and create ModelItem(s) to cut/copy
		if (o instanceof File)	{
			droppedNodes = FtpController.filesToModelItems(data);	// convert to list of ModelItem
		}
		else	{
			NetworkFileTransferable.NetworkFile nwf = (NetworkFileTransferable.NetworkFile)o;
			String [] pathes = new String[data.size()];
			
			for (int i = 0; i < data.size(); i++)	{
				NetworkFileTransferable.NetworkFile n = (NetworkFileTransferable.NetworkFile)data.get(i);
				pathes[i] = n.absolutePath;
				//System.err.println("FtpDndPerformer receiving "+pathes[i]);
			}
			
			AbstractMutableTreeModel model;
			List treeNodes;
			
			if (nwf.host != null)	{	// is a FTP transfer, all have the same source
				// checkout the FtpClient and the TreeModel
				ObservableFtpClient ftpClient = FtpClientFactory.getFtpClient(null, nwf.host, nwf.port, nwf.user, nwf.password, null);
				FtpClientFactory.freeFtpClient(ftpClient);

				FtpServerTreeModel m = (FtpServerTreeModel)TreeModelFactory.getFtpServerTreeModel(ftpClient);
				TreeModelFactory.freeFtpServerTreeModel(ftpClient);
				treeNodes = m.locatePathes(pathes);	// convert to list of DefaultMutableTreeNodes
				model = m;
			}
			else	{	// is a file in local filesystem
				FilesystemTreeModel m = TreeModelFactory.getFilesystemTreeModel();
				TreeModelFactory.freeFilesystemTreeModel();
				treeNodes = m.locatePathes(pathes);	// convert to list of DefaultMutableTreeNodes
				model = m;
			}

			droppedNodes = FtpController.toModelItems(treeNodes, model);	// convert to list of ModelItem
		}

		DefaultMutableTreeNode dropTarget = (DefaultMutableTreeNode) new TreeSelectionDnd(getTree()).getObjectFromPoint(p);
		System.err.println("... at drop node "+dropTarget);

		dropTargetList = new Vector();
		dropTargetList.add(dropTarget);

		// get frame to foreground
		ComponentUtil.getFrame(getTree()).setVisible(true);

		// need to focus the drop tree, as controller will construct
		// drop target ModelItem from current view and provide transfer size
		getTree().requestFocus();
		controller.setView(getTree());
		
		// open popup choice: copy / move / cancel
		showPopup();

		return true;
	}

	
	/** Implements this to receive popup choice "copy". */
	protected void copyCallback()	{
		controller.getClipboard().copy(droppedNodes);	// clipboard.setSourceModel MUST NOT be called!
		controller.cb_Paste(dropTargetList);
		cancelCallback();	// free lists
	}

	/** Implements this to receive popup choice "move". */
	protected void moveCallback()	{
		controller.getClipboard().cut(droppedNodes);	// clipboard.setSourceModel MUST NOT be called!
		controller.cb_Paste(dropTargetList);
		cancelCallback();	// free lists
	}

	/** Implements this to receive popup choice "cancel". */
	protected void cancelCallback()	{
		dropTargetList = null;
		droppedNodes = null;
	}

}