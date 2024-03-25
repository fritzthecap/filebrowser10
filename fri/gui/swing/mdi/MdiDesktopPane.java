package fri.gui.swing.mdi;

import java.beans.PropertyVetoException;
import java.beans.PropertyChangeEvent;
import javax.swing.JInternalFrame;
import fri.gui.swing.desktoppane.FrameManagingMdiPane;
import fri.gui.swing.desktoppane.ManagedInternalFrame;


/**
	Implements MdiPane to provide swapability with a TabbedPane.
*/

public class MdiDesktopPane extends FrameManagingMdiPane implements
	MdiPane
{
	private ContainerRemoveListener lsnr;


	/** Returns all internal containers (either JInternalFrames or JPanels subclasses). */
	public MdiFrame [] getMdiFrames()	{
		JInternalFrame [] frames = getAllFramesSorted();
		MdiFrame[] array = new MdiFrame[frames.length];
		for (int i = 0; i < frames.length; i++)	{
			array[i] = (MdiFrame)frames[i];
		}
		return array;
	}


	/** Returns the index of the currently focused internal container. */
	public int getSelectedIndex()	{
		return frames.indexOf(getSelectedFrame());
	}
	
	/** Sets the currently focused internal container by index. */
	public void setSelectedIndex(int index)	{
		switchToFrame(index);
	}
	
	/** Creates an empty internal container (JInternalFrame or JPanel subclass). Calls createMdiFrameImpl(). */
	public MdiFrame createMdiFrame(Object toRender)	{
		MdiFrame frame = createMdiFrameImpl(toRender);
		addFrame((ManagedInternalFrame)frame);
		return frame;
	}

	/** This method just contains the construction of the internal container. To be overridden. */
	protected MdiFrame createMdiFrameImpl(Object toRender)	{
		return new MdiInternalFrame(toRender);
	}

	/**
		Removes an internal container. Callback for MdiPane.ContainerRemovedListener to remove the peer container.
		This is not a window close callback but the direct remove of an invisible container.
		Do not call this directly, use close() instead.
	*/
	public void removeMdiFrame(MdiFrame containerToRemove)	{
		removeFrame((JInternalFrame)containerToRemove);	// do management when closing
		remove((JInternalFrame)containerToRemove);	// as it was not closed by "X", it must be removed now
	}

	/** Overridden to notify ContainerRemoveListener. */
	protected void removeFrame(JInternalFrame frame)	{
		super.removeFrame(frame);

		if (lsnr != null)
			lsnr.containerRemoved((MdiFrame)frame);
	}


	/**
		Overridden to call mdiFrameClosing() instead (which is the MdiPane adapter).
		@exception PropertyVetoException to stop any window close.
	*/
	protected final void frameClosing(ManagedInternalFrame frame, PropertyChangeEvent e)
		throws PropertyVetoException
	{
		mdiFrameClosing((MdiFrame)frame, e);
	}

	/**
		Override this to save data or stop closing any frame. Default this calls mdiFrame.close().
		@param container a JInternalFrame
		@exception PropertyVetoException to stop any window close.
	*/
	protected void mdiFrameClosing(MdiFrame container, PropertyChangeEvent e)
		throws PropertyVetoException
	{
		container.closing(e);
	}


	/**
		Overridden to call mdiFrameClosing() instead (which is the MdiPane adapter).
		@exception PropertyVetoException to stop any window close.
	*/
	protected final void frameActivated(ManagedInternalFrame frame)	{
		mdiFrameActivated((MdiFrame)frame);
	}

	/**
		Override this to set actions enabled. Default this calls mdiFrame.activated().
		@param container a MdiInternalTabPanel
	*/
	protected void mdiFrameActivated(MdiFrame container)	{
		container.activated();
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