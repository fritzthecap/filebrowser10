package fri.gui.swing.hexeditor;

import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.AbstractUpdateCommand;

/**
	The UpdateCommand for a hexbyte. This is not a standard edit,
	as it manipulates the table model directly (without triggering new
	update edits by the controller's undo listener).

	@author  Ritzberger Fritz
*/

public class UpdateCommand extends AbstractUpdateCommand
{
	/**
		Create a UpdateCommand, do not execute it.
		@param editor the HexTable where the change happened.
		@param oldValue the old value and its column, as ByteAndOffset
		@param newValue the new value and its column, as ByteAndOffset
	*/
	public UpdateCommand(Object editor, Object oldValue, Object newValue)	{
		super(editor, null, oldValue, newValue);
	}
	
	protected void setValue(ModelItem item, Object updateData)	{
		ByteAndPosition bap = (ByteAndPosition)updateData;
		((HexTable)getTargetEditor()).getModel().setValueAt(bap.theByte, bap.row, bap.column);
	}

}