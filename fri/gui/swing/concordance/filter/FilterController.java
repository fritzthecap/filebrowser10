package fri.gui.swing.concordance.filter;

import fri.gui.swing.actionmanager.connector.AbstractSwingController;
import fri.gui.swing.expressions.FilterTreeController;

/**
	Text filter controller that provides the OK-button.
*/

public class FilterController extends AbstractSwingController
{
	private FilterTreeController filterTreeController;
	
	public FilterController(FilterView view, FilterModel model, FilterTreeController filterTreeController)	{
		super(view);
		this.filterTreeController = filterTreeController;
		setModel(model);
	}

	protected void insertActions()	{
	}

	public boolean close()	{
		filterTreeController.close();
		((FilterModel)getModel()).save();
		return super.close();
	}

}
