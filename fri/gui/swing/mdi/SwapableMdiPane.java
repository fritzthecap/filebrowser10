package fri.gui.swing.mdi;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import fri.gui.CursorUtil;
import fri.gui.swing.util.MenuBarUtil;

/**
	MdiPane can switch between DesktopPane and TabbedPane.
	It exposes an Action that performs the GUI change.
	It extends JPanel and adds the appropriate container to CENTER.
	Call getComponent(0) to retrieve the current MdiPane that
	lies over this Component.
*/

public class SwapableMdiPane extends JPanel implements
	MdiPane.ContainerRemoveListener
{
	private Action swapAction;
	private MdiPane visiblePane, invisiblePane;
	private JMenu tabbedWindowMenu, desktopWindowMenu, currentWindowMenu;
	

	/** Allocate a SwitchableMdiPane and set its initial layout to DesktopPane. */
	public SwapableMdiPane()	{
		this(false);
	}
	
	/** Allocate a SwitchableMdiPane and set its initial layout according to passed argument. */
	public SwapableMdiPane(boolean initWithTabbedPane)	{
		super(new BorderLayout());
		
		if (initWithTabbedPane)	{
			visiblePane = createTabbedPaneImpl();
		}
		else	{
			visiblePane = createDesktopPaneImpl();
		}
		
		add((Component)visiblePane);
		
		visiblePane.setContainerRemoveListener(this);
	}


	/** Factory method to create the DesktopPane. Override to create specific pane. */
	protected MdiPane createDesktopPaneImpl()	{
		return new MdiDesktopPane();
	}
	
	/** Factory method to create the TabbedPane. Override to create specific pane. */
	protected MdiPane createTabbedPaneImpl()	{
		return new MdiTabbedPane();
	}


	/** Delegates the creation to the currently visible MdiPane. */
	public MdiFrame createMdiFrame(Object toRender)	{
		if (isOpen(toRender))
			return null;
		
		return visiblePane.createMdiFrame(toRender);
	}

	
	/** Loops through all containers and brings to front the one that renders passed data identifier. */
	public boolean isOpen(Object renderedObject)	{
			if (renderedObject instanceof MdiObjectResponsibilities)	// if wrapper object, unwrap
				renderedObject = ((MdiObjectResponsibilities)renderedObject).getRenderedObject();

		if (renderedObject != null)	{	// cannot seek undefined object
			MdiFrame [] ics = getMdiFrames();
			
			for (int i = 0; ics != null && i < ics.length; i++)	{
				Object o = ics[i].getRenderedObject();	// implementation returns File, not manager
				
				if (o != null && o.equals(renderedObject))	{
					visiblePane.setSelectedIndex(i);
					return true;
				}
			}
		}
		return false;
	}

	/** Close the passed MDI frame. */
	public void removeMdiFrame(MdiFrame containerToRemove)        {
		visiblePane.removeMdiFrame(containerToRemove);
	}
	

	/** Close all internal containers. Return true if all were closed successfully, else false. */
	public boolean close()	{
		return visiblePane.close();
	}


	/** Returns an array of the currently visible containers. This is only for temporary use! */
	public MdiFrame [] getMdiFrames()	{
		return visiblePane.getMdiFrames();
	}

	
	/** Returns the currently selected container (foreground). */
	public MdiFrame getSelectedMdiFrame()	{
		int i = visiblePane.getSelectedIndex();
		MdiFrame [] ics = getMdiFrames();
		if (i >= 0 && i < ics.length)
			return ics[i];
		return null;
	}

	/** Sets the passed container to foreground. */
	public void setSelectedMdiFrame(MdiFrame container)	{
		MdiFrame [] ics = getMdiFrames();
		for (int i = 0; ics != null && i < ics.length; i++)	{
			if (container == ics[i])	{
				visiblePane.setSelectedIndex(i);
				return;
			}
		}
	}
	

	/** Sets the passed container to foreground. */
	public int getSelectedIndex()	{
		return visiblePane.getSelectedIndex();
	}

	/** Sets the passed container to foreground. */
	public void setSelectedIndex(int index)	{
		visiblePane.setSelectedIndex(index);
	}
	


	/** Performs the switch from TabbedPane to DesktopPane and vice versa. */
	public void swap()	{
		// keep order!
		
		ensureSecondPane();
		
		visiblePane.unsetContainerRemoveListener(this);

		// swap pane Components
		CursorUtil.setWaitCursor(this);
		try	{
			remove((Component)visiblePane);
			add((Component)invisiblePane);	// add the invisible pane in the case someone loops to parent frame
			validate();	// needed to set size to desktoppane, else frames will be too big
			
			synchronizePanes();	// synchronize after exchanging panes
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}

		invisiblePane.setContainerRemoveListener(this);
		
		// swap pane member variables
		MdiPane tmpInvisible = invisiblePane;
		invisiblePane = visiblePane;
		visiblePane = tmpInvisible;
		
		swapMenu();

		repaint();	// need repaint as Components were exchanged
	}

	// ensure the other pane is allocated.
	private void ensureSecondPane()	{
		if (invisiblePane == null)	{
			if (isDesktopView())	{
				invisiblePane = createTabbedPaneImpl();
			}
			else	{
				invisiblePane = createDesktopPaneImpl();
			}
		}
	}


	/** Returns true if this currently holds a MdiDesktopPane. */
	public boolean isDesktopView()	{
		return visiblePane instanceof MdiDesktopPane;
	}
		
		
	// ensure both panes have the same panels.
	private void synchronizePanes()	{
		MdiFrame [] visibleContainers = getMdiFrames();
		MdiFrame [] targetContainers = invisiblePane.getMdiFrames();

		// add all internal panels at tail that are not yet contained.
		for (int i = 0; i < visibleContainers.length; i++)	{
			MdiFrame src = visibleContainers[i];
			MdiFrame tgt;

			// bring the rendered object or its manager to the invisible pane
			if (i < targetContainers.length)	{
				tgt = targetContainers[i];
				tgt.setExistingRenderingComponent(src.removeExistingRenderingComponent());
				//System.err.println("Having existing container at "+i);
			}
			else	{
				if (visibleContainers[i].getManager() != null)	{
					Object o = visibleContainers[i].getManager();	// holds the existing rendering Component
					src.removeExistingRenderingComponent();	// remove panel from source container
					tgt = invisiblePane.createMdiFrame(o);	// manager holds existing rendering Component, will be added
					//System.err.println("Creating new container at "+i);
				}
				else	{	// the following is just for compatibility when no MdiObjectResponsibilities is used
					Object o = visibleContainers[i].getRenderedObject();
					tgt = invisiblePane.createMdiFrame(o);
					tgt.setExistingRenderingComponent(src.removeExistingRenderingComponent());
				}
			}
			
			//tgt.setTitle(src.getTitle());
		}
		
		if (visibleContainers.length > 1)	{
			invisiblePane.setSelectedIndex(visiblePane.getSelectedIndex());
		}
	}


	/**
		Internally. Implements MdiPane.ContainerRemoveListener to synchronize both panes.
		This is called after a MdiFrame has been removed.
	*/
	public final void containerRemoved(MdiFrame removedContainer)	{
		if (invisiblePane != null)	{	// if it was ever allocated
			Object renderedObject = removedContainer.getRenderedObject();
			MdiFrame [] ics = invisiblePane.getMdiFrames();
			
			for (int i = 0; i < ics.length; i++)	{
				if (ics[i].getRenderedObject() == null && renderedObject == null ||
						renderedObject != null && ics[i].getRenderedObject() != null &&
							ics[i].getRenderedObject().equals(renderedObject))
				{
					invisiblePane.removeMdiFrame(ics[i]);
					break;
				}
			}
		}
		
		if (removedContainer.getManager() != null)	{	// send a closed event
			removedContainer.getManager().closed(removedContainer);
		}
	}


	
	
	/**
		Returns the Window menu. If DesktopPane is active, its menu is returned,
		else an empty one. In both cases the swap-Action gets inserted at 0.
	*/
	public JMenu getWindowMenu()	{
		boolean isFirst = false;
		
		if (isDesktopView())	{
			MdiDesktopPane dtp = (MdiDesktopPane)visiblePane;
			JMenu dtwm = dtp.getWindowMenu();
			
			if (desktopWindowMenu == null)	{	// called for first time
				desktopWindowMenu = dtwm;
				dtwm.insert(createSwapMenuItem(), 0);	// insert the swap item into desktop window menu
				dtwm.insertSeparator(1);
				isFirst = true;
			}
			
			currentWindowMenu = desktopWindowMenu;	// set the current menu
		}
		else	{	// new pane is tabbed pane
			if (tabbedWindowMenu == null)	{
				tabbedWindowMenu = new JMenu("Window");
				tabbedWindowMenu.add(createSwapMenuItem());
				isFirst = true;
			}
			
			currentWindowMenu = tabbedWindowMenu;	// set the current menu
		}
		
		if (isFirst)	{
			fillWindowMenu(currentWindowMenu);	// give subclasses a chance for menuitems in Window menu
		}
		
		return currentWindowMenu;
	}


	/** Subclasses can override this do-nothing method to add items to Window menu. */
	protected void fillWindowMenu(JMenu windowMenu)	{
	}


	/** Creating the "Swap MDI Pane" menu item. */
	protected JMenuItem createSwapMenuItem()	{
		return new JMenuItem(getSwapAction());
	}

	
	private void swapMenu()	{
		if (currentWindowMenu != null && currentWindowMenu.getParent() != null)	{
			JMenuBar menubar = (JMenuBar)currentWindowMenu.getParent();
			
			// find position of current menu
			int pos = Math.max(0, MenuBarUtil.getMenuIndex(menubar, currentWindowMenu));
			menubar.remove(currentWindowMenu);
			MenuBarUtil.insertMenu((JMenuBar)menubar, getWindowMenu(), pos);
			
			menubar.revalidate();
			menubar.repaint();

			if (isDesktopView())
				swapAction.putValue(Action.NAME, getSwapToTabbedMenuName());
			else
				swapAction.putValue(Action.NAME, getSwapToDesktopMenuName());
		}
	}



	/** Returns the Action that performs the switch from TabbedPane to DesktopPane and vice versa. */
	public Action getSwapAction()	{
		if (swapAction == null)	{
			swapAction = new AbstractAction(getSwapMenuName())	{
				public void actionPerformed(ActionEvent e)	{
					swap();
				}
			};
			swapAction.putValue(Action.SHORT_DESCRIPTION, getSwapToolTip());
		}
		
		return swapAction;
	}

	private String getSwapMenuName()	{
		if (isDesktopView())
			return getSwapToTabbedMenuName();
		else
			return getSwapToDesktopMenuName();
	}

	protected String getSwapToDesktopMenuName()	{
		return "Set Desktop View";
	}

	protected String getSwapToTabbedMenuName()	{
		return "Set Tabbed View";
	}

	protected String getSwapToolTip()	{
		return "Switch Between Tabbed And Desktop View";
	}

}