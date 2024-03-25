package fri.gui.swing.xmleditor.model;

import java.io.*;
import java.util.*;
import javax.swing.tree.*;
import org.w3c.dom.*;
import fri.util.Equals;
import fri.util.xml.*;
import fri.util.xml.xml4j.*;
import fri.util.props.PropertiesList;

/**
	The visual treeview element for a XML document.
	
	@author  Ritzberger Fritz
*/

public class XmlNode extends DefaultMutableTreeNode
{
	private final static short [] allNodeTypes = new short [] {
		Node.ELEMENT_NODE,
		Node.COMMENT_NODE,
		Node.TEXT_NODE,
		Node.ENTITY_REFERENCE_NODE,
		Node.CDATA_SECTION_NODE,
		Node.PROCESSING_INSTRUCTION_NODE,
		Node.DOCUMENT_NODE,
		Node.DOCUMENT_FRAGMENT_NODE,
		Node.DOCUMENT_TYPE_NODE,
	};

	private static Comparator comparator;	// global comparator object

	private boolean isLeaf;	// is this shown as leaf or container

	/** Buffers the node's text value. */
	private String longText;
	/** Flag that invalidates element text retrieval. */
	protected boolean longTextRetrieved;

	/** Buffers the node's list of attributes. */
	private PropertiesList attributesList;
	/** Flag that invalidates attributes list retrieval. */
	protected boolean attributesListRetrieved;

	private String uri;	// only valid in root
	private boolean uriChanged = false;	// only valid in root

	private transient Hashtable errorNodes;	// only valid in root: Node and Error-String tupels
	private transient DTDUtil dtdutil;	// only valid in root
	private boolean moreThanOneTextChild;	// true when element has more than one text child
	private short [] configuredNodeTypes;	// only valid in root

	private Hashtable tagMap;	// only valid in root
	private boolean isSearching;



	/**
		Construct the root node from an InputStream or an URI, which means
		parsing the document from the InputStream (when not null) or the URI.
		@param uri the identifier of the URI to parse
		@param input the document input stream,
			or null if DocumentBroker should open the URI
	*/
	public XmlNode(String uri, InputStream is, Configuration conf)
		throws Exception
	{
		super(null);	// userObject will be set later

		// first configure the XML node options for list()
		this.configuredNodeTypes = configureNodeTypes(conf.complexMode, conf.showComments, conf.showPIs);

		// open the document input stream
		if (is == null)
			is = DocumentBroker.getInstance(uri).getInputStream();

		// parse and build the XML-DOM in memory
		ParserWrapper parser = new ParserWrapper(
				is,
				uri,
				primitiveNodesAreConfigured(),
				conf.validate,
				conf.expandEntities);

		this.dtdutil = parser.getDTDUtil();	// get DOM implementation wrapper

		// set the document as user object
		setUserObject(dtdutil.getDocument());

		this.errorNodes = parser.getErrorNodes();	// get error nodes table
		this.uri = parser.getURI();	// when a XXX.dtd has been read, XXX.xml was generated
		System.err.println("Got URI from parser wrapper in root "+hashCode()+": "+this.uri);
		if (uri.equals(this.uri) == false)	{
			this.uriChanged = true;	// the file could exist, confirm when saving
		}

		this.isLeaf = false;	// root is always container

		// look for optional mapping of tag substitution attribute name
		this.tagMap = conf.getTagMapForRootTag(getRootTag());
	}


	/** Construct a new TreeNode from a W3C Node. This is used in <i>list()</i> method. */
	protected XmlNode(Node n)	{
		super(n);
	}


	/**
		Factory method to build a normal node for the tree, used in list() method.
		To be overridden if other XMLNode subclasses are wanted.
		@param node the Element that this XmlNode represents
	*/
	protected XmlNode createXmlNode(Node node)	{
		return new XmlNode(node);
	}


	/** Returns the w3c Node of this XmlNode. Public for DragAndDropHandler. */
	public Node getW3CNode()	{
		return (Node)getUserObject();
	}

	/** Returns the (type-casted) XML root node. */
	protected XmlNode getXmlRoot()	{
		return (XmlNode)getRoot();
	}

	/** Returns the DTD util, retrieved from root node. */
	protected DTDUtil getDTDUtil()	{
		XmlNode root = getXmlRoot();
		if (root == null)
			return null;
		return root.dtdutil;
	}

	/** Returns the Document that is represented by root XmlNode. */
	public Document getDocument()	{
		return getDTDUtil().getDocument();
	}

