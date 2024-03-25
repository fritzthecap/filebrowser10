package fri.gui.swing.expressions;

import java.util.Hashtable;
import fri.patterns.interpreter.expressions.*;
import fri.util.managers.InstanceManager;

/**
	Manages FilterTreeModel singletons.
	
	@author Fritz Ritzberger
*/

public class FilterTreeModelFactory
{
	private static InstanceManager factoryManager = new InstanceManager();
	private Hashtable cache = new Hashtable();

	public static FilterTreeModelFactory singleton()	{
		return (FilterTreeModelFactory)factoryManager.getInstance("FilterTreeModelFactory", new FilterTreeModelFactory());
	}
	
	public static void free()	{
		factoryManager.freeInstance("FilterTreeModelFactory");
	}
	
	private FilterTreeModelFactory()	{
	}

	/** Returns a singleton FilterTreeModel for a name. */
	public FilterTreeModel get(String name)	{
		Object o = cache.get(name);
		if (o != null)
			return (FilterTreeModel)o;
		
		FilterTreeNode root = FilterTreePersistence.load(name);
		if (root == null)
			root = newRoot(name);
		
		FilterTreeModel m = new FilterTreeModel(name, root);
		cache.put(name, m);
		return m;
	}

	/* Create a new model. Does not persist the new model! */
	private FilterTreeNode newRoot(String name)	{
		StringComparison comp = new StringComparison(new BeanVariable(), StringComparison.CONTAINS, new Constant(""));
		LogicalCondition cond = new LogicalCondition(LogicalCondition.OR, new Condition [] { comp });
		FilterTreeNode root = new FilterTreeNode(cond);
		System.err.println("created model "+name+" with expression "+cond);
		return root;
	}	
	
	/** Returns a singleton FilterTreeModel for a name. */
	public void rename(String name, String newName)	{
		FilterTreePersistence.rename(name, newName);
		FilterTreeModel m = (FilterTreeModel)cache.get(name);
		m.setName(newName);
		cache.put(newName, m);
	}

	/** Returns a singleton FilterTreeModel for a name. */
	public void delete(String name)	{
		FilterTreePersistence.delete(name);
		cache.remove(name);
	}

}