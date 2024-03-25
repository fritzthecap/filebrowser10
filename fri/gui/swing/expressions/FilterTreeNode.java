package fri.gui.swing.expressions;

import javax.swing.tree.MutableTreeNode;
import fri.patterns.interpreter.expressions.*;
import fri.gui.mvc.model.swing.*;

/**
	Filter treenode that represents one folder or leaf element of a filter expression tree.
*/

public class FilterTreeNode extends AbstractTreeNode
{
	private boolean listing;
	
	public FilterTreeNode(Condition userObject)	{
		super(userObject, true);
	}


	/** Overrides DefaultMutableTreeNode: returns true when userObject not instanceof Comparison. */
	public boolean getAllowsChildren() {
		return getUserObject() instanceof LogicalCondition;
	}

	/** Implements AbstractTreeNode: lists children from Condition userObject. */
	protected void list()	{
		if (getAllowsChildren())	{
			LogicalCondition condition = (LogicalCondition)getUserObject();
			Condition [] conditions = condition.getConditions();
			for (int i = 0; i < conditions.length; i++)	{
				listing = true;
				add(createTreeNode(conditions[i]));
				listing = false;
			}
		}
	}
	
	/** Implements AbstractTreeNode: casts the argument to Condition and returns a new unlinked FilterTreeNode containing it. */
	public AbstractTreeNode createTreeNode(Object expression)	{
		return new FilterTreeNode((Condition)expression);
	}


	// overridden treenode methods, redirecting actions to LogicalCondition data model.

	/** Overridden to remove the removed child from contained LogicalCondition. */
	public void remove(int childIndex)	{
		change(null, childIndex);
		super.remove(childIndex);	// after successful editing model
	}

	/** Overridden to insert the new child into contained LogicalCondition. */
	public void insert(MutableTreeNode newChild, int childIndex)	{
		if (listing == false)
			change(newChild, childIndex);
		super.insert(newChild, childIndex);
	}

	private void change(MutableTreeNode newChild, int childIndex)	{
		boolean isInsert = (newChild != null);
		
		LogicalCondition condition = (LogicalCondition)getUserObject();
		Condition [] conditions = condition.getConditions();
		Condition [] newConditions = new Condition[isInsert ? conditions.length + 1 : conditions.length - 1];

		System.arraycopy(conditions, 0, newConditions, 0, childIndex);

		if (isInsert)	{
			newConditions[childIndex] = (Condition) ((FilterTreeNode)newChild).getUserObject();
			System.arraycopy(conditions, childIndex, newConditions, childIndex + 1, conditions.length - childIndex);
		}
		else	{	// is remove
			if (childIndex < conditions.length - 1)
				System.arraycopy(conditions, childIndex + 1, newConditions, childIndex, conditions.length - (childIndex + 1));
		}

		condition.setConditions(newConditions);
	}

}
