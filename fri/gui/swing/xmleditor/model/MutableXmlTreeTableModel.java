package fri.gui.swing.xmleditor.model;

import java.util.*;
import java.beans.*;
import javax.swing.tree.*;
import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.controller.CommandArguments;
import org.w3c.dom.Node;

/**
	XML editor specific implementation of editable tree table model.
	It provides a model cache (and singleton factory) for MVC models.
	These instances must be set free after close. An instance counter
	frees the instance when no references are left
*/

public class MutableXmlTreeTableModel extends XmlTreeTableModel implements
	MutableModel
{
	/** Action flag for an insert, needed by PropertyChangeListeners. */
	public static final String INSERTED = "INSERTED";
	/** Action flag for a remove, needed by PropertyChangeListeners. */
	public static final String REMOVED = "REMOVED";
	/** Action flag for an update, needed by PropertyChangeListeners. */
	public static final String UPDATED = "UPDATED";

	private static Hashtable cache = new Hashtable();	// singleton model cache
	private static Hashtable clients = new Hashtable();	// singleton clients counter

	/**
		Factory to create or get a model for a XML rendering treetable.
		@exception read or XML parsing errors
	*/
	public static MutableXmlTreeTableModel getInstance(String uri, Configuration config)
		throws Exception
	{
		MutableXmlTreeTableModel model = (MutableXmlTreeTableModel)cache.get(uri);

		if (model == null)	{
			MutableXmlNode root = new MutableXmlNode(uri, null, config);
			model = new MutableXmlTreeTableModel(root);
			uri = root.getURI();

			if (config.showProlog == false)	{	// no artificial prolog attributes to edit encoding and version
				model.checkForReducedColumns();	// when no attributes, remove that table column
			}

			cache.put(uri, model);
			clients.put(uri, Integer.valueOf(1));
		}
		else	{
			//Thread.dumpStack();
			Integer cnt = (Integer)clients.get(uri);
			cnt = Integer.valueOf(cnt.intValue() + 1);
			clients.put(uri, cnt);
		}

		return model;
	}

	/**
		When a editor window gets closed, its model must be released.
	*/
	public static void freeInstance(String uri)	{
		Integer cnt = (Integer)clients.get(uri);
		if (cnt == null)
			return;

		if (cnt.intValue() == 1)	{
			cache.remove(uri);
			clients.remove(uri);
		}
		else	{
			cnt = Integer.valueOf(cnt.intValue() - 1);
			clients.put(uri, cnt);
		}
	}

	/**
		When a editor window saves a model as another URI, the cache must be updated.
	*/
	public static void renameInstance(String oldUri, String newUri)	{
		Integer cnt = (Integer)clients.get(oldUri);
		MutableXmlTreeTableModel model = (MutableXmlTreeTableModel)cache.get(oldUri);
		clients.remove(oldUri);
		cache.remove(oldUri);
		clients.put(newUri, cnt);
		cache.put(newUri, model);
	}


	
	
	private boolean changed = false;
	private PropertyChangeSupport changeNotifier;

	/** Create a mutable model with a root */
	private MutableXmlTreeTableModel(MutableXmlNode root) {
		super(root);
		changeNotifier = new PropertyChangeSupport(this);
	}

	/**
		Implements Model:
		Returns the environment the item is living in, i.e.
		its Model (this) and optionally its parent item and/or its position.
		@param item the ModelItem for which data are requested.
		@return data about Model, parent item and position.
	*/
	public CommandArguments getModelItemContext(ModelItem item)	{
		MutableXmlNode node = (MutableXmlNode)item.getUserObject();
		MutableXmlNode pnt = (MutableXmlNode)node.getParent();
		Integer pos = null;
		if (pnt != null)	{
			pos = Integer.valueOf(pnt.getIndex(node));
		}
		return new ModelParentAndPosition(this, new ControllerModelItem(pnt), pos);
	}

	/**
		Implements MutabeModel: Insert a new item.
		@param position information about where to insert the new item.
			This could contain a parent item or a integer position or both.
		@return inserted item, or null if action failed.
	*/
	public ModelItem doInsert(ModelItem item, CommandArguments destination)	{
		System.err.println("MutableModel "+this+" doInsert "+item+" into "+destination);
		MutableTreeNode node = (MutableTreeNode)item.getUserObject();
		ModelItem pntItem = destination.getParent();
		MutableTreeNode pnt = (MutableTreeNode)pntItem.getUserObject();
		int index = destination.getPosition().intValue();

		insertNodeInto(node, pnt, index);

		return item;
	}

	/**
		Implements MutabeModel: Delete an item.
		@param item the item to delete from this model.
		@return true if delete succeeded.
	*/
	public boolean doDelete(ModelItem item)	{
		System.err.println("MutableModel "+this+" doDelete "+item);
		MutableTreeNode node = (MutableTreeNode)item.getUserObject();

		if (node.getParent() != null)	// when new node was created, parent is null
			removeNodeFromParent(node);

		return true;
	}

	/**
		Save the model to some medium (that has been estimated by constructor).
	*/
	public void save()
		throws Exception
	{
		MutableXmlNode root = (MutableXmlNode)getRoot();
		root.save();
		setChanged(false);	// not reached if Exception is thrown
	}


	/**
		Returns the ProperyChangeSupport notifier object for changes in TreeTableModel.
		It provides notification about INSERTED, UPDATED and REMOVED events in the model.
	*/
	public PropertyChangeSupport getChangeNotifier()	{
		return changeNotifier;
	}


	// TableModel interface overriding

	/**
		Does NOT set a new value for the particular column, as this is done
		in the controller. Just fires a property change event with the node
		as source and the old and new value as UpdateObjectAndColumn objects.
		The controller must listen to that event and do the real work.
	*/
	public void setValueAt(Object aValue, Object node, int column) {
		Object old = getValueAt(node, column);
		//System.err.println("set value in "+node+", >"+aValue+"<, old >"+old+"<");

		boolean bothNull =	// when old value is null or empty and new value is empty
			aValue.toString().length() <= 0 &&
			(old == null || old.toString().length() <= 0);

		if (bothNull == false && aValue.equals(old) == false)	{
			//MutableXmlNode n = (MutableXmlNode)node;
			//n.setColumnObject(column, aValue);	// done in controller!

			PropertyChangeEvent e = new PropertyChangeEvent(
					node,	// event source
					UPDATED,	// type
					new UpdateObjectAndColumn(old, column),	// old value
					new UpdateObjectAndColumn(aValue, column));	// new value
			getChangeNotifier().firePropertyChange(e);

			setChanged(true);
		}
	}

	/** Returns true for attributes column and text column. */
	public boolean isCellEditable(Object node, int column) {
		MutableXmlNode n = (MutableXmlNode)node;

		if (column == LONGTEXT_COLUMN && n.isEditable())
			return true;

		if (column == ATTRIBUTES_COLUMN)
			return n.getAttributesCount() > 0;

		if (column == TAG_COLUMN)	{
			if (n.getTagReplacingAttributeName() != null)
				return true;
		}

		return false;
	}



	// TreeModel interface overriding

	/** Overridden to set changed flag. */
	public void insertNodeInto(MutableTreeNode nn, MutableTreeNode parent, int idx)	{
		super.insertNodeInto(nn, parent, idx);

		getChangeNotifier().firePropertyChange(INSERTED, -1, idx);
		setChanged(true);
	}

	/** Overridden to set changed flag. */
	public void removeNodeFromParent(MutableTreeNode node) {
		super.removeNodeFromParent(node);

		getChangeNotifier().firePropertyChange(REMOVED, node, null);
		setChanged(true);
	}



	// change service methods

	/** Get changed-flag for editor contents. */
	public boolean isChanged()	{
		return changed;
	}

	/** Set changed-flag for editor contents. */
	public void setChanged(boolean changed)	{
		this.changed = changed;
	}


	/**
		Find a local node that equals the passed Node.
		This is needed for drag and drop when source and target Component
		are the same.
		@return null if not found, else the found MutableXmlNode.
	*/
	public MutableXmlNode findNode(Node node)	{
		MutableXmlNode root = (MutableXmlNode)getRoot();
		return findNode(node, root, new NodeComparator());
	}

	private MutableXmlNode findNode(Node node, MutableXmlNode treeNode, NodeComparator comparator)	{
		if (comparator.compareNodes(treeNode.getW3CNode(), node) == 0)	{
			System.err.println("localized node "+node+" to local "+treeNode);
			return treeNode;
		}

		for (int i = 0; i < treeNode.getChildCount(); i++)	{
			MutableXmlNode xn = (MutableXmlNode)treeNode.getChildAt(i);
			MutableXmlNode found = findNode(node, xn, comparator);
			if (found != null)	{
				return found;
			}
		}

		return null;
	}

	/**
		Creates a new MutableXmlNode from passed W3C Node.
		This is needed for drag and drop when the node does not exist in this document.
	*/
	public MutableXmlNode createMutableXmlNode(Node node)	{
		MutableXmlNode root = (MutableXmlNode)getRoot();
		Node newW3CNode = root.createTypedW3CNode(node);
		MutableXmlNode newNode = (MutableXmlNode)root.createXmlNode(newW3CNode);
		System.err.println("created new node for "+node+" -> "+newNode);
		return newNode;
	}

}