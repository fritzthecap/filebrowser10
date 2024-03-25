package fri.gui.swing.mdi;

import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;
import java.awt.Component;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.TabbedPaneUI;
import fri.gui.awt.resourcemanager.ResourceIgnoringComponent;
import fri.gui.swing.tabbedpane.CloseableTabbedPane;

/**
	Implements MdiPane to provide swapability with a DesktopPane.
*/

public class MdiTabbedPane extends CloseableTabbedPane implements
	MdiPane,
	ChangeListener,
	ResourceIgnoringComponent	// do not let customize tab titles
{
	private ContainerRemoveListener lsnr;
	private boolean closeAllowed;


	/** Returns all internal containers (either JInternalFrames or JPanels subclasses). */
	public MdiFrame [] getMdiFrames()	{
		MdiFrame[] panels = new MdiFrame[getTabCount()];
		for (int i = 0; i < getTabCount(); i++)	{
			panels[i] = (MdiFrame)getComponentAt(i);
		}
		return panels;
	}
	
	/** Creates an empty internal container (JInternalFrame or JPanel subclass). Calls createMdiFrameImpl(). */
	public MdiFrame createMdiFrame(Object toRender)	{
		ensureListener();
		MdiFrame panel = createMdiFrameImpl(toRender);
		addTab("", (Component)panel);
		int newIndex = getTabCount() - 1;
		setSelectedIndex(newIndex);
		setToolTipTextAt(newIndex, ((JComponent)panel).getToolTipText());
		return panel;
	}
	
	/** This method just contains the construction of the internal container. To be overridden. */
	protected MdiFrame createMdiFrameImpl(Object toRender)	{
		return new MdiInternalTabPanel(toRender);
	}
	

	private void ensureListener()	{
		removeChangeListener(this);
		addChangeListener(this);
	}



	/** Overridden to display the correct title. */
	public String getTitleAt(int index)	{
		MdiFrame ic = (MdiFrame)getComponentAt(index);
		return ic.getTitle();
	}

	/* Overridden to display the correct tooltip. */
	public String getToolTipTextAt(int index)	{
		MdiFrame ic = (MdiFrame)getComponentAt(index);
		return ((JComponent)ic).getToolTipText();
	}

	/* Overridden to delegate to <i>getToolTipTextAt(index)</i>. */
	public String getToolTipText(MouseEvent event) {
		if (ui != null) {
			int index = ((TabbedPaneUI)ui).tabForCoordinate(this, event.getX(), event.getY());
			if (index >= 0)
				return getToolTipTextAt(index);
		}
		return super.getToolTipText(event);
	}



	/**
		Removes an internal container. Callback for MdiPane.ContainerRemovedListener to remove the peer container.
		This is not a window close callback but the direct remove of an invisible container.
		Do not call this directly, use close() instead.
	*/
	public void removeMdiFrame(MdiFrame containerToRemove)	{
		int i = indexOfComponent((Component)containerToRemove);
		removeTabAt(i);
	}


	/**
		This is the callback from window close button on CloseableTabbedPane.
		Overridden to close the contained editor, which calls the MdiPane adapter <i>mdiFrameClosing()</i>.
	*/
	protected final void closeTab()	{
		int index = getCloseCandidateIndex();
		if (index >= 0)	{
			MdiFrame ic = (MdiFrame)getMdiFrames()[index];
			close(ic);
		}
	}
	
	// Called when all are closing, or by close button
	private void close(MdiFrame toClose)	{
		try	{
			mdiFrameClosing(toClose, new PropertyChangeEvent(toClose, JInternalFrame.IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE));
			// exception can be thrown on cancel or save error, when not, we can remove the tab
			
			removeMdiFrame(toClose);
			
			if (lsnr != null)
				lsnr.containerRemoved(toClose);
		}
		catch (PropertyVetoException e)	{
			closeAllowed = false;
			System.err.println("Tab close was vetoed: "+e);
		}
	}

	
	/**
		Override this to save data or stop closing any frame. Default this calls mdiFrame.close().
		@param container a MdiInternalTabPanel
		@exception PropertyVetoException to stop any window close.
	*/
	protected void mdiFrameClosing(MdiFrame container, PropertyChangeEvent e)
		throws PropertyVetoException
	{
		container.closing(e);
	}

	
	/** Implements ChangeListener to catch tab change. Calls mdiFrameActivated(). */
	public void stateChanged(ChangeEvent e)	{
		int i = getSelectedIndex();
		MdiFrame [] containers = getMdiFrames();
		System.err.println("MdiTabbedPane stateChanged: "+i+", containers count "+containers.length);
		
		if (i < containers.length && i >= 0)	{
			MdiFrame selected = containers[i];
			mdiFrameActivated(selected);
		}
	}
	
	/**
		Override this to set actions enabled. Default this calls mdiFrame.activated().
		@param container a MdiInternalTabPanel
	*/
	protected void mdiFrameActivated(MdiFrame container)	{
		container.activated();
	}



	/** Close all internal containers. Return true if all were closed successfully, else false. */
	public boolean close()	{
		MdiFrame [] containers = getMdiFrames();
		
		closeAllowed = true;
		
		for (int i = containers.length - 1; closeAllowed && i >= 0; i--)	{
			setSelectedIndex(i);
			validate();	// else the button will have no parent found by JOptionPane
			close(containers[i]);
		}
		
		return closeAllowed;
	}



	/** For internal use. Add a listener that gets notified when a panel gets removed in a MdiPane. */
	public void setContainerRemoveListener(ContainerRemoveListener lsnr)	{
		this.lsnr = lsnr;
	}

	/** For internal use. Remove a listener that gets notified when a panel gets removed in a MdiPane. */
	public void unsetContainerRemoveListener(ContainerRemoveListener lsnr)	{
		this.lsnr = null;
	}

}