	/** Returns the type of this tree node: Node.ELEMENT_NODE, Node.COMMENT_NODE; ... */
	public short getNodeType()	{
		return getW3CNode().getNodeType();
	}

	/** Returns name of XML tag of this node. */
	public String getTagName()	{
		return getW3CNode().getNodeName();
	}	


	
	/**
		Sets the leaf property to true when the DTD does not allow child elements
		and the number of non-empty text nodes is zero or one.
	*/
	protected void init()	{
		isLeaf = getDTDUtil().isLeaf(getTagName());	// true when no other elements contained
		//System.err.println("DTDUtil says node \""+this+"\" is leaf: "+isLeaf);
		
		// avoid to hide nodes when there are actually more than one text child
		if (showAllSubNodes())	{
			if (getNodeType() == Node.ELEMENT_NODE && getDTDUtil().isElementDeclaredAsEMPTY(getTagName()) == false)
				isLeaf = false;
			
			// remove nodes that hold just spaces
			if (isLeaf == false)
				DOMUtil.removeEmptyTextNodes(getW3CNode());
		}
		else	// ensure that nothing is hidden
		if (DOMUtil.canShowLeadingChildrenInElement(getW3CNode()) == false)	{
			isLeaf = false;
			// happens even for containers, when mixed content is present
			// but when all primitive children are before first child element, false
			moreThanOneTextChild = true;
		}
	}


	/**
		Implements Fillable TreeNode:
		Lists the Node and creates visible child tree nodes from children.
	*/
	public void list()	{
		if (children == null)	{
			children = new Vector();
			
			List v = DOMUtil.getChildList(getW3CNode(), getConfiguredNodeTypes());

			for (int i = 0; v != null && i < v.size(); i++)	{
				Node n = (Node) v.get(i);

				XmlNode xn = createXmlNode(n);
				xn.setParent(this);
				children.add(xn);
			}
		}
	}


	/** Overridden to ensure the node is inited as soon as it has a parent (has a DTD). */
	public void setParent(MutableTreeNode parent)	{
		//System.err.println("  setting parent in "+this+", parent is "+parent);
		super.setParent(parent);	// makes DTD available

		if (parent != null)	{	// setting parent is done by list() or insert()
			init();
		}
		else	{	// set parent to null in XML Node: supersedes remove() implementation
			Node pnt = getW3CNode().getParentNode();

			if (getW3CNode().getParentNode() != null)	{
				pnt.removeChild(getW3CNode());

				if (getW3CNode().getParentNode() != null)
					Thread.dumpStack();
			}
		}
	}
	
	
	/** TreeModel service method: Returns number of children. */
	public int getChildCount() {
		list();
		return super.getChildCount();
	}

	/** TreeModel service method: Returns false if this is a leaf node. */
	public boolean getAllowsChildren() {
		return isRoot() || isLeaf() == false;
	}

	/** TreeModel service method: Returns true if this is a leaf node. */
	public boolean isLeaf() {
		return isLeaf;
	}


	/**
		Returns the tag name, or a replacing attribute value,
		rendered in the tree column of treetable,
	*/
	public String toString()	{
		String tagName = getTagName();

		if (getNodeType() == Node.ELEMENT_NODE)	{
			String s = getTagReplacingAttributeName();

			if (s != null)	{
				s = getNamedAttribute(s);
				if (s != null)
					return s;
			}
		}

		return tagName;
	}



	/** Always returns false: long text of this element is not editable. */
	public boolean isEditable()	{
		return false;
	}

	/** Always returns false: no element can be moved. */
	public boolean isManipulable()	{
		return false;
	}


	// begin interface to TreeTableModel

	/** TreeTable service method to get a display value from a XML node */
	public Object getColumnObject(int col) {
		switch (col)	{
			case XmlTreeTableModel.TAG_COLUMN:
				return toString();

			case XmlTreeTableModel.ATTRIBUTES_COLUMN:
				return ensureAttributeList();

			case XmlTreeTableModel.LONGTEXT_COLUMN:
				boolean show = isEditable() || getLongText() != null && getLongText().trim().length() > 0;
				return show ? getLongText() : "";

			default:
				throw new IllegalArgumentException("column does not exist: "+col);
		}
	}

	// end interface to TreeTableModel

	/**
		If all textnode are shown, the tree is very complex.
		For simple documents there is the option to configure
		TEXT_NODE not to be shown, then this method returns false
		with the effect that text nodes are shown in the long-text
		field of their element. This will be ignored for nodes that
		actually hold more than one child text nodes (DOMUtil.isLeaf()),
		but then one will not be able to create additional text nodes.

		@return true if actually there are more than one text child, or
			PCDATA, CDATA and Entity-reference nodes
			are configured to be displayed.
	*/
	protected boolean showAllSubNodes()	{
		return
			moreThanOneTextChild ||	// do not hide existing nodes even in simple mode
			primitiveNodesAreConfigured();	// show when configured
	}

