package fri.util.props;

import java.util.*;
import javax.swing.tree.DefaultMutableTreeNode;
import fri.util.NumberUtil;

/**
	This class provides a conversion of special Properties to a tree made of DefaultMutableTreeNode
	and vice versa. Up to 999 child properties can be stored within a "folder" property.
	The userObject of every DefaultMutableTreeNode is a Vector with all record attribute values
	(attribute structure is the same for all nodes).
	<p>
	Structure sample:
	<pre>
		person.name = Fritz Ritzberger
		person.email = fritz.ritzberger@chello.at
		person.000.email = franz.ratzbauer@yahoo.com
		person.000.name = Franz Ratzbauer
		person.000.000.email = florian.fuchs@google.com
		person.000.000.name = Florian Fuchs
		person.001.email = fred.wander@altavista.com
		person.001.name = Fred Wander
	</pre>
	The related tree would be:
	<pre>
		- [Fritz Ritzberger, fritz.ritzberger@chello.at],
		  - [Franz Ratzbauer, franz.ratzbauer@yahoo.com],
		    - [Florian Fuchs, florian.fuchs@google.com],
		  - [Fred Wander, franz.ratzbauer@altavista.com],
	</pre>
	
	The input parameters for this structure are:
	<ul>
		<li>Name of tree root (e.g. "person")</li>
		<li>Array of attribute names (e.g. attributes="[name, email]") of each node</li>
	</ul>
	<b>Both entity and attribute names must not contain whitespaces, dots, or chars not allowed in Properties names!</b>
	The entry methods are
	<ul>
		<li><i>public static DefaultMutableTreeNode convert(Properties props, String entity, String [] attributes)</i> and</li>
		<li><i>public static Properties convert(DefaultMutableTreeNode root, String entity, String [] attributes)</i>.</li>
	</ul>
	Persistence can be provided by ClassProperties (is not implemented here).
	The root DefaultMutableTreeNode can be used for the construction of a <i>DefaultTreeModel</i>.
	
	@author Fritz Ritzberger 2003
*/

public abstract class TreeProperties
{
	/** Returns a DefaultMutableTreeNode created from passed Properties. */
	public static DefaultMutableTreeNode convert(Properties props, String entity, Vector attributes)	{
		String [] attrs = new String[attributes.size()];
		attributes.toArray(attrs);
		return convert(props, entity, attrs);
	}
	
	/** Returns a DefaultMutableTreeNode created from passed Properties. */
	public static DefaultMutableTreeNode convert(Properties props, String entity, String [] attributes)	{
		List v = TableProperties.getRequestedPropertiesSorted(props, entity, attributes, new TagComparator());
		
		// retrieve all values for sorted list from properties
		// check for attributes sort order
		
		DefaultMutableTreeNode result = new DefaultMutableTreeNode();
		DefaultMutableTreeNode node = result;
		Vector row = null;
		String currentRecord = null;
		
		for (int i = 0; i < v.size(); i++)	{
			String name = (String)v.get(i);	// "person.email"
			String nextRecord = name.substring(0, name.lastIndexOf("."));	// "person"
			//System.err.println("looping with property "+name);

			if (currentRecord == null || currentRecord.equals(nextRecord) == false)	{	// start or change of record
				if (currentRecord != null)	{	// pack previous record
					node.setUserObject(row);
					
					DefaultMutableTreeNode pnt;
					if (currentRecord.length() < nextRecord.length())	{	// is child
						pnt = node;
					}
					else	{	// is sibling or child of some grand-parent
						pnt = (DefaultMutableTreeNode)node.getParent();	// sibling level

						if (currentRecord.length() > nextRecord.length())	{	// is child of some grand-parent
							int levelDelta = countLevels(currentRecord) - countLevels(nextRecord);
							for (; levelDelta > 0; levelDelta--)
								pnt = (DefaultMutableTreeNode)pnt.getParent();
						}
					}
					node = new DefaultMutableTreeNode();
					pnt.add(node);
				}

				currentRecord = nextRecord;

				row = new Vector(attributes.length);
				for (int j = 0; j < attributes.length; j++)	// initialize record
					row.addElement("");
			}

			String value = props.getProperty(name);	// get value from properties
			int attributeIndex = TableProperties.getAttributeIndex(attributes, name);	// get its index from attribute array
			
			row.setElementAt(value, attributeIndex);	// set the value to its position into row Vector
		}
		
		if (row != null)	// final row was not packed
			node.setUserObject(row);
		
		return result;
	}
	

	private static class TagComparator implements Comparator
	{
		/** Compares its two arguments for order. */
		public int compare(Object o1, Object o2)	{
			String s1 = (String)o1;
			String s2 = (String)o2;
			s1 = s1.substring(0, s1.lastIndexOf("."));
			s2 = s2.substring(0, s2.lastIndexOf("."));
			return s1.compareTo(s2);
		}
		
	}


	private static int countLevels(String name)	{
		int dots = 0;
		for (int i = 0; i < name.length(); i++)
			if (name.charAt(i) == '.')
				dots++;
		return dots;
	}
	
	
	

	/** Returns tree Properties conversion from passed TreeNode root. */
	public static Properties convert(DefaultMutableTreeNode root, String entity, Vector attributes)	{
		String [] attrs = new String[attributes.size()];
		attributes.toArray(attrs);
		return convert(root, entity, attrs);
	}
	
	/** Returns tree Properties conversion from passed TreeNode root. */
	public static Properties convert(DefaultMutableTreeNode root, String entity, String [] attributes)	{
		Properties props = new Properties();
		createRecord(null, root, entity, attributes, props);
		return props;
	}
	
	private static void createRecord(String recordNr, DefaultMutableTreeNode node, String entity, String [] attributes, Properties props)	{
		List row = (List)node.getUserObject();
		TableProperties.createRecord(recordNr, row, entity, attributes, props);

		for (int i = 0; i < node.getChildCount(); i++)	{
			String rn = NumberUtil.convertWithLeadingZeros(i, 3);
			rn = recordNr != null ? recordNr+"."+rn : rn;
			DefaultMutableTreeNode n = (DefaultMutableTreeNode)node.getChildAt(i);
			createRecord(rn, n, entity, attributes, props);
		}
	}



	public static void main(String [] args)	{
		Properties p = new Properties();
		p.setProperty("person.name", "Fritz Ritzberger");
		p.setProperty("person.email", "fritz.ritzberger@chello.at");
		p.setProperty("person.000.name", "Franz Ratzbauer");
		p.setProperty("person.000.email", "franz.ratzbauer@yahoo.com");
		p.setProperty("person.000.000.name", "Florian Fuchs");
		p.setProperty("person.000.000.email", "florian.fuchs@google.com");
		p.setProperty("person.001.name", "Fred Wander");
		p.setProperty("person.001.email", "fred.wander@altavista.com");
		DefaultMutableTreeNode root = TreeProperties.convert(p, "person", new String [] { "name", "email" });
		for (Enumeration e = root.preorderEnumeration(); e.hasMoreElements(); )	{
			DefaultMutableTreeNode tn = (DefaultMutableTreeNode)e.nextElement();
			Object [] path = tn.getPath();
			for (int j = 1; j < path.length; j++)
				System.err.print("	");
			System.err.println(tn);
		}
		p = TreeProperties.convert(root, "person", new String [] { "name", "email" });
		System.err.println(p);
	}
	
	
	private TreeProperties()	{}
}