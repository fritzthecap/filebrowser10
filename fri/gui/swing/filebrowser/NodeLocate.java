package fri.gui.swing.filebrowser;

import java.util.Vector;
import fri.util.os.OS;

/**
	Suchen von Pfaden in Baeumen, Suchen von Elementen in Child-Listen.
*/

public abstract class NodeLocate
{
	/** Rekursive Suche nach dem angegeben Pfad. */
	public static Listable locate(Listable root, String [] path)	{
		Listable node = root;

		for (int i = 0; node != null && i < path.length; i++)	{
			node = NodeLocate.search(node, path[i]);
		}
		
		return node;
	}

	
	/** Suche in den Child-Knoten nach dem angegeben Namen. */
	public static Listable search(Listable node, String name)	{
		Vector list = node.list();
		
		if (OS.supportsCaseSensitiveFiles() == false)
			name = name.toLowerCase();
		
		for (int i = 0; list != null && i < list.size(); i++)	{
			Listable l = (Listable)list.elementAt(i);
			String s = l.getLabel();
			
			if (OS.supportsCaseSensitiveFiles() == false)
				s = s.toLowerCase();
			
			if (s.equals(name))	{
				return l;
			}
		}
		
		//Thread.dumpStack();
		//System.err.println("NodeLocate, NOT found: "+name+" in "+node+", list = "+list);
		return null;
	}


	public static NetNode fileToNetNode(NetNode root, String [] path)	{
		NetNode node = root, prev;

		for (int i = 0; node != null && i < path.length; i++)	{
			prev = node;	// save
			
			node = (NetNode)NodeLocate.search(node, path[i]);
			
			if (node == null)	{	// out of date
				prev.list(true);	// refresh
				node = (NetNode)NodeLocate.search(prev, path[i]);
			}
		}
		
		return node;
	}
	
}