package fri.util.xml.xml4j;

import java.io.*;
import java.util.*;
import java.awt.Point;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.Entity;
import org.w3c.dom.DocumentType;
import com.ibm.xml.parser.Child;
import com.ibm.xml.parser.DTD;
import com.ibm.xml.parser.TXDocument;
import com.ibm.xml.parser.AttDef;
import com.ibm.xml.parser.ElementDecl;
import com.ibm.xml.parser.InsertableElement;
import com.ibm.xml.parser.Util;
import com.ibm.xml.parser.EntityDecl;
import com.ibm.xml.parser.MIME2Java;
import com.ibm.xml.parser.Visitor;
import com.ibm.xml.parser.NonRecursivePreorderTreeTraversal;
import fri.util.sort.quick.QSort;
import fri.util.sort.quick.Comparator;
import fri.util.xml.DOMUtil;

/**
 * XML4J dependent class. Document and document-type utilities.
 */
public class DTDUtil
{
	public static final int INDENT_SPACES = 2;
	
	private DTD dtd;
	private TXDocument doc;
	private Hashtable tableCache = new Hashtable();
	private boolean hasDTD;
	private byte [] byteOrderMark;


	/**
		Create a utility for a given DTD and a document.
		Argument hasDTD is false if a default DTD was created.
	*/
	public DTDUtil(DTD dtd, TXDocument doc, boolean hasDTD, byte [] byteOrderMark)	{
		this.dtd = dtd;
		this.doc = doc;
		this.hasDTD = hasDTD;
		this.byteOrderMark = byteOrderMark;
	}


	/** Returns TXDocument of this DTDUtil. */
	public TXDocument getDocument()	{
		return doc;
	}

	/** Returns the DTD of this document. */
	public Node getDTD()	{
		return dtd;
	}


	/**
		Returns true if the element has nothing but text nodes as children.
	*/
	public boolean isLeaf(String tag)	{
		if (isElementDeclaredAsANY(tag))
			return false;
			
		Vector v = getInsertableElements(tag);
		
		for (int i = 0; v != null && i < v.size(); i++)  {
			String s = (String)v.elementAt(i);
			
			if (s.equals(DTD.CM_PCDATA) == false)	{
				return false;
			}
		}
		
		return true;
	}


	/**
		Returns the String list of insertable elements for passed tag.
	*/
	public Vector getInsertableElements(String tag)	{
		return dtd.makeContentElementList(tag);
	}


	/**
		Returns the String list of insertable elements for an element with ANY content.
	*/
	public Vector getANYInsertableElements()	{
		Vector v = new Vector();
		v.add(getPCDATATagName());	// add PCDATA for ANY content

		Enumeration e = dtd.getElementDeclarations();

		// add all elements except root
		while (e.hasMoreElements())	{
			ElementDecl ed = (ElementDecl)e.nextElement();
			String name = ed.getName();
			if (name.equals(dtd.getName()) == false)
				v.add(name);
		}

		return v;
	}

	/**
		Returns true if the element for passed tag is declared as ANY.
	*/
	public boolean isElementDeclaredAsANY(String tag)	{
		int type = dtd.getContentType(tag);
		return type == ElementDecl.ANY || type == -1;	// -1: not declared at all
	}

	/**
		Returns true if the element for passed tag is declared as EMPTY.
	*/
	public boolean isElementDeclaredAsEMPTY(String tag)	{
		int type = dtd.getContentType(tag);
		return type == ElementDecl.EMPTY;
	}

	/**
		Returns true if the element for passed tag has PCDATA children (mixed or text element).
	*/
	public boolean hasElementPCDATA(String tag)	{
		Vector v = getInsertableElements(tag);

		for (int i = 0; v != null && i < v.size(); i++)	{
			String child = (String)v.elementAt(i);
			
			if (child.equals(DTD.CM_PCDATA))
				return true;
		}
		
		return false;
	}


