package fri.gui.swing.expressions;

import javax.swing.tree.MutableTreeNode;
import fri.patterns.interpreter.expressions.Condition;
import fri.gui.mvc.model.swing.*;

/**
	Filter tree-model that represents the structure of filter expressions.
*/
public class FilterTreeModelItem extends AbstractMutableTreeModelItem
{
	public FilterTreeModelItem(MutableTreeNode node)	{
		super(node);
	}

	protected Object cloneInMedium(Object treeNodeUserObject)	{
		Condition c = (Condition)treeNodeUserObject;
		return c.clone();
	}

	/*
	protected ModelItem createInMedium(CommandArguments createArguments)	{
		System.err.println("createInMedium, createArguments "+createArguments);
		// insert created expression into array
		Condition cond = (Condition)createArguments.getCreateData();
		Integer pos = createArguments.getPosition();
		return super.createInMedium(createArguments);
	}
	
	protected ModelItem moveInMedium(ModelItem target, CommandArguments pasteInfo)	{
		System.err.println("moveInMedium, target "+target+", paste info "+pasteInfo);
		return super.moveInMedium(target, pasteInfo);
	}
	*/

}
