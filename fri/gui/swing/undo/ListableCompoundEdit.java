package fri.gui.swing.undo;

import javax.swing.undo.*;
import java.util.Enumeration;

/**
	Target: the compound edit must be listable
*/

public class ListableCompoundEdit extends CompoundEdit
{
	public Enumeration elements()	{
		return edits.elements();
	}
}
