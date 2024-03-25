package fri.util.xml.xml4j;

import java.util.*;
import com.ibm.xml.parser.*;
import org.w3c.dom.*;

/**
	XML4J dependent class.<br>
	Construct a default DTD from a XML Document without an external DTD.
	Walks the tree and creates "element*" (mixed content) for each found node.
	<PRE>
		DTD dtd = DefaultDTD.create(document);
		// DTD will be constructed from existing elements
		// and added as first child to document:
		//	<!ELEMENT elementName (subElement1|subElement2)*>
		//	<!ELEMENT subElement1 (#PCDATA)*>
		//	<!ATTLIST subElement1 someAttribute CDATA #IMPLIED>
		//	<!ELEMENT subElement2 (#PCDATA)*>
	</PRE>

	@author  Ritzberger Fritz
*/

abstract class DefaultDTD
{
	/**
		Adds a default DTD, built from existing document elements, to the document.
		@return the created DTD.
	*/
	public static DTD create(TXDocument doc)	{
		Node docElement = doc.getDocumentElement();
		if (docElement == null)	{	// this is not a XML document
			throw new IllegalArgumentException("Object has no document element!");
		}

		String docElemName = docElement.getNodeName();

		// create an empty DTD
		DTD dtd = doc.createDTD(docElemName, null);

		// add DTD to TXDocument
		addDTD(dtd, doc);

		// analyze the document
		BuildElement root = new BuildElement(docElemName);
		Hashtable all = new Hashtable();
		all.put(docElemName, root);
		traverse(docElement, root, all);

		// fill the DTD
		all = new Hashtable(all.size());
		all.put(root, root);
		build(root, dtd, doc, all);

		return dtd;
	}

	private static void traverse(Node node, BuildElement element, Hashtable all)	{
		short type = node.getNodeType();
		if (type == Node.ELEMENT_NODE || type == Node.DOCUMENT_NODE)	{
			element.addAttributes(node.getAttributes());

			NodeList children = node.getChildNodes();

			for (int i = 0; i < children.getLength(); i++)	{
				Node child = children.item(i);

				if (child.getNodeType() == Node.ELEMENT_NODE)	{
					BuildElement e = element.addChild(child.getNodeName(), all);

					traverse(child, e, all);
				}
			}
		}
	}



	// Internal helper class.
	// Temporary accumulating element node tags, their children and their attributes
	private static class BuildElement extends Vector	// holding its children
	{
		String tag;
		Vector attributes;

		BuildElement(String tag)	{
			this.tag = tag;
		}

		// inserts element if not contained, returns element for passed tag
		BuildElement addChild(String tag, Hashtable all)	{
			BuildElement found;

			if ((found = (BuildElement)all.get(tag)) == null)	{	// accumulate elements unique
				found = new BuildElement(tag);
				all.put(tag, found);
			}

			if (indexOf(found) < 0)
				add(found);

			return found;
		}

		// accumulates attributes unique
		void addAttributes(NamedNodeMap attributes)	{
			for (int i = 0; attributes != null && i < attributes.getLength(); i++)	{
				Attr attr = (Attr)attributes.item(i);
				String name = attr.getName();

				if (this.attributes == null)
					this.attributes = new Vector();

				if (this.attributes.contains(name) == false)	// accumulate attributes unique
					this.attributes.add(name);
			}
		}

		/** Overridden to return tag hashcode. */
		public int hashCode()	{
			return tag.hashCode();
		}
		/** Overridden to compare tags. */
		public boolean equals(Object o)	{
			return tag.equals(((BuildElement)o).tag);
		}

	}


