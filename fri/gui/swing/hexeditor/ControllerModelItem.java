package fri.gui.swing.hexeditor;

import java.io.Serializable;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.controller.CommandArguments;

/**
	The wrapper for 1-n bytes involved in an action.
	Contains HexEditor "business logic": move, copy, insert, delete.
*/

public class ControllerModelItem implements
	ModelItem,	// standard Clipboard functionality
	Serializable
{
	private int end;
	private int start;
	private byte [] bytesToInsert;	// invalid on create
	private boolean movePending;	// valid only on move
	private HexTable table;	// valid only on move


	/**
		Create a worker item with passed start and end offset for a CreateCommand.
	*/
	public ControllerModelItem(int start, int end)	{
		this.start = start;
		this.end = end;
	}
	
	/**
		Create a worker item with passed start and end offset for a RemoveCommand or CopyCommand.
	*/
	public ControllerModelItem(int start, int end, byte [] bytesToSave)	{
		this(start, end);
		this.bytesToInsert = bytesToSave;
		
		if (end - start != bytesToSave.length)
			throw new IllegalArgumentException("Byte array length does not match start and end offsets: length="+bytesToSave.length+", start="+start+", end="+end);
	}

	/**
		Create a worker item with passed start and end offset for a MoveCommand.
		A MoveCommand will need to set the visible movePending property,
		which is done through the passed HexTable.
	*/
	public ControllerModelItem(int start, int end, byte [] bytesToSave, HexTable table)	{
		this(start, end, bytesToSave);
		this.table = table;
	}


	/** Returns the start offset. */
	public int getStart()	{
		return start;
	}

	/** Returns the (exclusive) end offset. */
	public int getEnd()	{
		return end;
	}

	/** Returns the (exclusive) end offset. */
	public byte [] getBytesToInsert()	{
		return bytesToInsert;
	}



	// start interface ModelItem

	/**
		Create or insert a item within or into this one. This comes from
		CreateCommand.doit() or RemoveCommand.undo().
	*/
	public ModelItem doInsert(CommandArguments createInfo)	{
		System.err.println("ControllerModelItem doInsert with createInfo "+createInfo);
		Object createData = createInfo.getCreateData();

		if (createData != null)	{	// is a newly created item
			if (createData instanceof Byte)	{
				bytesToInsert = new byte [] { ((Byte)createData).byteValue() };
			}
			else
			if (createData instanceof byte[])	{
				bytesToInsert = (byte[])createData;
			}
			else
				throw new IllegalArgumentException("Unknown type of insert data: "+createData);
		}
		// else: bytesToInsert must have been set by constructor!

		MutableModel m = createInfo.getModel();
		m.doInsert(this, null);
	
		return this;
	}

	/**
		Delete this item.
	*/
	public boolean doDelete(CommandArguments deleteInfo)	{
		System.err.println("ControllerModelItem doDelete "+this+" by "+deleteInfo);
		return deleteInfo.getModel().doDelete(this);
	}

	/**
		Move this item into the target item at given paste position and parent.
	*/
	public ModelItem doMove(ModelItem target, CommandArguments pasteInfo)	{
		System.err.println("ControllerModelItem doMove "+this+"; to target "+target+"; with pasteInfo="+pasteInfo);
		ControllerModelItem cmi = (ControllerModelItem)target;	// holds insert position
		
		int newStart = cmi.getStart();	// calculate new start offset

		// CAUTION when same model: when remove offset is before insert offset, the insert offset must be changed
		if (table.getModel() == pasteInfo.getModel())	{	// throw exception if table is null!
			int insertOffset = cmi.getStart();
			int removeOffset = getStart();
			
			boolean isUndo = (pasteInfo.getPosition() != null);
			// only the source model item context (undo) has position
			// this position is the one AFTER delete (does not know about offset shifts when moving to front)
			
			if (removeOffset < insertOffset && isUndo == false)	{
				newStart -= bytesToInsert.length;	// adjust new start offset
				System.err.println("shifting ControllerModelItem to new start offset: "+newStart);
			}
		}
		
		// remove this item from old model
		doDelete(new ByteCommandArguments(pasteInfo.getSendingModel()));

		// adjust start and end offsets to new position - keep order!
		this.start = newStart;
		this.end = this.start + bytesToInsert.length;

		// insert into new model
		doInsert(pasteInfo);

		return this;
	}

	/**
		Copy this item into the target item at given paste position and parent.
	*/
	public ModelItem doCopy(ModelItem target, CommandArguments pasteInfo)	{
		System.err.println("ControllerModelItem doCopy "+this+"; to target "+target+"; with pasteInfo="+pasteInfo);
		ControllerModelItem cmi = (ControllerModelItem)target;	// holds insert position

		// adjust start and end offsets to new position - keep order!
		this.start = cmi.getStart();
		this.end = this.start + bytesToInsert.length;
		
		doInsert(pasteInfo);

		return this;
	}



	/** Mark a item as "cutten". */
	public void setMovePending(boolean movePending)	{
		this.movePending = movePending;
		table.setMovePending(getStart(), getEnd(), movePending);
	}

	/** Returns the "cutten" property of this item. */
	public boolean isMovePending()	{
		return movePending;
	}



	/** Returns the last error. Not used. always returns NO_ERROR. */
	public String getError()	{
		return NO_ERROR;
	}

	/** Implements ModelItem: not used, always returns null. */
	public Object getUserObject()	{
		return null;
	}

	/** Implements ModelItem: not used, always returns null. */
	public Object clone()	{
		return null;
	}

	// end interface ModelItem

	public String toString()	{
		return super.toString()+" start="+start+", end="+end+" bytes.length="+(bytesToInsert != null ? bytesToInsert.length : 0)+", movePending="+movePending+", table="+(table == null ? "null" : "not null");
	}

}