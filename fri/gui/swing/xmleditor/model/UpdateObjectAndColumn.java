package fri.gui.swing.xmleditor.model;

/**
	Holding a changed value and its column.
	The value can be an attribute list or an element text.
*/

public class UpdateObjectAndColumn
{
	public final Object value;
	public final int column;

	public UpdateObjectAndColumn(Object value, int column)	{
		this.value = value;
		this.column = column;
	}

}
