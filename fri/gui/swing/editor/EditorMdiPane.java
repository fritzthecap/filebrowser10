package fri.gui.swing.editor;

import java.io.File;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import fri.gui.mvc.view.Selection;
import fri.gui.mvc.view.swing.PopupMouseListener;
import fri.gui.swing.mdi.*;
import fri.gui.swing.undo.DoAction;

/**
	The MDI pane that makes menu and toolbar layout.
	Provides editor Selection.
	Overrides some methods of superclass to set menu strings from controller.

	@author  Ritzberger Fritz
*/

public abstract class EditorMdiPane extends SwapableMdiPane implements
	Selection
{
	protected EditController controller;
	protected PopupMouseListener popupMouseListener;
	protected JLabel lineRenderer, positionRenderer;
	private AbstractButton customize;
	private EditHistoryFileChooser history;


	/**
		Create a desktop pane for internal frames.
		@param frame main window on which LAF setting will work.
	*/
	public EditorMdiPane(EditController controller)	{
		super(Config.getIsDesktopView() == false);
		this.controller = controller;
		new FileDndListener(this, this);
		this.history = new EditHistoryFileChooser(this);
	}


	/** Makes an absolute File if not null and relative. */
	protected Object standardizeFile(Object f)	{
		if (f != null && ((File)f).isAbsolute() == false)
			f = new File(((File)f).getAbsolutePath());
		return f;
	}
	
	/** Overridden to add every opened file to history. */
	public MdiFrame createMdiFrame(Object toRender)	{
		MdiFrame mdiFrame = super.createMdiFrame(toRender);
		if (mdiFrame != null)
			history.fileLoaded((File) ((EditFileManager) toRender).getRenderedObject());
		return mdiFrame;
	}
	
	void addHistory(File file)	{
		history.fileLoaded(file);
	}
	
	/**
		User close. This is called by main frame window when closing.
	*/
	public boolean close()	{
		boolean oldWarnDirty = Config.getWarnDirty();
		controller.setWarnDirty(false);
		
		boolean ret = super.close();
		
		controller.setWarnDirty(oldWarnDirty);
		
		if (ret == true)	{
			controller.close();
			history.close();
		}
		
		return ret;
	}


	/** Overridden to put Window menu item name from controller. */
	protected MdiPane createDesktopPaneImpl()	{
		return new MdiDesktopPane()	{
			protected String getWindowMenuName()	{
				return EditController.MENU_WINDOW;
			}
			protected String getCascadeMenuName()	{
				return EditController.MENUITEM_CASCADE;
			}
			protected String getTileHorizontalMenuName()	{
				return EditController.MENUITEM_TILEHORIZONTAL;
			}
			protected String getTileVerticalMenuName()	{
				return EditController.MENUITEM_TILEVERTICAL;
			}
		};
	}

	/** Overridden to install a Dnd listener to tabbed pane. */
	protected MdiPane createTabbedPaneImpl()	{
		MdiPane p = super.createTabbedPaneImpl();
		new FileDndListener((Component)p, this);
		return p;
	}
	

	/** Overridden to store state into Config variable. */
	public void swap()	{
		super.swap();
		Config.setIsDesktopView(isDesktopView());
	}



	/** Implements Selection: return the foreground EditorTextHolder. */
	public Object getSelectedObject()	{
		MdiFrame ic = getSelectedMdiFrame();
		if (ic != null)	{
			EditFileManager mgr = (EditFileManager)ic.getManager();
			//System.err.println("getSelectedObject "+ic+", manager is "+mgr);
			return mgr.getEditorTextHolder();
		}
		return null;
	}

	/** Implements Selection: sets the foreground EditorTextHolder. */
	public void setSelectedObject(Object o)	{
		MdiFrame ic = getContainerForEditor(o);
		if (ic != null)	{
			setSelectedMdiFrame(ic);
			
			// tabbed pane does not receive stateChanged() when falling to a panel right from the closed one
			((EditFileManager)ic.getManager()).activated(ic);
		}
	}

	/** Implements Selection by doing nothing: no possible action. */
	public void clearSelection()	{
	}
	
	
	public MdiFrame getContainerForEditor(Object editor)	{
		MdiFrame [] ics = getMdiFrames();
		for (int i = 0; i < ics.length; i++)	{
			EditFileManager mgr = (EditFileManager)ics[i].getManager();
			if (editor == mgr.getEditorTextHolder())	{
				return ics[i];
			}
		}
		return null;
	}


	/** Parent need customize menuitem for initing. */
	public AbstractButton getCustomizeItem()	{
		return customize;
	}
	
	/** Returns the label of the row number display in toolbar, can be null. */
	public JLabel getLineRenderer()	{
		return lineRenderer;
	}

	/** Returns the label of the column (character position in line) display in toolbar, can be null. */
	public JLabel getPositionRenderer()	{
		return positionRenderer;
	}


	/** Calls (in order)
			<i>fillToolBarLinePositionRenderers, fillToolBarNavigation, fillToolBarFile,
			fillToolBarEdit, fillToolBarUndoRedo, fillToolBarFind</i>
	*/
	public void fillToolBar(JToolBar toolbar)	{
		fillToolBarLinePositionRenderers(toolbar);
		fillToolBarNavigation(toolbar);
		fillToolBarFile(toolbar);
		fillToolBarEdit(toolbar);
		fillToolBarUndoRedo(toolbar);
		fillToolBarFind(toolbar);
	}

	protected void fillToolBarLinePositionRenderers(JToolBar toolbar)	{
		fillToolBarLineRenderer(toolbar);
		fillToolBarPositionRenderer(toolbar);
		toolbar.add(Box.createRigidArea(new Dimension(10, 0)));
	}

	// Line renderer is wider than column renderer
	protected void fillToolBarLineRenderer(JToolBar toolbar)	{
		lineRenderer = new JLabel(" ");
		lineRenderer.setToolTipText("Line Number of Cursor, 1-n");
		lineRenderer.setHorizontalAlignment(JLabel.RIGHT);
		lineRenderer.setBorder(BorderFactory.createLoweredBevelBorder());
		lineRenderer.setMinimumSize(new Dimension(60, lineRenderer.getMinimumSize().height));
		lineRenderer.setMaximumSize(new Dimension(60, lineRenderer.getMinimumSize().height));
		lineRenderer.setPreferredSize(new Dimension(60, lineRenderer.getPreferredSize().height));
		toolbar.add(lineRenderer);
	}

	protected void fillToolBarPositionRenderer(JToolBar toolbar)	{
		positionRenderer = new JLabel(" ");
		positionRenderer.setToolTipText("Column Number of Cursor, 0-n");
		positionRenderer.setHorizontalAlignment(JLabel.RIGHT);
		positionRenderer.setBorder(BorderFactory.createLoweredBevelBorder());
		positionRenderer.setMinimumSize(new Dimension(60, positionRenderer.getMinimumSize().height));
		positionRenderer.setMaximumSize(new Dimension(60, positionRenderer.getMinimumSize().height));
		positionRenderer.setPreferredSize(new Dimension(60, positionRenderer.getPreferredSize().height));
		toolbar.add(positionRenderer);
	}

	protected void fillToolBarNavigation(JToolBar toolbar)	{
		controller.visualizeAction(EditController.MENUITEM_BACK, toolbar);
		controller.visualizeAction(EditController.MENUITEM_FORWARD, toolbar);
		toolbar.addSeparator();
	}

	protected void fillToolBarFile(JToolBar toolbar)	{
		controller.visualizeAction(EditController.MENUITEM_NEW, toolbar);
		controller.visualizeAction(EditController.MENUITEM_OPEN, toolbar);
		controller.visualizeAction(EditController.MENUITEM_SAVE, toolbar);
		toolbar.addSeparator();
	}

	protected void fillToolBarEdit(JToolBar toolbar)	{
		controller.visualizeAction(EditController.MENUITEM_CUT, toolbar);
		controller.visualizeAction(EditController.MENUITEM_COPY, toolbar);
		controller.visualizeAction(EditController.MENUITEM_PASTE, toolbar);
		toolbar.addSeparator();
	}

	protected void fillToolBarUndoRedo(JToolBar toolbar)	{
		controller.visualizeAction(DoAction.UNDO, toolbar);
		controller.visualizeAction(DoAction.REDO, toolbar);
		toolbar.addSeparator();
	}

	protected void fillToolBarFind(JToolBar toolbar)	{
		controller.visualizeAction(EditController.MENUITEM_FIND, toolbar);
	}
	


	public void fillMenuBar(JMenuBar menubar)	{
		JMenu file = new JMenu(EditController.MENU_FILE);
		file.setMnemonic(file.getText().charAt(0));
		fillMenuFile(file);
		menubar.add(file);

		JMenu edit = new JMenu(EditController.MENU_EDIT);
		edit.setMnemonic(edit.getText().charAt(0));
		fillMenuEdit(edit);
		menubar.add(edit);

		JMenu winmenu = getWindowMenu();	// calls fillWindowMenu for first time
		winmenu.setMnemonic(winmenu.getText().charAt(0));
		menubar.add(winmenu);
	}

	protected void fillMenuFile(JMenu file)	{
		controller.visualizeAction(EditController.MENUITEM_NEW, file, false);
		controller.visualizeAction(EditController.MENUITEM_OPEN, file, false);
		controller.visualizeAction(EditController.MENUITEM_SAVE, file, false);
		controller.visualizeAction(EditController.MENUITEM_SAVEAS, file, false);
		controller.visualizeAction(EditController.MENUITEM_SAVEALL, file, false);

		file.addSeparator();
		JMenuItem historyItem = new JMenuItem("History");
		historyItem.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				history.show();
			}
		});
		file.add(historyItem);
		file.addSeparator();

		JMenu options = new JMenu(EditController.MENU_OPTIONS);
		fillMenuOptions(options);
		file.add(options);

		file.addSeparator();

		controller.visualizeAction(EditController.MENUITEM_CLOSE, file, false);
	}

	protected void fillMenuOptions(JMenu options)	{
		controller.visualizeCheckableAction(EditController.MENUITEM_WARNDIRTY, Config.getWarnDirty(), options);
	}

	protected void fillMenuEdit(JMenu edit)	{
		fillMenuEditCutCopyPaste(edit);
		fillMenuEditUndoRedo(edit);
		fillMenuEditFind(edit);
	}
	
	protected void fillMenuEditCutCopyPaste(JMenu edit)	{
		controller.visualizeAction(EditController.MENUITEM_CUT, edit, false);
		controller.visualizeAction(EditController.MENUITEM_COPY, edit, false);
		controller.visualizeAction(EditController.MENUITEM_PASTE, edit, false);
		edit.addSeparator();
	}

	protected void fillMenuEditUndoRedo(JMenu edit)	{
		controller.visualizeAction(DoAction.UNDO, edit, false);
		controller.visualizeAction(DoAction.REDO, edit, false);
		edit.addSeparator();
	}

	protected void fillMenuEditFind(JMenu edit)	{
		controller.visualizeAction(EditController.MENUITEM_FIND, edit, false);
	}


	/** Adds Cut, Copy, Paste, Undo, Redo and Find. */
	public void fillPopupMenu(JPopupMenu popup)	{
		fillPopupMenuCutCopyPaste(popup);
		fillPopupMenuUndoRedo(popup);
		fillPopupMenuReload(popup);
		fillPopupMenuFind(popup);

		popupMouseListener = new PopupMouseListener(popup);
	}

	protected void fillPopupMenuCutCopyPaste(JPopupMenu popup)	{
		controller.visualizeAction(EditController.MENUITEM_CUT, popup, false);
		controller.visualizeAction(EditController.MENUITEM_COPY, popup, false);
		controller.visualizeAction(EditController.MENUITEM_PASTE, popup, false);
		popup.addSeparator();
	}

	protected void fillPopupMenuUndoRedo(JPopupMenu popup)	{
		controller.visualizeAction(DoAction.UNDO, popup, false);
		controller.visualizeAction(DoAction.REDO, popup, false);
		popup.addSeparator();
	}

	protected void fillPopupMenuFind(JPopupMenu popup)	{
		controller.visualizeAction(EditController.MENUITEM_FIND, popup, false);
	}

	protected void fillPopupMenuReload(JPopupMenu popup)	{
		controller.visualizeAction(EditController.MENUITEM_RELOAD, popup, false);
		popup.addSeparator();
	}


	/** Overridden to add items to Window menu. */
	protected void fillWindowMenu(JMenu windowMenu)	{
		controller.visualizeAction(EditController.MENUITEM_BACK, windowMenu, false, 0);
		controller.visualizeAction(EditController.MENUITEM_FORWARD, windowMenu, false, 1);
		windowMenu.insertSeparator(2);
		AbstractButton ab = controller.visualizeAction(EditController.MENUITEM_CUSTOMIZEGUI, windowMenu, false, 3);
		if (customize == null)	{
			customize = ab;
		}
		else	{	// we must trigger the customize item from newly created item as we do not know its ActionListener
			ab.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					customize.doClick();
				}
			});
		}
		windowMenu.insertSeparator(4);
	}



	/** Overridden to return EditController.MENUITEM_SET_DESKTOP. */
	protected String getSwapToDesktopMenuName()	{
		return EditController.MENUITEM_SET_DESKTOP;
	}

	/** Overridden to return EditController.MENUITEM_SET_TABBED. */
	protected String getSwapToTabbedMenuName()	{
		return EditController.MENUITEM_SET_TABBED;
	}

}