	/**
		Returns a list of attributes as Strings.
	*/
	public Vector getAttributeDeclarations(String tag)	{
		Enumeration e = dtd.getAttributeDeclarations(tag);
		Vector v = null;
		while (e.hasMoreElements())	{
			AttDef att = (AttDef)e.nextElement();
			if (v == null)
				v = new Vector();
			v.add(att.getName());
		}
		return v;
	}


	/**
		Determine if the passed tag is insertable into passed node at passed position.
		This method assumes that only element nodes are displayed when countOnlyElements is true.
		Returns both visual (x == pos) and DOM position (y) within the node.
		@param node container for the tag to be inserted
		@param tag name of new node
		@param pos visual position where new node is to be inserted
		@return Point where x is the viaual position (pos) and y the node position
			(do <i>element.insert()</i> with that position).
	*/
	public Point isElementInsertable(Element node, String tag, final int visualPos, short [] configuredNodeTypes)	{
		Hashtable table = getHashtable(node.getNodeName());
		int i = getNodePosition(node, visualPos, configuredNodeTypes);
		Point p = new Point(visualPos, i);
		boolean append = (node.getChildNodes().getLength() == p.y);
		Hashtable validTable;

		if (append)
			validTable = dtd.getInsertableElements(node, p.y, table);
		else
			validTable = dtd.getInsertableElementsForValidContent(node, p.y, table);

		//System.err.println("tag "+tag+", at visual/node pos "+p.x+"/"+p.y+", append = "+append);

		if (((InsertableElement)validTable.get(DTD.CM_ERROR)).status)	{
			if (append == false)	// do again without validation, else nothing will be insertable
				validTable = dtd.getInsertableElements(node, p.y, table);
		}

		InsertableElement insElem = (InsertableElement)validTable.get(tag);
		if (insElem != null && insElem.status)	{
			return p;
		}

		//System.err.print("... status error: "+insElem);
		return null;
	}

	private Hashtable getHashtable(String tag)	{
		// buffer the hashtables of an element
		Hashtable table = (Hashtable)tableCache.get(tag);
		if (table == null)	{
			table = dtd.prepareTable(tag);
			tableCache.put(tag, table);
		}
		return table;
	}

	/**
		Returns the node position for passed visual position when
		not all node types are displayed in treeview.
	*/
	public int getNodePosition(Node node, final int pos, short [] configuredNodeTypes)	{
		NodeList chldr = node.getChildNodes();
		int elemPos = 0, i = 0;
		int len = chldr.getLength();
		Node n;

		// look for ELEMENT node of given position, do not count disallowed nodes between
		for (; elemPos < pos && i < len; i++)	{
			n = chldr.item(i);

			if (inArray(n.getNodeType(), configuredNodeTypes))
				elemPos++;
		}

		// if found, read ahead until next allowed node (read away space-only nodes)
		if (pos > 0 && elemPos == pos && i < len)	{
			n = chldr.item(i);

			while (i < len && inArray(n.getNodeType(), configuredNodeTypes) == false)	{
				i++;	// increment if disallowed node

				if (i < len)	// read next node if not at end
					n = chldr.item(i);
			}
		}

		return i;
	}

	private boolean inArray(short nodeType, short [] configuredNodeTypes)	{
		for (int i = 0; i < configuredNodeTypes.length; i++)
			if (configuredNodeTypes[i] == nodeType)
				return true;
		return false;
	}


	/**
		Create a new element. No text child is created.
		Attributes with default values are appended.
		@return newly created node.
	*/
	public Element createElement(String tag)	{
		Element elem = getDocument().createElement(tag);

		// add attributes that have default values
		for (Enumeration en = dtd.getAttributeDeclarations(tag); en.hasMoreElements(); )	{
			AttDef att = (AttDef)en.nextElement();

			String s = att.getDefaultStringValue();
			if (s != null)
				elem.setAttribute(att.getName(), s);
			else
			if (att.getDefaultType() == AttDef.REQUIRED)
				System.err.println("WARNING: attribute is REQUIRED but has no default value: "+tag+"->"+att.getName());
		}

		return elem;
	}
	
