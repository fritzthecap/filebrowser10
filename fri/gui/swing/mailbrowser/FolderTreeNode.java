package fri.gui.swing.mailbrowser;

import javax.swing.tree.*;
import javax.mail.*;
import fri.util.sort.quick.*;
import fri.util.error.Err;
import fri.util.mail.ObservableReceiveMail;
import fri.util.mail.LocalStore;

public class FolderTreeNode extends DefaultMutableTreeNode
{
	private boolean listed;
	private String storeNodeName;
	private boolean movePending;	
	
	/* Root constructor. */
	public FolderTreeNode()	{
		super(Language.get("Mail_Connections"));
	}

	/* First level constructor (stores). */
	public FolderTreeNode(ObservableReceiveMail store)	{
		super(store);
		storeNodeName = makeStoreNodeName(store);
	}

	/* Folder constructor. */
	public FolderTreeNode(Folder folder)	{
		super(folder);
	}


	private String makeStoreNodeName(ObservableReceiveMail store)	{
		URLName url = store.getURLName();
		if (url != null)
			return
					url.getProtocol()+":"+
					(url.getHost() != null
						? "//"+(url.getUsername() != null ? url.getUsername()+"@" : "")+url.getHost()+(url.getPort() >= 0 ? ":"+url.getPort() : "")
						: "")+
					(url.getFile() != null ? url.getFile() : "");
		else
			return store.getStore().toString();
	}


	/** Returns the ReceiveMail object that has changed its directory to this node's folder. */
	public ObservableReceiveMail getReceiveMail()	{
		if (getLevel() <= 0)
			return null;
		
		FolderTreeNode tn = this;
		String [] path = new String[getLevel() - 1];
		
		for (int i = path.length - 1; tn.getLevel() > 1; i--)	{
			path[i] = tn.toString();
			tn = (FolderTreeNode)tn.getParent();
		}
		
		ObservableReceiveMail rm = (ObservableReceiveMail)tn.getUserObject();
		try	{
			rm.cd((String)null);	// change to root
			if (path.length > 0)	{
				//System.err.println("changing to folder "+path[path.length - 1]+" ...");
				rm.cd(path);
			}
		}
		catch (AuthenticationFailedException e)	{
			if (rm.getAuthenticator() != null)	{
				AuthenticatorDialog dlg = (AuthenticatorDialog)rm.getAuthenticator();
				dlg.setPassword(null);
				if (dlg.wasCanceled() == false)
					Err.error(e);
			}
			else	{
				Err.error(e);
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		
		return rm;
	}


	public void setUserObject(Object o)	{
		super.setUserObject(o);
		
		if (isStore())	{	// getting another store, forget current children
			refresh();
			getChildCount();
			storeNodeName = makeStoreNodeName((ObservableReceiveMail)o);
			System.err.println("refreshing store to "+o);
		}
	}
	
	/** Sets the child list to null and the listed flag to false to enable new getChildCount(). */
	private void refresh()	{
		listed = false;
		children = null;
	}

	public int getChildCount()	{
		if (listed == false && (isStore() || isFolder()))	{
			listed = true;

			try	{
				Folder f;
				boolean localStore = false;
				
				if (isStore())	{	// stores
					ObservableReceiveMail rm = getReceiveMail();
						
					localStore = rm.isLocalStore();
					try	{
						f = rm.pwd();
					}
					catch (AuthenticationFailedException e)	{
						//listed = false;
						return 0;
					}
				}
				else	{	// folders
					f = (Folder)getUserObject();
				}
				
				Folder [] flds = f.list();	// POP3Folder throws: "javax.mail.MessagingException: not a directory"
				//System.err.println("FolderTreeNode.getChildCount in "+(isFolder() ? ((Folder)getUserObject()).getFullName() : ""+this)+", child folders length "+flds.length);
			
				if (getLevel() >= 1)
					new QSort(new FolderComparator(localStore && getLevel() == 1)).sort(flds);
				
				for (int i = 0; i < flds.length; i++)	{
					//System.err.println("... inserting child folder "+flds[i]+", instanceof "+flds[i].getClass()+", hashcode "+flds[i].hashCode());
					insert(new FolderTreeNode(flds[i]), i);
				}
			}
			catch (Exception e)	{
				e.printStackTrace();
				// Err.error(e);
			}
		}

		return super.getChildCount();
	}

	public boolean isLeaf()	{
		return false;
	}
	

	public boolean isStore()	{
		return getLevel() == 1;
	}
	
	public boolean isFolder()	{
		return getLevel() > 1;
	}


	public boolean existsChild(String name)	{
		for (int i = 0; i < getChildCount(); i++)
			if (getChildAt(i).toString().equals(name))
				return true;
		return false;
	}


	public String toString()	{
		if (isStore())	{
			if (storeNodeName == null)
				storeNodeName = makeStoreNodeName((ObservableReceiveMail) getUserObject());
			return storeNodeName;
		}
		else
		if (isFolder())	{
			Folder f = (Folder) getUserObject();
			return f.getName();
		}
		
		return super.toString();
	}



	public boolean getMovePending()	{
		return movePending;
	}

	public void setMovePending(boolean movePending)	{
		this.movePending = movePending;
	}




	private class FolderComparator implements Comparator
	{
		private boolean standardFolders;
		
		FolderComparator(boolean standardFolders)	{
			this.standardFolders = standardFolders;
		}
		
		public int compare(Object o1, Object o2)	{
			String f1 = ((Folder)o1).getName().toLowerCase();
			String f2 = ((Folder)o2).getName().toLowerCase();
			if (standardFolders)	{
				f1 = map(f1);
				f2 = map(f2);
			}
			return f1.compareTo(f2);
		}
		public boolean equals(Object o)	{
			return false;
		}
		private String map(String f)	{
			if (f.equals(LocalStore.INBOX))
				return "0";
			else
			if (f.equals(LocalStore.OUTBOX))
				return "1";
			else
			if (f.equals(LocalStore.SENT))
				return "2";
			else
			if (f.equals(LocalStore.DRAFTS))
				return "3";
			else
			if (f.equals(LocalStore.TRASH))
				return "4";
			else
				return f;
		}
	}

}