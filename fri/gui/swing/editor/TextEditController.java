package fri.gui.swing.editor;

import java.lang.reflect.Field;
import java.util.Enumeration;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.AbstractDocument;
import javax.swing.undo.*;
import fri.util.text.encoding.Encodings;
import fri.gui.swing.undo.*;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.spinnumberfield.NumberEditorListener;
import fri.gui.swing.golinedialog.GoLineDialog;
import fri.gui.swing.concordance.textarea.TextareaConcordanceDialog;
import fri.gui.swing.encoding.EncodingChooser;
import fri.gui.swing.iconbuilder.Icons;

/**
	Holds additional actions and callbacks for a text editor.
	
	@author  Ritzberger Fritz
*/

public class TextEditController extends EditController implements
	NumberEditorListener	// tab size changed
{
	public static final String MENU_SAVEWITH = "Save With Newline";
	public static final String MENUITEM_GOTO = "Goto";
	public static final String MENUITEM_CONCORDANCE = "Line Concordances";
	public static final String MENUITEM_PLATFORMNEWLINE = "Platform";
	public static final String MENUITEM_UNIX_NEWLINE = "UNIX";
	public static final String MENUITEM_WINDOWS_NEWLINE = "WINDOWS";
	public static final String MENUITEM_MAC_NEWLINE = "MAC";
	public static final String MENUITEM_AUTOINDENT = "Autoindent";
	public static final String MENUITEM_WRAPLINES = "Wrap Lines";
	public static final String MENU_ENCODING = "Encoding";
	public static final String MENUITEM_CHOOSE_ENCODING = "Choose General Encoding";
	public static final String MENUITEM_DETECT_BYTEORDERMARK = "Detect Encoding From Byte Order Mark";
	public static final String MENUITEM_CREATE_BYTEORDERMARK = "Create Byte Order Mark On New Files (UTF-8, UTF-16 Only)";
	public static final String MENUITEM_DETECT_XMLHEADER = "Detect XML Or HTML Header Encoding";

	private String encoding;
	private AbstractButton encodingItem;
	private TextareaConcordanceDialog concordanceDialog;

	/**
		Create a text edit controller.
		It works only after <i>setMdiPane()</i> passed a valid container creator and selection holder.
		@param frame main window on which "close()" will be called on Menu-Exit.
	*/
	public TextEditController(GuiApplication parent)	{
		super(parent);
	}


	/** Add additional actions to a hashtable and visualize them. */
	protected void insertActions()	{
		super.insertActions();
		
		registerAction(MENUITEM_GOTO, Icons.get(Icons.gotoLine), "Go To Line Number", KeyEvent.VK_G, InputEvent.CTRL_MASK);
		registerAction(MENUITEM_CONCORDANCE, Icons.get(Icons.concordance), "Detect Concordant Lines");
		registerAction(MENUITEM_UNIX_NEWLINE);
		registerAction(MENUITEM_WINDOWS_NEWLINE);
		registerAction(MENUITEM_MAC_NEWLINE);
		registerAction(MENUITEM_PLATFORMNEWLINE);
		registerAction(MENUITEM_AUTOINDENT, (String)null, "Indent Automatically On Newline", 0, 0);
		registerAction(MENUITEM_WRAPLINES, (String)null, "No Horizontal Scrollbar", 0, 0);
		registerAction(MENUITEM_CHOOSE_ENCODING);
		registerAction(MENUITEM_DETECT_BYTEORDERMARK);
		registerAction(MENUITEM_DETECT_XMLHEADER);
		registerAction(MENUITEM_CREATE_BYTEORDERMARK);

		Action cut = new DefaultEditorKit.CutAction();
		cut.putValue(Action.NAME, MENUITEM_CUT);
		cut.setEnabled(false);
		registerAction(cut, Icons.get(Icons.cut), "Cut Selection To Clipboard", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		Action copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, MENUITEM_COPY);
		copy.setEnabled(false);
		registerAction(copy, Icons.get(Icons.copy), "Copy Selection To Clipboard", KeyEvent.VK_C, InputEvent.CTRL_MASK);
		Action paste = new DefaultEditorKit.PasteAction();
		paste.putValue(Action.NAME, MENUITEM_PASTE);
		paste.setEnabled(false);
		registerAction(paste, Icons.get(Icons.paste), "Paste Clipboard Into Selection", KeyEvent.VK_V, InputEvent.CTRL_MASK);
	}


	/** Set disabled "Goto" item. */
	protected void setInitEnabled()	{
		super.setInitEnabled();
		setEnabled(MENUITEM_GOTO, false);
		setEnabled(MENUITEM_CONCORDANCE, false);
	}

	
	/**
		Returns the EditorTextHolder that is related to the pending undo action. 
		@param action DoAction.UNDO or DoAction.REDO.
	*/
	protected EditorTextHolder getEditorPendingForUndo(String action)	{
		// get pending edit from manager
		UndoableEdit edit =
				action.equals(DoAction.UNDO) ? doListener.getFirstEditToBeUndone()
				: action.equals(DoAction.REDO) ? doListener.getFirstEditToBeRedone() : null;

		Object document;
		if (edit != null && (document = getDocumentFromUndoableEdit(edit)) != null)	{
			EditorTextHolder [] e = getAllEditors();
			for (int i = 0; i < e.length; i++)	{
				Object editorDocument = e[i].getUndoableEditIdentifier();
				if (editorDocument != null && editorDocument == document)	{
					return e[i];
				}
			}
		}
		
		return null;	// user pressed continuously Ctl-Z ???
	}

	private Object getDocumentFromUndoableEdit(UndoableEdit edit)	{
		AbstractDocument.DefaultDocumentEvent docEdit = null;
		if (edit instanceof AbstractDocument.DefaultDocumentEvent)	{
			docEdit = (AbstractDocument.DefaultDocumentEvent) edit;
		}
		else	{	// workaround above Java 1.8 
			try	{
				Field ddeField = edit.getClass().getDeclaredField("dde");
				ddeField.setAccessible(true);
				Object dde = ddeField.get(edit);
				docEdit = (AbstractDocument.DefaultDocumentEvent) dde;
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
		return (docEdit == null) ? null : docEdit.getDocument();
	}


	// callbacks

	public void cb_Detect_Encoding_From_Byte_Order_Mark(Object editor)	{
		boolean detect = isChecked((AbstractButton) currentActionEvent.getSource());
		Config.setDetectEncodingFromByteOrderMark(detect);
	}
	
	public void cb_Detect_XML_Or_HTML_Header_Encoding(Object editor)	{
		boolean detect = isChecked((AbstractButton) currentActionEvent.getSource());
		Config.setDetectXmlOrHtmlHeaderEncoding(detect);
	}
	
	public void cb_Create_Byte_Order_Mark_On_New_Files__UTF_8__UTF_16_Only_(Object editor)	{
		boolean create = isChecked((AbstractButton) currentActionEvent.getSource());
		Config.setCreateByteOrderMark(create);
		setEditorsChanged();
	}

	public void cb_UNIX(Object editor)	{
		Config.setNewline(Config.UNIX_NEWLINE);
		setEditorsChanged();
	}
	
	public void cb_WINDOWS(Object editor)	{
		Config.setNewline(Config.WINDOWS_NEWLINE);
		setEditorsChanged();
	}
	
	public void cb_MAC(Object editor)	{
		Config.setNewline(Config.MAC_NEWLINE);
		setEditorsChanged();
	}
	
	public void cb_Platform(Object editor)	{
		Config.setNewline(null);
		setEditorsChanged();
	}

	private void setEditorsChanged()	{
		EditorTextHolder [] e = getAllEditors();
		for (int i = 0; i < e.length; i++)	{
			if (e[i] instanceof BasicTextEditArea)	{
				((BasicTextEditArea) e[i]).setChanged();
			}
		}
	}


	public void cb_Goto(Object editor)	{
		new GoLineDialog(getFrame(), (JTextComponent)((EditorTextHolder)editor).getTextComponent());
	}


	public void cb_Wrap_Lines(Object editor)	{
		boolean wrap = isChecked((AbstractButton)currentActionEvent.getSource());
		Config.setWrapLines(wrap);

		EditorTextHolder [] e = getAllEditors();
		for (int i = 0; i < e.length; i++)	{
			if (e[i].getTextComponent() instanceof JTextArea)	{
				JTextArea ta = (JTextArea) e[i].getTextComponent();
				ta.setLineWrap(wrap);
			}
		}
	}
	

	public void cb_Autoindent(Object editor)	{
		boolean ai = isChecked((AbstractButton)currentActionEvent.getSource());
		Config.setAutoIndent(ai);
	}


	public void setEncodingItem(AbstractButton encodingItem)	{
		this.encodingItem = encodingItem;
		String displayEncoding = encoding != null ? encoding : Config.getEncoding() != null ? Config.getEncoding() : Encodings.defaultEncoding;
		encodingItem.setText(MENUITEM_CHOOSE_ENCODING+" ("+displayEncoding+")");
	}
	
	public void cb_Choose_General_Encoding(Object editor)	{
		EncodingChooser chooser = new EncodingChooser(getFrame(), encoding);
		
		if (chooser.wasCanceled() == false)	{
			EditorTextHolder [] editors = getAllEditors();

			int ret = JOptionPane.showConfirmDialog(
				getFrame(),
				"Really Change Encoding"+(editors != null && editors.length > 0 ? " For All Open Editors" : "")+"?\n"+
					"This Can Be Reset By Selecting \"System-Default\" Encoding.",
				"Change Encoding",
				JOptionPane.YES_NO_OPTION);
			
			if (ret != JOptionPane.YES_OPTION)
				return;

			encoding = chooser.getEncoding();
			if (Encodings.defaultEncoding.equals(encoding))
				encoding = null;
			
			Config.setEncoding(encoding);	// stores this encoding globally
			setEncodingItem(encodingItem);

			for (int i = 0; i < editors.length; i++)	{
				if (editors[i] instanceof TextEditArea)	{
					TextEditArea ta = (TextEditArea)editors[i];
					ta.setEncoding(encoding);
				}
			}
		}
	}
		
	public void cb_Line_Concordances(Object editor)	{
		JTextComponent ta = (JTextComponent)((EditorTextHolder)editor).getTextComponent();
		if (concordanceDialog == null)	{
			concordanceDialog = new TextareaConcordanceDialog(getFrame(), ta);
		}
		else	{
			concordanceDialog.init(ta);
			concordanceDialog.setVisible(true);
		}
	}


	// events from MDI framework (called from FileEditorManager)

	/** A new editor was opened. Add all listeners. */
	public void editorOpened(EditorTextHolder editor)	{
		super.editorOpened(editor);
		
		setEnabled(MENUITEM_GOTO, true);
		setEnabled(MENUITEM_CONCORDANCE, true);
	}

	/** An editor was selected. Change the accelerator sensor and feed the search window with new text. */
	public void editorActivated(EditorTextHolder editor)	{
		super.editorActivated(editor);
		
		// workaround caret disappering
		JTextComponent ta = (JTextComponent)editor.getTextComponent();
		ta.getCaret().setVisible(true);
		
		if (concordanceDialog != null && concordanceDialog.isVisible())
			concordanceDialog.init(ta);
	}

	public void editorClosed(EditorTextHolder editor)	{
		super.editorClosed(editor);
		
		if (concordanceDialog != null && mdiPane.getMdiFrames().length <= 0)
			concordanceDialog.setVisible(false);
	}

	/** Clean all edits for the passed Document from UndoManager. */
	protected void cleanEdits(DoListener doListener, Object undoableEditIdentifier)	{
		if (undoableEditIdentifier == null)
			return;
			
		for (Enumeration e = doListener.elements(); e.hasMoreElements(); )	{
			UndoableEdit edit = (UndoableEdit)e.nextElement();
			Object editId = null;

			if (edit instanceof AbstractDocument.DefaultDocumentEvent)	{
				editId = ((AbstractDocument.DefaultDocumentEvent)edit).getDocument();
			}
			else	{
				System.err.println("Unidentifyable edit class in UndoManager: "+edit);
				Thread.dumpStack();
			}

			if (editId != null && editId == undoableEditIdentifier)	{
				//System.err.println("killing edit "+edit);

				edit.die();
			}
		}
		
		doListener.removeDeadEdits();
	}
	

	// interface NumberEditorListener

	/** Implements NumberEditorListener to set tab size for all editors. */
	public void numericValueChanged(long newValue)	{
		EditorTextHolder [] e = getAllEditors();
		Config.setTabSize((int)newValue);
		
		for (int i = 0; i < e.length; i++)	{
			if (e[i].getTextComponent() instanceof JTextArea)	{
				JTextArea ta = (JTextArea)e[i].getTextComponent();
				ta.setTabSize((int)newValue);
			}
			else	{
				System.err.println("Unknown kind of textarea: "+e[i].getTextComponent().getClass());
			}
		}
	}


	public void close()	{
		if (concordanceDialog != null)	{
			concordanceDialog.dispose();
			concordanceDialog = null;
		}
		super.close();
	}

}

