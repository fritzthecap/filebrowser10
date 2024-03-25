package fri.gui.swing.text;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.undo.*;
import javax.swing.text.*;
import javax.swing.event.*;
import fri.util.text.LineSize;

/**
	Modal dialog that lets edit a passed text. Use <i>getText()</i>
	to retrieve the edit result after constructing the dialog.
	<p>
	This dialog lets set tab width and wrap line property.
	It closes when pressing ESCAPE, without saving changed text.
*/

public class MultilineEditDialog extends JDialog implements
	KeyListener,	// listen for ESCAPE key
	MouseListener,	// listen for mouse to open popup
	ActionListener,	// listen for popup menu items
	DocumentListener	// listen for changes
{
	private final static String defaultTitle = "Text Editor";
	private final static int MAXROWS = 40, MINROWS = 8, MAXCOLS = 64, MINCOLS = 16;
	protected static int tabWidth = 4;
	protected static boolean lineWrap = true;
	protected JTextArea textarea;
	private String original;
	private JPopupMenu popup = new JPopupMenu();
	private JMenuItem w1, w2, w3, w4, w6, w8;
	private JCheckBoxMenuItem wrap;
	protected boolean escaped;
	protected boolean changed;

	
	/** Create a modal dialog with title "Text Editor" that lets edit passed text.  */
	public MultilineEditDialog(Frame parent, JComponent launcher, String text)	{
		this(parent, launcher, text, null, true);
	}
	
	/** Create a modal dialog that lets edit passed text and shows given title in titlebar.  */
	public MultilineEditDialog(Frame parent, JComponent launcher, String text, String title)	{
		this(parent, launcher, text, title, true);
	}

	/** Create a dialog that lets edit passed text and shows given title in titlebar.  */
	public MultilineEditDialog(Frame parent, JComponent launcher, String text, String title, boolean modal)	{
		super(parent, (title == null || title.length() <= 0) ? defaultTitle : title, modal);

		build(text, parent);
		start(parent, launcher);
	}

	/** Create a dialog that lets edit passed text and shows given title in titlebar.  */
	public MultilineEditDialog(Dialog parent, JComponent launcher, String text, String title, boolean modal)	{
		super(parent, (title == null || title.length() <= 0) ? defaultTitle : title, modal);

		build(text, parent);
		start(parent, launcher);
	}

	/** Create a modal dialog with title "Text Editor" that lets edit passed text.  */
	public MultilineEditDialog(Dialog parent, JComponent launcher, String text)	{
		this(parent, launcher, text, null, true);
	}
	

	private void start(Window parent, JComponent launcher)	{
		// dialog without titlebar
		/* This would throw NoClassDefFoundException as WindowFocusListener is not present in Java 1.4
		if (shouldOpenUndecorated() && launcher != null && OS.isAboveJava13)	{
			setUndecorated(true);
			WindowFocusListener wlsnr = new WindowFocusListener()	{
				public void windowLostFocus(WindowEvent e) {
					if (e.getOppositeWindow() instanceof FrameServiceDialog.WindowImpl || e.getOppositeWindow() instanceof GoLineDialog)
						return;	// was a find or goline dialog
						
					MultilineEditDialog.this.removeWindowFocusListener(this);
					dispose();
				}
				public void windowGainedFocus(WindowEvent e) {
				}
			};
			addWindowFocusListener(wlsnr);
		}
		*/
		
		pack();

		boolean wasLocated = false;
		if (launcher != null && shouldOpenUndecorated())	{
			try	{
				fri.gui.swing.LocationUtil.locateUnderLauncher(this, parent, launcher);
				wasLocated = true;
			}
			catch (Throwable error)	{
				System.err.println("Error was thrown on locating dialog: "+error.toString());
			}
		}
		
		if (wasLocated == false)	{
			fri.gui.LocationUtil.centerOverParent(this, parent);
		}

		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				textarea.setCaretPosition(0);
			}
		});
		
		setVisible(true);
	}

	protected boolean shouldOpenUndecorated()	{
		return true;
	}
	
	private void build(String text, Window parent)	{
		original = text;
		escaped = false;
		
		// create fitting textarea
		int rows = Math.max(LineSize.getLineCount(text), MINROWS);
		int cols = Math.max(LineSize.getMaximumLineLength(text), MINCOLS);
		rows = Math.min(rows + 3, MAXROWS);
		cols = Math.min(cols + 3, MAXCOLS);

		textarea = createTextArea(text, rows, cols);
		setLineWrap(lineWrap);	// will be short text, but maybe long line!
		setTabWidth(tabWidth);
		setToolTipText("Press ALT-F4 to commit or ESCAPE to cancel");

		popup = createPopup();

		// listen for popup events
		if (popup != null)
			textarea.addMouseListener(this);

		// listen for escape
		//addKeyListener(this);
		textarea.addKeyListener(this);

		// listen for changes
		textarea.getDocument().addDocumentListener(this);

		// listen for undo/redo
		addUndoManagement(textarea);

		JScrollPane sp = new JScrollPane(textarea);
		getContentPane().add(sp);
	}


	/** Adding Ctl-Z and Ctl-Y to textarea. */
	protected void addUndoManagement(JTextComponent textarea)	{
		final UndoManager undoMgr = new UndoManager();

		textarea.getDocument().addUndoableEditListener(new UndoableEditListener()	{
			public void undoableEditHappened(UndoableEditEvent e) {
				undoMgr.addEdit(e.getEdit());
			}
		});

		Keymap keymap = JTextComponent.addKeymap("MyUndoBindings", textarea.getKeymap());

		Action undoAction = new AbstractAction("Undo")	{
			public void actionPerformed(ActionEvent e)	{
				try	{ undoMgr.undo(); }	catch (CannotUndoException ex)	{}
			}
		};
		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK);
		keymap.addActionForKeyStroke(key, undoAction);

		Action redoAction = new AbstractAction("Redo")	{
			public void actionPerformed(ActionEvent e)	{
				try	{ undoMgr.redo(); }	catch (CannotRedoException ex)	{}
			}
		};
		key = KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK);
		keymap.addActionForKeyStroke(key, redoAction);

		textarea.setKeymap(keymap);
	}

	/** Create some JTextArea. */
	protected JTextArea createTextArea(
		String text,
		int supposedRows,
		int supposedColumns)
	{
		textarea = new JTextArea(text, supposedRows, supposedColumns);
		return textarea;
	}

	/** Create the popup showing on textarea. */
	protected JPopupMenu createPopup()	{
		// build popup
		JPopupMenu popup = new JPopupMenu();

		// line wrap choice
		wrap = new JCheckBoxMenuItem("Wrap Lines", true);
		// will be short text, but maybe long lines
		popup.add(wrap);
		wrap.addActionListener(this);
		popup.addSeparator();
		// tab size choice
		JMenu tabWidth = new JMenu("Tab Width");
		popup.add(tabWidth);
		tabWidth.add(w1 = new JMenuItem("1"));
		w1.addActionListener(this);
		tabWidth.add(w2 = new JMenuItem("2"));
		w2.addActionListener(this);
		tabWidth.add(w3 = new JMenuItem("3"));
		w3.addActionListener(this);
		tabWidth.add(w4 = new JMenuItem("4"));
		w4.addActionListener(this);
		tabWidth.add(w6 = new JMenuItem("6"));
		w6.addActionListener(this);
		tabWidth.add(w8 = new JMenuItem("8"));
		w8.addActionListener(this);

		return popup;
	}

	protected JPopupMenu getPopup()	{
		return popup;
	}


	/** Returns the edited text, or the original when ESCAPE was pressed. */
	public String getText()	{
		if (escaped)
			return original;
		else
			return textarea.getText();
	}


	/** Implements KeyListener close dialog on ESCAPE. */
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
			escapePressed();
		}
	}
	public void keyTyped(KeyEvent e)	{}
	public void keyReleased(KeyEvent e)	{}

	protected void escapePressed()	{
		escaped = true;

		if (changed)	{
			int ret = JOptionPane.showConfirmDialog(
					this,
					"Keep Changes?",
					"Confirm Changes",
					JOptionPane.YES_NO_CANCEL_OPTION);

			if (ret != JOptionPane.YES_OPTION && ret != JOptionPane.NO_OPTION)
				return;

			if (ret == JOptionPane.YES_OPTION)
				escaped = false;
		}

		dispose();
	}
	
	/** Implements MouseListener to popup line wrap menu. */
	public void mousePressed(MouseEvent e)	{
		showPopupOnEvent(e);
	}
	/** Implements MouseListener to popup line wrap menu. */
	public void mouseReleased(MouseEvent e)	{
		showPopupOnEvent(e);
	}
	public void mouseClicked(MouseEvent e)	{}
	public void mouseEntered(MouseEvent e)	{}
	public void mouseExited(MouseEvent e)	{}
	
	private void showPopupOnEvent(MouseEvent e)	{
		if (e.isPopupTrigger() && popup != null)
			popup.show(textarea, e.getX(), e.getY());
	}

	/** Implements ActionListener to wrap lines and set tab width. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == wrap)	{
			lineWrap = !textarea.getLineWrap();
			setLineWrap(lineWrap);
		}
		else
		if (e.getSource() == w1 ||
				e.getSource() == w2 ||
				e.getSource() == w3 ||
				e.getSource() == w4 ||
				e.getSource() == w6 ||
				e.getSource() == w8)
		{
			String n = ((JMenuItem)e.getSource()).getText();
			tabWidth = Integer.valueOf(n).intValue();
			setTabWidth(tabWidth);
		}
	}
	
	protected void setTabWidth(int tw)	{
		textarea.setTabSize(tw);
		
		boolean b = textarea.getLineWrap();
		textarea.setLineWrap(!b);
		textarea.setLineWrap(b);
	}

	protected void setLineWrap(boolean lw)	{
		textarea.setLineWrap(lw);
	}

	protected void setToolTipText(String t)	{
		textarea.setToolTipText(t);
	}


	/** Implements DocumentListener to catch changed state. */
	public void changedUpdate(DocumentEvent e)	{
		changed = true;
	}

	/** Implements DocumentListener to catch changed state. */
	public void insertUpdate(DocumentEvent e)	{
		changed = true;
	}

	/** Implements DocumentListener to catch changed state. */
	public void removeUpdate(DocumentEvent e)	{
		changed = true;
	}

}