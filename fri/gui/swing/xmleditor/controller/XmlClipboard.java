package fri.gui.swing.xmleditor.controller;

import fri.util.error.Err;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.clipboard.MdiClipboard;
import fri.gui.swing.treetable.JTreeTable;

/**
	The XML specific Clipboard for node exchange
	within one Document or between Documents.

	@author  Ritzberger Fritz
*/

public class XmlClipboard extends MdiClipboard
{
	/** Do-nothing constructor. */
	public XmlClipboard()	{}

	/**
		Returns the treetable where the items to paste came from.
		Needed for drag&drop.
	*/
	public JTreeTable getSourceComponent()	{
		return (JTreeTable)getSourceEditor();
	}

	/**
		Sets the treetable where the items to paste came from.
		Needed for drag&drop.
	*/
	public void setSourceComponent(JTreeTable treetable)	{
		setSourceEditor(treetable);
	}

	protected void errorHandling(ModelItem source, ModelItem target, boolean isMove)	{
		Err.error(new Exception(source.getError()));
	}

}