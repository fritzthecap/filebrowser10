package fri.gui.swing.xmleditor.model;

import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.CommandArguments;

/**
	Controller class for holding the model, parent and position
	of a node within a hierarchy.
*/

public class ModelParentAndPosition extends CommandArguments
{
	public ModelParentAndPosition(MutableModel model, ModelItem parent, Integer position)	{
		this.model = model;
		this.parent = parent;
		this.position = position;
	}
}