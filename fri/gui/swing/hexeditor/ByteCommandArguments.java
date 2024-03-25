package fri.gui.swing.hexeditor;

import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.CommandArguments;

/**
	CommandArguments for HexTableModel actions, holding the MutableModel
	and optionally the original start offset (for MoveCommands).
*/

public class ByteCommandArguments extends CommandArguments
{
	/** Constructor for all except MoveCommands, holding just the model. */
	public ByteCommandArguments(MutableModel model)	{
		this.model = model;
	}

	/** Constructor for MoveCommands, additionally holding old start offset. */
	public ByteCommandArguments(MutableModel model, int start)	{
		this(model);
		if (start < 0)
			throw new IllegalArgumentException("Can not use a start offset < 0 for a MoveCommand: "+start);
		this.position = new Integer(start);
	}

	/**
		Returns a new target ControllerModelItem with old start offset.
		Used in DefaultMoveCommand to retrieve source model item context.
	*/
	public ModelItem getParent()	{
		if (getPosition() == null)
			throw new IllegalArgumentException("ByteCommandArguments do not contain start offset!");
			
		int start = getPosition().intValue();
		return new ControllerModelItem(start, start);
	}
}