	/**
		Create a new element, including all possible element children, recursively.
		@param tag tag of node to create
		@return newly created element node.
	*/
	public Element createElementRecursive(String tag)	{
		return createElementRecursive(tag, new Hashtable());
	}

	private Element createElementRecursive(String tag, Hashtable done)	{
		if (done.get(tag) != null)
			return null;
		done.put(tag, tag);
			
		Element parent = createElement(tag);
		Vector v = getInsertableElements(tag);

		for (int i = 0; v != null && i < v.size(); i++)	{
			String childTag = (String)v.elementAt(i);

			if (childTag.equals(DTD.CM_PCDATA) == false)	{
				Element child = createElementRecursive(childTag, done);
				if (child != null)
					parent.appendChild(child);
			}
		}

		return parent;
	}


	/**
		Returns a list of String holding all entity names of this DTD,
	*/
	public Vector getEntityNames()	{
		Vector v = null;
		DocumentType docType = getDocument().getDoctype();
		NamedNodeMap entities = null;

		try	{	// workaround xml4j bug: NullPointerException
			entities = docType != null ? docType.getEntities() : null;
		}
		catch (NullPointerException e)	{
		}

		if (entities == null || entities.getLength() <= 0)	{
			v = getEntitiesByImpl();
		}
		else	{
			for (int i = 0; entities != null && i < entities.getLength(); i++)	{
				if (v == null)
					v = new Vector(entities.getLength());
				
				Node e = (Node)entities.item(i);
				v.add(e.getNodeName());
			}
		}

		// sort case-sensitive to keep standard entities like "apos" at end of list
		Comparator comp = new Comparator()	{
			public boolean equals(Object o)	{
				return false;
			}
			public int compare(Object o1, Object o2)	{
				return o1.toString().compareTo(o2.toString());
			}
		};

		return new QSort(comp).sort(v);	// return sorted entitiy list
	}

	/** If some parser does not support DOM methods, this can be overridden to get entity list. */
	protected Vector getEntitiesByImpl()	{
		return null;
	}


	/**
		Returns the value for passed entity reference node,
		having the entity name in getNodeName().
	*/
	public String getEntityValue(Node node)	{
		String entity = node.getNodeName();
		DocumentType docType = getDocument().getDoctype();
		NamedNodeMap entities = docType != null ? docType.getEntities() : null;

		if (entities != null)	{
			Entity e = (Entity)entities.getNamedItem(entity);

			if (e != null)	{
				String s = e.getNodeValue();

				if (s == null)	// xml4j does not provide this correctly
					s = getEntityValueByImpl(entity);

				return s == null ? null : s.length() > 1 ? s.trim() : s;	// do NOT trim one space!
			}
		}

		return null;
	}

	/** If some parser does not support DOM methods, this can be overridden to get entity value. */
	protected String getEntityValueByImpl(String entity)	{
		EntityDecl e = dtd.getEntityDecl(entity, false);
		if (e == null)
			return null;

		String s = e.getValue();

		if (s == null)	{
			try	{
				if (e.getPublicId() != null && e.getPublicId().length() > 0)
					s = "PUBLIC "+e.getPublicId();
			}
			catch (NullPointerException ex)	{
			}

			try	{
				if (e.getSystemId() != null && e.getSystemId().length() > 0)
					s = (s == null ? "SYSTEM " : s+" ")+e.getSystemId();
			}
			catch (NullPointerException ex)	{
			}
		}

		return s;
	}



	/** Returns the tag name for comments in this DOM implementation: "#comment". */
	public String getCommentTagName()	{
		return Child.NAME_COMMENT;
	}

	/** Returns the tag name for CDATA in this DOM implementation: "#cdata-section". */
	public String getCDATATagName()	{
		return Child.NAME_CDATA;
	}

	/** Returns the tag name for PCDATA in this DOM implementation: "#PCDATA". */
	public String getPCDATATagName()	{
		return DTD.CM_PCDATA;
	}


	/** Identifies the tag name for PCDATA in this DOM implementation: "#PCDATA" or "#text". */
	public boolean isPCDATA(String tag)	{
		return tag.equals(getPCDATATagName()) || tag.equals(Child.NAME_TEXT);
	}