	private boolean primitiveNodesAreConfigured()	{
		return
			isConfiguredNodeType(Node.TEXT_NODE) ||
			isConfiguredNodeType(Node.CDATA_SECTION_NODE) ||
			isConfiguredNodeType(Node.ENTITY_REFERENCE_NODE);
	}


	/**
		Returns text from the XML Node. Delegates to DOMUtil.
		If showAllTextNodes() is true, the text of an element
		is shown in the element itself.
	*/
	protected String getLongText()	{
		if (longTextRetrieved == false)	{
			longTextRetrieved = true;
			
			if (showAllSubNodes() == false && getNodeType() == Node.ELEMENT_NODE)
				if (isLeaf())
					longText = DOMUtil.getElementTextNodeValue((Element)getW3CNode());
				else
					longText = DOMUtil.getLeadingChildrenValue((Element)getW3CNode());
			else
			if (getNodeType() == Node.ENTITY_REFERENCE_NODE)
				longText = getDTDUtil().getEntityValue(getW3CNode());
			else
				longText = DOMUtil.getNodeValue(getW3CNode());
		}
		return longText;
	}


	/** Returns value text of the passed attribute of this node. */
	protected String getNamedAttribute(String name)	{
		return DOMUtil.getAttributeText((Element)getW3CNode(), name);
	}


	private List ensureAttributeList()	{
		if (attributesListRetrieved == false)	{
			attributesListRetrieved = true;

			if (getNodeType() == Node.ELEMENT_NODE || getNodeType() == Node.DOCUMENT_NODE)	{
				attributesList = (getNodeType() == Node.DOCUMENT_NODE)
						? buildAttributeListForProlog()
						: buildAttributeList();
			}
		}
		
		return attributesList;
	}

	/** Sets this node into searching mode - no attributes are hidden by root.tagMap. */
	public void setSearchMode(boolean searching)	{
		attributesListRetrieved = false;
		getXmlRoot().isSearching = searching;
	}

	private PropertiesList buildAttributeList()	{
		PropertiesList attributesList = null;

		// Render ALL attributes, even those without value
		Vector attrs = getNodeType() == Node.ELEMENT_NODE ?
				getDTDUtil().getAttributeDeclarations(getTagName())
				: null;
				
		// There could be a DTD that does not define ELEMENT but only ENTITY declarations
		if (attrs == null)	{
			NamedNodeMap attrList = getW3CNode().getAttributes();
			for (int i = 0; attrList != null && i < attrList.getLength(); i++)	{
				if (attrs == null)
					attrs = new Vector(attrList.getLength());
				attrs.add(attrList.item(i).getNodeName());
			}
		}

		if (attrs != null)	{
			Vector names = new Vector();
			Vector values = new Vector();

			// look for a tag-replacing attribute in this tag, do not add this to attributes
			String tagReplace = getTagReplacingAttributeName();
			
			for (int i = 0; i < attrs.size(); i++)	{	// loop attributes
				String name = (String)attrs.get(i);
				String value = getNamedAttribute(name);
				if (value == null)
					value = "";

				// do not take a tag-replacing attribute into list
				if (tagReplace == null || tagReplace.equals(name) == false)	{
					names.add(name);
					values.add(value);
				}
			}
			
			attributesList = new PropertiesList(names, values);
		}

		return attributesList;
	}

	private PropertiesList buildAttributeListForProlog()	{
		Vector names = new Vector();
		Vector values = new Vector();
		names.add("version");
		String v = getDTDUtil().getVersion();
		values.add(v != null ? v : "");
		names.add("encoding");
		String e = getDTDUtil().getEncoding();
		values.add(e != null ? e : "");
		names.add("standalone");
		String s = getDTDUtil().getStandalone();
		values.add(s != null ? s : "");
		System.err.println("Document version="+v+" encoding="+e+" standalone="+s);

		PropertiesList attributesList = new PropertiesList(names, values);

		return attributesList;
	}


	/**
		Returns number of attributes in this node.
	*/
	public int getAttributesCount()	{
		List l = ensureAttributeList();

		if (l != null)
			return l.size();

		return -1;
	}


