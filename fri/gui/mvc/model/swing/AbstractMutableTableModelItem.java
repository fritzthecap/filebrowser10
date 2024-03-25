package fri.gui.mvc.model.swing;

import java.util.Vector;
import fri.gui.mvc.model.*;
import fri.gui.mvc.controller.CommandArguments;

/**
	ModelItem for AbstractMutableTableModel that performs default behaviour for create, delete, copy and move.
	The ModelItem represents a temporary wrapper for a table row Vector, providing methods for a controller.
	This is NOT a view item!
	<p>
	The implementation follows the assumption that there is no other medium than the TableModel.
	If there is a database or filesystem or ... behind this item, methods must be overridden.
	
	@author  Ritzberger Fritz
*/

public abstract class AbstractMutableTableModelItem extends AbstractModelItem
{
	/** The last given position where to insert an item. */
	protected Integer pos;

	
	/** Create an item that performs copy, move, delete or insert. */
	public AbstractMutableTableModelItem(DefaultTableRow userObject)	{
		super(userObject);
	}


		
	public void setMovePending(boolean movePending)	{
		((DefaultTableRow)getUserObject()).setMovePending(movePending);
	}

	public boolean isMovePending()	{
		return ((DefaultTableRow)getUserObject()).isMovePending();
	}


	/**
		Factory method: Create a new specific table row from passed Vector.
		When null is passed, an empty row must be created.
	*/
	protected abstract DefaultTableRow createTableRow(Vector v);
	


	/** Returns the position from creation command argument. */
	protected Integer createdPositionInMedium(ModelItem createdItem)	{
		return pos;
	}

	/** Returns a new ModelItem from createInfo, under the assumption that there is no other medium than the TableModel. */
	protected ModelItem createInMedium(CommandArguments createInfo)	{
		AbstractMutableTableModel model = (AbstractMutableTableModel)createInfo.getModel();
		
		if (createInfo.getCreateData() instanceof Integer)	{
			pos = (Integer)createInfo.getCreateData();
			return model.createModelItem(createTableRow(null));
		}
		else	{
			pos = null;	// append behind
			Vector v = (Vector)createInfo.getCreateData();
			return model.createModelItem(createTableRow(v));
		}
	}

	/** Just returns true under the assumption that there is no other medium than the TableModel. */
	protected boolean deleteInMedium(CommandArguments deleteInfo)	{
		return true;
	}

	/** Returns a ModelItem clone of this item. */
	public Object clone()	{
		AbstractMutableTableModel model = (AbstractMutableTableModel)pasteInfo.getModel();
		Vector v = (Vector)getUserObject();
		return model.createModelItem(createTableRow(v));	// must clone, else this item would be found by delete on move
	}

}
