package fri.gui.swing.xmleditor.model;

import java.util.Comparator;
import org.w3c.dom.*;

/**
	A convenience implementation for DOM Node interface:
	compare returns 0 if two Nodes are equal. The comparison
	is made semi-deep, that means only non-elements are
	compared recursively.
	<p/>
	Mind that the compare method expects non-null XmlNode arguments.
	
	@author  Ritzberger Fritz
*/

public class NodeComparator implements Comparator
{
	/**
		Method not supported. Use compare(o1, o2) == 0.
	*/
	public boolean equals(Object o)	{
		throw new UnsupportedOperationException("equals(o) not supported. Use compare(o1, o2) == 0.");
	}

	/**
		Returns 0 for equality and 1 for non-equality.
		The comparison is made semi-deep, that means all non-element
		children will be compared.

		@param o1 XmlNode one
		@param o2 XmlNode two
		@return 0 for equality, else 1
	*/
	public int compare(Object o1, Object o2)	{
		XmlNode xn1 = (XmlNode)o1;
		XmlNode xn2 = (XmlNode)o2;

		Node n1 = xn1.getW3CNode();
		Node n2 = xn2.getW3CNode();

		return compareNodes(n1, n2);
	}

	/**
		Returns 0 for equality and 1 for non-equality.
		The comparison is made semi-deep, that means all non-element
		children will be compared.

		@param n1 Node one
		@param n2 Node two
		@return 0 for equality, else 1
	*/
	public int compareNodes(Node n1, Node n2)	{
		return compareNodes(n1, n2, true);
	}


	private int compareNodes(Node n1, Node n2, boolean deep)	{
		// compare types
		if (n1.getNodeType() != n2.getNodeType())	{
			//System.err.println("node type differs: "+n1.getNodeType()+" - "+n2.getNodeType());
			return 1;
		}

		// compare tag names
		if (n1.getNodeName().equals(n2.getNodeName()) == false)	{
			//System.err.println("node name differs: "+n1.getNodeName()+" - "+n2.getNodeName());
			return 1;
		}
		
		//System.err.println("compareNodes "+n1+" - "+n2);

		// compare value length
		String s1 = n1.getNodeValue();
		String s2 = n2.getNodeValue();
		s1 = s1 != null ? s1.trim() : s1;
		s2 = s2 != null ? s2.trim() : s2;
		int slen1 = s1 != null ? s1.length() : 0;
		int slen2 = s2 != null ? s2.length() : 0;

		if (slen1 != slen2)	{
			//System.err.println("text value length differs");
			return 1;
		}

		// compare attributes length
		NamedNodeMap a1 = n1.getAttributes();
		NamedNodeMap a2 = n2.getAttributes();
		int alen1 = a1 != null ? a1.getLength() : 0;
		int alen2 = a2 != null ? a2.getLength() : 0;

		if (alen1 != alen2)	{
			//System.err.println("attributes length differs");
			return 1;
		}

		// compare children length
		NodeList cli1 = n1.getChildNodes();
		NodeList cli2 = n2.getChildNodes();
		int clen1 = cli1 != null ? cli1.getLength() : 0;
		int clen2 = cli2 != null ? cli2.getLength() : 0;

		// comparing children on 1st or 2nd level would not work
		boolean compareChildren = true;
		if (n1.getNodeType() == Node.DOCUMENT_NODE || n1.getParentNode() != null && n1.getParentNode().getNodeType() == Node.DOCUMENT_NODE)
			compareChildren = false;
		
		if (compareChildren && clen1 != clen2)	{	// #document node can have DTD node or not
			//System.err.println("children length differs: "+clen1+" - "+clen2+", type is "+n1.getNodeType());
			return 1;
		}

		// compare values
		if (s1 != null && s2 != null && s1.equals(s2) == false)	{
			//System.err.println("text value differs!");
			return 1;
		}

		// compare attributes
		for (int i = 0; i < alen1; i++)	{
			Attr att1 = (Attr)a1.item(i);
			Attr att2 = (Attr)a2.item(i);

			if (compareNodes(att1, att2, true) != 0)	{	// deep, attributes usually have flat structure
				//System.err.println("attribute node differs: "+att1+" - "+att2);
				return 1;
			}
		}

		// compare children
		for (int i = 0; compareChildren && i < clen1; i++)	{
			Node nn1 = cli1.item(i);
			Node nn2 = cli2.item(i);

			if (nn1 == null && nn2 == null)
				continue;

			if (nn1 != null && nn2 == null || nn1 == null && nn2 != null)	{
				//System.err.println("child node pointer is null!");
				return 1;
			}

			if (nn1.getNodeType() != nn2.getNodeType())	{
				//System.err.println("child node type differs!");
				return 1;
			}

			if (nn1.getNodeName().equals(nn2.getNodeName()) == false)	{
				//System.err.println("child node name differs!");
				return 1;
			}

			if (deep || nn1.getNodeType() != Node.ELEMENT_NODE)	{
				if (compareNodes(nn1, nn2, deep) != 0)	{	// only elements and attributes have deep structure
					return 1;
				}
			}
		}

		return 0;	// must be equal
	}

}
