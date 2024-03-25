package fri.gui.swing.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import fri.gui.CursorUtil;
import fri.gui.LocationUtil;
import fri.gui.swing.*;
import fri.gui.awt.geometrymanager.GeometryManager;

/**
	A nonmodal dialog that is closes when its frame gets closed.
	Provides a KeyListener for ESCAPE key, to be installed on
	derivation's Components.

	@author Fritz Ritzberger
*/

public abstract class FrameServiceDialog implements
	ComponentListener
{
	protected Frame parentFrame;
	protected Window parentWindow;
	protected JDialog window;
	private WindowAdapter parentCloseListener;
	private boolean automaticLocation = true;
	private KeyListener escapeListener;


	/** Provides a nonmodal dialog. */
	public FrameServiceDialog(Frame parentFrame) {
		this.parentFrame = parentFrame;

		initParent(parentFrame);

		// install dialog close listener
		ensureWindow().addWindowListener(new WindowAdapter()	{
			public void windowOpened(WindowEvent e) {
				dialogWindowOpened();
			}
			public void windowClosing(WindowEvent e) {
				dialogWindowClosing();
				close();
			}
		});
	}

	/** Pack the dialog according to its last geometry. */
	protected void pack()	{
		new GeometryManager(ensureWindow()).pack();
	}
	
	/** Sets another parent Component. The frame window close listener gets reinstalled. */
	protected void initParent(Component someComponent)	{
		// listen for parent close
		if (parentWindow != null && parentCloseListener != null)	{
			parentWindow.removeWindowListener(parentCloseListener);
		}
		
		parentWindow = ComponentUtil.getWindowForComponent(someComponent);

		if (parentCloseListener == null)	{
			parentCloseListener = new WindowAdapter()	{
				public void windowClosing(WindowEvent e)	{
					parentWindowClosing();
					close();
					dispose();
				}
			};
		}
		
		parentWindow.addWindowListener(parentCloseListener);
	}

	/** Called when parent window closes. This implementation does nothing, to be overridden. */
	protected void parentWindowClosing()	{
	}
	
	/** Called when dialog closes. This implementation does nothing, to be overridden. */
	protected void dialogWindowClosing()	{
	}
	
	/** Called when dialog opens. This implementation does nothing, to be overridden. */
	protected void dialogWindowOpened()	{
	}

	/** Returns a KeyListener that closes the dialog on ESCAPE key. */
	protected KeyListener getCloseOnEscapeKeyListener()	{
		if (escapeListener == null)	{	// listen for escape
			escapeListener = new KeyAdapter()	{
				public void keyPressed(KeyEvent e)	{
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
						close();
					}
				}
			};
			ensureWindow().addKeyListener(escapeListener);
		}
		return escapeListener;
	}
	

	private JDialog ensureWindow()	{
		if (window == null)	{
			if (parentWindow instanceof Dialog)
				window = new WindowImpl((Dialog)parentWindow, false);
			else
				window = new WindowImpl((Frame)parentWindow, false);

			window.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		}
		return window;
	}

	/** Disposes the dialog. */
	public void dispose()	{
		ensureWindow().dispose();	// windowClosing() will be called
	}

	/** Sets the dialog visible or invisible. */
	public void setVisible(boolean visible)	{
		ensureWindow().setVisible(visible);
	}

	/** Returns true if dialog is currently showing. */
	public boolean isVisible()	{
		return getDialog() != null ? getDialog().isVisible() : false;
	}

	/** Returns the dialog's content pane for adding Components. */
	protected Container getContentPane()	{
		return ensureWindow().getContentPane();
	}

	/** Returns the dialog. */
	protected JDialog getDialog()	{
		return window;
	}
	
	protected void setTitle(String title)	{
		ensureWindow().setTitle(title);
	}

	protected void centerOverParent()	{
		LocationUtil.centerOverParent(ensureWindow(), parentWindow);
	}

	protected void setFreeViewLocation()	{
		if (automaticLocation == false)
			return;
			
		ensureWindow().removeComponentListener(this);
		ensureWindow().addComponentListener(this);
		
		LocationUtil.setFreeViewLocation(ensureWindow(), parentWindow);
	}

	/** Set wait or default cursor to dialog window and parent window of textarea. */
	protected void setWaitCursor(boolean wait)	{
		if (wait)	{
			CursorUtil.setWaitCursor(window);
			CursorUtil.setWaitCursor(parentWindow);
		}
		else	{
			CursorUtil.resetWaitCursor(parentWindow);
			CursorUtil.resetWaitCursor(window);
		}
	}

	/** Closes the dialog by <i>setVisible(false)</i>. */
	protected void close()	{
		automaticLocation = true;
		ensureWindow().removeComponentListener(this);
		parentWindow.removeWindowListener(parentCloseListener);
		setVisible(false);
	}


	/** Implements ComponentListener: do no more automatic location when user moved dialog manually. */
	public void componentResized(ComponentEvent e)	{
		automaticLocation = false;
	}
	/** Implements ComponentListener: do no more automatic location when user moved dialog manually. */
	public void componentMoved(ComponentEvent e)	{
		automaticLocation = false;
	}
	public void componentShown(ComponentEvent e)	{}
	public void componentHidden(ComponentEvent e)	{}



	/**
	 * Marker class for the service dialog.
	 */
	public static class WindowImpl extends JDialog
	{
		WindowImpl(Dialog parent, boolean modal)	{
			super(parent, modal);
		}
		WindowImpl(Frame parent, boolean modal)	{
			super(parent, modal);
		}
	}

}