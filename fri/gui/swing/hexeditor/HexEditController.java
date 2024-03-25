package fri.gui.swing.hexeditor;

import java.beans.*;
import java.awt.Toolkit;
import java.awt.Point;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.CaretEvent;
import fri.gui.mvc.controller.clipboard.*;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.model.*;
import fri.gui.swing.undo.*;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.util.CommitTable;
import fri.gui.swing.searchdialog.*;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.editor.EditController;
import fri.gui.swing.editor.EditorTextHolder;

/**
	Holds actions, callbacks and undoable edit handling for a hex editor.
	
	@author  Ritzberger Fritz
*/

public class HexEditController extends EditController implements
	PropertyChangeListener,
	KeyListener
{
	public static final String MENUITEM_INSERT = "Insert";
	public static final String MENUITEM_EDIT = "Edit";
	public static final String MENUITEM_REMOVE = "Remove";
	public static final String MENU_VIEW = "View";
	public static final String MENUITEM_HEX = "Hexadecimal";
	public static final String MENUITEM_DEC = "Decimal";
	public static final String MENUITEM_OCT = "Octal";
	public static final String MENUITEM_BIN = "Binary";
	public static final String MENUITEM_CHAR = "Character";
	public static final String MENUITEM_16_COLUMNS = "16 Columns";
	public static final String MENUITEM_32_COLUMNS = "32 Columns";
	public static final String MENUITEM_64_COLUMNS = "64 Columns";
	
	private static final Byte NEW_DEFAULT = new Byte((byte)0);

	private MdiClipboard clipboard;


	/**
		Create a text edit controller.
		It works only after <i>setMdiPane()</i> passed a valid container creator and selection holder.
		@param parent main window on which "close()" will be called on Menu-Exit.
	*/
	public HexEditController(GuiApplication parent)	{
		super(parent);
		
		clipboard = new MdiClipboard();
		clipboard.setDoListener(doListener);
	}


	/** Add additional actions to a hashtable and visualize them. */
	protected void insertActions()	{
		super.insertActions();
		
		registerAction(MENUITEM_CUT, Icons.get(Icons.cut), "Cut Selection To Clipboard", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_COPY, Icons.get(Icons.copy), "Copy Selection To Clipboard", KeyEvent.VK_C, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_PASTE, Icons.get(Icons.paste), "Paste Clipboard Into Selection", KeyEvent.VK_V, InputEvent.CTRL_MASK);

		registerAction(MENUITEM_INSERT, Icons.get(Icons.newLine), "Insert New Byte Before Selection", KeyEvent.VK_INSERT, 0);
		registerAction(MENUITEM_EDIT, Icons.get(Icons.fieldEdit), "Edit Selected Byte", KeyEvent.VK_F2, 0);
		registerAction(MENUITEM_REMOVE, Icons.get(Icons.delete), "Remove Selected Bytes", KeyEvent.VK_DELETE, 0);
		
		registerAction(MENUITEM_HEX);
		registerAction(MENUITEM_DEC);
		registerAction(MENUITEM_OCT);
		registerAction(MENUITEM_BIN);
		registerAction(MENUITEM_CHAR);

		registerAction(MENUITEM_16_COLUMNS);
		registerAction(MENUITEM_32_COLUMNS);
		registerAction(MENUITEM_64_COLUMNS);
	}


	/** The action enabled setting for an empty editor pane. */
	protected void setInitEnabled()	{
		super.setInitEnabled();
		
		setEnabled(MENUITEM_INSERT, false);
		setEnabled(MENUITEM_EDIT, false);
		setEnabled(MENUITEM_REMOVE, false);
	}


	/** Overridden to commit cell editing when an action is performed. */
	public void actionPerformed(ActionEvent e)	{
		if (currentEditor != null && ((HexTable)currentEditor).isEditing())
			CommitTable.commit((HexTable)currentEditor);
			
		super.actionPerformed(e);
	}
	

	/** Overridden to enable insert if file is empty. */
	public void cb_Open(Object editor)	{
		super.cb_Open(editor);
		setEnabled(MENUITEM_INSERT, currentEditor.getFile().length() <= 0);	// else must select where cell to insert to get enabled
	}
	
	
	private ControllerModelItem createControllerModelItem(HexTable table, boolean isMove)	{
		int start = table.getSelectionStart();
		int end = table.getSelectionEnd();
		if (start >= end)
			throw new IllegalStateException("Selection start is bigger equal selection end, can not create ControllerModelItem!");
		
		return createControllerModelItem(start, end, table, isMove);
	}

	private ControllerModelItem createControllerModelItem(int start, int end, HexTable table, boolean isMove)	{
		byte [] savedBytes = new byte[end - start];
		HexTableModel m = (HexTableModel)table.getModel();
		byte [] allBytes = m.getBytes();
		System.arraycopy(allBytes, start, savedBytes, 0, savedBytes.length); 

		return isMove ?
				new ControllerModelItem(start, end, savedBytes, table) :
				new ControllerModelItem(start, end, savedBytes);
	}
	
	private void executeCommand(UndoableCommand edit)	{
		clipboard.getDoListener().beginUpdate();
		edit.doit();	// in the case that nested actions happen, this is enclosed in begin/end update
		clipboard.getDoListener().addEdit(edit);
		clipboard.getDoListener().endUpdate();
	}

	
	public void cb_Cut(Object editor)	{
		ControllerModelItem cmi = createControllerModelItem((HexTable)editor, true);
		clipboard.cut(
				editor,
				new ControllerModelItem [] { cmi });
		copyToSystemClipboard(cmi);
	}
	
	public void cb_Copy(Object editor)	{
		ControllerModelItem cmi = createControllerModelItem((HexTable)editor, false);
		clipboard.copy(
				editor,
				new ControllerModelItem [] { cmi });
		copyToSystemClipboard(cmi);
	}


	public void cb_Paste(Object editor)	{
		HexTable table = (HexTable)editor;
		int start = table.getSelectionStart();
		int end = table.getSelectionEnd();
		if (start < 0)	{
			start = end = table.getCaretPosition();
			if (start < 0)	{
				Toolkit.getDefaultToolkit().beep();
				return;	//throw new IllegalStateException("No paste position is selected!");
			}
		}

		// check if pasting from system clipboard
		HexTable cutTable = (HexTable)clipboard.getSourceEditor();
		if (cutTable == null)	{	// try paste from system clipboard
			pasteFromSystemClipboard(editor, start, end);
			return;
		}
		
		// check for impossible paste action, within cutten range
		MutableModel cutModel = (MutableModel)cutTable.getModel();
		ModelItem [] mi = clipboard.getSourceModelItems();
		ControllerModelItem toPaste = (ControllerModelItem)mi[0];	// there is always only one
		if (cutModel == table.getModel())	{	// paste within the same model
			int endCheck = (start == end) ? end + 1 : end;
			//System.err.println("Paste range: "+start+"/"+endCheck+" Cut/Copy range: "+toPaste.getStart()+"/"+toPaste.getEnd());
			
			// ranges must not overlap
			if (toPaste.getStart() >= start && toPaste.getStart() < endCheck || toPaste.getEnd() > start && toPaste.getEnd() <= endCheck ||
					start >= toPaste.getStart() && start < toPaste.getEnd() || endCheck > toPaste.getStart() && endCheck <= toPaste.getEnd())
			{
				Toolkit.getDefaultToolkit().beep();
				System.err.println("Paste range and cut/copy range are overlapping!");
				return;
			}
		}
		
		// check if replacing selection or not
		if (confirmReplace(editor, start, end) == false)
			return;
		
		ControllerModelItem pasteTarget = new ControllerModelItem(start, start);
		ByteCommandArguments args = new ByteCommandArguments((MutableModel)table.getModel());
		args.setSendingModel(cutModel);
		
		clipboard.paste(
				editor,
				new ControllerModelItem [] { pasteTarget },
				args);
				
		table.select(start, start + toPaste.getEnd() - toPaste.getStart());
	}


	private boolean confirmReplace(Object editor, int start, int end)	{
		// check if replacing selection or not
		if (start < end)	{	// must remove selection before paste
			// as selection is not like in a standard table, show confirm dialog
			int ret = JOptionPane.showConfirmDialog(
					ComponentUtil.getWindowForComponent(((EditorTextHolder)editor).getTextComponent()),
					"Replace selected range "+start+" - "+end+" with clipboard contents?\n"+
							"If \"No\" is selected, clipboard contents will be pasted BEFORE selected range.",
					"Replace Selection",
					JOptionPane.YES_NO_CANCEL_OPTION); 

			if (ret == JOptionPane.YES_OPTION)	// if YES, remove selection before pasting clipboard contents
				cb_Remove(editor);
			
			if (ret != JOptionPane.NO_OPTION)
				return false;
		}
		return true;
	}

	private void pasteFromSystemClipboard(Object editor, int start, int end)	{
		if (Toolkit.getDefaultToolkit().getSystemClipboard() == null)
			return;
			
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
		if (t != null)	{
			String s;
			try	{
				s = (String)t.getTransferData(DataFlavor.stringFlavor);
			}
			catch (Exception e)	{
				s = t.toString();
			}
			
			if (s.length() > 0)	{
				if (confirmReplace(editor, start, end) == false)
					return;
				
				HexTable table = (HexTable)editor;
				byte [] bytes = s.getBytes();
				
				clipboard.getDoListener().beginUpdate();
				ControllerModelItem cmi = new ControllerModelItem(start, start + bytes.length);
				DefaultCreateCommand edit = new DefaultCreateCommand(
						editor,
						cmi,
						(MutableModel)table.getModel(),
						bytes);
				edit.doit();
				clipboard.getDoListener().addEdit(edit);
				clipboard.getDoListener().endUpdate();
				
				table.select(start, start + bytes.length);
			}
		}
	}

	private void copyToSystemClipboard(ControllerModelItem cmi)	{
		if (Toolkit.getDefaultToolkit().getSystemClipboard() == null)
			return;
			
		byte [] bytes = cmi.getBytesToInsert();
		StringSelection ss = new StringSelection(new String(bytes));
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
	}


	public void cb_Insert(Object editor)	{
		HexTable table = (HexTable)editor;
		HexTableModel m = (HexTableModel)table.getModel();
		int pos = table.getCaretPosition();
		if (pos < 0 || pos > m.getBytes().length)
			pos = m.getBytes().length;
			
		ControllerModelItem cmi = new ControllerModelItem(pos, pos + 1);
		DefaultCreateCommand edit = new DefaultCreateCommand(
				editor,
				cmi,
				(MutableModel)table.getModel(),
				NEW_DEFAULT);
		executeCommand(edit);
		
		table.clearSelection();
		clipboard.clear();

		// edit the inserted cell
		Point p = m.positionToPoint(pos);
		table.select(pos, pos);
		table.editCellAt(p.x, p.y);
	}

	
	public void cb_Remove(Object editor)	{
		HexTable table = (HexTable)editor;
		ControllerModelItem cmi = createControllerModelItem(table, false);
		DefaultRemoveCommand edit = new DefaultRemoveCommand(
				editor,
				cmi,
				(MutableModel)table.getModel());
		executeCommand(edit);

		clipboard.clear();
		
		int pos = Math.min(cmi.getStart(), ((HexTableModel) table.getModel()).getBytes().length - 1);
		if (pos >= 0)
			table.select(pos, pos + 1);	// TODO: why is this not working? Must select start to end, inclusive!
	}


	public void cb_Edit(Object editor)	{
		HexTable table = (HexTable)editor;
		int row = table.getSelectedRow();
		int col = table.getSelectedColumn();
		if (row >= 0 && col >= 0 && table.isCellEditable(row, col))	{
			table.editCellAt(row, col);
		}
	}

	/**
		Implements PropetyChangeListener to listen for cell editing.
		Creates an undable edit for changed text and calls <i>edit.doit()</i>.
	*/
	public void propertyChange(PropertyChangeEvent e)	{
		UndoableCommand edit = new UpdateCommand(
				e.getSource(),	// editor
				e.getOldValue(),
				e.getNewValue());
		executeCommand(edit);
	}


	
	public void cb_Hexadecimal(Object editor)	{
		loopEditors("base", 16);
	}
	
	public void cb_Decimal(Object editor)	{
		loopEditors("base", 10);
	}
	
	public void cb_Octal(Object editor)	{
		loopEditors("base", 8);
	}
	
	public void cb_Binary(Object editor)	{
		loopEditors("base", 2);
	}
	
	public void cb_Character(Object editor)	{
		loopEditors("base", Character.MIN_RADIX - 2);
	}


	public void cb_16_Columns(Object editor)	{
		loopEditors("columnCount", 16);
	}
	
	public void cb_32_Columns(Object editor)	{
		loopEditors("columnCount", 32);
	}
	
	public void cb_64_Columns(Object editor)	{
		loopEditors("columnCount", 64);
	}
	
	
	private void loopEditors(String name, int value)	{
		EditorTextHolder [] editors = getAllEditors();
		for (int i = 0; editors != null && i < editors.length; i++)	{
			HexTable table = (HexTable)editors[i];
			
			if (name.equals("columnCount"))	{
				table.setColumnCount(value);
				Config.setColumnCount(value);
			}
			else
			if (name.equals("base"))	{
				table.setBase(value);
				Config.setBase(value);
			}
			else
				throw new IllegalArgumentException("Unknown argument for loopEditors: "+name);
		}
	}


	// MdiFrame events
	
	/** Overridden to call super and to add/remove a PropertyChangeListener to catch cell editing. */
	protected void listen(EditorTextHolder editor, boolean set)	{
		super.listen(editor, set);
		
		if (set)	{
			((HexTable)editor).getChangeNotifier().addPropertyChangeListener(this);
			editor.getTextComponent().addKeyListener(this);
		}
		else	{
			((HexTable)editor).getChangeNotifier().removePropertyChangeListener(this);
			editor.getTextComponent().removeKeyListener(this);
		}
	}


	/**
		Returns the EditorTextHolder that is related to the pending undo action. 
		@param action DoAction.UNDO or DoAction.REDO.
	*/
	protected EditorTextHolder getEditorPendingForUndo(String action)	{
		return (EditorTextHolder)clipboard.getEditorPendingForUndo(action);
	}


	/** Clean all edits for the passed Document from UndoManager. This assumes that doListener has been set to clipboard. */
	protected void cleanEdits(DoListener doListener, Object undoableEditIdentifier)	{
		clipboard.cleanEdits(undoableEditIdentifier);
	}


	/** Overridden to set edit items enabled as soon as selection or caret exists. */
	public void caretUpdate(CaretEvent e) {
		super.caretUpdate(e);
		
		int dot = e.getDot();
		int mark = e.getMark();
		//System.err.println("caret update with dot="+dot+", mark="+mark);
		
		boolean canDelete = dot >= 0 && dot != mark;
		boolean canEdit = dot >= 0 && Math.abs(dot - mark) <= 1;
		boolean canInsert = dot >= -1;	// 0
		
		setEnabled(MENUITEM_REMOVE, canDelete);
		setEnabled(MENUITEM_EDIT, canEdit);
		setEnabled(MENUITEM_INSERT, canInsert);
		setEnabled(MENUITEM_CUT, canDelete);
		setEnabled(MENUITEM_COPY, canDelete);
		setEnabled(MENUITEM_PASTE, canInsert);
	}


	// interface KeyListener
	
	/** Implements KeyListener to catch TAB (block-indent) and ENTER (auto-indent) */
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
			clipboard.clear();
		}
	}
	public void keyReleased(KeyEvent e)	{}
	public void keyTyped(KeyEvent e)	{}



	/** Create a SearchReplace window that creates undoable edits on replacing. */
	protected AbstractSearchReplace createFindDialog(JFrame frame, EditorTextHolder textarea)	{
		return new SearchReplace(frame, textarea)	{
			/** Set text by creating Commands and executing them. */
			protected void setReplacedTextToTextHolder(String newText)	{
				HexTable table = (HexTable)currentEditor;
				HexTableModel m = (HexTableModel)table.getModel();

				clipboard.getDoListener().beginUpdate();

				ControllerModelItem cmiRemove = createControllerModelItem(0, m.getBytes().length, table, false);
				DefaultRemoveCommand editRemove = new DefaultRemoveCommand(
						currentEditor,
						cmiRemove,
						m);
				editRemove.doit();
				clipboard.getDoListener().addEdit(editRemove);

				if (newText.length() > 0)	{
					byte [] newBytes = table.stringToBytes(newText);
					ControllerModelItem cmiInsert = new ControllerModelItem(0, newBytes.length);
					DefaultCreateCommand editInsert = new DefaultCreateCommand(
							currentEditor,
							cmiInsert,
							m,
							newBytes);
					editInsert.doit();
					clipboard.getDoListener().addEdit(editInsert);
				}
				
				clipboard.getDoListener().endUpdate();
			}
		};
	}


	public void close()	{
		super.close();
		Config.store();
	}
	
}
