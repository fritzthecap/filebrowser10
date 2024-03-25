package fri.gui.swing.editor;

import java.io.File;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import fri.gui.swing.undo.*;
import fri.gui.swing.mdi.MdiFrame;
import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.searchdialog.*;
import fri.gui.swing.filechooser.*;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.iconbuilder.Icons;

/**
	Holds basic actions and callbacks for an editor.
	CAUTION: This class does NOT insertAction Cut, Copy and Paste! Subclasses
		MUST do this as this is editor-specific, this controller manages them!
	
	@author  Ritzberger Fritz
*/

public abstract class EditController extends ActionConnector implements
	CaretListener,	// show cursor position in labels
	EditorTextHolder.ChangeListener	// text was changed
{
	public static final String MENU_FILE = "File";
	public static final String MENUITEM_NEW = "New";
	public static final String MENUITEM_OPEN = "Open";
	public static final String MENUITEM_SAVE = "Save";
	public static final String MENUITEM_SAVEAS = "Save As ...";	// watch for "cb_Save_As____(Object editor)"!
	public static final String MENUITEM_SAVEALL = "Save All";
	public static final String MENUITEM_CLOSE = "Close";
	public static final String MENU_OPTIONS = "Options";
	public static final String MENUITEM_WARNDIRTY = "Dirty File Warning";
	public static final String MENU_EDIT = "Edit";
	public static final String MENUITEM_CUT = "Cut";
	public static final String MENUITEM_COPY = "Copy";
	public static final String MENUITEM_PASTE = "Paste";
	public static final String MENUITEM_FIND = "Find";
	public static final String MENUITEM_FINDNEXT = "Find Next";
	public static final String MENUITEM_RELOAD = "Reload";
	public static final String MENU_WINDOW = "Window";
	public static final String MENUITEM_BACK = "Back";
	public static final String MENUITEM_FORWARD = "Forward";
	public static final String MENUITEM_SET_TABBED = "Set Tabbed MDI View";
	public static final String MENUITEM_SET_DESKTOP = "Set Desktop MDI View";
	public static final String MENUITEM_CUSTOMIZEGUI = "Customize GUI";
	public static final String MENUITEM_CASCADE = "Cascade";
	public static final String MENUITEM_TILEHORIZONTAL = "Tile Horizontal";
	public static final String MENUITEM_TILEVERTICAL = "Tile Vertical";

	protected DoListener doListener;
	protected EditorMdiPane mdiPane;
	protected EditorTextHolder currentEditor;
	protected AbstractSearchReplace searchWindow;
	private File justClosed;
	private File justOpened;
	private GuiApplication parent;
	private FocusHistory focusHistory;


	/**
		Create a text edit controller.
		It works only after <i>setMdiPane()</i> passed a valid container creator and selection holder.
		@param frame main window on which "close()" will be called on Menu-Exit.
	*/
	public EditController(GuiApplication parent)	{
		super(null, null, null);

		this.parent = parent;

		insertActions();
		installFallbackActionListener();
		setInitEnabled();
	}


	/**
		Set the MDI pane that creates internal containers when a file is opened.
		This call is as necessary as the constructor.
	*/
	public void setMdiPane(EditorMdiPane mdiPane)	{
		this.mdiPane = mdiPane;
		this.selection = mdiPane;

		focusHistory = new FocusHistory(mdiPane);
		focusHistory.setFocusActions((Action)get(MENUITEM_BACK), (Action)get(MENUITEM_FORWARD));
	}
	

	/**
		Add basic editor actions. This calls (in order):
			<i>insertFileActions, insertEditActions, insertUndoActions, insertFindActions,
			insertNavigationActions, insertWindowSwitchActions, insertWarnDirtyAction</i>.
			CLOSE, CUSTOMIZEGUI are added.
	*/
	protected void insertActions()	{
		insertFileActions();
		insertEditActions();
		insertUndoActions();
		insertFindActions();
		insertReloadAction();
		insertNavigationActions();
		insertWindowSwitchActions();
		insertWarnDirtyAction();

		registerAction(MENUITEM_CLOSE);
		registerAction(MENUITEM_CUSTOMIZEGUI, (String)null, "Customize GUI", 0, 0);
	}

	protected void insertNavigationActions()	{
		registerAction(MENUITEM_BACK, Icons.get(Icons.back), "Previous Window", KeyEvent.VK_LEFT, InputEvent.ALT_MASK);
		registerAction(MENUITEM_FORWARD, Icons.get(Icons.forward), "Next Window", KeyEvent.VK_RIGHT, InputEvent.ALT_MASK);
	}
	
	protected void insertWindowSwitchActions()	{
		// accelerators for switching windows
		registerAction("switch_0", (String)null, "", KeyEvent.VK_1, InputEvent.ALT_MASK);
		registerAction("switch_1", (String)null, "", KeyEvent.VK_2, InputEvent.ALT_MASK);
		registerAction("switch_2", (String)null, "", KeyEvent.VK_3, InputEvent.ALT_MASK);
		registerAction("switch_3", (String)null, "", KeyEvent.VK_4, InputEvent.ALT_MASK);
		registerAction("switch_4", (String)null, "", KeyEvent.VK_5, InputEvent.ALT_MASK);
		registerAction("switch_5", (String)null, "", KeyEvent.VK_6, InputEvent.ALT_MASK);
		registerAction("switch_6", (String)null, "", KeyEvent.VK_7, InputEvent.ALT_MASK);
		registerAction("switch_7", (String)null, "", KeyEvent.VK_8, InputEvent.ALT_MASK);
		registerAction("switch_8", (String)null, "", KeyEvent.VK_9, InputEvent.ALT_MASK);
		registerAction("switch_9", (String)null, "", KeyEvent.VK_0, InputEvent.ALT_MASK);
	}
	
	protected void insertFileActions()	{
		insertFileNewAction();
		registerAction(MENUITEM_OPEN, Icons.get(Icons.openFolder), "Open Document", KeyEvent.VK_O, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_SAVE, Icons.get(Icons.save), "Save Document", KeyEvent.VK_S, InputEvent.CTRL_MASK);
		insertFileSaveAsAction();
		registerAction(MENUITEM_SAVEALL, (String)null, "", 0, 0);
	}

	protected void insertFileNewAction()	{
		registerAction(MENUITEM_NEW, Icons.get(Icons.newDocument), "New Document", KeyEvent.VK_N, InputEvent.CTRL_MASK);
	}
	
	protected void insertFileSaveAsAction()	{
		registerAction(MENUITEM_SAVEAS, (String)null, "", 0, 0);
	}
	
	protected void insertFindActions()	{
		registerAction(MENUITEM_FIND, Icons.get(Icons.find), "Search And Replace Text", KeyEvent.VK_F, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_FINDNEXT, (String)null, "", KeyEvent.VK_F3, 0);
	}

	protected void insertEditActions()	{
	}

	protected void insertWarnDirtyAction()	{
		registerAction(MENUITEM_WARNDIRTY, (String)null, "", 0, 0);
	}

	protected void insertUndoActions()	{
		DoAction undo = new DoAction(DoAction.UNDO);
		undo.addWillPerformActionListener(this);
		DoAction redo = new DoAction(DoAction.REDO);
		redo.addWillPerformActionListener(this);
		doListener = new DoListener(undo, redo);
		registerAction(undo, Icons.get(Icons.undo), "Undo Previous Action", KeyEvent.VK_Z, InputEvent.CTRL_MASK);
		registerAction(redo, Icons.get(Icons.redo), "Redo Undone Action", KeyEvent.VK_Y, InputEvent.CTRL_MASK);
	}

	protected void insertReloadAction()	{
		registerAction(MENUITEM_RELOAD);
	}

	
	/** The action enabled setting for an empty editor pane. */
	protected void setInitEnabled()	{
		setEnabled(MENUITEM_CUT, false);
		setEnabled(MENUITEM_COPY, false);
		setEnabled(MENUITEM_PASTE, false);
		if (get(MENUITEM_FIND) != null)
			setEnabled(MENUITEM_FIND, false);
		setEnabled(MENUITEM_SAVE, false);
		setEnabled(MENUITEM_SAVEAS, false);
		setEnabled(MENUITEM_SAVEALL, false);

		if (mdiPane != null)	{
			if (mdiPane.getLineRenderer() != null)
				mdiPane.getLineRenderer().setText(" ");

			if (mdiPane.getPositionRenderer() != null)
				mdiPane.getPositionRenderer().setText(" ");
		}
	}
	
	
	private void installFallbackActionListener()	{
		if (fallback == null)	{
			fallback = new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					if (e.getActionCommand().startsWith("switch_"))	{	// switch to an indexed internal container
						String s = e.getActionCommand();
						s = s.substring(s.lastIndexOf("_") + 1);
						int i = Integer.valueOf(s).intValue();
						
						mdiPane.setSelectedIndex(i);
					}
					else
					if (e.getActionCommand().equals(DoAction.UNDO) || e.getActionCommand().equals(DoAction.REDO))	{	// undo will perform
						// Undo WillPerformActionListener: bring frame to front on which the undo will happen
						System.err.println("actionPerformed, will perform "+e.getActionCommand());
						EditorTextHolder editor = getEditorPendingForUndo(e.getActionCommand());
						
						if (editor != null)	{	// can raise from Ctl-Z or Ctl-Y from keyboard
							setSelectedEditor(editor);
						}
					}
				}
			};
		}
	}

	/**
		Returns the EditorTextHolder that is related to the pending undo action. 
		@param action DoAction.UNDO or DoAction.REDO.
	*/
	protected abstract EditorTextHolder getEditorPendingForUndo(String action);


	/** Needed by EditFileManager to bring frame to front when confirm save when closing. */
	public void setSelectedEditor(EditorTextHolder editor)	{
		mdiPane.setSelectedObject(editor);
	}
	

	/** Returns the Frame this controller is attached to. */
	protected GuiApplication getFrame()	{
		return parent;
	}

	/** Returns an array with all EditorTextHolders in the current MDI pane. */
	protected EditorTextHolder [] getAllEditors()	{
		MdiFrame [] ics = mdiPane.getMdiFrames();
		EditorTextHolder [] editors = new EditorTextHolder [ics.length];
		
		for (int i = 0; ics != null && i < ics.length; i++)	{
			EditFileManager mgr = (EditFileManager)ics[i].getManager();
			editors[i] = mgr.getEditorTextHolder();
		}
		
		return editors;
	}



	// callbacks

	public void cb_Back(Object editor)	{
		focusHistory.back();
	}
	
	public void cb_Forward(Object editor)	{
		focusHistory.forward();
	}


	public void cb_Open(Object editor)	{
		try	{
			File [] files = DefaultFileChooser.openDialog(
				justOpened != null
						? justOpened
						: justClosed != null
							? justClosed
							: editor != null
								? ((EditorTextHolder)editor).getFile()
								: null,
					getFrame(),
					EditorFrame.class);

			for (int i = 0; files != null && i < files.length; i++)	{
				mdiPane.createMdiFrame(files[i]);
			}
		}
		catch (CancelException ex)	{
		}
	}
	
	
	public void cb_New(Object editor)	{
		mdiPane.createMdiFrame(null);
	}
	
	
	public void cb_Dirty_File_Warning(Object editor)	{
		setWarnDirty(isChecked((AbstractButton)currentActionEvent.getSource()));
	}

	/** Sets the dirty file warning flag to passed value. Public because needed by EditorMdiPane when closing. */
	public void setWarnDirty(boolean warnDirty)	{
		System.err.println("Dirty File Warning: "+warnDirty);
		Config.setWarnDirty(warnDirty);

		EditorTextHolder [] e = getAllEditors();
		for (int i = 0; i < e.length; i++)	{
			e[i].setWarnDirty(warnDirty);
		}
	}

	
	public void cb_Find(Object editor)	{
		find((EditorTextHolder)editor);
	}

	public void cb_Find_Next(Object editor)	{
		findSilent((EditorTextHolder)editor);
	}

	public void cb_Reload(Object editor)	{
	    EditorTextHolder e = (EditorTextHolder) editor;
	    if (e.getChanged())	{	// if dirty, confirm reload that dismisses changes
			int ret = JOptionPane.showConfirmDialog(
					getFrame(),
					"Really Reload Editor?\nThere Are Unsaved Changes!",
					"Reload",
					JOptionPane.YES_NO_OPTION);
				
			if (ret != JOptionPane.YES_OPTION)
				return;
	    }
		e.reload();
	}
	

	public void cb_Close(Object editor)	{
		getFrame().close();
	}


	public void cb_Save(Object editor)	{
		save((EditorTextHolder)editor);
	}

	public void cb_Save_As____(Object editor)	{
		save((EditorTextHolder)editor, true);
	}

	public void cb_Save_All(Object editor)	{
		EditorTextHolder [] e = getAllEditors();
		for (int i = 0; i < e.length; i++)	{
			save(e[i]);
			MdiFrame mdiFrame = mdiPane.getContainerForEditor(e[i]);
			renderChangeInTitle(e[i].getChanged(), mdiFrame);
		}
		setSaveAllEnabled();
	}


	/** This is called from EditFileManager when closing and save confirmation was committed. */
	public boolean save(EditorTextHolder editor)	{
		return save(editor, editor.getFile() == null);
	}
	
	// The saving editor is NOT guaranteed to be in foreground, as this method is called by all save methods.
	private boolean save(EditorTextHolder editor, boolean isSaveAs)	{
		boolean ok = isSaveAs ? editor.saveAs() : editor.save();

		if (isSaveAs && editor.getFile() != null)	{
			// set title in the case the file was newly created
			MdiFrame mdiFrame = mdiPane.getContainerForEditor(editor);
			mdiFrame.setRenderedObject(editor.getFile());
			mdiPane.addHistory(editor.getFile());
		}

		setSaveAllEnabled();
		return ok;
	}

	private void setSaveAllEnabled()	{
		boolean changed = false;
		EditorTextHolder [] e = getAllEditors();
		for (int i = 0; changed == false && i < e.length; i++)	{
			if (e[i].getChanged())	{
				changed = true;
			}
		}
		setEnabled(MENUITEM_SAVEALL, changed);
	}




	// events from MDI framework (called from FileEditorManager)

	/** A new editor was opened. Add all listeners. */
	public void editorOpened(EditorTextHolder editor)	{
		System.err.println("editorOpened ...");

		editor.setWarnDirty(Config.getWarnDirty());

		if (editor.getFile() != null)
			justOpened = editor.getFile();

		listen(editor, true);
		
		setEnabled(MENUITEM_PASTE, true);
		if (get(MENUITEM_FIND) != null)
			setEnabled(MENUITEM_FIND, true);
		setEnabled(MENUITEM_SAVEAS, true);

		focusHistory.set(editor);
	}

	/** An editor was selected. Change the accelerator sensor and feed the search window with new text. */
	public void editorActivated(EditorTextHolder editor)	{
		System.err.println("editorActivated ...");

		currentEditor = editor;

		changeAllKeyboardSensors((JComponent)editor.getTextComponent());
		
		setEnabled(MENUITEM_SAVE, editor.getChanged());
		boolean canCutCopy = editor.getSelectionStart() >= 0 && editor.getSelectionStart() < editor.getSelectionEnd();
		setEnabled(MENUITEM_CUT, canCutCopy);
		setEnabled(MENUITEM_COPY, canCutCopy);
		setLineLabelInfo(editor.getCaretPosition());
		
		focusHistory.set(editor);
		setSearchTextWhenVisible(editor);

		ComponentUtil.requestFocus(editor.getTextComponent());
	}

	/** An editor was closed. */
	public void editorClosed(EditorTextHolder editor)	{
		System.err.println("editorClosed ...");

		justClosed = editor.getFile();

		listen(editor, false);

		focusHistory.back();
		focusHistory.remove(editor);

		if (mdiPane.getMdiFrames().length <= 0)	{	// no editors left
			doListener.discardAllEdits();
			setInitEnabled();
			
			if (searchWindow != null)
				searchWindow.setVisible(false);
		}
		else	{
			cleanEdits(doListener, editor.getUndoableEditIdentifier());
		}
		
		setSaveAllEnabled();	// check if "Save All" makes sense
	}

	protected abstract void cleanEdits(DoListener doListener, Object undoableEditIdentifier);

	/**
		This is called from editorOpened() with true and from editorClosed() with false.
		Add or remove all necessary listeners to the editor.
	*/
	protected void listen(EditorTextHolder editor, boolean set)	{
		if (set)	{
			editor.setUndoListener(doListener);
			editor.setCaretListener(this);
			editor.setChangeListener(this);
			installDndListener(editor, set);
		}
		else	{
			editor.unsetUndoListener(doListener);
			editor.unsetCaretListener(this);
			editor.unsetChangeListener(this);
			installDndListener(editor, set);
		}
	}

	/** Install a DnD listener on opened editor. Gets called on open and close. */
	protected void installDndListener(EditorTextHolder editor, boolean set)	{
		if (set)
			new FileDndListener(editor.getTextComponent(), mdiPane);
	}



	/** Finish by user close. This is called by EditorMdiPane when closing. */
	public void close()	{
		Config.store();

		if (searchWindow != null)	{
			searchWindow.dispose();
			searchWindow = null;
		}
	}


	// interface CaretListener
	
	/** Interface CaretListener: updates line/character labels. */
	public void caretUpdate(CaretEvent e) {
		boolean selection = (e.getDot() != e.getMark());
		setEnabled(MENUITEM_CUT, selection);
		setEnabled(MENUITEM_COPY, selection);
		setLineLabelInfo(e.getDot());
	}

	/** Set info about line and character position. */
	protected void setLineLabelInfo(int dot)	{
		if (currentEditor == null)
			return;
			
		Point p = currentEditor.caretToPoint(dot);
		
		if (mdiPane.getLineRenderer() != null)
			mdiPane.getLineRenderer().setText(p.y >= 0 ? Integer.toString(p.y) : "");
			
		if (mdiPane.getPositionRenderer() != null)
			mdiPane.getPositionRenderer().setText(p.x >= 0 ? Integer.toString(p.x) : "");
	}


	// interface ChangeListener

	/** Implements EditorTextHolder.ChangeListener: sets "Save" and "Save All" enabled. Notifies search window if existent. */
	public void changed(boolean dirty)	{
		setEnabled(MENUITEM_SAVE, dirty);
		
		if (dirty)	{
			setEnabled(MENUITEM_SAVEALL, true);	// one was changed
		}
		else	{
			setSaveAllEnabled();	// must look at all editors
		}
		
		renderChangeInTitle(dirty);
		
		if (searchWindow != null)
			searchWindow.setTextChanged();
	}

	private void renderChangeInTitle(boolean dirty)	{
		renderChangeInTitle(dirty, mdiPane.getSelectedMdiFrame());
	}
	
	private void renderChangeInTitle(boolean dirty, MdiFrame editorFrame)	{
		String title = editorFrame.getTitle();
		boolean changed = false;
		String DIRTYMARK = "*";
		
		if (title.startsWith(DIRTYMARK) == false && dirty)	{
			editorFrame.setTitle(DIRTYMARK+title);
			changed = true;
		}
		else
		if (title.startsWith(DIRTYMARK) && dirty == false)	{
			editorFrame.setTitle(title.substring(DIRTYMARK.length()));
			changed = true;
		}
		
		if (changed)	{
			mdiPane.revalidate();
			mdiPane.repaint();
		}
	}
	
	// find methods

	/** Create a SearchReplace window. Override this to create another find dialog. */
	protected AbstractSearchReplace createFindDialog(JFrame frame, EditorTextHolder textarea)	{
		return new SearchReplace(frame, textarea);
	}
	
	private boolean ensureFindDialog(EditorTextHolder textarea)	{
		if (searchWindow == null)	{
			searchWindow = createFindDialog(getFrame(), textarea);
			return true;
		}
		return false;
	}

	private void find(EditorTextHolder textarea)	{
		if (ensureFindDialog(textarea) == false)	{
			searchWindow.init(textarea, true);
			searchWindow.setVisible(true);
		}
	}
	
	private void findSilent(EditorTextHolder textarea)	{
		if (ensureFindDialog(textarea) == false)	{
			if (searchWindow.getCurrentTextArea() != textarea)
				searchWindow.init(textarea);
			searchWindow.findNext();
		}
	}

	/** Open the search dialog with the passed parameters. Wait until text is loaded. */
	public void find(final String pattern, final String syntax, final boolean ignoreCase, final boolean wordMatch)	{
		if (currentEditor == null)	{
			System.err.println("no frame is open for finding pattern "+pattern);
			return;
		}

		// wait on textarea for finished loading
		final Runnable showFindWindow = new Runnable()	{
			public void run()	{
				ensureFindDialog(currentEditor);
				searchWindow.init(currentEditor, pattern, syntax, ignoreCase, wordMatch);
			}
		};
		checkLoadingAndInvokeLater(currentEditor, showFindWindow);
	}

	// runs the runnable if textarea is loading, waits in a thread and starts runnable after.
	private void checkLoadingAndInvokeLater(final EditorTextHolder textarea, final Runnable toInvoke)	{
		if (textarea.isLoading())	{	// wait for thread to have loaded file		
			Runnable r = new Runnable()	{
				public void run()	{
					synchronized(textarea)	{
						if (textarea.isLoading())	{
							try	{
								System.err.println("waiting for textarea to finish loading ...");
								textarea.wait();
							}
							catch (InterruptedException e)	{
							}
						}
					}
					EventQueue.invokeLater(toInvoke);
				}
			};

			new Thread(r).start();
		}
		else	{
			System.err.println("not waiting as textarea seems not to be loading");
			toInvoke.run();
		}
	}
		
	private void setSearchTextWhenVisible(final EditorTextHolder textarea)	{
		if (searchWindow != null && searchWindow.isVisible() == true)	{
			Runnable r = new Runnable()	{
				public void run()	{
					System.err.println("setting new text to search window");
					searchWindow.init(textarea);
				}
			};
			checkLoadingAndInvokeLater(textarea, r);
		}
	}

}
