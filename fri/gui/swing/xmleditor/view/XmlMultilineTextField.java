package fri.gui.swing.xmleditor.view;

import java.awt.Window;
import java.awt.Frame;
import javax.swing.CellEditor;
import fri.gui.swing.text.MultilineTextField;
import fri.gui.swing.text.MultilineEditDialog;

/**
	Derivation of MultilineTextField that sets a title
	from the edited node tag, and sets a property if
	the edited text can be CDATA.
*/

class XmlMultilineTextField extends MultilineTextField
{
	private boolean canBeCDATA;
	private CellEditor cellEditor;

	XmlMultilineTextField()	{
		super();
	}

	/** If the multiline text dialog should stop cell editing after close, call this. */
	public void setCellEditorForStop(CellEditor cellEditor)	{
		this.cellEditor = cellEditor;
	}

	/** Flag for attaching a "Mark as CDATA" popup menu item. */
	public void setCanBeCDATA(boolean canBeCDATA)	{
		this.canBeCDATA = canBeCDATA;
	}

	/** Overridden to create ElementEditDialog. */
	protected MultilineEditDialog createEditDialog(Window parent, String text, String title)	{
		return new ElementEditDialog((Frame) parent, this, text, title, canBeCDATA);
	}

	/** Overridden to call super and stop cell editing after. */
	public void openEditor()	{
		super.openEditor();

		if (cellEditor != null)
			cellEditor.stopCellEditing();
	}

}
