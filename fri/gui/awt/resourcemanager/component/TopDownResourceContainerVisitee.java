package fri.gui.awt.resourcemanager.component;

import java.lang.reflect.Method;
import java.util.*;
import fri.gui.awt.component.visitor.*;
import fri.util.reflect.ReflectUtil;
import fri.gui.awt.resourcemanager.ResourceIgnoringComponent;

/**
	Additionally loops through Choice/JComboBox and generates
	artificial Components for all contained items.
	These are able to set and get a text from their items.
*/

public class TopDownResourceContainerVisitee extends TopDownContainerVisitee
{
	public TopDownResourceContainerVisitee(Object c, ContainerVisitor visitor)	{
		super(c, visitor);
	}

	/**
		Additionally test for resource methods not contained in Component tree and build artifical components if found.
		This method imposes that additional found components hold text values. If there is no String value,
		they will not be added.
	*/
	protected void traverse(Object c, ContainerVisitor visitor, Object userObject)	{
		List list;
		if (c instanceof ResourceIgnoringComponent == false && (list = getAdditionalResourceHolders(c)) != null)	{
			userObject = visitor.visit(c, userObject);

			for (int i = 0; i < list.size(); i++)
				super.traverse(list.get(i), visitor, userObject);
				
			traverseWithoutVisit(c, visitor, userObject);
		}
		else	{
			super.traverse(c, visitor, userObject);
		}
	}

	protected List getAdditionalResourceHolders(Object c)	{
		Method [] itemCountAndGetMethod = getCountAndGetItemMethod(c);
		if (itemCountAndGetMethod == null)	// no list items contained, not a combobox
			return null;
		
		if (isRuntimeEditable(c))	// avoid editable combos
			return null;

		List list = null;
		try	{
			int cnt = getCount(c, itemCountAndGetMethod[0]);
			list = new ArrayList(cnt);
			
			for (int i = 0; i < cnt; i++)	{
				Object o = getItem(c, itemCountAndGetMethod[1], i);
				if (o instanceof String == false)	// test for String class not to catch some Object, ClassCastException must be thrown
					throw new Exception("List item is not a String");
				list.add(createArtificialComponent(c, itemCountAndGetMethod[1], i));
			}
		}
		catch (Exception e)	{
			return null;	// item is not String but programmatic type
		}

		return list;
	}
	
	/** Returns false as AWT has no editable list components (which MUST NOT be customized). To be overriden by Swing implementation. */
	protected boolean isRuntimeEditable(Object c)	{
		return false;
	}

	/** Returns the item count method on index 0 and the item get method on index 1, or null if not found. To be overridden by Swing implementation. */
	protected Method [] getCountAndGetItemMethod(Object c)	{
		Method [] itemCountAndGetMethod = new Method[2];
		itemCountAndGetMethod[0] = ReflectUtil.getMethod(c, "getItemCount");
		if (itemCountAndGetMethod[0] != null)	{
			itemCountAndGetMethod[1] = ReflectUtil.getMethod(c, "getItemAt", new Class[] { int.class });
			if (itemCountAndGetMethod[1] == null)
				itemCountAndGetMethod[1] = ReflectUtil.getMethod(c, "getItem", new Class[] { int.class });
		}
		return itemCountAndGetMethod[0] != null && itemCountAndGetMethod[1] != null ? itemCountAndGetMethod : null;
	}

	/** Returns the item count of the item container. To be overridden by Swing implementation. */
	protected int getCount(Object c, Method itemCountMethod)
		throws Exception
	{
		Integer itg = (Integer) itemCountMethod.invoke(c, new Object[0]);
		return itg.intValue();
	}
	
	/** Returns the item at the passed index. To be overridden by Swing implementation. */
	protected Object getItem(Object c, Method itemGetMethod, int index)
		throws Exception
	{
		return itemGetMethod.invoke(c, new Object[] { Integer.valueOf(index) });
	}

	/** Creates an ArtificialItemTextComponent. To be overridden by Swing implementation. */
	protected Object createArtificialComponent(Object c, Method itemGetMethod, int index)
		throws Exception
	{
		return new ArtificialItemTextComponent(c, itemGetMethod, index);
	}

}
