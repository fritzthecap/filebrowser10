package fri.gui.swing.filebrowser;

import javax.swing.tree.*;

/**
	Alle Knoten des uebergebenen Modelles kopieren und eine Referenz
	auf das alte User-Object einhaengen (wird nicht kopiert).
	Dies wird verwendet, um die Erzeugung neuer Explorer Fenster,
	die dieselbe Expansion haben sollen, zu beschleunigen.
*/

public abstract class TreeModelClone
{
	public static synchronized DefaultTreeModel cloneTreeModel(
		DefaultTreeModel model,
		TreePanel tp)
	{
		BufferedTreeNode oldroot = (BufferedTreeNode)model.getRoot();
		NetNode userobject = (NetNode)oldroot.getUserObject();
		BufferedTreeNode newroot = new BufferedTreeNode(userobject, tp);
		//newroot.setAllowsChildren(true);
		copyNodes(oldroot, newroot, tp);
		DefaultTreeModel newmodel = new DefaultTreeModel(newroot);
		return newmodel;
	}
	
	private static void copyNodes(
		BufferedTreeNode oldnode,
		BufferedTreeNode newnode,
		TreePanel tp)
	{
		int anz = oldnode.getChildCount();
		for (int i = 0; i < anz; i++)	{
			BufferedTreeNode dold = (BufferedTreeNode)oldnode.getChildAt(i);
			NetNode userobject = (NetNode)dold.getUserObject();
			BufferedTreeNode dnew = new BufferedTreeNode(userobject, tp);
			dnew.setAllowsChildren(userobject.isLeaf() == false);
			newnode.add(dnew);
			//System.err.println("cloneNode "+dnew+", allows children "+dnew.getAllowsChildren());
			copyNodes(dold, dnew, tp);
		}
	}
}