	/** Identifies the tag name for PCDATA in this DOM implementation: "#cdata-section". */
	public boolean isCDATA(String tag)	{
		return tag.equals(getCDATATagName());
	}

	/** Identifies the tag name for comments in this DOM implementation: "#comment". */
	public boolean isComment(String tag)	{
		return tag.equals(getCommentTagName());
	}

	/**
		Create Text-, Comment- and CDATA-nodes.
		@param tag #PCDATA, #CDATA, #COMMENT 
		@return newly created empty node with appropriate type
	*/
	public Node createPrimitiveNode(String tag)	{
		if (tag.equals(getPCDATATagName()))	{
			return getDocument().createTextNode("");
		}
		else
		if (tag.equals(getCommentTagName()))	{
			return getDocument().createComment("");
		}
		else
		if (tag.equals(getCDATATagName()))	{
			return getDocument().createCDATASection("");
		}
		else	{
			throw new IllegalArgumentException("Unknown node type: "+tag);
		}
	}


	/** Creates a processing instruction with given target and empty data. */
	public Node createPI(String target)	{
		return getDocument().createProcessingInstruction(target, "");
	}

	/** Creates a entity reference with given name and empty data. */
	public Node createEntityReference(String entityName)	{
		return getDocument().createEntityReference(entityName);
	}

	/** Returns true if any attribute is defined in DTD. */
	public boolean hasAnyAttributes()	{
		Enumeration e1 = dtd.getElementDeclarations();

		while (e1 != null && e1.hasMoreElements())	{
			ElementDecl ed = (ElementDecl)e1.nextElement();
			String tag = ed.getName();

			Enumeration e2 = dtd.getAttributeDeclarations(tag);
			if (e2 != null && e2.hasMoreElements())	{
				return true;
			}
		}
		return false;
	}



	/** Document utility: returns the string representation of version in prolog. */
	public String getVersion()	{
		return getDocument().getVersion();
	}

	/** Document utility: sets the string representation of version in prolog. */
	public void setVersion(String value)	{
		getDocument().setVersion(value);
	}


	/** Document utility: returns the string representation of encoding in prolog. */
	public String getEncoding()	{
		return getDocument().getEncoding();
	}

	/** Document utility: sets the string representation of encoding in prolog. */
	public void setEncoding(String value)	{
		getDocument().setEncoding(value);
	}


	/** Document utility: returns the string representation of standalone property in prolog. */
	public String getStandalone()	{
		return getDocument().getStandalone();
	}

	/** Document utility: sets the string representation of standalone property in prolog. */
	public void setStandalone(String value)	{
		getDocument().setStandalone(value);
	}



	/** When a default DTD was created, it must be removed before saving. */
	private void removeDTD()	{
		System.err.println("removing DTD from document ...");
		DefaultDTD.removeDTD(getDocument());
	}

	/** When a default DTD was created, it must be added again after saving. */
	private void addDTD()	{
		System.err.println("... adding DTD to document");
		DefaultDTD.addDTD(dtd, getDocument());
	}


	/** Checks if the passed name is a valid target name for a processing instruction. */
	public boolean checkPITarget(String target)	{
		return target != null && Util.checkName(target) && target.toLowerCase().equals("xml") == false;
	}



	/**
		Prints the document to passed OutputStream as HTML.
	*/
	public void printAsHtml(OutputStream out)
		throws Exception
	{
		print(false, out, false, true);
	}
	
	/**
		Prints the document to passed OutputStream. Closes the stream.
		If out is null, System.out is used.
		If withDefaultDTD is false, a default DTD will be removed and
		added again after printing.
	*/
	public void print(boolean withByteOrderMark, OutputStream out, boolean withDefaultDTD)
		throws Exception
	{
		print(withByteOrderMark, out, withDefaultDTD, false);
	}
	
