package fri.gui.swing.expressions;

import java.awt.Container;

/**
	Encapsulates the build process for the FilterTree MVC.
*/

public class FilterTreeMvcBuilder
{
	private FilterTreeController controller;
	
	public void build()	{
		build(null);
	}
	
	public void build(Container viewContainer)	{
		FilterTreeView view = newFilterTreeView();
		controller = new FilterTreeController(view);	// model is implicit by FilterTreePersistence and modelManager 
		view.renderActions(controller);
		
		if (viewContainer != null)
			viewContainer.add(view);
	}

	protected FilterTreeView newFilterTreeView()	{
		return new FilterTreeView();
	}
	
	public FilterTreeController getController()	{
		return controller;
	}



	public static void main(String [] args)	{
		FilterTreeMvcBuilder builder = new FilterTreeMvcBuilder();
		javax.swing.JFrame f = new javax.swing.JFrame("FilterTreeView");
		builder.build(f.getContentPane());
		f.pack();
		f.setVisible(true);
		FilterTreeMvcBuilder builder2 = new FilterTreeMvcBuilder();
		javax.swing.JFrame f2 = new javax.swing.JFrame("FilterTreeView");
		builder2.build(f2.getContentPane());
		f2.pack();
		f2.setLocation(20, 20);
		f2.setVisible(true);
	}

}
