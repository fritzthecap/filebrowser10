package fri.gui.swing.concordance.filter;

import java.awt.Container;
import fri.gui.swing.expressions.*;

/**
	Encapsulates the build process for the Filter MVC.
*/

public class FilterMvcBuilder
{
	private FilterController controller;
	
	public void build(Container viewContainer)	{
		FilterTreeMvcBuilder builder = newFilterTreeMvcBuilder();
		builder.build();
		FilterTreeController filterTreeController = builder.getController();

		FilterView view = new FilterView((FilterTreeView)filterTreeController.getView());
		controller = new FilterController(view, new FilterModel(), filterTreeController);
		
		viewContainer.add(view);
	}

	protected FilterTreeMvcBuilder newFilterTreeMvcBuilder()	{
		return new FilterTreeMvcBuilder();
	}
	
	public FilterController getController()	{
		return controller;
	}



	public static void main(String [] args)	{
		FilterMvcBuilder builder = new FilterMvcBuilder();
		javax.swing.JPanel p = new javax.swing.JPanel(new java.awt.BorderLayout());
		builder.build(p);
		javax.swing.JOptionPane.showMessageDialog(null, p, "Text Filter Settings", javax.swing.JOptionPane.PLAIN_MESSAGE);
	}

}