	/**
		Returns true if one can save the document without overwrite accident
		(checked by DocumentBroker).
	*/
	public boolean canSaveWithoutOverwrite()	{
		if (uriChanged == false)
			return true;

		try	{
			return DocumentBroker.getInstance(getXmlRoot().uri).canSaveWithoutOverwriteCheck();
		}
		catch (Exception e)	{
			e.printStackTrace();
			return false;
		}
	}

	/** Saving the document to passed URL (done by DocumentBroker). */
	public void save()
		throws Exception
	{
		OutputStream out = null;
		try	{
			out = DocumentBroker.getInstance(getXmlRoot().uri).getOutputStream();
			uriChanged = false;	// the file was created, must not be confirmed anymore
			getDTDUtil().print(true, out, false);
		}
		finally	{
			try	{ out.close(); }	catch (Exception e)	{}
		}
	}


	/**
		Returns the DTD as XML text.
	*/
	public String getDTDAsString()
		throws Exception
	{
		OutputStream out = new ByteArrayOutputStream();
		getDTDUtil().print(getDTDUtil().getDTD(), out);
		return out.toString();
	}

	/**
		Returns the whole document as XML text.
		If there has been a default DTD constructed, it will not be contained when
		argument <i>withDefaultDTD</i> is false.
	*/
	public String getDocumentAsString(boolean withDefaultDTD)
		throws Exception
	{
		OutputStream out = new ByteArrayOutputStream();
		getDTDUtil().print(false, out, withDefaultDTD);
		return out.toString();
	}

	/**
		Returns the whole document as HTML, without DTD.
	*/
	public String getDocumentAsHtml()
		throws Exception
	{
		OutputStream out = new ByteArrayOutputStream();
		getDTDUtil().printAsHtml(out);
		return out.toString();
	}



	/** Returns the current URI of this Document. */
	public String getURI()	{
		String uri = getXmlRoot().uri;
		//System.err.println("Returning URI from root "+hashCode()+": "+uri);
		return uri;
	}

	/** Sets the current URI of this Document (does not save). */
	public void setURI(String uri)	{
		String thisUri = getXmlRoot().uri;

		if (Equals.equals(thisUri, uri) == false)	{
			getXmlRoot().uri = uri;
			uriChanged = true;
		}
	}


	/** Returns error message if this node had errors when root was constructed. */
	public String getError()	{
		if (getXmlRoot().errorNodes != null)
			return (String)getXmlRoot().errorNodes.get(getW3CNode());
		return null;
	}


	/** Returns true if errors occured when root was constructed. */
	public boolean hasDocumentErrors()	{
		return getXmlRoot().errorNodes != null && getXmlRoot().errorNodes.size() > 0;
	}

	/** Resets errors. */
	public void resetDocumentErrors()	{
		getXmlRoot().errorNodes = null;
	}


	/** Returns the root tag of the document. */
	public String getRootTag()	{
		return firstValidElement(getXmlRoot()).getTagName();
	}

	/** Returns the passed node, or the root-element if given node is the document node. */
	protected XmlNode firstValidElement(XmlNode n)	{
		if (n.getNodeType() == Node.DOCUMENT_NODE)	{	// if root, find the single document element
			for (int i = 0; i < n.getChildCount(); i++)	{
				XmlNode ch = (XmlNode)n.getChildAt(i);
				if (ch.getNodeType() == Node.ELEMENT_NODE)
					return ch;
			}
		}
		return n;
	}


	/**
		Return true if there is at least one attribute definition in DTD.
		This is for collapsing attribute column if false.
	*/
	public boolean hasAnyAttributes()	{
		return getDTDUtil().hasAnyAttributes();
	}

	/**
		Return true if there is at least one non-empty text in one element.
		This is for collapsing text column if false.
	*/
	public boolean hasAnyTexts()	{
		if (firstValidElement(getXmlRoot()).getChildCount() <= 0)
			return true;	// make no assumptions if empty document
		return DOMUtil.hasAnyTexts(getW3CNode());
	}


	/** Returns a display string for this node type, or error message if error node. */
	public String getToolTipText()	{
		String tt = getToolTipTextForType();

		String s = getTagReplacingAttributeName();
		if (s != null)
			tt = tt+", Attribute \""+s+"\"";

		return tt;
	}

	private String getToolTipTextForType()	{
		String s = getError();
		if (s != null)
			return s;

		switch (getNodeType())	{
			case Node.TEXT_NODE: return "Text";
			case Node.ELEMENT_NODE: return "Element"+" \""+getTagName()+"\"";
			case Node.COMMENT_NODE: return "Comment";
			case Node.CDATA_SECTION_NODE: return "CDATA Section";
			case Node.DOCUMENT_NODE: return "Document";
			case Node.DOCUMENT_FRAGMENT_NODE: return "Document Fragment";
			case Node.ENTITY_REFERENCE_NODE: return "Entity Reference";
			case Node.PROCESSING_INSTRUCTION_NODE: return "Processing Instruction \""+getTagName()+"\"";
			case Node.DOCUMENT_TYPE_NODE: return "Document Type";
			default: return null;
		}
	}


