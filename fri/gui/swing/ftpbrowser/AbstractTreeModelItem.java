package fri.gui.swing.ftpbrowser;

import javax.swing.tree.DefaultMutableTreeNode;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.CommandArguments;
import fri.gui.mvc.model.swing.AbstractMutableTreeModelItem;

/**
	The basic ModelItem providing move pending state.<br>
	This ModelItem does NOT belong to the model, it is constructed on the fly
	and is not retained after its work (copy, move, insert, delete) is done.
	
	@author Fritz Ritzberger
*/

public abstract class AbstractTreeModelItem extends AbstractMutableTreeModelItem
{
	private String baseName;
	private AbstractTreeNode parentObject;

	
	public AbstractTreeModelItem(DefaultMutableTreeNode userObject)	{
		super(userObject);
	}


	/** All errors from filesystem or FTP are to be handled here. A GUI dialog has to be shown (TODO). */
	protected void error(Exception e)	{
		ProgressAndErrorReporter.error(e);
	}



	/**
		Compares classes, if different returns false.
		Calls super, if this returns true and is not copy, returns true.
		Else delegates to other isActionToSelf() method.
	*/
	protected boolean isActionToSelf(ModelItem targetItem, CommandArguments pasteInfo, boolean isCopy)	{
		if (targetItem.getClass().equals(getClass()) == false)
			return false;
			
		boolean identical = super.isActionToSelf(targetItem, pasteInfo, isCopy);
		if (isCopy == false && identical)	// cover case moving folder to folder
			return true;
			
		return isActionToSelf(targetItem, isCopy, identical);
	}
	
	protected boolean isActionToSelf(ModelItem targetItem, boolean isCopy, boolean identical)	{
		AbstractTreeNode fn = (AbstractTreeNode) getUserObject();
		AbstractTreeNode fnTarget = (AbstractTreeNode) targetItem.getUserObject();

		if (identical && isCopy)	{	// copy folder to itself -> make savecopy name
			fnTarget = (AbstractTreeNode) fn.getParent();
			((AbstractTreeModelItem) targetItem).parentObject = fnTarget;	// must shift target to parent folder
			makeSaveCopyName(fnTarget, fn.toString());
		}
		else	{	// construct child and check if it is a child in target
			boolean exists = fnTarget.existsChild(fn.toString()) != null;
			
			if (exists == false || isCopy == false)
				return exists;
			else	// make savecopy name
				makeSaveCopyName(fnTarget, fn.toString());
		}
		
		return false;
	}

	private void makeSaveCopyName(AbstractTreeNode fnTarget, String basename)	{
		int nr = 1;
		do	{
			this.baseName = "Copy"+nr+"_"+basename;
			nr++;
		}
		while (fnTarget.existsChild(this.baseName) != null);
	}


	/** Returns the basename of this node, or an alternate name when action is a savecopy. */
	protected String getBaseName()	{
		return baseName != null ? baseName : getUserObject().toString();
	}

	/** Overridden to enable a shift to the parent node if it is a folder savecopy. */
	public Object getUserObject()	{
		return parentObject != null ? parentObject : super.getUserObject();
	}

}
