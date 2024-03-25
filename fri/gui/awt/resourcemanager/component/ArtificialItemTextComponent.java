package fri.gui.awt.resourcemanager.component;

import java.awt.Component;
import java.lang.reflect.Method;
import fri.gui.awt.resourcemanager.component.ArtificialComponent;

/**
	The proxy object for an item within a Choice/List/JComboBox (covers AWT and Swing).
	Can set and get text to and from its item. It has a name ("listitem")
	like a Component, which will be retrieved by ResourceComponentName.
*/

class ArtificialItemTextComponent extends ArtificialComponent
{
	private Method itemGetMethod, insertMethod, removeMethod;
	
	/** Construct an item proxy with the container (Choice/JComboBox/List), the getter method and the index. */
	public ArtificialItemTextComponent(Object parent, Method itemGetMethod, int index)
		throws Exception
	{
		this.parentComponent = (Component) parent;
		this.index = index;
		this.itemGetMethod = itemGetMethod;
		
		// search for removeIndex method
		try	{	// try Swing JComboBox
			removeMethod = parentComponent.getClass().getMethod("removeItemAt", new Class [] { int.class });
		}
		catch (NoSuchMethodException e)	{	// try AWT
			removeMethod = parentComponent.getClass().getMethod("remove", new Class [] { int.class });
		}
		
		// search for insertAt method
		try	{	// try Swing JComboBox
			insertMethod = parentComponent.getClass().getMethod("insertItemAt", new Class [] { Object.class, int.class });
		}
		catch (NoSuchMethodException e)	{
			try	{	// try AWT Choice
				insertMethod = parentComponent.getClass().getMethod("insert", new Class [] { String.class, int.class });
			}
			catch (NoSuchMethodException e2)	{	// try AWT List
				insertMethod = parentComponent.getClass().getMethod("add", new Class [] { String.class, int.class });
			}
		}
	}
	
	/** Simply returns "listitem". The label text will be retrieved and appended by ResourceComponentName. */
	public String getName()	{
		return "listitem";
	}

	
	/** Returns the item text by calling to the parent Component via reflection. */
	public String getText()	{
		try	{
			Object o = itemGetMethod.invoke(parentComponent, new Object [] { new Integer(index) });
			return (String) o;
		}
		catch (Exception e)	{
			System.err.println("WARNING: Could not get text from item "+index+": "+e.getMessage());
			return null;
		}
	}

	/** Sets the new item text by calling to the parent Component via reflection, calling setTitleAt, or remove and insert. */
	public void setText(String newText)	{
		try	{
			removeMethod.invoke(parentComponent, new Object [] { new Integer(index) });
			insertMethod.invoke(parentComponent, new Object [] { newText, new Integer(index) });
		}
		catch (Exception e)	{
			System.err.println("WARNING: Could not set text to item "+index+": "+e.getMessage());
		}
	}

}
