package fri.gui.swing.filebrowser;

import java.util.*;	

/**
	The main target of this class is to hold a list of file nodes
	of same type and the commands that were defined for this type.
	It obtains a list of integers as key for this launch group.
	These indizes reference into the passed OpenEventTableModel.
*/

public class LaunchGroup
{
	Vector nodes = new Vector();
	Integer [] index;
	OpenEventTableModel model;
	
	
	public LaunchGroup(OpenEventTableModel model, Integer [] index)	{
		this.model = model;
		this.index = index;
	}
	
	// build methods
	
	public void addNode(NetNode n)	{
		nodes.addElement(n);
	}
	
	public boolean equals(Integer [] index2)	{
		if (index.length != index2.length)
			return false;
		for (int i = 0; i < index.length; i++)
			if (index[i].intValue() != index2[i].intValue())
				return false;
		return true;
	}
	
	// service methods
	
	public boolean isUnique()	{
		return index.length == 1;
	}

	public String [] getCmdNames()	{
		String [] sarr = new String [index.length];
		for (int i = 0; i < index.length; i++)	{
			int idx = index[i].intValue();
			sarr[i] = (String)model.getValueAt(idx, OpenCommandList.SHORTNAME_COLUMN);
		}
		return sarr;
	}

	public String getCommand()	{
		return getCommand(index[0].intValue());
	}
	public String getCommand(String cmdname, String patt)	{
		return getCommand(searchRow(cmdname, patt));
	}
	private String getCommand(int idx)	{
		return (String)model.getValueAt(idx, OpenCommandList.COMMAND_COLUMN);
	}

	public String getPath()	{
		return getPath(index[0].intValue());
	}
	public String getPath(String cmdname, String patt)	{
		return getPath(searchRow(cmdname, patt));
	}
	private String getPath(int idx)	{
		return (String)model.getValueAt(idx, OpenCommandList.PATH_COLUMN);
	}
	
	public String [] getEnvironment()	{
		return getEnvironment(index[0].intValue());
	}
	public String [] getEnvironment(String cmdname, String patt)	{
		return getEnvironment(searchRow(cmdname, patt));
	}
	private String [] getEnvironment(int idx)	{
		return model.getEnvironment(idx);
	}

	public boolean getMonitor()	{
		return getMonitor(index[0].intValue());
	}
	public boolean getMonitor(String cmdname, String patt)	{
		return getMonitor(searchRow(cmdname, patt));
	}
	private boolean getMonitor(int idx)	{
		return ((Boolean)model.getValueAt(idx, OpenCommandList.MONITOR_COLUMN)).booleanValue();
	}

	public boolean getLoop()	{
		return getLoop(index[0].intValue());
	}
	public boolean getLoop(String cmdname, String patt)	{
		return getLoop(searchRow(cmdname, patt));
	}
	private boolean getLoop(int idx)	{
		return ((Boolean)model.getValueAt(idx, OpenCommandList.LOOP_COLUMN)).booleanValue();
	}

	public boolean getInvariant()	{
		return getInvariant(index[0].intValue());
	}
	public boolean getInvariant(String cmdname, String patt)	{
		return getInvariant(searchRow(cmdname, patt));
	}
	private boolean getInvariant(int idx)	{
		return ((Boolean)model.getValueAt(idx, OpenCommandList.INVARIANT_COLUMN)).booleanValue();
	}

	public NetNode [] getNodes()	{
		NetNode [] narr = new NetNode [nodes.size()];
		nodes.copyInto(narr);
		return narr;
	}

	public String getPattern(int i)	{
		int idx = index[i].intValue();
		String s = (String)model.getValueAt(idx, OpenCommandList.PATTERN_COLUMN);
		return s;
	}

	// helper methods
	
	private int searchRow(String cmdname, String patt)	{
		for (int i = 0; i < index.length; i++)	{
			int idx = index[i].intValue();
			String s = (String)model.getValueAt(idx, OpenCommandList.SHORTNAME_COLUMN);
			String p = (String)model.getValueAt(idx, OpenCommandList.PATTERN_COLUMN);
			if (s.equals(cmdname) && p.equals(patt))
				return idx;
		}
		return 0;
	}
}