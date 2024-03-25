package fri.util.props;

import java.util.*;
import fri.util.NumberUtil;

/**
	This class provides a conversion of special Properties to a Vector of Vectors
	and vice versa. Sort order is significant. Up to 9999 records can be stored
	within Properties created by this class.
	<p>
	Structure sample:
	<pre>
		person.0000.name = Fritz Ritzberger
		person.0000.email = fritz.ritzberger@chello.at
		person.0001.email = franz.ratzbauer@yahoo.com
		person.0001.name = Franz Ratzbauer
	</pre>
	The related 2D Vector for the attribute order "name", "email" would be:
	<pre>
		[
			[Franz Ratzbauer, franz.ratzbauer@yahoo.com],
			[Fritz Ritzberger, fritz.ritzberger@chello.at]
		]
	</pre>
	
	The input parameters for this structure are:
	<ul>
		<li>Name of entity (e.g. entity="person")</li>
		<li>Array of attribute names (e.g. attributes="[name, email]")</li>
	</ul>
	<b>Both entity and attribute names must not contain whitespaces, dots, or chars not allowed in Properties names!</b>
	The entry methods are
	<ul>
		<li><i>public static Vector convert(Properties props, String entity, String [] attributes)</i> and</li>
		<li><i>public static Properties convert(Vector list2D, String entity, String [] attributes)</i>.</li>
	</ul>
	Persistence can be provided by ClassProperties (is not implemented here).
	The 2D Vector of Vectors can be used for <i>defaultTableModel.setDataVector()</i>.
	
	@author Fritz Ritzberger 2003
*/

public abstract class TableProperties
{
	/** Returns a Vector of Vectors created from passed Properties. */
	public static Vector convert(Properties props, String entity, List attributes)	{
		String [] attrs = new String[attributes.size()];
		attributes.toArray(attrs);
		return convert(props, entity, attrs);
	}
	
	/** Returns a Vector of Vectors created from passed Properties. */
	public static Vector convert(Properties props, String entity, String [] attributes)	{
		List v = getRequestedPropertiesSorted(props, entity, attributes, null);
		
		// retrieve all values for sorted list from properties
		// check for attributes sort order
		
		Vector result = new Vector(v.size());
		Vector row = null;
		String currentRecord = null;
		
		for (int i = 0; i < v.size(); i++)	{
			String name = (String)v.get(i);	// "person.001.email"
			String nextRecord = name.substring(0, name.lastIndexOf("."));	// "person.001"

			if (currentRecord == null || currentRecord.equals(nextRecord) == false)	{	// start or change of record
				if (currentRecord != null)	{	// pack previous record
					result.add(row);
				}
				currentRecord = nextRecord;

				row = new Vector(attributes.length);
				for (int j = 0; j < attributes.length; j++)	// initialize record
					row.addElement("");
			}

			String value = props.getProperty(name);	// get value from properties
			int attributeIndex = getAttributeIndex(attributes, name);	// get its index from attribute array
			
			row.setElementAt(value, attributeIndex);	// set the value to its position into row Vector
		}
		
		if (row != null)	// final row was not packed
			result.add(row);
		
		return result;
	}
	

	static List getRequestedPropertiesSorted(Properties props, String entity, String [] attributes, Comparator comparator)	{
		ArrayList v = new ArrayList(props.size());
		
		// get all names into list
		for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			
			if (isAttributeOf(entity, attributes, name))
				v.add(name);
		}

		// sort the name list
		Object [] arr = v.toArray();
		Arrays.sort(arr, comparator);
		return Arrays.asList(arr);
	}
	
	private static boolean isAttributeOf(String entity, String [] attributes, String name)	{
		if (name.startsWith(entity+".") == false)
			return false;
		
		boolean ok = false;
		for (int i = 0; ok == false && i < attributes.length; i++)	{
			if (name.endsWith("."+attributes[i]))
				ok = true;
		}
		return ok;
	}

	static int getAttributeIndex(String [] attributes, String name)	{
		for (int i = 0; i < attributes.length; i++)
			if (name.endsWith("."+attributes[i]))
				return i;
		return -1;	// error, will not happen as names were checked for attribute extension
	}
	
	

	/** Returns a Vector of Vectors created from passed Properties. */
	public static Properties convert(List list2D, String entity, List attributes)	{
		String [] attrs = new String[attributes.size()];
		attributes.toArray(attrs);
		return convert(list2D, entity, attrs);
	}
	
	/** Returns Properties from passed sorted Vector of Vectors. Records with no name and no value (empty string) are dismissed. */
	public static Properties convert(List list2D, String entity, String [] attributes)	{
		Properties props = new Properties();	//list2D.size() * attributes.length
		
		for (int i = 0; i < list2D.size(); i++)	{
			List row = (List)list2D.get(i);
			String recordNr = NumberUtil.convertWithLeadingZeros(i, 4);
			createRecord(recordNr, row, entity, attributes, props);
		}
		
		return props;
	}
	
	
	static void createRecord(String recordNr, List row, String entity, String [] attributes, Properties props)	{
		for (int i = 0; i < attributes.length && i < row.size(); i++)	{
			String value = (String)row.get(i);
			
			if (value != null && value.length() > 0)	{
				String name = entity+"."+(recordNr != null && recordNr.length() > 0 ? recordNr+"." : "")+attributes[i];
				props.setProperty(name, value);
			}
		}
	}


	public static void main(String [] args)	{
		Properties p = new Properties();
		p.setProperty("person.0001.name", "Fritz Ritzberger");
		p.setProperty("person.0001.email", "fritz.ritzberger@chello.at");
		p.setProperty("person.0002.name", "Franz Ratzbauer");
		p.setProperty("person.0002.email", "franz.ratzbauer@yahoo.com");
		Vector v = TableProperties.convert(p, "person", new String [] { "name", "email" });
		System.err.println(v);
		p = TableProperties.convert(v, "person", new String [] { "name", "email" });
		System.err.println(p);
	}
	
	
	private TableProperties()	{}
}