package fri.gui.swing.mdi;

/**
	Internal interface to melt DesktopPane and TabbedPane.
	Every pane that can manage several internal panels
	(JDesktopPane, JTabbedPane) implements this interface role
	to be part of SwitchableMdiPane.
	<p>
	Additionally following overrideable methods are implemented
	in the two concrete implementations of MdiPane (that even
	can be catched in MdiObjectResponsibilities):
	<ul>
		<li>mdiFrameClosing()</li>
		<li>mdiFrameContainerActivated()</li>
	</ul>
*/

public interface MdiPane
{
	/** Returns all internal containers (either all JInternalFrames or all JPanels subclasses). */
	public MdiFrame [] getMdiFrames();
	
	/** Creates an empty internal container (JInternalFrame or JPanel subclass) and adds it. */
	public MdiFrame createMdiFrame(Object toRender);

	/** Removes an internal container. This is not a close but the remove of an invisible container. */
	public void removeMdiFrame(MdiFrame containerToRemove);
	
	/** Returns the index of the currently focused internal container. */
	public int getSelectedIndex();
	
	/** Sets the currently focused internal container by index. */
	public void setSelectedIndex(int index);

	/** Actively closes all internal containers. Return true if all were closed successfully, else false. */
	public boolean close();



	/**
		For internal use. Implementers are notified if a internal container gets closed.
	*/
	public interface ContainerRemoveListener
	{
		/** Notification about an internal container closed. */
		public void containerRemoved(MdiFrame removedContainer);
	}
	
	/** For internal use. Add a listener that gets notified when a panel gets removed in a MdiPane. */
	public void setContainerRemoveListener(ContainerRemoveListener lsnr);
	
	/** For internal use. Remove a listener that gets notified when a panel gets removed in a MdiPane. */
	public void unsetContainerRemoveListener(ContainerRemoveListener lsnr);
}