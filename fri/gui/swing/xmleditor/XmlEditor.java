package fri.gui.swing.xmleditor;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import fri.util.error.Err;
import fri.gui.swing.error.GUIErrorHandler;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.xmleditor.view.XmlMdiPane;
import fri.gui.swing.xmleditor.controller.XmlEditController;
import fri.gui.swing.htmlbrowser.HttpProxyDialog;

/**
	XML Editor main class.

	@author Fritz Ritzberger, 2002
*/

public class XmlEditor extends GuiApplication
{
	private static final String version = "1.1";
	private static XmlEditor singleton;
	private GUIErrorHandler eh;
	private XmlMdiPane pane;


	public static XmlEditor singleton(String [] uris)	{
		if (singleton == null)	{
			singleton = uris == null ? new XmlEditor() : new XmlEditor(uris);
			HttpProxyDialog.load();
		}
		else	{
			singleton.setVisible(true);
			for (int i = 0; uris != null && i < uris.length; i++)
				singleton.pane.createEditor(uris[i]);
		}
		return singleton;
	}
	
	
	/** Open an empty editor window. */
	public XmlEditor()	{
		this(null);
	}

	/** Open an editor window holding all given arguments that are XML files. */
	public XmlEditor(String [] args)	{
		super("XML Editor "+version);

		// create a error handler that can show dialogs
		eh = new GUIErrorHandler(this);
		Err.setHandler(eh);

		// create a MDI pane and a controller for all internal frames
		XmlEditController controller = new XmlEditController(this);
		pane = new XmlMdiPane(this, controller);
		// set the menubar from controller and MDI pane
		setJMenuBar(pane.getMenuBar());

		getContentPane().add(pane);
		getContentPane().add(pane.getToolBar(), BorderLayout.NORTH);

		// initialize GUIApplication
		init();

		// open all XML or DTD from commandline
		for (int i = 0; args != null && i < args.length; i++)	{
			pane.createEditor(args[i]);
		}
	}


	/** Overridden to set the GUI error handler to global error management, */
	public void windowActivated(WindowEvent e)	{
		super.windowActivated(e);
		Err.setHandler(eh);
	}

	/** Overridden to check for unsaved changes in MDI pane. */
	public boolean close()	{
		if (pane.close())
			return super.close();
		else
			return false;
	}



	/**
		XML Editor Application Main.<br>
		Syntax: java fri.gui.swing.xmleditor.XmlEditor [file.xml file.dtd ...]
	*/
	public static void main(String [] args)
		throws Exception
	{
		new XmlEditor(args);
	}
}