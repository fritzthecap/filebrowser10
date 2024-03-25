package fri.gui.swing.xmleditor.controller.edits;

import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.controller.CommandArguments;

/**
	Class that stores two models and the paste-flag (into createData).
	The sourceModel is the model where the source item comes from,
	the destinationModel is the model where the source item goes to.
*/

public class PasteArguments extends CommandArguments
{
	public PasteArguments(MutableModel destinationModel, Integer flag, MutableModel sourceModel)	{
		this.model = destinationModel;
		this.createData = flag;
		this.sendingModel = sourceModel;
	}

}