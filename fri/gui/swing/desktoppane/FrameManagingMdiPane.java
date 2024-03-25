package fri.gui.swing.desktoppane;

import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;

/**
	The MDI view containing internal frames. It can create multiple
	internal frames showing different documents. It listenes to activation
	of these frames.
	<p>
	FrameManagingMdiPane provides standard window menu items like tile, cascade, etc.,
	and it provides an addable menu containing them ("Window").
*/

public class FrameManagingMdiPane extends LayoutedMdiPane implements
	InternalFrameListener,	// activating and closing internal frames
	VetoableChangeListener	// softly closing an internal frame
{
	private JMenu windowMenu;	// "Window" menu
	private JRadioButtonMenuItem cascade, tileVertical, tileHorizontal;
	private ButtonGroup windowGroup = new ButtonGroup();	// window list radio group
	private ActionListener windowMenuActionListener;

	private class WindowMenuActionListener implements ActionListener
	{
		/** Implements ActionListener: handle radiomenuitem that activates its window. */
		public void actionPerformed(ActionEvent e)	{
			if (e.getSource() == cascade)	{
				setNewLayout(CASCADED);
			}
			else
			if (e.getSource() == tileHorizontal)	{
				setNewLayout(TILED_HORIZONTAL);
			}
			else
			if (e.getSource() == tileVertical)	{
				setNewLayout(TILED_VERTICAL);
			}
			else
			if (e.getSource() instanceof JMenuItem)	{	// window activation
				JMenuItem mi = (JMenuItem)e.getSource();
				JInternalFrame [] frames = getAllFrames();
	
				for (int i = 0; i < frames.length; i++)	{
					ManagedInternalFrame frame = (ManagedInternalFrame)frames[i];
	
					if (mi == frame.getWindowMenuItem())	{
						switchToFrame(frame);
						return;
					}
				}
	
				throw new IllegalArgumentException("Menuitem not implemented: "+e.getActionCommand());
			}
		}
	}


	/** Create a MDI view for internal frames. */
	public FrameManagingMdiPane()	{
		putClientProperty("JDesktopPane.dragMode", "outline");
		windowMenuActionListener = new WindowMenuActionListener();
	}


	protected String getWindowMenuName()	{
		return "Window";
	}
	protected String getCascadeMenuName()	{
		return "Cascade";
	}
	protected String getTileHorizontalMenuName()	{
		return "Tile Horizontal";
	}
	protected String getTileVerticalMenuName()	{
		return "Tile Vertical";
	}
	

	/** Returns the menu containing standard window menu items and the radiomenuitem window activation choice. */
	public JMenu getWindowMenu()	{
		if (windowMenu == null)	{
			windowMenu = new JMenu(getWindowMenuName());

			ButtonGroup group = new ButtonGroup();
			cascade = new JRadioButtonMenuItem(getCascadeMenuName(), layoutmode == CASCADED);
			cascade.addActionListener(windowMenuActionListener);
			windowMenu.add(cascade);
			group.add(cascade);
			tileHorizontal = new JRadioButtonMenuItem(getTileHorizontalMenuName(), layoutmode == TILED_HORIZONTAL);
			tileHorizontal.addActionListener(windowMenuActionListener);
			windowMenu.add(tileHorizontal);
			group.add(tileHorizontal);
			tileVertical = new JRadioButtonMenuItem(getTileVerticalMenuName(), layoutmode == TILED_VERTICAL);
			tileVertical.addActionListener(windowMenuActionListener);
			windowMenu.add(tileVertical);
			group.add(tileVertical);

			windowMenu.addSeparator();	// window radio menu choice after separator
		}

		return windowMenu;
	}


	/** Adds the window menu to passed menubar. */
	public void fillMenuBar(JMenuBar menubar)	{
		menubar.add(getWindowMenu());
	}



	/** Add a newly created internal frame. */
	public void addFrame(ManagedInternalFrame frame)	{
		super.addFrame(frame);

		// add this' listeners
		frame.addInternalFrameListener(this);
		frame.addVetoableChangeListener(this);

		getWindowMenu();	// ensure menu is allocated
		
		// create a radio menu item for new frame and add it to windowMenu
		JRadioButtonMenuItem windowMenuItem = new JRadioButtonMenuItem(frame.getTitle(), false);
		windowGroup.add(windowMenuItem);
		windowMenu.add(windowMenuItem);
		frame.setWindowMenuItem(windowMenuItem);	// store the menuitem into frame

		windowMenuItem.addActionListener(windowMenuActionListener);	// listen to activation events
		windowGroup.setSelected(windowMenuItem.getModel(), true);	// foreground

		// consider current layout
		if (setNewLayout(layoutmode))	// returns true if maximized state
			try	{ frame.setMaximum(true); }	catch (Exception e)	{}

		// show window
		String vers = System.getProperty("java.version");
		if (vers != null && vers.startsWith("1.2"))	{
			System.err.println("calling show() as Java version is "+vers);
			frame.show();
		}
		else	{
			frame.setVisible(true);
		}
	}


	/**
		Service method.
		Loops through all frames and brings to front the one that renders passed data identifier.
	*/
	public boolean isOpen(Object renderedObject)	{
		JInternalFrame [] frames = getAllFrames();
		
		for (int i = 0; frames != null && i < frames.length; i++)	{
			ManagedInternalFrame f = (ManagedInternalFrame)frames[i];
			Object o = f.getRenderedObject();

			if (o != null && o.equals(renderedObject))	{
				switchToFrame(f);
				return true;
			}
		}

		return false;
	}



	// begin interface InternalFrameListener

	/** Implements InternalFrameListener: select rediomenuitem. */
	public void internalFrameActivated(InternalFrameEvent e)	{
		ManagedInternalFrame frame = (ManagedInternalFrame)e.getSource();
		frameActivated(frame);	// let subclasses activate
		windowGroup.setSelected(frame.getWindowMenuItem().getModel(), true);
	}

	/** Implements InternalFrameListener: remove rediomenuitem, activate another. */
	public void internalFrameClosed(InternalFrameEvent e)	{
		//System.err.println("internalFrameClosed ");
		ManagedInternalFrame frame = (ManagedInternalFrame)e.getSource();
		removeFrame(frame);
		frameClosed(frame);	// let subclasses close
	}

	protected void removeFrame(JInternalFrame f)	{
		ManagedInternalFrame frame = (ManagedInternalFrame)f;
		frame.removeInternalFrameListener(this);
		frame.removeVetoableChangeListener(this);
		
		super.removeFrame(frame);
		
		JMenuItem mi = frame.getWindowMenuItem();
		try	{ windowMenu.remove(mi); }	catch (Exception ex)	{}	// jdk1.2 bug
		windowGroup.remove(mi);
		mi.removeActionListener(windowMenuActionListener);
	}

	public void internalFrameClosing(InternalFrameEvent e)	{ }	
	public void internalFrameDeactivated(InternalFrameEvent e)	{ }
	public void internalFrameDeiconified(InternalFrameEvent e)	{ }
	public void internalFrameIconified(InternalFrameEvent e)	{ }
	public void internalFrameOpened(InternalFrameEvent e)	{ /* FRi 990917 event kommt nicht jdk1.3beta */ }

	// end interface InternalFrameListener


	/**
		Override this to do cleanups after close of a frame.
	*/
	protected void frameActivated(ManagedInternalFrame frame)	{
	}

	/**
		Override this to do cleanups after close of a frame.
	*/
	protected void frameClosed(ManagedInternalFrame frame)	{
	}

	

	// begin interface VetoableChangeListener

	/**
		Override this to save changes before frame closes.
		@exception PropertyVetoException to stop any window close.
	*/
	protected void frameClosing(ManagedInternalFrame frame, PropertyChangeEvent e)
		throws PropertyVetoException
	{
	}


	/** Implements VetoableChangeListener to listen for internal frame close pending. Calls frameClosing() when IS_CLOSED_PROPERTY. */
	public void vetoableChange(PropertyChangeEvent e)
		throws PropertyVetoException
	{
		if (e.getPropertyName().equals(JInternalFrame.IS_CLOSED_PROPERTY))	{
			if ((Boolean)e.getOldValue() == Boolean.FALSE &&
					(Boolean)e.getNewValue() == Boolean.TRUE)
			{
				ManagedInternalFrame frame = (ManagedInternalFrame)e.getSource();
				frameClosing(frame, e);
			}
		}
	}
	
	// end interface VetoableChangeListener


	/**
		Closes all internal frames. Method frameClosing() will be called for each internal frame.
		Return true if all internal frames have been closed successfully.
		When this returns false, the parent frame MUST NOT be closed!
	*/
	public boolean close()	{
		JInternalFrame [] frames = getAllFramesSorted();
		if (frames == null)
			return true;

		for (int i = frames.length - 1; i >= 0; i--)	{
			JInternalFrame f = (JInternalFrame)frames[i];
			
			switchToFrame(f);	// get it to foreground

			try	{
				f.setClosed(true); 
			}
			catch (Exception e)	{	// vetoableChange throws PropertyVetoException
				System.err.println("Close error: "+e.getMessage());
				return false;
			}
		}
		
		return true;
	}

}
