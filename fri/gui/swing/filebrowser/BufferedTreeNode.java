package fri.gui.swing.filebrowser;

import javax.swing.tree.*;
import java.util.*;
import java.io.File;
import fri.util.sort.quick.*;
import fri.util.FileUtil;
import fri.util.os.OS;
import fri.gui.CursorUtil;

/**
	Verantwortlichkeiten:
	Buffern von TreeNode's, um einen schnellen Aufbau der Tree-View zu ermoeglichen.
	Child-Knoten sollen gefiltert und sortiert werden: Das Sortierkriterium und den
	Filter verwalten.
	Child-Knoten sollen nicht freigegeben werden, da sie Sub-Baeume halten,
	die nur neu eingelesen werden sollen, wenn das Rescan-Flag gesetzt wird.
	Dieses wird mittels Selektion des Knotens gesetzt und durch Deselektion
	wieder aufgehoben. Wird bei selektiertem Knoten ein Refresh ausgeloest, muss
	der ganze Tree unterhalb des Knotens neu gelesen werden und das Flag wieder
	zurueckgesetzt werden.
	
	Im Gegensatz zu NetNode, dessen Netz nur einmal pro Prozess aufgebaut wird,
	wird das Netz dieser Knotenstruktur pro Browser-Instanz neu aufgebaut.
	Jede Browser-Instanz kann also seine eigenen Filter und Sortier-Kriterien
	benutzen.
*/

