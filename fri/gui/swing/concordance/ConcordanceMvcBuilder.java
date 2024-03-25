package fri.gui.swing.concordance;

import java.io.File;
import java.awt.Container;

public class ConcordanceMvcBuilder
{
	private ConcordanceController controller;
	
	public void build(Container viewContainer, File [] files)	{
		ConcordanceView view = newConcordanceView();
		controller = new ConcordanceController(view);
		view.renderActions(controller);
		
		viewContainer.add(view);
		
		if (files != null && files.length > 0)
			controller.open(files);
	}

	protected ConcordanceView newConcordanceView()	{
		return new ConcordanceView();
	}

	public ConcordanceController getController()	{
		return controller;
	}

}
