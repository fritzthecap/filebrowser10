package fri.gui.swing.xmleditor.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import fri.gui.CursorUtil;
import fri.gui.swing.filechangesupport.FileChangeSupport;
import fri.gui.swing.text.*;
import fri.gui.swing.xmleditor.model.DocumentLoader;

/**
	A modal dialog that lets edit the text of a document.
	When closing and text was changed, it tries to re-parse document text
	and load it into the passed DocumentLoader.
*/

public class DocumentEditDialog extends ElementEditDialog implements
	WindowListener
{
	private DocumentLoader loader;
	private JMenuItem validate;
	private boolean closed;
	private FileChangeSupport.Reloader changeSupport;


	public DocumentEditDialog(Frame parent, String text, String title, DocumentLoader loader, FileChangeSupport.Reloader changeSupport)	{
		super(parent, null, text, title, false);
		setModal(false);
		this.loader = loader;
		this.changeSupport = changeSupport;
		super.setVisible(true);
	}

	protected boolean shouldOpenUndecorated()	{
		return false;
	}

	/**
		Overridden to install window listener, constructor paramers must be loaded,
		setVisible() will be called from this' constructor.
	*/
	public void setVisible(boolean visible)	{
		if (visible && loader == null)	{
			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			removeWindowListener(this);
			addWindowListener(this);
		}
		else	{
			super.setVisible(visible);
		}
	}


	/** Overridden to allocate ComfortTextArea. */
	protected JTextArea createTextArea(
		String text,
		int supposedRows,
		int supposedColumns)
	{
		ComfortTextArea ta = new ComfortTextArea(text);
		ta.setRows(supposedRows);
		ta.setColumns(supposedColumns);
		ta.setLineWrap(false);	// will be multiline text

		ta.getPopupMenu().insert(validate = new JMenuItem("Revalidate"), 0);
		validate.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		validate.addActionListener(this);
		ta.getPopupMenu().insert(new JPopupMenu.Separator(), 1);

		return this.textarea = ta;
	}

	/** Overridden to do nothing: ComfortTextArea does it. */
	protected JPopupMenu createPopup()	{
		return null;
	}

	/** Overridden to do nothing: ComfortTextArea does it. */
	protected void setTabWidth(int tw)	{}

	/** Overridden to do nothing: ComfortTextArea does it. */
	protected void setLineWrap(boolean lw)	{}

	protected JPopupMenu getPopup()	{
		return ((ComfortTextArea)textarea).getPopupMenu();
	}


	private void validateDocument()	{
		CursorUtil.setWaitCursor(this);
		try	{
			if (loader.validateDocument(getText()) == true)	{
				JOptionPane.showMessageDialog(
						this,
						"Valid Document.",
						"Validation Success",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}


	/** Overrides ActionListener to validate document. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == validate)	{
			validateDocument();
		}
		else	{
			super.actionPerformed(e);
		}
	}

	/** Overridden to catch ESCAPE and ask for save. */
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
			windowClosing(null);
		}
		else
		if (e.getKeyCode() == KeyEvent.VK_R && (e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK)	{
			validateDocument();
		}
	}

	/** Implements WindowListener: try to parse document when changed. */
	public void windowClosing(WindowEvent e)	{
		if (!closed)	{
			if (changed)	{
				int ret = JOptionPane.showConfirmDialog(
						this,
						"Keep Changes?",
						"Confirm Changes",
						JOptionPane.YES_NO_CANCEL_OPTION);
	
				if (ret == JOptionPane.YES_OPTION)	{
					CursorUtil.setWaitCursor(this);
					boolean ok = false;
					try	{
						ok = loader.loadDocument(getText());	// messages should have been provided
					}
					finally	{
						CursorUtil.resetWaitCursor(this);
					}

					if (ok == false)
						return;

					changeSupport.fileWasNotReloaded();
				}
				else
				if (ret != JOptionPane.NO_OPTION)	{
					return;	// was canceled
				}
			}
			

			dispose();
			closed = true;
		}
	}
	
	
	public void windowActivated(WindowEvent e)	{}
	public void windowClosed(WindowEvent e)	{}
	public void windowDeactivated(WindowEvent e)	{}
	public void windowDeiconified(WindowEvent e)	{}
	public void windowIconified(WindowEvent e)	{}
	public void windowOpened(WindowEvent e)	{}

}