package fri.gui.mvc.model.swing;

import javax.swing.tree.DefaultMutableTreeNode;
import fri.gui.mvc.model.ModelItem;

/**
	Utilities in conjunction with tree and tree nodes.
	@author Ritzberger Fritz
*/

public abstract class TreeModelItemUtil
{
	/**
		Needed for detecting impossible hierarchical action: pasting to sub-folder.
		<p />
		Returns <i>new DefaultMutableTreeNode [] { target, source }</i> if target is descendant
		of one of the source items. If there is no conflict, returns null.
		The <i>sourceItems[i].getUserObject()</i> must return a <i>DefaultMutableTreeNode</i>!
		<p />
		Mind that this checks by identity "==", not by userObject path!
	*/
	public static DefaultMutableTreeNode [] checkForDescendants(DefaultMutableTreeNode target, ModelItem [] sourceItems)	{
		for (int i = 0; i < sourceItems.length; i++)	{
			DefaultMutableTreeNode source = (DefaultMutableTreeNode)sourceItems[i].getUserObject();
			
			if (source != target && source.isNodeDescendant(target))	{	// target below source means error
				return new DefaultMutableTreeNode [] { target, source };
			}
		}
		return null;
	}


	private TreeModelItemUtil()	{}
	
}