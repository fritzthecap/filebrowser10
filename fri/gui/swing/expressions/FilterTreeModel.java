package fri.gui.swing.expressions;

import javax.swing.tree.MutableTreeNode;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.swing.AbstractMutableTreeModel;

/**
	Filter tree-model that represents a filter expression.
*/

public class FilterTreeModel extends AbstractMutableTreeModel
{
	private String name;
	
	public FilterTreeModel(String name, FilterTreeNode root)	{
		super(root);
		this.name = name;
	}

	/**
		Concrete AbstractMutableTreeModel.
		Returns new FilterTreeModelItem, creates FilterTreeNode if argument is null.
	*/
	public ModelItem createModelItem(MutableTreeNode node)	{
		return new FilterTreeModelItem(node);
	}

	/** Returns the name of this model. */
	public String getName()	{
		return name;
	}
	
	/** Changes the name of this model persistently (renames file). */
	public void setName(String name)	{
		this.name = name;
	}
	
	/**
		Save the model to the name passed in constructor by means of FilterTreePersistence.
	*/
	public boolean save()	{
		System.err.println("saving FilterTreeModel "+getName());
		return FilterTreePersistence.store(getName(), (FilterTreeNode)getRoot());
	}

}
