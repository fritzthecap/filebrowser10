package fri.gui.swing.mailbrowser.rules.editor;

import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.mailbrowser.Language;

/**
	Frame application for mail rules editing.
*/

public class RulesFrame extends GuiApplication
{
	private RulesPanel panel;
	
	public RulesFrame()	{
		super(Language.get("Mail_Rules_Editor"));
		
		panel = new RulesPanel();
		getContentPane().add(panel);

		init();	// show the frame
	}

	public boolean close()	{
		if (panel.close())
			return super.close();
		return false;
	}


	/** Rules editor application main. */
	public static void main(String [] args)	{
		new RulesFrame();
	}

}
