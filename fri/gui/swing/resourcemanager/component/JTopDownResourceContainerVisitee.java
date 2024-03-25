package fri.gui.swing.resourcemanager.component;

import java.lang.reflect.Method;
import java.util.*;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JComboBox;
import fri.util.reflect.ReflectUtil;
import fri.gui.awt.component.visitor.ContainerVisitor;
import fri.gui.awt.resourcemanager.ResourceIgnoringComponent;
import fri.gui.awt.resourcemanager.component.TopDownResourceContainerVisitee;

/**
	Additionally loops through JTabbedPane.
*/

public class JTopDownResourceContainerVisitee extends TopDownResourceContainerVisitee
{
	public JTopDownResourceContainerVisitee(Object c, ContainerVisitor visitor)	{
		super(c, visitor);
	}

	/** Overridden to catch JTabbedPane and JComboBox. */
	protected List getAdditionalResourceHolders(Object c)	{
		List list = super.getAdditionalResourceHolders(c);
		
		if (c instanceof Component)	{
			Component component = (Component)c;
			
			// catch each container in a tabbed pane
			Component parent = component.getParent();
			if (parent instanceof JTabbedPane && parent instanceof ResourceIgnoringComponent == false)	{
				JTabbedPane tabbedPane = (JTabbedPane) parent;
				int index = tabbedPane.indexOfComponent(component);
				if (list == null)
					list = new ArrayList(1);
				list.add(new ArtificialTabComponent(tabbedPane, index));
			}
			
			// catch the cell editor of a combobox
			if (component instanceof JComboBox && component instanceof ResourceIgnoringComponent == false && ((JComboBox)component).isEditable())	{
				JComboBox combo = (JComboBox)component; 
				if (combo.getEditor() != null)	{
					Component editor = combo.getEditor().getEditorComponent();
					if (editor != null)	{
						if (list == null)
							list = new ArrayList(1);
						list.add(editor);
					}
				}
			}
		}
		return list;
	}
	
	/** Overriden to prevent editable JComboBox items for being customizeable. */
	protected boolean isRuntimeEditable(Object c)	{
		if (c instanceof JComboBox == false)
			return super.isRuntimeEditable(c);
		return ((JComboBox) c).isEditable();
	}

	/** Overridden to catch TableHeader. */
	protected Method [] getCountAndGetItemMethod(Object c)	{
		Method [] itemCountAndGetMethod = new Method[2];
		if ((itemCountAndGetMethod[0] = ReflectUtil.getMethod(c, "getColumnModel")) != null)	// try JTable column model
			itemCountAndGetMethod[1] = itemCountAndGetMethod[0];	// will be handled in overrides
		
		return itemCountAndGetMethod[0] != null && itemCountAndGetMethod[1] != null
				? itemCountAndGetMethod
				: super.getCountAndGetItemMethod(c);
	}

	/** Overridden to catch TableHeader. */
	protected int getCount(Object c, Method itemCountMethod)
		throws Exception
	{
		if (itemCountMethod.getName().equals("getColumnModel"))
			return ((JTable) c).getColumnCount();
		return super.getCount(c, itemCountMethod);
	}
	
	/** Overridden to catch TableHeader. */
	protected Object getItem(Object c, Method itemGetMethod, int index)
		throws Exception
	{
		if (itemGetMethod.getName().equals("getColumnModel"))
			return ((JTable) c).getColumnModel().getColumn(index).getHeaderValue();
		return super.getItem(c, itemGetMethod, index);
	}

	/** Overridden to catch TableHeader. */
	protected Object createArtificialComponent(Object c, Method itemGetMethod, int index)
		throws Exception
	{
		if (itemGetMethod.getName().equals("getColumnModel"))
			return new ArtificialColumnHeaderComponent(c, index);
		return super.createArtificialComponent(c, itemGetMethod, index);
	}

}
