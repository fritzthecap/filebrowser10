package fri.util.props;

import java.io.Serializable;
import java.util.*;

/**
	A List wrapper for name/value pairs, using an inner Tupel class.
	Used in PropertiesTextField.

	@author  Ritzberger Fritz
*/

public class PropertiesList extends Vector
{
	/** Do nothing constructor. */
	public PropertiesList()	{
	}

	/** Create a PropertiesList from name/value lists with predefined order. This will NOT be sorted. */
	public PropertiesList(List names, List values)	{
		if (names != null && values != null && names.size() != values.size() ||
				names == null && values != null || names != null && values == null)
			throw new IllegalArgumentException("Names and values list must be of same size!");
			
		for (int i = 0; i < names.size(); i++)	{
			String name = names.get(i).toString();
			String value = values.get(i).toString();

			Tuple t = new Tuple(name, value);
			add(t);
		}
	}
	
	/** Create a PropertiesList from Properties. This WILL be sorted. */
	public PropertiesList(Properties props)	{
		super(props == null ? 10 : props.size());

		if (props == null)	{
			props = new Properties();
		}

		// sort the names
		Vector v = new Vector(props.size());

		for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
			v.add(e.nextElement());
		}

		String [] arr = new String [v.size()];
		v.copyInto(arr);
		Arrays.sort(arr);

		for (int i = 0; i < arr.length; i++)	{
			String name = arr[i];
			String value = props.getProperty(name);

			Tuple t = new Tuple(name, value);
			add(t);
		}
	}
	

	/** Clones this list deep, including all Tuple elements. The name/value pairs within Tuples will not be cloned. */
	public Object clone()	{
		PropertiesList clone = new PropertiesList();

		for (int i = 0; i < size(); i++)
			clone.add(((Tuple)get(i)).clone());

		return clone;
	}

	/** Sets the value for passed name. @exception IllegalArgumentException if name does not exist. */
	public void setValue(String name, String value)	{
		for (int i = 0; i < size(); i++)	{
			Tuple t = (Tuple)get(i);

			if (t.name.equals(name))	{
				t.value = value;
				return;
			}
		}
		throw new IllegalArgumentException("Name is not in PropertiesList: "+name);
	}


	/** The list element that exposes name and value Strings. */
	public static class Tuple implements
		Cloneable,
		Serializable
	{
		public String name, value;

		Tuple(String name, String value)	{
			this.name = name;
			this.value = value;
		}

		public String toString()	{
			return name+"=\""+value+"\"";
		}

		public boolean equals(Object o)	{
			Tuple t = (Tuple)o;
			return name.equals(t.name) && value.equals(t.value);
		}

		public Object clone()	{
			return new Tuple(name, value);
		}
	}

}