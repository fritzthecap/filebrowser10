package fri.gui.swing.filebrowser;

import java.util.Vector;
import fri.util.sort.quick.Comparator;
import fri.gui.swing.undo.*;

/**
	Target:<br>
	Hide node-type and node-transactions from tree.
	The tree invokes this functions on his nodes when the user
	issues functionality the tree provides.<br>
*/

public interface NetNode extends Comparator, Listable, Filterable
{
	/** Root name for systems with no common root for all filesystems (WINDOWS). */
	public static final String ARTIFICIAL_ROOT = "Computer";
	
	/** Flag for method setSortFlag() */
	public final static int SORT_BY_NAME = 0;

	/** Flag for method setSortFlag() */
	public final static int SORT_BY_SIZE = 1;

	/** Flag for method setSortFlag() */
	public final static int SORT_BY_TIME = 2;

	/** Flag for method setSortFlag() */
	public final static int SORT_BY_EXTENSION = 3;

	/** Flag for method setSortFlag() */
	public final static int SORT_DEFAULT = SORT_BY_EXTENSION;
	
	
	
	/**
		If NetNode is used as Comparator for sorting, by what should it sort.
		The Flag must be restored after operation to prevent other instances from
		private settings!
	*/
	public void setSortFlag(int sortFlag);

	/**
		@return the static root node of filesystem
	*/
	public NetNode getRoot();

	/**
		Fill the node if no children and return children.
		@param refresh true if the list should be re-read from medium.
		@return list of children.
	*/
	public Vector list(boolean refresh);

	/**
		@param refresh true if the list should be re-read from medium.
		@param andDrives test (slow) containers for existence ("A:").
		@return list of children of type NetNode.
	*/
	public Vector list(boolean refresh, boolean andDrives);

	/**
		No children-refreshed event for NetNode-listeners is generated, this method is fast!
		@return list of children
	*/
	public Vector listSilent();
	
	/**
		No children-refreshed event for NetNode-listeners is generated, this method is fast!
		@return list of children
	*/
	public Vector listSilent(boolean refresh);

	/**
		Initialize or refresh node properties like size, time ...(not children).
	*/
	public void init();
		
	/**
		Listen for refreshes from medium.
	*/
	public void addNetNodeListener(NetNodeListener l);
	public void removeNetNodeListener(NetNodeListener l);
	public Vector getNetNodeListeners();

	/**
		Compare two NetNodes
	*/
	public boolean equals(NetNode n);
	
	/**
		Is the node manipulable, can it be renamed, deleted, moved or copied.
	*/
	public boolean isManipulable();
	public boolean canCreateChildren();
	
	/**
		Return text for tooltip on this node
	*/
	public String getToolTipText();

	/**
		Helper to create a node from an arrived drag and drop object.
		@param userobject should be the "primitive" contents of a NetNode,
			that holds a full path of its location.
	*/
	public NetNode construct(Object userobject);

	/**
		Return the transferable object of this node for sending
		it to drag and drop handler. For FileNode this is the File of the node.
	*/
	public Object getObject();
	
	/**
		@return parent of this node. If root, null is returned.
	*/
	public NetNode getParent();
	
	/**
		Return all path names of the location of this node for localization
		when dragded items are coming in from another process.
	*/
	public String [] getPathComponents();
	
	/**
		Rename or move the node, change display name.
		@param newname the new visible name of the node.
			This is the last path component.
	*/
	public NetNode rename(String newname);

	/**
		Return true if this node can not be overwritten.
	*/
	public boolean isReadOnly();
	
	/**
		Delete the node from the tree and move it to a defined
		place on physical medium, from where it can be restored.
	*/
	public boolean remove() throws Exception;
	/*
		Delete the node from the tree and move it to a defined
		place on physical medium, from where it can be restored.
	*
	public boolean removeNoCopy() throws Exception;
	*/

	/**
		Delete the node from the tree and from physical medium.
	*/
	public boolean delete() throws Exception;
	
	/**
		Remove all sub-containers and nodes in this container.
	*/
	public boolean empty() throws Exception;

	/**
		Copy the node to the given one. Do not overwrite an existing node.
		@param target container where the node should be copied to.
	*/
	public NetNode copy(NetNode target) throws Exception;
	/**
		Copy this node to a new node with a default copy name
		@return new node, result of copy action, null when copy failed.
	*/
	public NetNode saveCopy() throws Exception;

	/**
		Move the node to the given one. Try to copy and remove if move fails (across drives).
		Do not overwrite an existing node.
		@param target container where the node should be moved to.
	*/
	public NetNode move(NetNode target) throws Exception;

	/**
		Is the node marked to be moved?
		Such nodes appear disabled in the tree.
	*/
	public boolean getMovePending();

