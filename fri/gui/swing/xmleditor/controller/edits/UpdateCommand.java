package fri.gui.swing.xmleditor.controller.edits;

import java.awt.Component;
import fri.gui.swing.util.RefreshTreeTable;
import fri.gui.swing.treetable.JTreeTable;
import fri.gui.swing.xmleditor.model.MutableXmlNode;
import fri.gui.swing.xmleditor.model.UpdateObjectAndColumn;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.AbstractUpdateCommand;

/**
	The UpdateCommand for a XML Nodes. This is not a standard edit,
	as it manipulates the tree nodes directly (without triggering new
	update edits by the controller's undo listener).

	@author  Ritzberger Fritz
*/

public class UpdateCommand extends AbstractUpdateCommand
{
	private MutableXmlNode treenode;

	/**
		Create a UpdateCommand, do not execute it.
		@param treenode the MutableXmlNode where the change happened.
		@param oldValue the old value and its column, as UpdateObjectAndColumn
		@param newValue the new value and its column, as UpdateObjectAndColumn
	*/
	public UpdateCommand(Component c, Object treenode, Object oldValue, Object newValue)	{
		super(c, null, oldValue, newValue);
		this.treenode = (MutableXmlNode)treenode;
	}
	
	/**
		Set the new value directly to the treetable column,
		passes by the model and its undo listener.
		This doit() is called by redo() in superclass.
	*/
	public Object doit()	{
		setValue(null, newValue);
		return treenode;
	}

	protected void setValue(ModelItem item, Object updateData)	{
		UpdateObjectAndColumn arg = (UpdateObjectAndColumn)updateData;
		
		// trick out the model (and its undo listener!) by calling the node directly
		treenode.setColumnObject(arg.column, arg.value);

		if (getTargetEditor() instanceof JTreeTable)	{	// could be TextArea?
			RefreshTreeTable.refresh((JTreeTable)getTargetEditor());
		}
	}

}