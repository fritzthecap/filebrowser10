package fri.gui.mvc.controller;

import fri.util.application.Closeable;
import fri.gui.mvc.model.Model;
import fri.gui.mvc.view.View;

/**
	Implementers must be able to switch to a served View dynamically.
	A Controller always references its Model via <i>view.getModel()</i>.
	It should not store any data or state.
	<p>
	The <i>close()</i> call should close the contained View.
	<p>
	Implementers will provide a set of Actions for their View.
	
	@author  Ritzberger Fritz
*/

public interface Controller extends Closeable
{
	/** Set a View into the controller. */
	public void setView(View view);

	/** Returns the current View of the controller. */
	public View getView();

	/** Returns the current View's Model or null if View is null. */
	public Model getModel();
	
	/** Sets a new Model into this controller's View. Throws NullPointerExeption if View is null! */
	public void setModel(Model model);

}
