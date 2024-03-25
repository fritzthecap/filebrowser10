package fri.gui.swing.xmleditor.model;

import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.controller.CommandArguments;

/**
	The wrapper for XmlNode when involved in an action. Contains "business logic"
	move, copy, create, delete.
*/

public class ControllerModelItem implements
	ModelItem	// standard Clipboard functionality
{
	private MutableXmlNode node;
	private String error;


	/** Create a worker item for passed node in passed model. */
	public ControllerModelItem(MutableXmlNode node)	{
		this.node = node;
	}

	/** Returns the XmlNode of this ModelItem wrapper. */
	public MutableXmlNode getXmlNode()	{
		return node;
	}



	// Translate PASTE_AFTER and PASTE_BEFORE to the parent node,
	// calculate position for PASTE_WITHIN ("null" argument)
	private ModelParentAndPosition translatePasteInfo(ModelItem target, CommandArguments pasteInfo)	{
		error = NO_ERROR;

		if (pasteInfo instanceof ModelParentAndPosition)	{	// internal paste within at given position
			return (ModelParentAndPosition)pasteInfo;
		}

		Integer pasteFlag = (Integer)pasteInfo.getCreateData();

		if (pasteFlag == null)	{	// paste within
			// calculate position where to insert
			MutableXmlNode xn = getXmlNode();
			MutableXmlNode xt = ((ControllerModelItem)target).getXmlNode();
			int pos = xn.isInsertableEverywhere() ?
					xt.getChildCount() : xt.getInsertablePosition(xn.getInsertionTagName());

			if (pos < 0)	{
				error = "Node "+node+" not insertable in "+target+" at position "+pos;
				return null;
			}

			return new ModelParentAndPosition(pasteInfo.getModel(), target, new Integer(pos));
		}
		else	{
			MutableModel m = pasteInfo.getModel();	// target model
			CommandArguments ca = m.getModelItemContext(target);	// target's parent and position

			if (pasteFlag == CommandArguments.PASTE_BEFORE)	{	// paste before
				return (ModelParentAndPosition)ca;
			}
			else
			if (pasteFlag == CommandArguments.PASTE_AFTER)	{	// paste after
				int pos = ca.getPosition().intValue() + 1;
				return new ModelParentAndPosition(m, ca.getParent(), new Integer(pos));
			}
		}

		throw new IllegalArgumentException("Wrong pasteInfo: "+pasteInfo);
	}


	private ModelItem doInsert(ModelParentAndPosition nap, ControllerModelItem actor)	{
		System.err.println("ControllerModelItem doInsert "+actor+" to "+nap);

		try	{
			MutableModel m = nap.getModel();
			m.doInsert(actor, nap);
			return actor;
		}
		catch (IllegalArgumentException e)	{
			e.printStackTrace();
			error = e.getMessage();
		}

		return null;
	}


	// start interface ModelItem

	/**
		Move this item into the target item at given paste position and parent.
	*/
	public ModelItem doMove(ModelItem target, CommandArguments pasteInfo)	{
		System.err.println("ControllerModelItem doMove "+node+" to target "+target+" pasteInfo="+pasteInfo);
		// remove this node
		doDelete(new ModelParentAndPosition(pasteInfo.getSendingModel(), null, null));
		// after removal calculate new position
		ModelParentAndPosition nap = translatePasteInfo(target, pasteInfo);
		// insert node
		return doInsert(nap, this);
	}


	/**
		Copy this item into the target item at given paste position and parent.
	*/
	public ModelItem doCopy(ModelItem target, CommandArguments pasteInfo)	{
		System.err.println("ControllerModelItem doCopy "+node+" to target "+target+" pasteInfo="+pasteInfo);
		ModelParentAndPosition nap = translatePasteInfo(target, pasteInfo);
		return doInsert(nap, (ControllerModelItem)clone());
	}


	/**
		Create or insert a node within or before/behind this one. This comes from
		CreateCommand.doit() or RemoveCommand.undo().
	*/
	public ModelItem doInsert(CommandArguments createInfo)	{
		System.err.println("ControllerModelItem doInsert with createInfo "+createInfo);

		error = NO_ERROR;

		ModelParentAndPosition nap = null;
		ControllerModelItem actor = null;

		Object createData = createInfo.getCreateData();

		if (createData instanceof String)	{
			String tag = (String)createData;
			MutableXmlNode created = null;

			try	{
				created = getXmlNode().createInsertableNode(tag);
			}
			catch (NotInsertableException e)	{
				//e.printStackTrace();
				error = e.getMessage();
				return null;
			}

			actor = new ControllerModelItem(created);
			Integer pos = new Integer(created.getInsertPosition());
			nap = new ModelParentAndPosition(createInfo.getModel(), this, pos);
		}
		else	{
			actor = this;
			nap = (ModelParentAndPosition)createInfo;
		}

		return doInsert(nap, actor);
	}


	/**
		Delete this item.
	*/
	public boolean doDelete(CommandArguments deleteInfo)	{
		error = NO_ERROR;

		System.err.println("ControllerModelItem doDelete "+this+" by "+deleteInfo);

		deleteInfo.getModel().doDelete(this);

		return true;
	}


	/** Mark a node as "cutten". */
	public void setMovePending(boolean movePending)	{
		getXmlNode().setMovePending(movePending);
	}

	/** Returns the "cutten" property of this node. */
	public boolean isMovePending()	{
		return getXmlNode().getMovePending();
	}


	/** Returns the last error that happened when some action took place. */
	public String getError()	{
		return error;
	}


	/** Implements ModelItem: returns the XmlNode. */
	public Object getUserObject()	{
		return getXmlNode();
	}

	/** Implements ModelItem: clone this item. */
	public Object clone()	{
		return new ControllerModelItem((MutableXmlNode)getXmlNode().clone());
	}

	// end interface ModelItem



	/** Renders the tag name of the XML node. */
	public String toString()	{
		return getXmlNode().toString();
	}

	/** Delegates to getXmlNode().equals(). */
	public boolean equals(Object o)	{
		return ((ControllerModelItem)o).getXmlNode().equals(getXmlNode());
	}

}