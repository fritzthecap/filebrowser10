package fri.gui.mvc.model.swing;

import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import fri.gui.mvc.util.swing.EventUtil;
import fri.gui.mvc.controller.CommandArguments;
import fri.gui.mvc.model.*;

/**
	Abstract implementation of a DefaultTableModel with MVC framework capability.
	The <i>doInsert</i> and <i>doDelete</i> methods are safe against being called
	from a background thread by using <i>EventUtil.invokeSynchronous</i>.
*/

public abstract class AbstractMutableTableModel extends DefaultTableModel implements
	MutableModel
{
	public AbstractMutableTableModel(Vector data, Vector columnModel)	{
		super(data, columnModel);
	}
	
	
	/** Subclasses must implement the allocation of a ModelItem from a table row Vector. */
	public abstract ModelItem createModelItem(Vector row);

	
	/** Returns the context of the passed model item, which is a position that is found by seeking the item. */
	public CommandArguments getModelItemContext(ModelItem item)	{
		int i = getRowPosition((Vector)item.getUserObject());
		Integer pos = i < 0 ? null : new Integer(i);
		return new CommandArguments.Context(this, pos);
	}

	/** Inserts the passed item at position passed by command arguments, if no position is present, appends it. */
	public ModelItem doInsert(final ModelItem item, final CommandArguments arg)	{
		Runnable r = new Runnable()	{
			public void run()	{
				Vector newChild = (Vector) item.getUserObject();
				Integer index = arg.getPosition();
				insertRow(index != null ? index.intValue() : getRowCount() /*append behind*/, newChild);
			}
		};
		EventUtil.invokeSynchronous(r);
		return item;
	}

	/** Deletes this ModelItem from model by position, which is found by seeking it. */
	public boolean doDelete(final ModelItem item)	{
		Runnable r = new Runnable()	{
			public void run()	{
				int i = getRowPosition((Vector)item.getUserObject());
				//System.err.println("AbstractMutableTableModel, remove position is "+i);
				if (i >= 0)	// avoid double delete by DefaultDeleteCommand and by MesageCountListener
					removeRow(i);
			}
		};
		//Thread.dumpStack();
		EventUtil.invokeSynchronous(r);
		return true;
	}


	/** Returns the row at passed index or null if index is not in space. */
	public Vector getRow(int index)	{
		return index >= 0 && index < getDataVector().size() ? (Vector)getDataVector().get(index) : null;
	}


	/** Finds a ModelItem's userObject Vector by comparing with "==". */
	protected int getRowPosition(Vector row)	{
		return getRowPosition(row, getDataVector());
	}
	
	/** Finds a ModelItem's userObject in a given data Vector by comparing with "==". */
	protected int getRowPosition(Vector row, Vector dataVector)	{
		for (int i = 0; i < dataVector.size(); i++)	{
			if (dataVector.get(i) == row)	{
				//System.err.println("found item at position "+i+", item is: "+row);
				return i;
			}
		}
		return -1;
	}

	/** Finds a given data Vector by comparing with equals. */
	public int locate(Vector row)	{
		for (int i = 0; i < getDataVector().size(); i++)	{
			if (getDataVector().get(i).equals(row))	{
				return i;
			}
		}
		return -1;
	}

}