	private static void build(BuildElement element, DTD dtd, TXDocument doc, Hashtable all)	{
		//<!ELEMENT elementName (#PCDATA|subElement1|subElement2) >

		// establish mixed content model, #PCDATA first
		CMNode current = new CMLeaf(DTD.CM_PCDATA);

		int count = element.size();

		for (int i = 0; i < count; i++)	{	// loop child elements
			BuildElement child = (BuildElement)element.get(i);	// child
			CMNode n = new CMLeaf(child.tag);
			current = new CM2op('|', current, n);	// aggregate in left node
		}
		
		current = new CM1op('*', current);

		ContentModel cm = doc.createContentModel(current); 
		ElementDecl ed = doc.createElementDecl(element.tag, cm);
		dtd.appendChild(ed);


		//<!ATTLIST subElement someAttribute CDATA #IMPLIED>

		if (element.attributes != null)	{
			Attlist al = doc.createAttlist(element.tag);

			for (int i = 0; i < element.attributes.size(); i++)	{
				String name = (String)element.attributes.get(i);

				AttDef attrDef = doc.createAttDef(name);
				attrDef.setDeclaredType(AttDef.CDATA);
				attrDef.setDefaultType(AttDef.IMPLIED);
					
				al.addElement(attrDef);
			}

			dtd.appendChild(al);
		}


		// Recursion to reach children

		for (int i = 0; i < count; i++)	{	// loop child elements
			BuildElement child = (BuildElement)element.get(i);	// child

			if (all.get(child) == null)	{
				all.put(child, child);
				build(child, dtd, doc, all);
			}
		}
	}



	/** When storing a document the DTD might not be stored. */
	public static DTD removeDTD(TXDocument doc)	{
		if (doc.getDTD() != null)
			return (DTD)doc.removeChild(doc.getDTD());
		return null;
	}

	/** When storing a document the DTD might not be stored. */
	public static void addDTD(DTD dtd, TXDocument doc)	{
		if (doc.getFirstChild() != null && doc.getFirstChild() instanceof DTD)
			removeDTD(doc);
		doc.insertFirst(dtd);
	}



/*
	public static void main(String [] args)
		throws Exception
	{
		String xmlText = "<?xml version=\"1.0\"?>"+
				"<ADDRESSBOOK>"+
				"	<PERSON>"+
				"		<LASTNAME>Ritzberger</LASTNAME>"+
				"		<FIRSTNAME>Niklas</FIRSTNAME>"+
				"	</PERSON>	"+
				"	<PERSON>"+
				"		<LASTNAME synonym=\"family\">Ritzberger</LASTNAME>"+
				"		<FIRSTNAME synonym=\"nickname\" opt=\"true\">Fritz</FIRSTNAME>"+
				"		<COMPANY>FriWare Factory</COMPANY>"+
				"		<FIRSTNAME synonym=\"nickname\" opt=\"true\" some=\"thing\">Viktor</FIRSTNAME>"+
				"		<EMAIL>fri@home.at</EMAIL>"+
				"	</PERSON>	"+
				"</ADDRESSBOOK>";
		
		java.io.InputStream is = new java.io.ByteArrayInputStream(xmlText.getBytes());
		Parser p = new Parser("some.xml");
		p.setEndBy1stError(false);	// sonst Abbruch weil keine DTD
		
		//p.setWarningNoDoctypeDecl(false);	// trotzdem >Attribute, "synonym", is not declared in element, "LASTNAME".<
		//p.setWarningNoXMLDecl(false);
		
		TXDocument doc = p.readStream(is);
		
		DTD dtd = DefaultDTD.create(doc);
		
		String charset = "ISO-8859-1";
		// geht nicht:
		//dtd.setEncoding(charset);
		//dtd.toXMLString(new PrintWriter(new OutputStreamWriter(System.out, MIME2Java.convert(charset))));

		// geht nicht:
		//dtd.setEncoding(charset);
		//dtd.printExternal(new PrintWriter(new OutputStreamWriter(System.out, MIME2Java.convert(charset))), MIME2Java.convert(charset));

		doc.setEncoding(charset);
		doc.toXMLString(new PrintWriter(new OutputStreamWriter(System.out, MIME2Java.convert(charset))));
	}
*/
}
