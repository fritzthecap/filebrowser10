package fri.gui.swing.mailbrowser;

import java.io.File;
import java.util.StringTokenizer;
import javax.swing.tree.MutableTreeNode;
import fri.util.mail.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.swing.AbstractMutableTreeModel;
import fri.gui.mvc.model.swing.TreeNodeUtil;

public class FolderTreeModel extends AbstractMutableTreeModel
{
	public FolderTreeModel()	{
		super(new FolderTreeNode());
	}


	/** Allocation of a ModelItem from a MutableTreeNode. */
	public ModelItem createModelItem(MutableTreeNode node)	{
		return new FolderTreeModelItem(node);
	}

	/**
		Adds the passed store to end of store list on first tree level.
	*/
	public FolderTreeNode addStore(ObservableReceiveMail store)	{
		FolderTreeNode root = (FolderTreeNode)getRoot();
		FolderTreeNode newStore = new FolderTreeNode(store);
		insertNodeInto(newStore, root, root.getChildCount());
		return newStore;
	}


	/**
		Returns the local inbox tree node (first store, node named "inbox").
	*/
	public FolderTreeNode getLocalInbox()	{
		FolderTreeNode localStore = getLocalStore();
		return find2ndLevelNode(localStore, LocalStore.INBOX);
	}

	/**
		Returns trash folder for the store the passed tree node lies under.
	*/
	public FolderTreeNode getTrashNode(FolderTreeNode node)	{
		return find2ndLevelNode(node, LocalStore.TRASH);
	}

	/**
		Returns draft folder for the store the passed tree node lies under.
	*/
	public FolderTreeNode getDraftNode(FolderTreeNode node)	{
		return find2ndLevelNode(node, LocalStore.DRAFTS);
	}
	
	/**
		Returns outbox folder for the store the passed tree node lies under.
	*/
	public FolderTreeNode getOutboxNode(FolderTreeNode node)	{
		return find2ndLevelNode(node, LocalStore.OUTBOX);
	}
	
	/**
		Returns sent-mail folder for the store the passed tree node lies under.
	*/
	public FolderTreeNode getSentNode(FolderTreeNode node)	{
		return find2ndLevelNode(node, LocalStore.SENT);
	}
	
	private FolderTreeNode find2ndLevelNode(FolderTreeNode node, String folderName)	{
		FolderTreeNode store = node;
		while (store.isStore() == false)
			store = (FolderTreeNode)store.getParent();

		if (store.getReceiveMail().isLocalStore() == false)	// a remote folder is selected
			store = getLocalStore();
			
		int cnt = store.getChildCount();
		for (int i = 0; i < cnt; i++)	{
			FolderTreeNode tn = (FolderTreeNode)store.getChildAt(i);
			if (tn.toString().equalsIgnoreCase(folderName))
				return tn;
		}
		return null;
	}


	/**
		Returns the folder that lies under passed path in local store.
		Path is a string where parts are separated by File.separator or "/" (programmatical separator).
	*/
	public FolderTreeNode getFolderByPath(String path)	{
		StringTokenizer stok = new StringTokenizer(path, "/"+File.separator);
		String [] pathArray = new String[stok.countTokens()];
		for (int i = 0; stok.hasMoreTokens(); i++)
			pathArray[i] = stok.nextToken();
			
		return (FolderTreeNode)TreeNodeUtil.locate(getLocalStore(), pathArray);
	}
	


	// Returns the local inbox tree node.
	private FolderTreeNode getLocalStore()	{
		FolderTreeNode root = (FolderTreeNode)getRoot();
		for (int i = 0; i < root.getChildCount(); i++)	{
			FolderTreeNode store = (FolderTreeNode)root.getChildAt(i);
			ObservableReceiveMail rm = store.getReceiveMail();
			if (rm.isLocalStore())
				return store;	// return first found
		}
		return null;
	}


	/** Returns a default name for a new folder that can not conflict with another folder within parent. */
	public String createNewDefaultName(FolderTreeNode parent)
		throws Exception
	{
		// search a default name for the new folder
		String name = "newFolder";
		boolean found = false;
		int cnt = 1;
		
		do	{
			String s = name+cnt;
			cnt++;
			found = (getChildByName(parent, s) != null);
			if (!found)
				name = s;
		}
		while (found);
		
		return name;
	}


	public boolean mustBeReallyDeleted(FolderTreeNode tn)	{
		ObservableReceiveMail rm = tn.getReceiveMail();
		FolderTreeNode trashNode = getTrashNode(tn);	// find trash folder of this local store
		return rm.isLocalStore() == false || trashNode == null || trashNode.isNodeDescendant(tn);
	}

}
