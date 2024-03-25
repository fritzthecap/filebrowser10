package fri.gui.swing.xmleditor.controller;

import java.util.*;
import java.io.*;
import java.beans.*;
import java.awt.*;
import java.util.List;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import fri.util.error.Err;
import fri.gui.CursorUtil;
import fri.gui.text.TextHolder;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.view.swing.SelectionDnd;
import fri.gui.swing.*;
import fri.gui.swing.util.*;
import fri.gui.swing.undo.*;
import fri.gui.swing.tree.TreeExpander;
import fri.gui.swing.actionmanager.*;
import fri.gui.swing.treetable.*;
import fri.gui.swing.filechangesupport.FileChangeSupport;
import fri.gui.swing.filechooser.*;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.xmleditor.model.*;
import fri.gui.swing.xmleditor.view.*;
import fri.gui.swing.xmleditor.controller.edits.*;

/**
	The controller for the XML editor window, holding all actions and callbacks.
	XML "business logic" is implemented here: which items can be presented as insertable,
	which paste-options are available for a certain selection.
	Cut/Copy/Paste is delegated to XmlClipboard, which in turn delegates to ControllerModelItem.

	@author  Ritzberger Fritz
*/
 
public class XmlEditController extends ContextChangingController implements
	PropertyChangeListener,	// listen to updates in TreeTableModel
	FileChangeSupport.Reloader	// reload the XML document when the originator file changed
{
	public static final String MENUITEM_OPEN = "Open";
	public static final String MENUITEM_SAVE = "Save";
	public static final String MENUITEM_SAVEAS = "Save As";

	public static final String MENU_NEW = "New";
	public static final String MENU_TAGS = "Elements";
	public static final String MENU_ENTITIES = "Entities";
	public static final String MENU_OTHERS = "Others";
	public static final String MENUITEM_DELETE = "Delete";
	public static final String MENUITEM_CUT = "Cut";
	public static final String MENUITEM_COPY = "Copy";
	public static final String MENUITEM_PASTE = "Paste";
	public static final String MENUITEM_UNDO = "Undo";
	public static final String MENUITEM_REDO = "Redo";

	public static final String MENUITEM_PASTE_BEFORE = "Paste Before";
	public static final String MENUITEM_PASTE_WITHIN = "Paste Within";
	public static final String MENUITEM_PASTE_AFTER = "Paste After";

	public static final String MENUITEM_EDIT = "Edit As Text";
	public static final String MENUITEM_VIEW = "View As HTML";
	public static final String MENUITEM_VALIDATE = "Validate";
	public static final String MENUITEM_EXPAND = "Expand";
	public static final String MENUITEM_CONFIGURE = "Configure";
	public static final String MENUITEM_SHOW_DTD = "Show DTD";
	public static final String MENUITEM_FIND = "Find";
	public static final String MENUITEM_FIND_NEXT = "Find Next";

	public static final String MENUITEM_CLOSE = "Close";

	private static final String TOOLTIP_OPEN = "Open Document";
	private static final String TOOLTIP_SAVE = "Save Document";

	private static final String TOOLTIP_NEW = "Create New Element Within Selection";
	private static final String TOOLTIP_DELETE = "Delete Selection";
	private static final String TOOLTIP_CUT = "Cut Selection";
	private static final String TOOLTIP_COPY = "Copy Selection";
	private static final String TOOLTIP_PASTE = "Paste Element To Selection";
	private static final String TOOLTIP_UNDO = "Undo Previous Action";
	private static final String TOOLTIP_REDO = "Redo Previously Undone Action";

	private static final String TOOLTIP_EDIT = "Edit Document As Text";
	private static final String TOOLTIP_VIEW = "View Document As HTML";
	private static final String TOOLTIP_VALIDATE = "Validate Document";
	private static final String TOOLTIP_EXPAND = "Expand All Nodes Under Selection";
	private static final String TOOLTIP_CONFIGURE = "Configure Parser And Display Options";
	private static final String TOOLTIP_SHOW_DTD = "Show Document Type Definition";
	private static final String TOOLTIP_FIND = "Find Text Within Document Texts And Attributes";

	private boolean pasteWithin, pasteBefore, pasteAfter;
	boolean ignoreSelectionChanged;	// used in XmlSearchReplace
	private boolean enableOnlyInsertable = true;

	private XmlClipboard clipboard = new XmlClipboard();

	private FrameCreator frameCreator;
	private Window parent;

	private XmlSearchReplace searchDialog;

	/** ActionListener that enables Save button when Undo or Redo is pressed.
		It switches the frame to front where the edit happened. */
	private ActionListener undoRedoSaveEnabler = new ActionListener()	{
		public void actionPerformed(ActionEvent e)	{
			// Undo&Redo undergoes change listener, so SAVE must be enabled manually here
			XmlEditController.this.setEnabled(MENUITEM_SAVE, true);

			Component c = (Component)clipboard.getEditorPendingForUndo(e.getActionCommand());
			setSelectedEditor(c);

			if (searchDialog != null)
				searchDialog.setTextChanged();
		}
	};

	/** It seems that ESCAPE is not delivered by JComponent. */
	private KeyAdapter escapeListener = new KeyAdapter()	{
		/** Clear any cutten item from movePending. */
		public void keyPressed(KeyEvent e)	{
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
				clipboard.clear();
				RefreshTreeTable.refresh((JTreeTable)e.getSource());	// show enabled items
				setEnabledActions();
			}
		}
	};



	/** Create a controller and define all XML Edit-Actions. */
	public XmlEditController(Window parent)	{
		super(null, new XmlTreeTableSelection(null));
		
		this.parent = parent;

		ActionManager.menuItemSeparator = "&";	// set a safe separator for submenus that can not appear in XML tag names
		this.fallback = createFallbackActionListener();

		registerAction(MENUITEM_OPEN, Icons.get(Icons.openFolder), TOOLTIP_OPEN, KeyEvent.VK_O, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_SAVE, Icons.get(Icons.save), TOOLTIP_SAVE, KeyEvent.VK_S, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_SAVEAS);
		registerFillableAction(MENU_NEW, Icons.get(Icons.newLine), TOOLTIP_NEW, KeyEvent.VK_INSERT, 0);
		registerAction(MENUITEM_DELETE, Icons.get(Icons.deleteLine), TOOLTIP_DELETE, KeyEvent.VK_DELETE, 0);
		registerAction(MENUITEM_CUT, Icons.get(Icons.cut), TOOLTIP_CUT, KeyEvent.VK_X, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_COPY, Icons.get(Icons.copy), TOOLTIP_COPY, KeyEvent.VK_C, InputEvent.CTRL_MASK);
		registerFillableAction(MENUITEM_PASTE, Icons.get(Icons.paste), TOOLTIP_PASTE, KeyEvent.VK_V, InputEvent.CTRL_MASK);

		DoAction undo = new DoAction(DoAction.UNDO, Icons.get(Icons.undo));
		undo.putValue(Action.NAME, MENUITEM_UNDO);
		registerAction(undo, TOOLTIP_UNDO, KeyEvent.VK_Z, InputEvent.CTRL_MASK);
		undo.addWillPerformActionListener(undoRedoSaveEnabler);
	
		DoAction redo = new DoAction(DoAction.REDO, Icons.get(Icons.redo));
		redo.putValue(Action.NAME, MENUITEM_REDO);
		registerAction(redo, TOOLTIP_REDO, KeyEvent.VK_Y, InputEvent.CTRL_MASK);
		redo.addWillPerformActionListener(undoRedoSaveEnabler);
	
		DoListener lsnr = new DoListener(undo, redo);	// one undo listener per controller
		clipboard.setDoListener(lsnr);

		registerAction(MENUITEM_VALIDATE, Icons.get(Icons.validate), TOOLTIP_VALIDATE, KeyEvent.VK_R, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_EDIT, Icons.get(Icons.documentEdit), TOOLTIP_EDIT, KeyEvent.VK_E, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_VIEW, Icons.get(Icons.eye), TOOLTIP_VIEW, KeyEvent.VK_H, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_EXPAND, Icons.get(Icons.tree), TOOLTIP_EXPAND);
		registerAction(MENUITEM_CONFIGURE, Icons.get(Icons.configure), TOOLTIP_CONFIGURE);
		registerAction(MENUITEM_SHOW_DTD, Icons.get(Icons.dtd), TOOLTIP_SHOW_DTD);
		registerAction(MENUITEM_FIND, Icons.get(Icons.find), TOOLTIP_FIND, KeyEvent.VK_F, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_FIND_NEXT, (String)null, (String)null, KeyEvent.VK_F3, 0);

		registerAction(MENUITEM_CLOSE);

		setAllDisabled();
	}

	/**
		When opening a new document, a frame must be created. This falls out of
		the responsibility of controller, so there MUST be a FrameCreator to do this.
		@param frameCreator that knows how to open a new document window.
	*/
	public void setFrameCreator(FrameCreator frameCreator)	{
		this.frameCreator = frameCreator;
	}


	/** Overridden to enable "Open", "Close" and "Configure" buttons. */
	public void setAllDisabled()	{
		super.setAllDisabled();

		// following items must be always enabled
		setEnabled(MENUITEM_OPEN, true);
		setEnabled(MENUITEM_CLOSE, true);
		setEnabled(MENUITEM_CONFIGURE, true);
	}

	/** Overridden to set FileChangeSupport to dirty when save is enabled. */
	public void setEnabled(String action, boolean enabled)	{
		super.setEnabled(action, enabled);

		if (enabled && action.equals(MENUITEM_SAVE))	{
			getTreeTable().getFileChangeSupport().setFileIsDirty();
		}
	}



	/** Returns the selection holder, for Drag & Drop. */
	public SelectionDnd getSelectionHolderDnd()	{
		return (SelectionDnd)getSelection();
	}

	/** Returns the clipboard, for Drag & Drop. */
	public XmlClipboard getClipboard()	{
		return clipboard;
	}


	private MutableXmlNode getXmlRoot()	{
		MutableXmlTreeTableModel m = getXmlTreeTableModel();
		return (MutableXmlNode)m.getRoot();
	}

	private List getSelectionList()	{
		return (List)getSelection().getSelectedObject();
	}

	private Component getParent()	{
		if (getTreeTable() == null)
			return ComponentUtil.getWindowForComponent(parent);
		return ComponentUtil.getWindowForComponent(getTreeTable());
	}

	private XmlTreeTable getTreeTable()	{
		return (XmlTreeTable)defaultKeySensor;
	}



	/** Implements FileChangeSupport.Reloader: re-read the Document from its URI. */
	public void reload()	{
		CursorUtil.setWaitCursor(getTreeTable());
		try	{
			new DocumentReloader(getTreeTable()).reloadFromURI();
		}
		finally	{
			CursorUtil.resetWaitCursor(getTreeTable());
		}
	}

	/** Implements FileChangeSupport.Reloader: Gets called when user chooses NOT to reload file. */
	public void fileWasNotReloaded()	{
		setEnabled(MENUITEM_SAVE, true);
		getXmlTreeTableModel().setChanged(true);
	}


	private void reloadFromMemory()	{	// Re-parse when prolog or configuration changed
		new DocumentReloader(getTreeTable()).reloadFromMemory();
	}



	/** Brings to front the internal frame that contains the passed Component. */
	public void setSelectedEditor(Component c)	{
		frameCreator.setSelectedEditor(c);
	}

		

	/**
		Implements PropetyChangeListener to listen for cell editing.
		Sets save button enabled.
		Creates an undable edit for changed text or attribute.
	*/
	public void propertyChange(PropertyChangeEvent e)	{
		//Thread.dumpStack();
		setEnabled(MENUITEM_SAVE, true);

		if (searchDialog != null)
			searchDialog.setTextChanged();

		if (e.getPropertyName().equals(MutableXmlTreeTableModel.UPDATED))	{
			updateNode((MutableXmlNode)e.getSource(), e.getOldValue(), e.getNewValue(), true);
		}
		// all other events are triggered and managed by this controller
	}

	/**
		Update a node with passed value that can belong to any column.
		Argument <i>startUndoTransaction</i> is needed by XmlSearchReplace
		when doing a lot of replacements in ONE undoable transaction.
	*/
	void updateNode(MutableXmlNode node, Object oldValue, Object newValue, boolean startUndoTransaction)	{
		if (startUndoTransaction)	{
			clipboard.getDoListener().beginUpdate();
		}
		else	{
			setEnabled(MENUITEM_SAVE, true);	// called from XmlSearchReplace
		}

		UndoableCommand edit = new UpdateCommand(
				getTreeTable(),
				node,
				oldValue,
				newValue);
		edit.doit();
		clipboard.getDoListener().addEdit(edit);

		if (node.warning != null)	// warn about substitutions
			Err.warning(node.warning);

		if (startUndoTransaction)
			clipboard.getDoListener().endUpdate();

		// look if document core settings are changed
		if (startUndoTransaction && node.isRoot() && newValue instanceof List)	{
			int ret = JOptionPane.showConfirmDialog(
					getParent(),
					"Parse Document With New Prolog?",
					"Prolog Changed",
					JOptionPane.YES_NO_OPTION);

			if (ret == JOptionPane.YES_OPTION)	{
				reloadFromMemory();
			}
		}
	}


	/** Adjust enabled/disabled state of menu/toolbar according to selection.*/
	public void setEnabledActions()	{
		List selection = getSelectionList();
		setEnabledActions(selection);
	}


	private List lastSelection;

	public void setEnabledActions(List selection)	{
		if (ignoreSelectionChanged)
			return;

		if (selection != null && selection.equals(lastSelection))
			return;
			
		lastSelection = selection;

		System.err.println("setting actions enabled ... "+selection);
		// define pre-conditions

		boolean ok = selection != null && selection.size() > 0;
		boolean canCreate = ok;
		boolean canDelete = ok;
		boolean canCopy = ok;
		boolean canPaste = ok && clipboard.isEmpty() == false;
		boolean canExpand = ok;

		// explore canXXX

		short type = -1;
		MutableXmlNode node = null;

		for (int i = 0; ok && i < selection.size(); i++)	{
			ControllerModelItem cmi = (ControllerModelItem)selection.get(i);
			node = (MutableXmlNode)cmi.getXmlNode();

			if (node.isLeaf())
				canCreate = canExpand = false;

			if (i == 0)
				type = node.getNodeType();
			else
				if (node.getNodeType() != type)	// all containers must be of same type
					canPaste = canCreate = false;
			
			if (node.isManipulable() == false)
				canCopy = canDelete = false;
		}
		
		// set enabled actions

		fillActionNEW(node, canCreate);
		
		setEnabled(MENUITEM_DELETE, canDelete);
		setEnabled(MENUITEM_CUT, canDelete);
		setEnabled(MENUITEM_COPY, canCopy);
		setEnabled(MENUITEM_EXPAND, canExpand);

		fillActionPASTE(node, canPaste);

		boolean enable;

		if (getXmlTreeTableModel() != null)	{
			setEnabled(MENUITEM_SAVE, getXmlTreeTableModel().isChanged());
			enable = true;
		}
		else	{
			setEnabled(MENUITEM_SAVE, false);
			enable = false;
		}

		setEnabled(MENUITEM_SAVEAS, enable);
		setEnabled(MENUITEM_EDIT, enable);
		setEnabled(MENUITEM_VIEW, enable);
		setEnabled(MENUITEM_SHOW_DTD, enable);
		setEnabled(MENUITEM_VALIDATE, enable);
		setEnabled(MENUITEM_FIND, enable);
	}



	private void fillActionNEW(MutableXmlNode node, boolean canCreate)	{
		if (canCreate)	{
			MenuTree mt = getActionItemsNEW(node);
			if (mt == null || mt.size() <= 0)
				canCreate = false;
			else
				fillAction(MENU_NEW, mt);
		}
		setEnabled(MENU_NEW, canCreate);
	}


	// erzeugt Struktur, die im Neu-Popup sichtbar wird.
	private MenuTree getActionItemsNEW(final MutableXmlNode node)	{
		List tags = node.getInsertableWithinTags();
		List entities = node.getEntityNames();
		List primitives = node.getPrimitiveNodeTypes();

		if (tags == null && primitives == null && entities == null)
			return null;

		MenuTree menu = new MenuTree(MENU_NEW);
		MenuTree tagMenu = null;
		MenuTree primMenu = null;
		MenuTree entMenu = null;

		if (tags != null)	{
			if (primitives != null || entities != null)
				menu.add(tagMenu = new MenuTree(MENU_TAGS));
			else
				tagMenu = menu;	// having only tags
		}

		if (entities != null)	{
			if (primitives != null || tags != null)
				menu.add(entMenu = new MenuTree(MENU_ENTITIES));
			else
				entMenu = menu;	// having only entities
		}

		if (primitives != null)	{
			if (tags != null || entities != null)
				menu.add(primMenu = new MenuTree(MENU_OTHERS));
			else
				primMenu = menu;	// having only primitives
		}

		for (int i = 0; tagMenu != null && i < tags.size(); i++)	{
			String tag = (String)tags.get(i);
			if (enableOnlyInsertable)	{
				boolean enabled = (node.getInsertablePosition(tag) >= 0);
				tagMenu.add(new MenuTree(tag, enabled));
			}
			else	{
				tagMenu.add(tag);
			}
		}

		for (int i = 0; entMenu != null && i < entities.size(); i++)	{
			String name = (String)entities.get(i);
			entMenu.add(name);
		}

		for (int i = 0; primMenu != null && i < primitives.size(); i++)	{
			String tag = (String)primitives.get(i);
			primMenu.add(tag);
		}

		return menu;
	}


	/** XmlDndPerformer needs to set paste flags explicitely, as setSelection is deferred in treetable. */
	public void fillActionPASTE(MutableXmlNode node, boolean canPaste)	{
		pasteWithin = pasteAfter = pasteBefore = false;

		String [] menu = null;
		boolean enabled = canPaste;

		if (canPaste)	{
			menu = getActionItemsPASTE(node, clipboard.getSourceModelItems());
			enabled = pasteWithin || pasteBefore || pasteAfter;
		}

		fillAction(MENUITEM_PASTE, menu);
		setEnabled(MENUITEM_PASTE, enabled);
	}

	// creates choice for paste action, appropriate to clipboard items
	private String [] getActionItemsPASTE(MutableXmlNode node, ModelItem [] pasteNodes)	{
		// set all possible paste-flags for selected node
		testPasteNodes(node, pasteNodes);

		if (pasteWithin && pasteAfter && pasteBefore)
			return new String [] { MENUITEM_PASTE_BEFORE, MENUITEM_PASTE_WITHIN, MENUITEM_PASTE_AFTER };
		else
		if (pasteAfter && pasteBefore)
			return new String [] { MENUITEM_PASTE_BEFORE, MENUITEM_PASTE_AFTER };

		return null;	// generate no submenu
	}

	// creates list of insert parameters for paste nodes
	private void testPasteNodes(MutableXmlNode node, ModelItem [] pasteNodes)	{
		// try insert within
		if (node.isLeaf() == false)	{	// paste within is possible only in container nodes
			if (testInsert(node, pasteNodes))	{
				pasteWithin = true;
				System.err.println("... paste WITHIN is true");
			}
		}

		// test if document root
		MutableXmlNode parent = (MutableXmlNode)node.getParent();
		if (parent == null)	// no paste before/after on root level
			return;

		// try insert before and after: test all items
		int childIndex = parent.getIndex(node);	// calculate insert position
		for (int i = 0; i < pasteNodes.length; i++)	{
			if (i == 0)	{
				pasteBefore = true;
				pasteAfter = true;
			}

			ControllerModelItem cmi = (ControllerModelItem)pasteNodes[i];
			MutableXmlNode xn = cmi.getXmlNode();

			if (xn.isInsertableEverywhere() == false)	{
				String tag = xn.getInsertionTagName();
				pasteBefore = pasteBefore && (parent.getRealInsertPosition(tag, childIndex) == childIndex);
				pasteAfter = pasteAfter && (parent.getRealInsertPosition(tag, childIndex + 1) == childIndex + 1);
			}
		}
		System.err.println("... paste BEFORE is "+pasteBefore+", paste AFTER is "+pasteAfter);
	}


	// Returns false if at least one of the pasteNodes is not insertable in parent node
	private boolean testInsert(MutableXmlNode node, ModelItem [] pasteNodes)	{
		// for each paste node check insertablity
		boolean ok = true;

		for (int i = 0; ok && i < pasteNodes.length; i++)	{
			ControllerModelItem cmi = (ControllerModelItem)pasteNodes[i];
			MutableXmlNode xn = cmi.getXmlNode();

			if (xn.isInsertableEverywhere() == false)	{
				String tag = xn.getInsertionTagName();
				ok = (node.getInsertablePosition(tag) >= 0);
			}
		}

		return ok;
	}



	// overridings

	/**
		Overridden to change the PropertyChange event listener to new view.
	*/
	public void setView(JTreeTable sensor)	{
		if (sensor == getTreeTable())
			return;

		if (getXmlTreeTableModel() != null)	{
			getXmlTreeTableModel().getChangeNotifier().removePropertyChangeListener(this);
			getTreeTable().removeKeyListener(escapeListener);
		}

		super.setView(sensor);

		if (getXmlTreeTableModel() != null)	{
			getXmlTreeTableModel().getChangeNotifier().addPropertyChangeListener(this);
			getTreeTable().addKeyListener(escapeListener);

			File file = new File(getXmlRoot().getURI());

			if (getTreeTable().getFileChangeSupport() == null)	{	// watch external file changes
				FileChangeSupport fchs = new FileChangeSupport(file, getTreeTable(), this);
				getTreeTable().setFileChangeSupport(fchs);
			}

			DefaultFileChooser.setChooserFile(file);	// open file in this' path

			if (searchDialog != null && searchDialog.getCurrentTextArea() != ensureTextHolder())	{
				searchDialog.init(ensureTextHolder());
			}
		}
	}

	/**
		Overridden to clean edits of closed window from undo manager.
		This must be called when an editor window has closed.
	*/
	public void closeView(JTreeTable sensor)	{
		// remove property change listener
		MutableXmlTreeTableModel treeTableModel = (MutableXmlTreeTableModel)sensor.getTreeTableModel();
		XmlNode root = (XmlNode)treeTableModel.getRoot();
		treeTableModel.getChangeNotifier().removePropertyChangeListener(this);
		MutableXmlTreeTableModel.freeInstance(root.getURI());

		// remove the change watcher
		XmlTreeTable xtt = (XmlTreeTable)sensor;
		xtt.getFileChangeSupport().setActive(false);

		// set the default file for an open dialog to currently closed file
		DefaultFileChooser.setChooserFile(new File(root.getURI()));

		// save the column widths
		xtt.saveColumnWidth();

		if (searchDialog != null && searchDialog.getCurrentTextArea() == xtt.getTextHolder())	{
			searchDialog.dispose();
			searchDialog = null;
		}

		super.closeView(sensor);

		clipboard.cleanEdits(sensor);
	}

	// overridings end


	// create an ActionListener for dynamically created menuitems (FillableAction)

	private ActionListener createFallbackActionListener()	{
		return new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				String cmd = e.getActionCommand();

				// split the command from parent menu items
				// dynamic menu items are concatenated with separator
				String sep = ((FillableManagedAction)get(MENU_NEW)).menuItemSeparator;
				int i = cmd.lastIndexOf(sep);
				String subCmd = cmd.substring(i + 1);

				// implement action recognition
				if (cmd.startsWith(MENU_NEW+sep))	{
					// extract submenu item
					int start = cmd.indexOf(sep) + 1;
					int end = cmd.indexOf(sep, start);
					String subItem = end > start ? cmd.substring(start, end) : null;
					if (subItem != null && subItem.equals(MENU_ENTITIES))	// mark the item as entity
						subCmd = "&"+subCmd+";";	// this is understood in MutableXmlNode

					// create a new item
					doNew(subCmd);
					return;
				}
				else
				if (cmd.startsWith(MENUITEM_PASTE+sep))	{
					if (subCmd.equals(MENUITEM_PASTE_BEFORE))	{
						doPaste(getSelectionList(), CommandArguments.PASTE_BEFORE);
						return;
					}
					else
					if (subCmd.equals(MENUITEM_PASTE_WITHIN))	{
						doPaste(getSelectionList(), null);
						return;
					}
					else
					if (subCmd.equals(MENUITEM_PASTE_AFTER))	{
						doPaste(getSelectionList(), CommandArguments.PASTE_AFTER);
						return;
					}
				}
				throw new IllegalArgumentException("Unknown menu item: "+cmd);
			}
		};
	}





	// action callbacks: "cb_<resourcename>", must be public to be found by reflection

	
	/** This occurs when INSERT was pressed. It shows a popup choice of insertable items. */
	public void cb_New(Object selection)	{
		if (selection != null)	{
			JPopupMenu popup = getPopupMenu(MENU_NEW);	// menu was provided by setEnabledActions
			if (popup != null)
				popup.show(getTreeTable(), recentX, recentY);
		}
	}

	private void doNew(String tagName)	{	// called from fallback action listener
		List selection = getSelectionList();

		if (selection != null)	{
			tagName = checkForPI(tagName);	// open dialog to input target name
			if (tagName == null)
				return;	// canceled when entering PI target

			Iterator it = ((List)selection).iterator();

			getSelection().clearSelection();	// will select the inserted item(s)

			clipboard.getDoListener().beginUpdate();

			while (it.hasNext())	{
				ModelItem mi = (ModelItem)it.next();

				UndoableCommand edit = new DefaultCreateCommand(
						getTreeTable(),
						mi,
						getXmlTreeTableModel(),
						tagName);
				ModelItem newItem = (ModelItem)edit.doit();

				if (mi.getError() != ControllerModelItem.NO_ERROR)	{
					Err.warning(mi.getError());
				}
				else	{
					clipboard.getDoListener().addEdit(edit);
					((XmlTreeTableSelection)getSelection()).addSelectedObject(newItem);
				}
			}

			clipboard.getDoListener().endUpdate();
		}
	}

	private String checkForPI(String tagName)	{
		if (tagName.equals(MutableXmlNode.PI_MENU_NAME))	{
			boolean repeat = false;
			String target;

			do	{
				target = JOptionPane.showInputDialog(getParent(), "Enter Processing Instruction Target:");
				if (target == null)
					return null;

				repeat = getXmlRoot().checkPITarget(target) == false;

				if (repeat)
					JOptionPane.showMessageDialog(
							getParent(),
							"Not A Valid PI Target: \""+target+"\"",
							"Error",
							JOptionPane.INFORMATION_MESSAGE);
			}
			while (repeat);

			tagName = tagName+":"+target;	// this is understood in MutableXmlNode
		}
		return tagName;
	}


	/** Delete all selected items, optional recursively. */
	public void cb_Delete(Object selection)	{
		if (selection != null)	{
			Iterator it = ((List)selection).iterator();
			
			clipboard.getDoListener().beginUpdate();
			int next = -1;

			while (it.hasNext())	{
				ModelItem mi = (ModelItem)it.next();

				// prepare selection row after delete
				if (next == -1)
					next = ((TreeTableSelection)getSelection()).getRowForNode(((ControllerModelItem)mi).getXmlNode());

				UndoableCommand edit = new DefaultRemoveCommand(
						getTreeTable(),
						mi,
						getXmlTreeTableModel());
				edit.doit();

				if (mi.getError() != ControllerModelItem.NO_ERROR)	{
					Err.warning(mi.getError());
				}
				else	{
					clipboard.getDoListener().addEdit(edit);
				}
			}

			clipboard.getDoListener().endUpdate();

			// set selection to next node
			((TreeTableSelection)getSelection()).setSelection(next);	// ensures >= 0
		}
	}


	/** Cut all selected items to clipboard. */
	public void cb_Cut(Object selection)	{
		if (selection != null)	{
			clipboard.cut(getTreeTable(), (List)selection);
			RefreshTreeTable.refresh(getTreeTable());	// show disabled cutten items
			setEnabledActions();	// enable paste
		}
	}


	/** Copy all selected items to clipboard. */
	public void cb_Copy(Object selection)	{
		if (selection != null)	{
			clipboard.copy(getTreeTable(), (List)selection);
			setEnabledActions();	// enable paste
		}
	}


	/** Drag & Drop passes the mouse coordinates for a popup open at pasting. */
	public void setRecentPoint(Point p)	{
		recentX = p.x;
		recentY = p.y;
	}


	private boolean checkIdenticalNodes(ModelItem [] clippedNodes, List selection)	{
		for (int i = 0; clippedNodes != null && i < clippedNodes.length; i++)	{
			ControllerModelItem mi1 = (ControllerModelItem)clippedNodes[i];

			if (mi1.isMovePending() == false)	// allow copy to same node
				return false;

			for (int j = 0; selection != null && j < selection.size(); j++)	{
				ControllerModelItem mi2 = (ControllerModelItem)selection.get(j);

				if (mi1.getXmlNode() == mi2.getXmlNode())	{
					return true;
				}
			}
		}
		return false;
	}

	private void pasteError(List selection)	{
		clipboard.clear();
		getSelection().setSelectedObject(selection);	// was set by XmlDndPerformer
		setEnabledActions();	// disable paste button
		RefreshTreeTable.refresh(getTreeTable());	// enable disabled node
		Toolkit.getDefaultToolkit().beep();
	}

	/** Paste all cutten items to selection. */
	public void cb_Paste(Object selection)	{
		if (selection != null)	{
			// check if dropped at same item
			if (checkIdenticalNodes(clipboard.getSourceModelItems(), (List)selection))	{
				pasteError((List)selection);
				System.err.println("Can not paste node to itself!");
				return;
			}

			if (pasteBefore && pasteAfter)	{	// menu items have been provided by setEnabledActions
				JPopupMenu popup = getPopupMenu(MENUITEM_PASTE);
				popup.show(getTreeTable(), recentX, recentY);
			}
			else
			if (pasteWithin)	{	// flags have been provided by setEnabledActions on valueChange
				doPaste((List)selection, null);
			}
			else 
			if (pasteBefore)	{
				doPaste((List)selection, CommandArguments.PASTE_BEFORE);
			}
			else
			if (pasteAfter)	{
				doPaste((List)selection, CommandArguments.PASTE_AFTER);
			}
			else	{
				pasteError((List)selection);
				Err.warning("Can not paste to selection.");
			}
		}
	}

	private void doPaste(List selection, Integer pasteFlag)	{
		// provide CommandArguments
		PasteArguments pasteInfo = new PasteArguments(
				getXmlTreeTableModel(),
				pasteFlag,
				(MutableModel)clipboard.getSourceComponent().getTreeTableModel());

		ignoreSelectionChanged = true;	// tune performance
		Object [] pasted = null;
		try	{
			pasted = clipboard.paste(getTreeTable(), selection, pasteInfo);	// delegate to clipboard
		}
		finally	{
			ignoreSelectionChanged = false;
		}

		if (pasted != null)	{	// select the pasted nodes
			List l = Arrays.asList(pasted);
			getSelection().setSelectedObject(l);
		}

		// clear clipboard from involved Components
		clipboard.freeEditors();
	}


	/** Open a new XML document. */
	public void cb_Open(Object selection)	{
		if (getTreeTable() != null)	// keep column width when opening new editor window
			getTreeTable().rememberColumnWidth();

		try	{
			String [] uris = new OpenDialog(getParent(), getClass()).getURIsToOpen();
			for (int i = 0; uris != null && i < uris.length; i++)	{	// open all chosen files
				openURI(uris[i]);
			}
		}
		catch (CancelException e)	{
			// ignore exception from canceling dialog
		}
	}
	
	/**
		Opens the passed file in a new editor window.
	*/
	public void openURI(String uri)	{
		frameCreator.createEditor(uri);
	}


	/** Save the XML document. */
	public void cb_Save(Object selection)	{
		save();
	}


	/** Save the XML document (public as this is used by close window check routine). */
	public boolean save()	{
		CommitTreeTable.commit(getTreeTable());	// save contents

		XmlNode root = getXmlRoot();

		try	{
			if (root.hasDocumentErrors())	{
				int ret = JOptionPane.showConfirmDialog(
						getParent(),
						"The document contains errors or has lost data at load parsing!\n\n"+
							"Do you really want to save?",
						"Invalid Document",
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);

				if (ret == JOptionPane.NO_OPTION)
					return true;	// not saving invalid document

				if (ret != JOptionPane.YES_OPTION)
					return false;	// proceeding was canceled!
			}

			if (getTreeTable().getConfiguration().expandEntities)	{
				int ret = JOptionPane.showConfirmDialog(
						getParent(),
						"Entity references have been substituted by their values.\n"+
							"These references will be lost!\n\n"+
							"Do you really want to save?",
						"Entity References Expanded",
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);

				if (ret == JOptionPane.NO_OPTION)
					return true;	// not saving invalid document

				if (ret != JOptionPane.YES_OPTION)
					return false;	// proceeding was canceled!
			}

			if (root.canSaveWithoutOverwrite() == false)	{
				int ret = JOptionPane.showConfirmDialog(
						getParent(),
						"Overwrite \""+root.getURI()+"\"?",
						"Document Exitsts",
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);

				if (ret == JOptionPane.NO_OPTION)
					return saveAs();	// offer other location

				if (ret != JOptionPane.YES_OPTION)
					return false;	// proceeding was canceled!
			}

			getXmlTreeTableModel().save();	// save XML document
			getTreeTable().getFileChangeSupport().setFile(new File(root.getURI()));	// notify change support
			setEnabled(MENUITEM_SAVE, false);

			return true;	// was stored successfully
		}
		catch (Exception e)	{
			Err.error(e);
			return false;
		}
	}


	/** Save the XML document to another location. */
	public void cb_Save_As(Object selection)	{
		saveAs();
	}


	/** Save the XML document under another path. */
	public boolean saveAs()	{
		try	{
			XmlNode root = getXmlRoot();
			String oldUri = root.getURI();
			File f = DefaultFileChooser.saveDialog(new File(oldUri), getParent(), getClass());

			if (f != null)	{
				root.setURI(f.getPath());

				boolean ret = save();

				if (ret)	{
					MutableXmlTreeTableModel.renameInstance(oldUri, root.getURI());
					frameCreator.setRenderedEditorObject(getTreeTable(), root.getURI());
				}
				else	{
					root.setURI(oldUri);
				}
				
				return ret;
			}
		}
		catch (CancelException e)	{
		}
		return false;
	}



	/** Validate the document. */
	public void cb_Validate(Object selection)	{
		DocumentReloader validator = new DocumentReloader(getTreeTable());
		String text = validator.getDocumentAsString(true);	// get text from current document

		if (validator.validateDocument(text))	{
			getXmlRoot().resetDocumentErrors();	// reset errors in current document
			RefreshTreeTable.refresh(getTreeTable());	// remove red color from error items

			JOptionPane.showMessageDialog(
					getParent(),
					"Valid Document: "+getXmlRoot().getURI(),
					"Validation Success",
					JOptionPane.INFORMATION_MESSAGE);
		}
		// else: message has been provided
	}




	/** View the XML document as plain text. */
	public void cb_Edit_As_Text(Object selection)	{
		DocumentReloader reloader = new DocumentReloader(getTreeTable());
		String text = reloader.getDocumentAsString(false);

		if (text != null)	{
			DocumentEditDialog dlg = new DocumentEditDialog(
					ComponentUtil.getFrame(getParent()),	// parent Component
					text,	// to edit
					getXmlRoot().getURI(),	// title
					reloader,	// when finishing
					this);	// notify unsaved
		}
	}


	/** View the XML document as HTML text. */
	public void cb_View_As_HTML(Object selection)	{
		Object msg;

		try	{
			String html = getXmlRoot().getDocumentAsHtml();
			JEditorPane pane = new JEditorPane();
			pane.setContentType("text/html");
			EditorKit kit = pane.getEditorKit();
			Document doc = kit.createDefaultDocument();
			doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
			pane.setEditable(false);
			pane.setText(html);
			msg = new JScrollPane(pane);
			pane.setCaretPosition(0);
		}
		catch (Exception e)	{
			msg = e.toString();
		}

		JOptionPane pane = new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION);
		JDialog dlg = pane.createDialog(getParent(), "HTML View");	// - "+getXmlRoot().getURI());
		dlg.setResizable(true);
		new GeometryManager(dlg).show();
	}



	/** Expand all branches under selected node. */
	public void cb_Expand(Object selection)	{
		if (selection != null)	{
			List list = (List)selection;

			ignoreSelectionChanged = true;

			try	{
				for (int i = 0; i < list.size(); i++)	{
					ControllerModelItem cmi = (ControllerModelItem)list.get(i);
					XmlNode n = cmi.getXmlNode();
					TreeExpander.expandBranches(getTreeTable().getTree(), n);
				}
			}
			finally	{	// MUST set ignoreSelectionChanged to false!!!
				ignoreSelectionChanged = false;
			}
		}
	}


	/** Configure parser and display options. */
	public void cb_Configure(Object selection)	{
		boolean isDefault = getTreeTable() == null;
		Configuration conf = !isDefault ? getTreeTable().getConfiguration() : Configuration.getDefault();

		// show a modal dialog
		ConfigureDialog dlg = new ConfigureDialog(
				ComponentUtil.getFrame(getParent()),	// parent Component
				conf,
				isDefault,
				isDefault ? null : getXmlRoot().getRootTag(),
				enableOnlyInsertable);

		// retrieve result
		conf = dlg.getConfiguration();

		if (conf != null && getTreeTable() != null)	{	// was changed and committed
			getTreeTable().setConfiguration(conf);
			reloadFromMemory();
			// other frames must be re-opened to show changed configuration
		}

		enableOnlyInsertable = dlg.getEnableOnlyInsertable();
	}


	/** Show the DTD in a text window. */
	public void cb_Show_DTD(Object selection)	{
		Object msg;
		boolean error = false;

		try	{
			String dtd = getXmlRoot().getDTDAsString();
			JTextArea ta = new JTextArea(dtd);
			ta.setEditable(false);
			msg = new JScrollPane(ta);
			ta.setCaretPosition(0);
		}
		catch (Exception e)	{
			msg = e.toString();
			error = true;
		}

		JOptionPane pane = new JOptionPane(
				msg,
				error ? JOptionPane.ERROR_MESSAGE : JOptionPane.PLAIN_MESSAGE,
				JOptionPane.DEFAULT_OPTION);
		JDialog dlg = pane.createDialog(getParent(), "Document Type Definition");	// - "+getXmlRoot().getURI());
		dlg.setResizable(true);
		new GeometryManager(dlg).show();
	}


	/** Find text within document. */
	public void cb_Find(Object selection)	{
		if (ensureFindDialog() == false)	{
			searchDialog.init(ensureTextHolder(), true);
			searchDialog.setVisible(true);
		}
	}

	/** Find next location in document. */
	public void cb_Find_Next(Object selection)	{
		if (ensureFindDialog() == false)	{
			if (searchDialog.getCurrentTextArea() != ensureTextHolder())
				searchDialog.init(ensureTextHolder());
			searchDialog.findNext();
		}
	}

	private boolean ensureFindDialog()	{
		ensureTextHolder();

		if (searchDialog == null)	{
			JFrame frame = (JFrame)ComponentUtil.getFrame(getParent());
			searchDialog = new XmlSearchReplace(frame, this, ensureTextHolder());
			return true;
		}

		return false;
	}

	private TextHolder ensureTextHolder()	{
		if (getTreeTable().getTextHolder() == null)
			getTreeTable().setTextHolder(new SearchTextHolder(getTreeTable()));
		return getTreeTable().getTextHolder();
	}


	/** Close the container window of all internal frames. */
	public void cb_Close(Object selection)	{
		frameCreator.closeContainerWindow();
	}

}