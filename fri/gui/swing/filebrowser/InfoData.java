package fri.gui.swing.filebrowser;

import java.util.Vector;
import fri.util.sort.quick.*;


/**
	Node list for info-frame. Constructor provides only a
	gettable list of nodes, does not insert nodes!
*/

public class InfoData extends FileTableData
{
	private NetNode parentNode = null;
	private Vector data;
	private int whichPath;
	
	
	/** construct the list for the table model */
	public InfoData(
		NetNode [] nodes,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden,
		int whichPath)
	{
		this.whichPath = whichPath;
		
		// remove unused columns starting from back
		if (whichPath == InfoTableModel.NO_PATH)
			cols.remove(super.getPathColumn());
		
		Vector v = null;
		if (nodes.length == 1)	{
			parentNode = nodes[0];	// this table has a defined parent node
			v = parentNode.list();	// and represents the list the parent node			
			v = filter(v, filter, include, showfiles, showhidden);
		}
		else	{
			v = new Vector();
			for (int i = 0; i < nodes.length; i++)	{
				v.add(nodes[i]);
			}
		}

		QSort sorter = new QSort(nodes[0]);
		data = sorter.sort(v);

		files = new Vector(data.size());
	}
	
	
	public int getPathColumn()	{
		return whichPath == InfoTableModel.NO_PATH ? -1 : 1;
	}
	public int getNameColumn()	{
		return whichPath == InfoTableModel.NO_PATH ? 1 : 2;
	}
	public int getExtensionColumn()	{
		return whichPath == InfoTableModel.NO_PATH ? 2 : 3;
	}
	public int getSizeColumn()	{
		return whichPath == InfoTableModel.NO_PATH ? 3 : 4;
	}
	public int getTimeColumn()	{
		return whichPath == InfoTableModel.NO_PATH ? 4 : 5;
	}
	public int getAccessColumn()	{
		return whichPath == InfoTableModel.NO_PATH ? 5 : 6;
	}


	
	/** @return the (filtered) list of nodes */
	public Vector getData()	{
		return data;
	}
	
	
	/** @return the parent node listed or null if none present */
	public NetNode getParentNode()	{
		return parentNode;
	}
	
}