package fri.gui.swing.mailbrowser;

import java.util.*;
import java.awt.*;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.tree.*;
import javax.mail.URLName;
import fri.util.error.Err;
import fri.util.mail.*;
import fri.util.managers.InstanceManager;
import fri.util.props.ClassProperties;
import fri.gui.mvc.view.swing.*;
import fri.gui.swing.tree.CustomJTree;

/**
	Panel that holds the mail folder tree.
*/

public class FolderTree extends JPanel
{
	private JTree tree;
	private SelectionDnd selection;
	private static InstanceManager modelFactory = new InstanceManager();
	
	
	public FolderTree()	{
		super(new BorderLayout());
		
		boolean alreadyLoaded = modelFactory.existsInstance("MailFolderTreeModel");
		TreeModel m = (TreeModel)modelFactory.getInstance("MailFolderTreeModel", new FolderTreeModel());
		
		tree = new CustomJTree(m)	{
			/** Decide if a tree node is editable */
			public boolean isPathEditable(TreePath tp)	{
				if (isEditable() == false || tp.getPathCount() <= 1)
					return false;
					
				FolderTreeNode tn = (FolderTreeNode)tp.getLastPathComponent();
				if (tn.isStore())	// a store node
					return true;
					
				ObservableReceiveMail rm = tn.getReceiveMail();
				return rm.canRename();
			}
		};
		tree.setEditable(true);
		tree.setRowHeight(16);
		tree.setCellRenderer(new FolderTreeCellRenderer());
		
		add(new JScrollPane(tree), BorderLayout.CENTER);
		
		if (alreadyLoaded == false)	{
			try	{
				// add the local store
				FolderTreeNode newStore = getModel().addStore(LocalStore.getRoot());
				
				// expand local store and its standard folders
				FolderTreeNode root = (FolderTreeNode)getModel().getRoot();
				tree.expandPath(new TreePath(new Object [] { root, newStore }));
				int cnt = newStore.getChildCount();
				
				for (int i = 0; i < cnt; i++)	{
					FolderTreeNode fld = (FolderTreeNode)newStore.getChildAt(i);
					ObservableReceiveMail rm = fld.getReceiveMail();
					
					if (rm.canRename() == false && fld.toString().equals(LocalStore.TRASH) == false)	// standard folder, but not trash
						tree.expandPath(new TreePath(new Object [] { root, newStore, fld }));
				}
		
				// add user defined stores
				Properties props = ClassProperties.getProperties(FolderTree.class);
	
				for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
					String name = (String)e.nextElement();
					if (name.startsWith("connection"))	{	// other properties (split divider) are stored in the same file
						String url = props.getProperty(name);
						getModel().addStore(createStore(url));
					}
				}
			}
			catch (Exception e)	{
				Err.error(e);
			}
		}
	}



	public ObservableReceiveMail createStore(String url)
		throws Exception
	{
		return createStore(new ReceiveProperties(url), url);
	}
	
	public ObservableReceiveMail createStore(ReceiveProperties mailProps, String url)
		throws Exception
	{
		ObservableReceiveMail store;

		if (mailProps.isLocal())
			store = new ObservableReceiveMail(url, true);
		else
			store = new ObservableReceiveMail(mailProps, new AuthenticatorDialog(this, mailProps.getPassword()));

		return store;
	}
	


	public JTree getSensorComponent()	{
		return tree;
	}
	
	public SelectionDnd getSelection()	{
		if (selection == null)
			selection = new TreeSelectionDnd(tree);
		return selection;
	}

	public FolderTreeModel getModel()	{
		return (FolderTreeModel)tree.getModel();
	}


	public void close()	{
		// close and save all open stores
		
		// leave current folder
		List sel = (List)getSelection().getSelectedObject();
		if (sel != null && sel.size() == 1)	{
			FolderTreeNode tn = (FolderTreeNode)sel.get(0);
			if (tn.getLevel() > 1)	{
				try	{
					ObservableReceiveMail rm = tn.getReceiveMail();
					rm.leaveCurrentFolder();
				}
				catch (Exception e)	{
					e.printStackTrace();
				}
			}
		}
		
		modelFactory.freeInstance("MailFolderTreeModel");
		boolean stillAlive = modelFactory.existsInstance("MailFolderTreeModel");

		if (stillAlive == false)	{
			ClassProperties.clear(FolderTree.class);
			FolderTreeNode root = (FolderTreeNode)getModel().getRoot();
			boolean localStoreFound = false;

			for (int i = 0; i < root.getChildCount(); i++)	{
				FolderTreeNode store = (FolderTreeNode)root.getChildAt(i);
				ObservableReceiveMail rm = (ObservableReceiveMail)store.getUserObject();
				// do not call getReceiveMail() as this would perfomr cd() on unconnected folders, which means a connect!

				if (rm.isConnected())	{
					try	{
						rm.close();
					}
					catch (Exception e)	{
						// Err.error(e);
					}
				}
				
				if (localStoreFound == false && rm.isLocalStore())	{	// first found store is local store
					localStoreFound = true;
					LocalStore.setUrl(rm.getURLName().toString());
				}
				else	{
					URLName urlName = rm.getURLName();
					ClassProperties.put(FolderTree.class, "connection"+i, urlName.toString());
				}
			}
			
			ClassProperties.store(FolderTree.class);
			ConnectionSingletons.store();
			LocalStore.close();
		}
	}



	private class FolderTreeCellRenderer extends MovePendingTreeCellRenderer
	{
		public Component getTreeCellRendererComponent(
			JTree tree,
			Object value,
			boolean selected,
			boolean expanded,
			boolean leaf,
			int row,
			boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			
			FolderTreeNode tn = (FolderTreeNode)value;
			if (tn.getLevel() == 2)	{
				ObservableReceiveMail rm = tn.getReceiveMail();
				if (rm != null && rm.isLocalStore() && rm.canRename() == false)	{
					setText(Language.get(getText()));	// localized inbox, outbox, ...
				}
			}

			return this;
		}
	}

}