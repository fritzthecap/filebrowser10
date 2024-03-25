package fri.gui.swing.filebrowser;

import java.util.*;

/**
	Target: Bind together nodes in launch-groups by equality
		of indices. So files matching same pattern can be launched together.
	Behaviour: Builds a list of LaunchGroup elements.
*/

public class LaunchGroups extends Vector
{
	public LaunchGroups(
		NetNode [] n,
		Integer [][] indices,
		OpenEventTableModel model)
	{
		for (int i = 0; i < n.length; i++)	{
			Integer [] index = indices[i];
			if (index == null)
				continue;
			
			LaunchGroup l = new LaunchGroup(model, index);
			boolean found = false;
			
			// search for an equal group
			for (int j = 0; found == false && j < size(); j++)	{
				LaunchGroup lg = (LaunchGroup)elementAt(j);
				
				if (lg.equals(index))	{	// wenn diese Gruppe dieselben Indizes aufweist
					lg.addNode(n[i]);
					found = true;
					//System.err.println("associated launch group "+l+" with group "+lg);
				}
			}	// end search
			
			if (!found)	{
				//System.err.println("added launch group "+l);
				l.addNode(n[i]);
				addElement(l);
			}
		}	// end for all
	}
	
}