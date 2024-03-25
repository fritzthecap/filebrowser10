package fri.gui.mvc.view.swing;

/**
	Implementers are required to quit with their data by ending
	cell- or other editors when this method gets called
	(else a transaction could miss the data of unfinished editor sessions
	e.g. in a JTable).
	<p>
	@author  Ritzberger Fritz
*/

public interface Commitable
{
	/**
		For View: Flush data to the Model (in memory) that are contained
		only in the View (open editors).<br>
		For Model: Store all data to a database (for transactions).
	*/
	public void commit();
	
}
