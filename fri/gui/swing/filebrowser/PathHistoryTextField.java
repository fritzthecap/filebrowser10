package fri.gui.swing.filebrowser;

import javax.swing.ComboBoxEditor;
import java.awt.Component;

class PathHistoryTextField extends PathTextField implements
	ComboBoxEditor
{
	PathHistoryTextField(TreeExpander texp)	{
		super(texp);
	}
	
	/** Returns the component that should be added to the tree hierarchy for this editor. */
	public Component getEditorComponent() 	{
		return this;
	}

	/** Returns the edited item. */
	public Object getItem() 	{
		return super.getText();
	}

	/** Set the item that should be edited. */
	public void setItem(Object anObject)	{
		if (anObject == null)
			return;
		super.setText((String)anObject);
	}

}