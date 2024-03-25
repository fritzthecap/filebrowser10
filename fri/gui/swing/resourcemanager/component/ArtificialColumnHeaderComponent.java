package fri.gui.swing.resourcemanager.component;

import javax.swing.*;
import fri.gui.awt.resourcemanager.component.ArtificialComponent;

/**
	The proxy object for a column header within a JTable.
	Gets its tab index at construction and offers indexed text resource method.
*/

class ArtificialColumnHeaderComponent extends ArtificialComponent
{
	/** Construct an item proxy with the container (Choice/JComboBox/List), the getter method and the index. */
	public ArtificialColumnHeaderComponent(Object parent, int index)	{
		this.parentComponent = (JTable) parent;	// ensure by cast that it is a JTable
		this.index = index;
	}

	/** Simply returns "columnheader". The label text will be retrieved and appended by ResourceComponentName. */
	public String getName()	{
		return "columnheader";
	}


	/** Returns the column header text by calling to the parent Component via reflection. */
	public String getText()	{
		return (String) ((JTable)parentComponent).getColumnModel().getColumn(index).getHeaderValue();
	}

	/** Sets the new column header text by calling to the parent Component via reflection. */
	public void setText(String newText)	{
		((JTable)parentComponent).getColumnModel().getColumn(index).setHeaderValue(newText);
	}

}
