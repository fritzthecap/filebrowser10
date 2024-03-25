package fri.gui.swing.xmleditor.view;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import fri.util.os.OS;
import fri.gui.CursorUtil;
import fri.gui.swing.mdi.*;
import fri.gui.swing.xmleditor.controller.*;
import fri.gui.swing.editor.EditHistoryFileChooser;

/**
	The MDI view containing internal XML editor frames. It can create multiple
	internal frames showing different XML documents. It listenes to activation
	of these frames and then sets the current treetable to controller.
	<p>
	XmlMdiPane provides standard window menu items like tile, cascade, etc.,
	and it provides an addable menu containing these ("Window").
	It provides a toolbar holding all actions launchable on XML documents.
	The toolbar is filled from controller, that provides a popup offering
	the same actions.
*/

public class XmlMdiPane extends SwapableMdiPane implements
	FrameCreator	// open document windows, close container window
{
	private WindowListener windowListener;	// the listener that closes the container window
	private XmlEditController controller;
	private JToolBar toolbar;
	private JMenuBar menubar;
	private EditHistoryFileChooser history;


	/**
		Create a MDI view for internal XML editor frames, opening
		an initial XML document internal frame window with passed URI.
		@param windowListener parent frame window listener that closes the parent frame. windowClosing(null) will be called on this.
		@param controller the controller that renders in toolbar
	*/
	public XmlMdiPane(WindowListener windowListener, XmlEditController controller)	{
		this(windowListener, controller, null);
	}

	/**
		Create a MDI view for internal XML editor frames, opening
		an initial XML document internal frame window with passed URI.
		@param windowListener parent frame window listener that closes the parent frame. windowClosing(null) will be called on this.
		@param controller the controller that renders in toolbar
		@param uri the unified resource locator of the XML document to open
	*/
	public XmlMdiPane(WindowListener windowListener, XmlEditController controller, String uri)	{
		this.windowListener = windowListener;
		this.controller = controller;
		this.history = new EditHistoryFileChooser(this);

		controller.setFrameCreator(this);

		if (uri != null)	{
			createEditor(uri);
		}
		
		new FileDndPerformer(this, controller);
		
		setPreferredSize(new Dimension(700, 800));
	}


	/** Returns the toolbar containing all actions of the XML edit panel. */
	public JToolBar getToolBar()	{
		if (toolbar == null)	{
			toolbar = new JToolBar(JToolBar.HORIZONTAL);
			if (OS.isAboveJava13) toolbar.setRollover(true);
			fillToolBar(toolbar);

			// build action popup and install it
			controller.installPopup(fillPopup(new JPopupMenu()));
		}
		return toolbar;
	}

	/** Returns the menubar containing all actions of the XML edit panel. */
	public JMenuBar getMenuBar()	{
		if (menubar == null)	{
			menubar = new JMenuBar();
			fillMenuBar(menubar);
		}
		return menubar;
	}


	private void fillToolBar(JToolBar toolbar)	{
		controller.visualizeAction(XmlEditController.MENUITEM_OPEN, toolbar);
		controller.visualizeAction(XmlEditController.MENUITEM_SAVE, toolbar);
		toolbar.addSeparator();
		controller.visualizeAction(XmlEditController.MENU_NEW, toolbar);
		controller.visualizeAction(XmlEditController.MENUITEM_DELETE, toolbar);
		toolbar.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_CUT, toolbar);
		controller.visualizeAction(XmlEditController.MENUITEM_COPY, toolbar);
		controller.visualizeAction(XmlEditController.MENUITEM_PASTE, toolbar);
		toolbar.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_UNDO, toolbar);
		controller.visualizeAction(XmlEditController.MENUITEM_REDO, toolbar);
		toolbar.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_EXPAND, toolbar);
		controller.visualizeAction(XmlEditController.MENUITEM_FIND, toolbar);
		toolbar.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_EDIT, toolbar);
		controller.visualizeAction(XmlEditController.MENUITEM_VIEW, toolbar);
		controller.visualizeAction(XmlEditController.MENUITEM_SHOW_DTD, toolbar);
		controller.visualizeAction(XmlEditController.MENUITEM_VALIDATE, toolbar);
		toolbar.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_CONFIGURE, toolbar);
	}

	/** Overridden to include controller actions. Appends super.fillMenuBar() at end. */
	public void fillMenuBar(JMenuBar menubar)	{
		JMenu file = new JMenu("Document");
		controller.visualizeAction(XmlEditController.MENUITEM_OPEN, file, false);
		controller.visualizeAction(XmlEditController.MENUITEM_SAVE, file, false);
		controller.visualizeAction(XmlEditController.MENUITEM_SAVEAS, file, false);
		file.addSeparator();
		JMenuItem historyItem = new JMenuItem("History");
		historyItem.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				history.show();
			}
		});
		file.add(historyItem);
		file.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_EDIT, file, false);
		controller.visualizeAction(XmlEditController.MENUITEM_VIEW, file, false);
		controller.visualizeAction(XmlEditController.MENUITEM_SHOW_DTD, file, false);
		controller.visualizeAction(XmlEditController.MENUITEM_VALIDATE, file, false);
		file.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_CONFIGURE, file, false);
		file.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_CLOSE, file, false);
		menubar.add(file);

		JMenu edit = new JMenu("Edit");
		controller.visualizeAction(XmlEditController.MENU_NEW, edit, false);
		controller.visualizeAction(XmlEditController.MENUITEM_DELETE, edit, false);
		edit.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_CUT, edit, false);
		controller.visualizeAction(XmlEditController.MENUITEM_COPY, edit, false);
		controller.visualizeAction(XmlEditController.MENUITEM_PASTE, edit, false);
		edit.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_UNDO, edit, false);
		controller.visualizeAction(XmlEditController.MENUITEM_REDO, edit, false);
		edit.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_EXPAND, edit, false);
		controller.visualizeAction(XmlEditController.MENUITEM_FIND, edit, false);
		menubar.add(edit);

		menubar.add(getWindowMenu());	// add window menu
	}

	private JPopupMenu fillPopup(JPopupMenu popup)	{
		controller.visualizeAction(XmlEditController.MENU_NEW, popup, false);
		controller.visualizeAction(XmlEditController.MENUITEM_DELETE, popup, false);
		popup.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_CUT, popup, false);
		controller.visualizeAction(XmlEditController.MENUITEM_COPY, popup, false);
		controller.visualizeAction(XmlEditController.MENUITEM_PASTE, popup, false);
		popup.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_UNDO, popup, false);
		controller.visualizeAction(XmlEditController.MENUITEM_REDO, popup, false);
		popup.addSeparator();
		controller.visualizeAction(XmlEditController.MENUITEM_EXPAND, popup, false);
		controller.visualizeAction(XmlEditController.MENUITEM_FIND, popup, false);
		return popup;
	}



	/**
		Implements FrameCreator: calls createMdiFrame().
	*/
	public void createEditor(String uri)	{
		createMdiFrame(uri);
	}
	
	/**
		Overrides SwapableMdiPane top create an internal container.
		If the URI is already open, it is switched to foreground.
	*/
	public MdiFrame createMdiFrame(Object uri)	{
		if (uri instanceof File)	// happens on EditHistoryFileChooser
			uri = ((File) uri).getPath();
		
		CursorUtil.setWaitCursor(this);
		MdiFrame mdiFrame = null;
		try	{
			mdiFrame = super.createMdiFrame(new XmlFileManager((String)uri, controller));
			if (mdiFrame != null && uri != null && new File((String) uri).exists())
				history.fileLoaded((File) new File((String) uri));
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
			if (mdiFrame != null && mdiFrame.getRenderedObject() == null)	// error on load
				removeMdiFrame(mdiFrame);	// remove the empty MDI frame
		}
		return mdiFrame;
	}

	/** Overriden to close file history. */
	public boolean close()	{
		history.close();
		return super.close();
	}

	/** Implements FrameCreator: Close the container window of all internal frames. */
	public void closeContainerWindow()	{
		windowListener.windowClosing(null);
	}


	/** Implements FrameCreator: Close the container window of all internal frames. */
	public void setSelectedEditor(Object editor)	{
		MdiFrame ic = getContainerForEditor(editor);
		setSelectedMdiFrame(ic);
	}

	/** Implements FrameCreator: Returns the internal container for an editor. Called on saveAs(). */
	public void setRenderedEditorObject(Object editor, Object uri)	{
		MdiFrame ic = getContainerForEditor(editor);
		ic.setRenderedObject(uri);
		if (new File((String) uri).exists())
			history.fileLoaded(new File((String) uri));
	}
	
	private MdiFrame getContainerForEditor(Object editor)	{
		MdiFrame [] ics = getMdiFrames();
		for (int i = 0; i < ics.length; i++)	{
			XmlFileManager mgr = (XmlFileManager)ics[i].getManager();
			if (editor == mgr.getEditor())	{
				return ics[i];
			}
		}
		return null;
	}
	
}