public class BufferedTreeNode extends DefaultMutableTreeNode implements
	NetNodeListener,
	Listable
{
	/** Um Selektion aus persistenten Daten zu lesen, kann dieser Zustand bei Start gesetzt werden. */
	public static boolean initing;

	private int sortFlag = NetNode.SORT_DEFAULT;
	private boolean markedForReScan;	// nicht mit jeder neuen Instanz rescan ausloesen
	private boolean markedForFilter = true;	// zu Beginn muessen Knoten aufgebaut werden
	private boolean selected;
	private final TreePanel treepanel;
	private boolean listDrives;

	// helper variables
	private boolean childrenRefreshCalled;
	private String searchedName;
	private BufferedTreeNode foundNode;	
	private boolean fillInProgress;



	public BufferedTreeNode(Object userobject, TreePanel treepanel)	{
		this.treepanel = treepanel;
		init((NetNode)userobject);
	}
	
	private void init(NetNode n)	{
		setUserObject(n);
		setAllowsChildren(!n.isLeaf());
		n.addNetNodeListener(this);
		setSortFlag(treepanel.getSortFlag());

		// when initing, selection did not take place, ask treepanel
		if (BufferedTreeNode.initing)	{
			if (treepanel.checkForSelection((NetNode)getUserObject()))
				setSelected(true);
		}
	}	


	/** Returns the name of the file */
	public String getLabel()	{
		return ((NetNode)getUserObject()).getLabel();
	}
	
	
	/** Release listener connection to this node. */
	public void removeNodeListener()	{
		NetNode n = (NetNode)getUserObject();
		n.removeNetNodeListener(this);
	}


	/** Set listing slow drives */
	public void setListDrives()	{
		listDrives = true;
	}
	
	
	public void setMarkedForFilter(boolean value)	{
		//System.err.println("setMarkedForFilter "+value+" upon "+this);
		markedForFilter = value;
	}
	
	public boolean isMarkedForFilter()	{
		return markedForFilter;
	}

	/** rescan children from medium when searching children or filling node next time */	
	public void setMarkedForReScan(boolean value)	{
		//System.err.println("setMarkedForReScan "+value+" upon "+this);
		markedForReScan = value;
	}
	
	public boolean isMarkedForReScan()	{
		return markedForReScan;
	}


	/** set the sort criterium */
	public void setSelected(boolean selected)	{
		this.selected = selected;
		//System.err.println("setSelected "+selected+" in "+((NetNode)getUserObject()).getFullText());
	}
	
	
	public boolean isSelected()	{
		return selected;
	}


	/** set the sort criterium */
	public void setSortFlag(int sortFlag)	{
		this.sortFlag = sortFlag;
	}
	
	public int getSortFlag()	{
		return sortFlag;
	}


	/*
		Bei Bedarf Fuellen des Knotens mit Kind-Knoten und Rueckgabe eines per Name
		identifizierten Kind-Knotens. Es wird ausschliesslich im uebergebenen
		Knoten und nicht rekursiv in die Tiefe gesucht.
		@param targetName Name des Knotens, der zurueckgeliefert werden soll.
	*/
	public BufferedTreeNode searchNode(String targetName)	{
		return searchNode(targetName, true);
	}
	
	private BufferedTreeNode searchNode(String targetName, boolean showCursor)	{
		searchedName = targetName;
		foundNode = null;
		if (fillNode(showCursor) == false)	{	// already filled, search in buffered data
			foundNode = searchInChildren(targetName);
		}
		searchedName = null;
		return foundNode;
	}


	/*
		Rueckgabe eines per Name identifizierten Kind-Knotens.
		Es wird ausschliesslich im uebergebenen Knoten und nicht rekursiv in die Tiefe gesucht.
		Es wird nicht versucht den Knoten zu fuellen.
		@param targetName Name des Knotens, der zurueckgeliefert werden soll.
	*/
	private BufferedTreeNode searchInChildren(String targetName)	{
		return (BufferedTreeNode)NodeLocate.search(this, targetName);
	}



	/** @return the node used by the view with a path according to passed NetNode */
	public BufferedTreeNode localizeNode(NetNode n)	{
		//System.err.println("localizeNode "+n.getFullText()+" from >"+((NetNode)getUserObject()).getFullText()+"<");
		String [] path = n.getPathComponents();
		return localizeNode(path);
	}


	/**
		Localize a node by array of labels (String names) starting from this node.
		The node will be filled if necessary.
		If the passed array is a absolute path, "this" must be root.
		An absolute path starts with the first level under root.
	*/
	public BufferedTreeNode localizeNode(String [] oarr)	{
		BufferedTreeNode node = this, prev;
		
		for (int i = 0; i < oarr.length; i++)	{
			prev = node;	// save
				
			if ((node = node.searchNode(oarr[i], false)) == null)	{
				// try again as folder could be out of date as NetWatcher goes only through visible nodes
				//node.setMarkedForReScan(true);	// this would close all child folders
				((NetNode)prev.getUserObject()).list(true);
				
				if ((node = prev.searchNode(oarr[i], false)) == null)	{
					//Thread.dumpStack();
					System.err.println("WARNING: could not localize "+oarr[i]+" in "+prev);//+": "+prev.children);
					return null;
				}
			}
		}
		//Thread.dumpStack();
		//System.err.println("BufferedTreeNode.localizeNode returns "+node);
		return node;
	}


	/** Method to get a view node by a File */
	public static BufferedTreeNode fileToBufferedTreeNode(File f, BufferedTreeNode root)	{
		if (f == null)
			return null;
 		String [] path = FileUtil.getPathComponents(f, OS.isWindows);
 		return root.localizeNode(path);
	}




	/**
		Fuellen eines Knotens mit child-Knoten, falls er ein Verzeichnis ist.
	*/
	public boolean fillNode()	{
		return fillNode(true);
	}
	
	private boolean fillNode(boolean showCursor)	{
		//System.err.println("fillNode "+this+" children "+children);
		if (isMarkedForReScan() == false &&
				isMarkedForFilter() == false &&
				listDrives == false)
		{
			//System.err.println("... node \""+this+"\" not marked");
			return false;	// signalize: have not built new child-array, look at TreeNode children!
		}
	
		NetNode fn = (NetNode)getUserObject();
		if (fn.isLeaf() == true)	{
			return true;	// there are no children
		}
		
		if (showCursor)
			CursorUtil.setWaitCursor(treepanel);

		try	{
			// init
			if (isRoot() && listDrives)
				setMarkedForReScan(true);
			
			childrenRefreshCalled = false;
			fillInProgress = true;
			Vector v = fn.list(isMarkedForReScan(), listDrives);
			// triggers childrenRefreshed() if rescan is true
			
			if (childrenRefreshCalled == false && isMarkedForFilter())	{
				// kein rescan, sodass childrenRefreshed nicht von NetNode aufgerufen wurde
				childrenRefreshed(v);
			}
			
			// reset
			setMarkedForReScan(false);
			setMarkedForFilter(false);
			fillInProgress = false;
			listDrives = false;
		}
		finally	{
			if (showCursor)
				CursorUtil.resetWaitCursor(treepanel);
		}
		
		return childrenRefreshCalled;	// signalize: have searched all children
	}



	// interface NetNodeListener
	
	public void childrenRefreshed(Vector list)	{
		childrenRefreshCalled = true;
		//Thread.dumpStack();
		//System.err.println("childrenRefreshed instance at "+(new TreePath(this.getPath()))+" list = "+list);

		// check if file was converted to directory
		if (getAllowsChildren() == false && ((NetNode)getUserObject()).isLeaf() == false)	{
			setAllowsChildren(true);
		}
		
		// update list of children in this node
		
		boolean underSelection = fillInProgress && underSelection();
		String filter = underSelection ? treepanel.getFilter() : null;
		boolean include = underSelection ? treepanel.getInclude() : true;

		//System.err.println("filter with "+filter);
		list = NodeFilter.filter(
				filter,
				list,
				include,
				treepanel.getShowFiles(),
				treepanel.getShowHidden());
								
			
		// remove children not contained in new list			
		int anz = getChildCount();
		for (int i = anz - 1; i >= 0; i--)	{
			// Liste von hinten durchgehen, da herausgeloescht wird
			BufferedTreeNode d = (BufferedTreeNode)getChildAt(i);
			boolean found = false;
			NetNode fn1 = (NetNode)d.getUserObject();

			for (int j = 0; list != null && j < list.size(); j++)	{
				NetNode fn2 = (NetNode)list.elementAt(j);
				if (fn2.getLabel().equals(fn1.getLabel()))
					found = true;
			}

			if (found == false)	{	// nodeDeleted()
				d.removeNodeListener();
				treepanel.getModel().removeNodeFromParent(d);
				//System.err.println("    removing child node no more contained "+d);
			}
		}

		if (list != null && list.size() > 0)	{
			NetNode fn = (NetNode)getUserObject();

			// sort nodes
			boolean resort = false;	// try to recycle nodes, they may be expanded

			if (children == null || children.size() <= 0 || underSelection)	{
				setSortFlag(treepanel.getSortFlag());
				//System.err.println("resorting ...");
				resort = true;
			}
			
			// Die Liste der NetNodes muss auf jeden Fall sortiert werden.
			//System.err.println("resorting with "+sortFlag+" in "+fn.getFullText());
			fn.setSortFlag(getSortFlag());
			QSort sorter = new QSort(fn);
			list = sorter.sort(list);
			
			//for (int i = 0; i < list.size(); i++)
			//	System.err.println(((NetNode)list.elementAt(i)).getModified());
			// FRi 2007-09-18: for sorting by time the result is not correct! Check FileNode.compare() and QSort!
			
			for (int i = 0; i < list.size(); i++)	{
				NetNode fn1 = (NetNode)list.elementAt(i);
				BufferedTreeNode d;

				// create new child if not found
				if ((d = searchInChildren(fn1.getLabel())) == null)	{
					d = new BufferedTreeNode(fn1, treepanel);
					//System.err.println("    allocating new child node "+d);
					int max = treepanel.getModel().getChildCount(this);
					treepanel.getModel().insertNodeInto(d, this, i > max ? max : i);
				}
				else	{
					//System.err.println("    recycle existing child node "+d);
					if (resort)	{
						treepanel.getModel().removeNodeFromParent(d);
						int max = treepanel.getModel().getChildCount(this);
						treepanel.getModel().insertNodeInto(d, this, i > max ? max : i);
					}

					if (isMarkedForReScan())	{
						d.removeNodeListener();
						d.init(fn1);
					}
				}
				
				// optional search for child node
				if (searchedName != null && foundNode == null && fn1.getLabel().equals(searchedName))	{
					foundNode = d;
					//System.err.println("  found node "+foundNode);
				}
			}	// end for
		}	// end if list != null
	}



	private boolean underSelection()	{
		return true;
		/*
		// @return if this or any parent is selected
		BufferedTreeNode d = this;
		do	{
			if (d.isSelected())
				return true;
		}
		while ((d = (BufferedTreeNode)d.getParent()) != null);
		return false;
		*/
	}
	

	
	public void nodeRenamed()	{
		//System.err.println("nodeRenamed instance "+treepanel.getInstance()+" node "+this);
		treepanel.getTree().treeDidChange();
	}

	public void nodeDeleted()	{
		//System.err.println("nodeDeleted instance "+treepanel.getInstance()+" node "+this);
		treepanel.getModel().removeNodeFromParent(this);
		removeNodeListener();
	}
	
	public void nodeInserted(Object child)	{
		//System.err.println("nodeInserted instance "+treepanel.getInstance()+" child "+child+" in "+((NetNode)this.getUserObject()).getFullText());
		BufferedTreeNode b = searchInChildren(((NetNode)child).getLabel());	// overwrite?
		if (b == null)	{	// is not overwrite
			BufferedTreeNode b1 = new BufferedTreeNode(child, treepanel);
			treepanel.getModel().insertNodeInto(b1, this, getChildCount());
		}
		else
			System.err.println("WARNUNG: nodeInserted, not overwritten existing node");
	}
	
	public void movePending()	{
		treepanel.getTree().treeDidChange();
	}


	// interface Listable
	
	public Vector list()	{
		return children;
	}
	

//	public BufferedTreeNode [] listTreeNodes()	{
//		fillNode();
//		if (children == null || children.size() <= 0)
//			return null;
//			
//		BufferedTreeNode [] list = new BufferedTreeNode[children.size()];
//		children.copyInto(list);
//			
//		return list;
//	}
}