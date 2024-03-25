package fri.gui.swing.ftpbrowser;

import java.io.File;
import java.util.*;
import javax.swing.tree.*;
import fri.util.FileUtil;
import fri.gui.mvc.model.swing.AbstractMutableTreeModel;
import fri.gui.mvc.model.ModelItem;

/**
	A DefaultTreeModel that implements MutableModel to provide filesystem functionality for the controller.
*/

public class FilesystemTreeModel extends AbstractMutableTreeModel
{
	public FilesystemTreeModel()	{
		super(new FilesystemTreeNode(), true);
	}

	public ModelItem createModelItem(MutableTreeNode parent)	{
		return new FilesystemTreeModelItem((DefaultMutableTreeNode)parent);
	}

	/** Returns a List of DefaultMutableTreeNode representing the given pathes. */
	public List locatePathes(String [] pathes)	{
		return FilesystemTreeModel.locatePathes(pathes, true, this);
	}
	
	static List locatePathes(String [] pathes, boolean withRoot, AbstractMutableTreeModel model)	{
		Vector v = new Vector(pathes.length);
		
		for (int i = 0; i < pathes.length; i++)	{
			String [] parts = FileUtil.getPathComponents(new File(pathes[i]), withRoot, false);
			
			TreeNode n = model.locate(parts);
			
			if (n != null)	{
				v.add(n);
			}
			else	{
				System.err.println("WARNING: not found in tree model "+model.getClass()+": "+pathes[i]);
				Thread.dumpStack();
			}
		}
		
		return v;
	}
	
}
