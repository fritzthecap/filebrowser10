package fri.gui.swing.mailbrowser.rules.editor;

import java.awt.BorderLayout;
import javax.swing.*;
import fri.util.os.OS;

/**
	The panel that holds the toolbar and the rules table.
	It renders the actions of the controller within toolbar.
*/

public class RulesPanel extends JPanel
{
	private RulesController controller;
	
	public RulesPanel() {
		super(new BorderLayout());
		
		RulesTable rulesTable = new RulesTable();
		controller = new RulesController(rulesTable);
		
		JToolBar tb = new JToolBar(JToolBar.VERTICAL);
		if (OS.isAboveJava13) tb.setRollover(true);
		
		controller.visualizeAction(RulesController.ACTION_SAVE, tb);
		tb.addSeparator();
		controller.visualizeAction(RulesController.ACTION_NEW, tb);
		controller.visualizeAction(RulesController.ACTION_DELETE, tb);
		tb.addSeparator();
		controller.visualizeAction(RulesController.ACTION_CUT, tb);
		controller.visualizeAction(RulesController.ACTION_COPY, tb);
		controller.visualizeAction(RulesController.ACTION_PASTE, tb);

		add(tb, BorderLayout.WEST);
		add(rulesTable, BorderLayout.CENTER);
	}

	public boolean close()	{
		return controller.close();
	}

}
