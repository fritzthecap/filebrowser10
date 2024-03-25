package fri.gui.swing.concordance;

import fri.gui.mvc.view.Selection;

class ConcordanceSelection implements Selection
{
	private ConcordancePanel concordancePanel;
	
	public ConcordanceSelection(ConcordancePanel concordancePanel)	{
		this.concordancePanel = concordancePanel;
	}
	
	/** Get selection from the view. This returns null if nothing is selected. */
	public Object getSelectedObject()	{
		return concordancePanel.getSelectedBlock();
	}
	
	/** Does nothing. */
	public void setSelectedObject(Object o)	{
	}

	/** Does nothing. */
	public void clearSelection()	{
	}
		
}
