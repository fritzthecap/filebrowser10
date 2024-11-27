package fri.gui.mvc.controller;

import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.model.ModelItem;

/**
	Arguments for a Command.<br />
	The reason why these arguments are not passed directly to the Command is
	that DefaultClipboard needs exactly <b>one</b> argument to execute a Command
	(PasteInfo for copy/move), so this class wraps all arguments within one.
	The receiving Command then holds references to ModelItem(s) and CommandArgument(s).
	<p />
	Mainly MutableModels are contained within CommandArguments, besides there are
	create-data (name of new item), position (lists), parent (hierarchies).
	The reason why the Model is not referenced by ModelItem itself is that
	a ModelItem must be serializable (can not send the whole model),
	so the Model must be passed separately within a CommandArgument.
	<p />
	This class is a union for several Commands like New, Delete, Copy, Move.
	It supports hierarchies and lists.
	<p />
	Responsibilies:
	<ul>
		<li>Remember the <b>environment</b> of the item before the action took place:
			model, parent, position.
			</li>
		<li>Pass the <b>environment</b> where the item goes when the action takes place:
			model, parent, position.
			</li>
	</ul>
	This class provides methods that return member variables. Subclasses are required
	to implement appropriate constructors that set these variablesm, so this class
	does not have any constructor and is abstract. If an application needs more
	arguments it must define a subclass and cast its occurence in appropriate Command
	implementations.
	<p />
	The factory for objects of this class is <i>Model.getModelItemContext()</i>.

	@author  Ritzberger Fritz
*/

public class CommandArguments
{
	/** For copy/move commands, flag for semantics of "paste before" a node. */
	public static final Integer PASTE_BEFORE = Integer.valueOf(1);
	/** For copy/move commands, flag for semantics of "paste after" a node. */
	public static final Integer PASTE_AFTER = Integer.valueOf(2);

	/** For move commands, the receiver model where the item goes to.
		For copy, delete and create, the model where the item is created/deleted. */
	protected MutableModel model;
	
	/** For move commands, the model where the item comes from. */
	protected MutableModel sendingModel;

	/** For creation of a new item, the data the new item is created with. */
	protected Object createData;

	/** For hierarchies, the parent node is stored into this variable. */
	protected ModelItem parent;

	/** Position where to insert the node the action works on. */
	protected Integer position;


	/**
		Returns the model the action works on.
	*/
	public MutableModel getModel()	{
		return model;
	}

	/**
		Returns the target model for a move action. This returns <i>getModel()</i>.
	*/
	public MutableModel getReceivingModel()	{
		return model;
	}

	/**
		Returns all that is necessary to create an item, at least its name.
		As for copy, move and delete this is not necessary, this implementation
		return null.
	*/
	public Object getCreateData()	{
		return createData;
	}

	/**
		Returns null as positions are not always necessary:
		always append to end.
	*/
	public Integer getPosition()	{
		return position;
	}


	/**
		For hierarchies, returns the parent of the ModelItem.
	*/
	public ModelItem getParent()	{
		return parent;
	}

	/**
		For hierarchies, set the parent of the ModelItem.
		This is needed by AbstractModelItem, as the parent is
		passed explicitely by the clipboard (might not be contained in PasteArguments).
	*/
	public void setParent(ModelItem parent)	{
		this.parent = parent;
	}


	/**
		For move commands, returns the model where the item comes from.
		This model lets retrieve the environment of the source item.
	*/
	public MutableModel getSendingModel()	{
		return sendingModel;
	}

	/**
		For move commands, lets set the model where the item comes from.
		(The sending model for the move-undo-action cannot be set by the
		factory method <i>Model.getModelItemContext()</i>!)
	*/
	public void setSendingModel(MutableModel sendingModel)	{
		this.sendingModel = sendingModel;
	}


	public String toString()	{
		return super.toString()+": parent "+parent+", model "+model+", sending model "+sendingModel+", createData "+createData+", position "+position;
	}




	/** Semantic marker CommandArgument for deletion of nodes, containing the model. */
	public static class Delete extends CommandArguments
	{
		/** Constructor for delete on move by copy and delete. */
		public Delete(MutableModel model)	{
			this.model = model;
		}
	}


	/** Semantic marker CommandArgument for returning context of nodes, containing the model, position and optionally some tree parent. */
	public static class Context extends CommandArguments
	{
		/** Plain list or table constructor. */
		public Context(MutableModel model, Integer position)	{
			this.model = model;
			this.position = position;
		}

		/** Hierachical tree constructor. */
		public Context(MutableModel model, ModelItem parent, Integer position)	{
			this(model, position);
			this.parent = parent;
		}
	}


	/** Semantic marker CommandArgument for pasting of nodes, containing both models, and optionally the parent and the position. */
	public static class Paste extends CommandArguments
	{
		/** Paste constructor within the same model. Tree target parent will be passed by clipoard. */
		public Paste(MutableModel model)	{
			this(model, model);
		}

		/** Paste constructor within a different model. Tree target parent will be passed by clipoard. */
		public Paste(MutableModel sendingModel, MutableModel receivingModel)	{
			this(sendingModel, receivingModel, null, null);
		}

		/** Table paste constructor within the same model at a defined position. */
		public Paste(MutableModel model, Integer position)	{
			this(model, model, position);
		}

		/** Table paste constructor within a different model at a defined position. */
		public Paste(MutableModel sendingModel, MutableModel receivingModel, Integer position)	{
			this(sendingModel, receivingModel, null, position);
		}

		/** Internal tree paste constructor within a different model at a defined parent and position. */
		Paste(MutableModel sendingModel, MutableModel receivingModel, ModelItem parent, Integer position)	{
			this.sendingModel = sendingModel;
			this.model = receivingModel;
			this.parent = parent;
			this.position = position;
		}
	}


	/** Semantic marker CommandArgument for inserting nodes at certain position into a known model. */
	public static class Position extends CommandArguments
	{
		/** Constructor for insertion at a certain position. */
		public Position(Integer position)	{
			this.position = position;
		}
	}

}