	/**
		Mark node to be moved when paste is issued.
		Such nodes appear disabled in the tree.
	*/
	public void setMovePending(boolean value);

	/**
		Is this node a link to another location?
	*/
	public boolean isLink();

	/**
		Create a new leaf-node with default name in this node,
		which must be a container.
		@param name visible representation of the node to create.
	*/
	public NetNode createNode();

	/**
		Create a new container-node with default name in this node,
		which must be a container.
	*/
	public NetNode createContainer();
	public NetNode createContainer(String name);

	/**
		The passed Actions want to be enabled or disabled when edit actions take place.
		The DoListener must be implemented as singleton and serves all nodes.
	*/
	public void createDoListener(DoAction undo, DoAction redo);
	
	/**
		Return the singleton DoListener to begin and end transactions
	*/
	public DoListener getDoListener();
	
	/**
		Return a long String describing the node (full location)
		for display purpose only.
	*/
	public String getFullText();

	/**
		Return a long String describing a linked node's reference location
		for display purpose only.
	*/
	public String getFullLinkText();

	/**
		Return a String containing detailed info about the node
	*/	
	public String getInfoText();

	/**
		Return a String describing the last happened node transaction error
	*/
	public String getError();

	/**
		@return the error code of the last happened error.
		Use NetNode-Constants to identify
	*/
	public int getErrorCode();
	
	/** Error code: the node exists in the target container. Ask to overwrite ... */
	public static final int EXISTS = 1;
	/** Error code: two nodes are the same (can not copy/move to itself) */
	public static final int IDENTICAL = 2;
	
	/**
		Method to locate user.home
	*/
	public String [] getHomePathComponents();
	/**
		Method to locate current working directory (program home)
	*/
	public String [] getWorkPathComponents();
	
	
	/**
		Apply filter to list() to execute recursively filtering commands
		As all instances have references to system of FileNode, so reset filter
		immediately after command execution!
		@param filter filter to be applied to list. If filter is "*", "" or null,
			no filtering will be applied.
		@param doInclude filter is including (true) or excluding (false)
		@param showFiles if true folders are not filtered away, else no files are
			taken to list and filter is applied to folders
	*/
	public void setListFiltered(
		String filter, boolean doInclude, boolean showFiles, boolean showHidden);
	
	/**
		Reset fileter immediately after execution of command as other instances might
		have another filter.
	*/
	public void resetListFiltered();
	//public boolean isListFiltered();


	/** to watch long distance transactions, a controller is passed */
	public void setObserver(TransactionObserver observer, String target);
	public void setObserver(TransactionObserver observer);
	public void unsetObserver();

	/**
		Size info.
		@return size of this node and all sub-containers if containing such.
	*/
	public long getRecursiveSize();
	/**
		Set size info from extern
	*/
	public void setRecursiveSize(long recursiveSize);

	/** @return true if size of all sub-containers has been calculated */
	public boolean recursiveSizeReady();
	/**
		Size info.
		@return size only of this node.
	*/
	public long getSize();
	/**
		Read/write access info.
		@return access rights as string
	*/
	public String getReadWriteAccess();
	/**
		date/time info.
		@return date/time as string
	*/
	public String getTime();
	/**
		date/time info.
		@return date/time as long (milliseconds)
	*/
	public long getModified();
	/**
		Set date/time info.
		@return null if parse- or other error
	*/
	public Long setModified(String time);

	/**
		type info, e.g. "hidden folder"
		@return type of node as string
	*/
	public String getType();
	
	/**
		Initializes time and size of the node from medium.
		@return NetNode.NEEDS_REFRESH, NetNode.NOT_LISTED, NetNode.UP_TO_DATE
	*/
	public int checkForDirty();
	
	/** Return-value for checkForDirty(). The container needs a list(refresh = true). */
	public final static int NEEDS_REFRESH = 1;
	/** Return-value for checkForDirty(). The container was never listed. */
	public final static int NOT_LISTED = 2;
	/** Return-value for checkForDirty(). The container is up to date. */
	public final static int UP_TO_DATE = 3;
	/** Return-value for checkForDirty(). The container is not expanded. */
	public final static int NOT_EXPANDED = 4;
	
	
	/**
		Set the nodes expanded state.
		checkForDirty() will return NetNode.NOT_LISTED if it is invoked
		on a node with expanded set to false.
	*/
	public void setExpanded(boolean expanded);
	/**
		@return true if this container expanded, false if collapsed.
	*/
	public boolean isExpanded();

	/**
		@return the wastebasket path
	*/
	public NetNode getWastebasket();

	/**
		@return true if this node is under the wastebasket:
			it can only be deleted, not removed.
	*/
	public boolean underWastebasket();
	
}