package fri.gui.swing.tree;

import javax.swing.tree.*;

/**
	Convenience class for editable trees. Implementers of the contained interface
	can listen to node cell editor results and convert the entered String to some
	useful user object.
	<p>
	This class works even when no listener was defined.
	
	@author Fritz Ritzberger, 2003
*/

public class EditNotifyingTreeModel extends DefaultTreeModel
{
	private Listener lsnr;
	
	/** Implementers want to deny changes to nodes or convert the new value to an useful user object. */
	public interface Listener
	{
		/**
			Returns null if the listener wants to deny node change, else the returned object will be passed to node.
			The passed path contains the original unchanged node.
		*/
		public Object valueForPathChanging(TreePath path, Object newValue);
	}
	

	public EditNotifyingTreeModel(TreeNode root)	{
		super(root);
	}

	public EditNotifyingTreeModel(TreeNode root, boolean askAllowsChildren)	{
		super(root, askAllowsChildren);
	}
	
	public EditNotifyingTreeModel(TreeNode root, Listener renameListener)	{
		super(root);
		setRenameListener(renameListener);
	}
	
	
	/** Set a rename listener to this model. */
	public void setRenameListener(Listener renameListener)	{
		this.lsnr = renameListener;
	}
	
	/**
		Overridden to catch any treenode rename,
		and to provide the possibility to return a custom object, for listeners.
	*/
	public void valueForPathChanged(TreePath path, Object newValue)	{
		if (lsnr == null || (newValue = lsnr.valueForPathChanging(path, newValue)) != null)	{
			super.valueForPathChanged(path, newValue);
		}
	}

}
