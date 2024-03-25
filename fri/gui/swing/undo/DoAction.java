package fri.gui.swing.undo;

import java.awt.event.*;	// ActionEvent, ActionListener ...
import javax.swing.*;	// AbstractAction, ImageIcon ...
import javax.swing.undo.*;	// UndoManager etc.
import java.net.URL;
import java.util.Vector;

/**
 * Union implementation of Undo/Redo Action.
 * Use together with <i>DoListener</i> or any <i>UndoableEditListener</i>.<br>
 * Lifecycle:
 * <UL>
 * <LI>allocate with DoAction.UNDO or REDO and UndoManager as parameters
 * <LI>setCounterPart() of peer action. This is done by <i>DoListener</i>.
 * <LI>add Action to Component and get a Button or MenuItem
 * </UL>
 * Errors: meaning not set, counterPart not set, undoManager not set.
 * 
 * @author Fritz Ritzberger 
 */
public class DoAction extends AbstractAction implements ActionListener
{
	public static String UNDO = "Undo";
	public static String REDO = "Redo";
	
	protected UndoManager undoManager;
	protected String meaning;
	protected String presentationName = "";
	private DoAction counterPart;
	private boolean textual = false;
	private Vector willPerformActionListeners = null;

	/**
		Create an Undo/Redo Action.
		Method setUndoManager() must be called before using it (done by DoListener).
		@param meaning DoAction.UNDO or DoAction.REDO
	*/	
	public DoAction(String meaning) {
		this(meaning, (String) null);
	}

	/**
		@param meaning DoAction.UNDO or DoAction.REDO
		@param icon icon for Action
	*/	
	public DoAction(String meaning, Icon icon) {
		super(meaning, icon);
		
		if (meaning.equals(UNDO) == false && meaning.equals(REDO) == false)
			throw new IllegalArgumentException("Illegal meaning for DoAction: "+meaning);
		
		this.meaning = meaning;
		
		putValue(ACTION_COMMAND_KEY, meaning);
		
		putValue(ACCELERATOR_KEY, meaning.equals(UNDO)
				? KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK)
				: KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
		
		setEnabled(false);
	}

	/**
		@param meaning DoAction.UNDO or DoAction.REDO
		@param image filename to use as icon for Action
	*/	
	public DoAction(String meaning, String image) {
		this(meaning, image != null ? new ImageIcon(image) : null);
	}

	/**
		@param meaning DoAction.UNDO or DoAction.REDO
		@param url image URL to use as icon for Action
	*/	
	public DoAction(String meaning, URL image) {
		this(meaning, image != null ? new ImageIcon(image) : null);
	}

	/**
		@param undoManager undoManager to ask for doing
		@param meaning DoAction.UNDO or DoAction.REDO
	*/	
	public DoAction(UndoManager undoManager, String meaning) {
		this(meaning);
		setUndoManager(undoManager);
	}

	public DoAction(UndoManager undoManager, String meaning, URL image) {
		this(meaning, image);
		setUndoManager(undoManager);
	}

	public DoAction(UndoManager undoManager, String meaning, String image) {
		this(meaning, image);
		setUndoManager(undoManager);
	}

	/**
		Set the reference to the "doing" peer.
		@param counterPart
			undoAction.setCounterPart(redoAction) or
			redoAction.setCounterPart(undoAction)
	*/
	public void setCounterPart(DoAction counterPart)	{
		this.counterPart = counterPart;
	}

	/**
		Set the reference to the UndoManager
		@param undoManager undo list to call and ask for undoing/redoing.
	*/
	public void setUndoManager(UndoManager undoManager)	{
		this.undoManager = undoManager;
		setEnabled(undoManager.canUndo());
	}

	/**
		The Action should set its NAME property to render text on GUI.
		Default is false.
		@param textual render text if true
	*/
	public void setTextual(boolean textual)	{
		this.textual = textual;
	}

	/**
		Add a listener that will be notified to do cleanups before
		the undo/redo action is performed.
		@param al action listener that wants to be notified before action performs
	*/
	public void addWillPerformActionListener(ActionListener al)	{
		if (willPerformActionListeners == null)
			willPerformActionListeners = new Vector();
		willPerformActionListeners.addElement(al);
	}
	
	private void fireWillPerformActionEvent(ActionEvent e)	{
		for (int i = 0;
				willPerformActionListeners != null &&
					i < willPerformActionListeners.size();
				i++)
		{
			ActionListener al = (ActionListener)willPerformActionListeners.elementAt(i);
			al.actionPerformed(new ActionEvent(
					e.getSource(),
					e.getID(),
					meaning)
				);
		}
	}
	
	
	/** implementing ActionListener */
	public void actionPerformed(ActionEvent e) {
		//System.err.println("DoAction.actionPerformed "+e.getActionCommand());
		fireWillPerformActionEvent(e);
		// undo or redo by undo manager
		try {
			if (meaning.equals(REDO))
				undoManager.redo();
			else
			if (meaning.equals(UNDO))
				undoManager.undo();
			else
				throw new IllegalArgumentException("Incorrect semantic identifier in DoAction: "+meaning);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.err.println("FEHLER: Unable to "+meaning);
		}
		update();
		counterPart.update();
	}


	/** implementing ActionListener, enable the undo/redo action and
		get new presentation names. */
	public void update() {
		boolean canDo;
		if (meaning.equals(REDO))	{
			canDo = undoManager.canRedo();
			presentationName = undoManager.getRedoPresentationName();
		}
		else
		if (meaning.equals(UNDO))	{
			canDo = undoManager.canUndo();
			presentationName = undoManager.getUndoPresentationName();
		}
		else	{
			System.err.println("FEHLER: meaning not set correctly in DoAction");
			return;
		}

		if (textual)
			putValue(Action.NAME, canDo ? presentationName : meaning);

		setEnabled(canDo);
	}
	
}
