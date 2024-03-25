package fri.gui.awt.resourcemanager.resourceset.resource;

import java.lang.reflect.Method;
import java.awt.Component;
import fri.util.reflect.ReflectUtil;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;

/**
	The generic implementation for all types of Resources.
*/

public abstract class Resource implements
	Comparable
{
	protected Object initial;	// holding the (simple) initial programmatic value of the GUI component
	protected Object value;	// the (complex) struct holding the user-defined value
	private Object previous;	// save field for "value" during customize sessions
	private boolean previousComponentTypeBound;
	private boolean componentTypeBound;
	private Converter converter;
	private int sortPosition = -1;


	/** Do-nothing constructor, enables derived classes to initialize arbitrary. */
	protected Resource()	{
	}

	/** Constructor with a persistence value retrieved from properties. */
	protected Resource(String spec)	{
		value = getConverter().stringToObject(spec);
	}

	/** Constructor with a GUI-component to initialize from. */
	protected Resource(Object component)
		throws ResourceNotContainedException
	{
		defaultResourceDetection(component);
	}


	/** Creates a value representation converter (persistence value - GUI value - management structure). */
	protected abstract Converter createConverter();

	/** Returns the Resource-specific representation converter (persistence-GUI-structure). */
	protected Converter getConverter()	{
		if (converter == null)
			converter = createConverter();
		return converter;
	}


	/** Returns the symbolic type name for this resource: ResourceFactory.FONT, BORDER, ... */
	public abstract String getTypeName();


	/** Calls <i>initFromComponent()</i> and throws Exception if resource method name could not be applied. */
	protected void defaultResourceDetection(Object component)
		throws ResourceNotContainedException
	{
		if (initFromComponent(component) == false)
			throw new ResourceNotContainedException(getTypeName());
	}


	/** Returns the current value, containing the GUI-value. This gets called by the customize dialog. */
	public Object getUserValue()	{
		return value;
	}

	/** Sets the current value, containing the GUI-value. This gets called by the customize dialog. Does NOT visualize it! */
	public void setUserValue(Object value)	{
		this.value = value;
	}

	/** Returns the initial programmatic GUI value. This gets called by the compoennt choice dialog. */
	public Object getInitialValue()	{
		return initial;
	}

	/** Returns the user-defined value or, if null, the initial (programmatic) value, converted to complex struct (if necessary). */
	public Object getVisibleValue()	{
		return getUserValue() != null ? getUserValue() : getConverter().stringToObject(getConverter().objectToString(initial));
	}

	/**
		Set the resource value into passed component (do not visualize it). Does not care about null values!
		@param component receiver Component of resource.
		@param isNotReset true for set, false for reset to original value.
	*/
	public void setToComponent(Object component, boolean isNotReset)	{
		//System.err.println("setToComponent, isNotReset: "+isNotReset);
		Object o;
		if (isNotReset == false)	{	// is reset
			setUserValue(null);
			o = initial;
			setComponentTypeBound(false);
		}
		else	{
			o = getConverter().toGuiValue(getUserValue(), component);
		}
		visualizeOnComponent(component, o);
	}
		
	/** Sets the currently defined, or if none, the initial value into the passed component. */
	public void forceToComponent(Object component)	{
		visualizeOnComponent(component, getUserValue() == null ? initial : getConverter().toGuiValue(getUserValue(), component));
	}
	
	/** Visualizes a (simple) GUI value upon passed component. */
	protected void visualizeOnComponent(Object component, Object guiValue)	{
		String methodName = "set"+getMethodBaseName(component);
		Class [] paramTypes = new Class [] { getConverter().getGuiValueClass(component) };

		//System.err.println("looking for method: "+methodName+", argument "+paramTypes[0]+", on component "+component.getClass());
		if (ReflectUtil.getMethod(component, methodName, paramTypes) != null)	{
			//System.err.println("setting resource to component: "+methodName+", component "+component.getClass().getName()+", value "+guiValue);

			try	{
				ReflectUtil.invoke(component, methodName, paramTypes, new Object[] { guiValue });
				if (component instanceof Component)
					((Component)component).invalidate();
			}
			catch (Exception e)	{
				System.err.println("ERROR SETTING RESOURCE: "+e.toString()+(e.getCause() != null ? ", caused b "+e.getCause() : ""));
			}
		}
	}

	/** Default returns the getTypeName(). Override to return "Shortcut" for methods "getShortcut" and "setShortcut". */
	protected String getMethodBaseName(Object component)	{
		return getTypeName();
	}


	/** Initialize a resource value from passed component with original values (for reset). @return true if getter method was found. */
	public boolean initFromComponent(Object component)	{
		String methodBaseName = getMethodBaseName(component);
		if (methodBaseName == null)
			return false;
			
		String methodName = "get"+methodBaseName;
		Method m = ReflectUtil.getMethod(component, methodName);
		if (m != null)	{
			initial = checkInitialValue(ReflectUtil.invoke(component, methodName));
		}
			
		return m != null;
	}

	/** Gets the initial value when retrieved from component. Returns initial or null if the value is not valid. */
	protected Object checkInitialValue(Object initial)	{
		return initial;
	}


	/** Converts to persistence String. Delegates to the dynamically loaded Swing Converter. */
	public String toString()	{
		return getConverter().objectToString(value);
	}


	/** Called by ResourceSet before a customize session. */ 
	public void startCustomizing()	{
		previousComponentTypeBound = componentTypeBound;
		previous = value;
	}

	/** Called by ResourceSet to restore the value after a customize session. */ 
	public void rollbackCustomizing()	{
		value = previous;
		setComponentTypeBound(previousComponentTypeBound);
	}


	/** Set the resource valid for all instances of a certain component type. */
	public void setComponentTypeBound(boolean componentTypeBound)	{
		this.componentTypeBound = componentTypeBound;
	}
	
	/** Returns true if the resource is valid for all instances of a certain component type. The default is false. */
	public boolean isComponentTypeBound()	{
		return componentTypeBound;
	}


	/** Setting the sort position of this resource. Allowed only by factory that knows the order. */
	void setSortPosition(int sortPosition)	{
		this.sortPosition = sortPosition;
	}

	/** Implements Comparable to keep sort position order in resource set. */
	public int compareTo(Object other)	{
		if (sortPosition < 0)
			throw new IllegalStateException("Sort position was not set in "+this);
		Resource r = (Resource) other;
		return r.sortPosition - sortPosition;
	}

}
