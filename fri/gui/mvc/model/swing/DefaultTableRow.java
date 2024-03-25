package fri.gui.mvc.model.swing;

import java.util.Vector;
import fri.gui.mvc.model.Movable;

public class DefaultTableRow extends Vector implements Movable
{
	private boolean movePending;
	
	public DefaultTableRow(int size)	{
		super(size);
	}
	
	public DefaultTableRow(Vector v)	{
		super(v.size());
		addAll(v);
	}

	public boolean isMovePending()	{
		return movePending;
	}

	public void setMovePending(boolean movePending)	{
		this.movePending = movePending;
	}

}