	/**
		Returns a Comparator that can compare two XmlNodes.
		This implementation delegates to DTDUtil.
	*/
	public Comparator getNodeComparator()	{
		if (comparator == null)	{
			comparator = new NodeComparator();
		}
		return comparator;
	}


	/**
		Creates a Node typed for this parser from passed unknown Node.
		This is needed when drag&drop receives a Node from another
		XML environment. Delegates to DTDUtil.
	*/
	public Node createTypedW3CNode(Node node)	{
		return getDTDUtil().createTypedW3CNode(node);
	}



	/**
		Returns the configured types, except when in simple mode and
		there were more than one text children, then all node types are returned.
	*/
	protected short [] getConfiguredNodeTypes()	{
		short [] nodeTypes = moreThanOneTextChild ? allNodeTypes : getXmlRoot().configuredNodeTypes;
		return nodeTypes;
	}

	
	/** Returns true if the passed value is in list of configured node types. */
	protected boolean isConfiguredNodeType(short type)	{
		short [] nodeTypes = getConfiguredNodeTypes();
		if (nodeTypes == null)	// java.lang.NullPointerException
			return true;
		for (int i = 0; i < nodeTypes.length; i++)
			if (nodeTypes[i] == type)
				return true;
		return false;
	}
	
	/** Set the types of Nodes that are to be shown in tree. Default are all. */
	public void setConfiguredNodeTypes(short [] nodeTypes)	{
		getXmlRoot().configuredNodeTypes = nodeTypes;
	}

	private short [] configureNodeTypes(boolean complexMode, boolean showComments, boolean showPIs)	{
		if (complexMode)
			if (showComments)
				if (showPIs)
					return new short []	{
						Node.ELEMENT_NODE,
						Node.COMMENT_NODE,
						Node.TEXT_NODE,
						Node.ENTITY_REFERENCE_NODE,
						Node.CDATA_SECTION_NODE,
						Node.PROCESSING_INSTRUCTION_NODE,
					};
				else
					return new short []	{
						Node.ELEMENT_NODE,
						Node.COMMENT_NODE,
						Node.TEXT_NODE,
						Node.ENTITY_REFERENCE_NODE,
						Node.CDATA_SECTION_NODE,
					};
			else
				if (showPIs)
					return new short []	{
						Node.ELEMENT_NODE,
						Node.TEXT_NODE,
						Node.ENTITY_REFERENCE_NODE,
						Node.CDATA_SECTION_NODE,
						Node.PROCESSING_INSTRUCTION_NODE,
					};
				else
					return new short []	{
						Node.ELEMENT_NODE,
						Node.TEXT_NODE,
						Node.ENTITY_REFERENCE_NODE,
						Node.CDATA_SECTION_NODE,
					};
		else
			if (showComments)
				if (showPIs)
					return new short []	{
						Node.ELEMENT_NODE,
						Node.COMMENT_NODE,
						Node.PROCESSING_INSTRUCTION_NODE,
					};
				else
					return new short []	{
						Node.ELEMENT_NODE,
						Node.COMMENT_NODE,
					};
			else
				if (showPIs)
					return new short []	{
						Node.ELEMENT_NODE,
						Node.PROCESSING_INSTRUCTION_NODE,
					};
				else
					return new short []	{
						Node.ELEMENT_NODE,
					};
	}



	private Hashtable getTagMap()	{
		if (getXmlRoot().isSearching)
			return null;
		return getXmlRoot().tagMap;
	}

	/**
		Returns true if a tagmap processing instruction was found in this document.
		If true, the tree column could be editable.
	*/
	public boolean hasTagMap()	{
		return getTagMap() != null;
	}

	/**
		Returns the attribute name that replaces the tag in this node.
		or null if no replacement is configured.
	*/
	public String getTagReplacingAttributeName()	{
		if (getNodeType() != Node.ELEMENT_NODE)
			return null;

		Hashtable tagMap = getTagMap();
		if (tagMap == null)
			return null;
		String s = (String)tagMap.get(getTagName());	// try tag name
		if (s != null)
			return s;
		s = (String)tagMap.get("*");	// try wildcard
		if (s != null)
			return s;
		return null;
	}

}