	private void print(boolean withByteOrderMark, OutputStream out, boolean withDefaultDTD, boolean asHtml)
		throws Exception
	{
		boolean removedDTD = false;

		if (withDefaultDTD == false && hasDTD == false)	{	// temporary remove DTD
			removeDTD();
			removedDTD = true;
		}

		try	{
			if (withByteOrderMark && byteOrderMark != null)
				for (int i = 0; i < byteOrderMark.length; i++)
					out.write(byteOrderMark[i]);
			
			print(getDocument(), out, asHtml);
		}
		finally	{
			if (removedDTD)	{	// add temporary removed DTD
				addDTD();
			}
		}
	}

	/**
		Prints the passed node to passed OutputStream. Closes the stream.
		If out is null, System.out is used.
	*/
	public void print(Node node, OutputStream out)
		throws Exception
	{
		print(node, out, false);
	}
	
	/**
		Prints the passed node to passed OutputStream. Closes the stream.
		If out is null, System.out is used.
	*/
	public void print(Node node, OutputStream out, boolean asHtml)
		throws Exception
	{
		if (out == null)
			out = System.out;

		PrintWriter pw = null;
		try	{
			pw = getPrintWriter(out);
			
			Visitor visitor = asHtml
					? (Visitor)new HtmlPrintVisitor(pw, getPrintEncoding())
					: (Visitor)new FormatPrintVisitorFix(pw, getPrintEncoding(), INDENT_SPACES);
					
			new NonRecursivePreorderTreeTraversal(visitor).traverse(node);
		}
		finally	{
			try	{ pw.close(); }	catch (Exception ex)	{}
		}
	}

	/**
		Returns an appropriate PrintWriter for the passed OutputStream.
	*/
	private PrintWriter getPrintWriter(OutputStream out)
		throws UnsupportedEncodingException
	{
		String charset = getPrintEncoding();
		String jencode = MIME2Java.convert(charset);
		return new PrintWriter(new OutputStreamWriter(out, jencode));
	}

	private String getPrintEncoding()	{
		return getEncoding() == null || getEncoding().length() <= 0 ? "UTF-8" : getEncoding();
	}


	/**
		Creates a Node and all its subnodes (typed for this document) from
		passed unknown Node. This method is needed when drag and drop receives
		a Node from another XML environment.
	*/
	public Node createTypedW3CNode(Node node)	{
		Node typedNode;
		short type = node.getNodeType();

		switch (type)	{
			case Node.ELEMENT_NODE:
				typedNode = getDocument().createElement(node.getNodeName());
				if (node.getNodeValue() != null)
					typedNode.setNodeValue(node.getNodeValue());

				NamedNodeMap attributes = node.getAttributes();

				for (int i = 0; attributes != null && i < attributes.getLength(); i++)	{
					Attr attr = (Attr)attributes.item(i);
					((Element)typedNode).setAttributeNode((Attr)createTypedW3CNode(attr));
				}
			break;

			case Node.ATTRIBUTE_NODE:
				typedNode = getDocument().createAttribute(node.getNodeName());
				typedNode.setNodeValue(node.getNodeValue());
			break;

			case Node.DOCUMENT_FRAGMENT_NODE:
				typedNode = getDocument().createDocumentFragment();
			break;

			case Node.TEXT_NODE:
				return getDocument().createTextNode(node.getNodeValue());

			case Node.CDATA_SECTION_NODE:
				return getDocument().createCDATASection(node.getNodeValue());

			case Node.COMMENT_NODE:
				return getDocument().createComment(node.getNodeValue());

			case Node.ENTITY_REFERENCE_NODE:
				return getDocument().createEntityReference(node.getNodeName());

			case Node.PROCESSING_INSTRUCTION_NODE:
				return getDocument().createProcessingInstruction(node.getNodeName(), node.getNodeValue());

			default:
				throw new UnsupportedOperationException("Node type not adaptable: "+DOMUtil.nodeType(node));
		}


		NodeList children = node.getChildNodes();

		for (int i = 0; children != null && i < children.getLength(); i++)	{
			Node child = children.item(i);
			typedNode.appendChild(createTypedW3CNode(child));
		}

		return typedNode;
	}

}