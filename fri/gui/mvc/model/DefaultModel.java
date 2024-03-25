package fri.gui.mvc.model;

import fri.gui.mvc.controller.CommandArguments;

/**
	The default model implements the only method as do-nothing body.
	This can be the base for a "properties" model without ModelItems,
	needed to be a Model within the MVC.
	
	@author  Ritzberger Fritz
*/

public class DefaultModel implements Model
{
	/** This default implementation returns null. */
	public CommandArguments getModelItemContext(ModelItem item)	{
		return null;
	}

}