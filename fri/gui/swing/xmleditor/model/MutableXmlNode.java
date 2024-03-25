package fri.gui.swing.xmleditor.model;

import java.io.InputStream;
import java.util.*;
import java.awt.Point;
import javax.swing.tree.*;
import org.w3c.dom.*;
import fri.util.xml.*;
import fri.util.xml.xml4j.*;
import fri.util.props.PropertiesList;

/**
	The editable visual treeview element for a XML document.
	
	@author  Ritzberger Fritz
*/

public class MutableXmlNode extends XmlNode implements Cloneable
{
	/** The menu item for inserting processing instructions. It must be passed to
		createInsertableNode() as "Processing Instruction:[UserInputName]". */
	public static final String PI_MENU_NAME = "Processing Instruction";
	private transient boolean movePending = false;	// was the node cutten
	private transient int insertPosition = -1;	// helper variable
	private transient Vector insertableTags;	// element spcific list
	private transient Vector simpleModePrimitives;	// valid only in root
	private transient Vector complexModePrimitives;	// element specific list
	private transient Vector entityNames;	// valid only in root
	private transient boolean fillWithEmptyElements;	// valid only in root
	/** Optional warning after update command. */
	public transient String warning;

	/**
		Construct the root node from an InputStream or an URI.
		@param uri the identifier of the URI to parse, not null
		@param input the document input stream,
			or null if DocumentBroker should open the URI
	*/
	public MutableXmlNode(String uri, InputStream is, Configuration configuration)
		throws Exception
	{
		super(uri, is, configuration);

		if (isConfiguredNodeType(Node.COMMENT_NODE))	{
			simpleModePrimitives = new Vector();
			simpleModePrimitives.add(getDTDUtil().getCommentTagName());
		}

		if (isConfiguredNodeType(Node.PROCESSING_INSTRUCTION_NODE))	{
			if (simpleModePrimitives == null)
				simpleModePrimitives = new Vector();
			simpleModePrimitives.add(PI_MENU_NAME);
		}

		entityNames = getDTDUtil().getEntityNames();

		if (fillWithEmptyElements = configuration.createAllTagsEmpty)	{
			fillWithEmptyElements();
		}
	}

	/* Construct a new node from a Node. */
	private MutableXmlNode(Node n)	{
		super(n);
	}


	/**
		Factory method to build a normal node for the tree.
		@param node the Element that this XmlNode represents
	*/
	protected XmlNode createXmlNode(Node node)	{
		return new MutableXmlNode(node);
	}


	
	/** Calls super.init() and retrieves a list of insertable elments if this is not a leaf. */
	protected void init()	{
		super.init();

		if (isLeaf() == false && insertableTags == null)	{
			// retrieve insertable element tag names from DTD, this will go out to edit menu
			DTDUtil dtdutil = getDTDUtil();
			List v = dtdutil.isElementDeclaredAsANY(getTagName())
					? dtdutil.getANYInsertableElements()
					: dtdutil.getInsertableElements(getTagName());

			insertableTags = new Vector(v != null ? v.size() : 0);

			for (int i = 0; v != null && i < v.size(); i++)  {	// loop all names
				String s = (String)v.get(i);
				
				if (dtdutil.isPCDATA(s) == false)	{	// element nodes
					insertableTags.addElement(s);
				}
				else
				// mixed content, allow PCDATA in complex-mode
				if (showAllSubNodes() && complexModePrimitives == null)	{
					if (isConfiguredNodeType(Node.TEXT_NODE))
						ensureComplexModePrimitives().add(getDTDUtil().getPCDATATagName());

					// where PCDATA are, CDATA can be, too
					if (isConfiguredNodeType(Node.CDATA_SECTION_NODE))
						ensureComplexModePrimitives().add(getDTDUtil().getCDATATagName());
				}
			}

			if (isConfiguredNodeType(Node.COMMENT_NODE))	{
				ensureComplexModePrimitives().add(getDTDUtil().getCommentTagName());
			}

			if (isConfiguredNodeType(Node.PROCESSING_INSTRUCTION_NODE))	{
				ensureComplexModePrimitives().add(PI_MENU_NAME);
			}
		}
	}


	private Vector ensureComplexModePrimitives()	{
		if (complexModePrimitives == null)
			complexModePrimitives = new Vector();
		return complexModePrimitives;
	}


	/** Change text of a tree node. If userObject is a Node, just call super(). */
	public void setUserObject(Object userObject) {
		this.warning = null;

		if (userObject instanceof String)	{
			String s = (String)userObject;

			if (showAllSubNodes() == false && getNodeType() == Node.ELEMENT_NODE)	{
				if (isLeaf())
					DOMUtil.setElementTextNodeValue((Element)getW3CNode(), getDTDUtil().getDocument(), s);
				else
					DOMUtil.setLeadingChildrenValue((Element)getW3CNode(), getDTDUtil().getDocument(), s);
			}
			else	{
				this.warning = DOMUtil.setNodeValue(getW3CNode(), s);
			}

			this.longTextRetrieved = false;	// invalidate
		}
		else	{
			super.setUserObject(userObject);
		}
	}


	// begin interface to TreeTableModel

	/** TreeTable service method to set a column value to a XML node. */
	public void setColumnObject(int col, Object o) {
		//System.err.println("setColumnObject for "+col+" with value "+o);
		//Thread.dumpStack();
		switch (col)	{
			case XmlTreeTableModel.TAG_COLUMN:
				// should happen only when tag map exists
				String attr = getTagReplacingAttributeName();
				if (attr != null)	{
					setNamedAttribute(attr, (String)o);
				}
				break;
				
			case XmlTreeTableModel.ATTRIBUTES_COLUMN:
				PropertiesList list = (PropertiesList)o;
				for (int i = 0; i < list.size(); i++)	{
					PropertiesList.Tuple t = (PropertiesList.Tuple)list.get(i);
					setNamedAttribute(t.name, t.value);
				}
				this.attributesListRetrieved = false;	// invalidate
				break;
				
			case XmlTreeTableModel.LONGTEXT_COLUMN:
				setUserObject(o);
				break;
				
			default:
				throw new IllegalArgumentException("column does not exist: "+col);
		}
	}

	// end interface to TreeTableModel


	private void setNamedAttribute(String name, String newText)	{
		System.err.println("setting named attribute "+name+ " to value "+newText);

		if (getNodeType() == Node.DOCUMENT_NODE)	{
			if (name.equals("version"))	{	// 1.0
				getDTDUtil().setVersion(newText);
			}
			else
			if (name.equals("encoding"))	{	// ISO-8859-1, ...
				getDTDUtil().setEncoding(newText);
			}
			else
			if (name.equals("standalone"))	{	// yes, no
				getDTDUtil().setStandalone(newText);
			}
			else	{
				throw new IllegalArgumentException("Prolog attributes in document node have an unknown attribute name: "+name);
			}
		}
		else	{
			DOMUtil.setAttributeText((Element)getW3CNode(), name, newText);
		}
	}



	/**
		Returns false if the node is DOCUMENT node, true if it is not ELEMENT,
		true if simple-mode and there are PCDATA in children or type is ANY.
		Needed to response to edit requests for longtext column editor.
	*/
	public boolean isEditable()	{
		if (getNodeType() == Node.DOCUMENT_NODE)
			return false;

		if (getNodeType() == Node.ENTITY_REFERENCE_NODE)
			return false;	// editable only in DTD

		if (getNodeType() != Node.ELEMENT_NODE)
			return true;

		if (showAllSubNodes() == false &&	// in simple mode and ...
				(getDTDUtil().hasElementPCDATA(getTagName()) ||	// ... has PCDATA or ...
				getDTDUtil().isElementDeclaredAsANY(getTagName())))	// ... is of type ANY
			return true;

		return false;
	}

	/**
		Returns true if the node is moveable/deleteable, i.e.
		it is not the root or its singular child element.
	*/
	public boolean isManipulable()	{
		if (getNodeType() == Node.DOCUMENT_NODE)
			return false;

		MutableXmlNode pnt = (MutableXmlNode)getParent();
		if (getNodeType() == Node.ELEMENT_NODE && pnt != null && pnt.getNodeType() == Node.DOCUMENT_NODE)
			return false;

		return true;
	}




	/** Service method that returns true when the node was cutten. */
	public boolean getMovePending()	{
		return movePending;
	}

	/** Service method that lets set the state of a node when it was cutten. */
	public void setMovePending(boolean movePending)	{
		this.movePending = movePending;
	}



	// insert methods

	/**
		Returns list of String tags insertable within this node (as children).
		This is rendered in insertion menu.
	*/
	public List getInsertableWithinTags()	{
		return insertableTags != null && insertableTags.size() > 0 ? insertableTags : null;
	}

	/**
		Returns list of primitive node types as Strings (#text, #cdata-section,
		#processing-instruction) that can be inserted as children.
		This is rendered in insertion menu.
	*/
	public List getPrimitiveNodeTypes()	{
		if (showAllSubNodes() == false)
			if (isLeaf())
				return null;
			else
				return ((MutableXmlNode)getXmlRoot()).simpleModePrimitives;

		return complexModePrimitives;
	}

	/**
		Returns list of entity names that can be inserted as children within this node.
		This is rendered in insertion menu.
	*/
	public List getEntityNames()	{
		if (complexModePrimitives != null && getDTDUtil() != null && complexModePrimitives.indexOf(getDTDUtil().getPCDATATagName()) >= 0)	{
			Vector v = ((MutableXmlNode)getXmlRoot()).entityNames;
			System.err.println("insertable entities are: "+v);
			return v;
		}

		return null;
	}



	/** Overridden to insert the Node of the inserted XmlNode into the Node of this one. */
	public void insert(MutableTreeNode newChild, int childIndex)
		throws IllegalArgumentException
	{
		System.err.println("MutableXmlNode insert in "+this+", child "+newChild+", to index "+childIndex);
		MutableXmlNode xn = (MutableXmlNode)newChild;
		Point p = isElementInsertable(xn, childIndex);
		System.err.println(" ... Point is "+p);
		if (p == null || p.x != childIndex)
			throw new IllegalArgumentException("Node "+newChild+" is not insertable at index "+childIndex+" within "+this);

		super.insert(newChild, childIndex);

		// remove the new child from its old parent
		Node elem = xn.getW3CNode();
		//System.err.println("    inserted XML Node is "+elem+" value "+elem.getNodeValue());
		Node parent = elem.getParentNode();
		if (parent != null)	// could be pasting of cutten node
			parent.removeChild(elem);

		// insert the new child at destination position
		if (p.y >= DOMUtil.getChildCount(getW3CNode()))	{
			getW3CNode().appendChild(elem);
		}
		else	{
			Node refChild = DOMUtil.getChildAt(getW3CNode(), p.y);
			System.err.println(" ... refChild is "+refChild.getNodeName()+" value "+refChild.getNodeValue());
			getW3CNode().insertBefore(elem, refChild);
		}
	}


	//FRi 2002-11-08: remove() is done by setParent()

	private Point isElementInsertable(MutableXmlNode node, int insertPos)	{
		if (node.isInsertableEverywhere())	{
			Point p = new Point(insertPos, insertPos);

			if (showAllSubNodes() == false)
				p.y = getDTDUtil().getNodePosition(getW3CNode(), insertPos, getConfiguredNodeTypes());

			return p;
		}

		return isElementInsertable(node.getInsertionTagName(), insertPos);
	}


	private boolean isEntityInsertionTag(String tag)	{
		return tag.startsWith("&") && tag.endsWith(";");
	}


	/** Returns the insertable tag, for entities &xxx; and for all others getTagName(). */
	public String getInsertionTagName()	{
		if (getNodeType() == Node.ENTITY_REFERENCE_NODE)
			return "&"+getTagName()+";";

		return getTagName();
	}


	/**
		Create a new XmlNode for insertion.
		Use getInsertPosition() to determine the position where to insert.
		@param tag name of element to insert
		@param fillWithEmtpyElements if true all sub-elements will be created empty
		@return newly created node
	*/
	public MutableXmlNode createInsertableNode(String tag)
		throws NotInsertableException
	{
		Node child;
		int pos = getChildCount();

		// check if it is an entity reference node
		if (isEntityInsertionTag(tag))	{
			String entity = tag.substring("&".length(), tag.length() - ";".length());
			child = getDTDUtil().createEntityReference(entity);
		}
		else	// check if it is a processing instruction node
		if (tag.startsWith(PI_MENU_NAME+":"))	{
			String piName = tag.substring((PI_MENU_NAME+":").length());
			child = getDTDUtil().createPI(piName);
		}
		else	// check if it is PCDATA or CDATA or comment
		if (getDTDUtil().isPCDATA(tag) ||
				getDTDUtil().isCDATA(tag) ||
				getDTDUtil().isComment(tag))
		{
			child = getDTDUtil().createPrimitiveNode(tag);
		}
		else	{	// must be some element tag
			pos = getInsertablePosition(tag);
			if (pos < 0)
				throw new NotInsertableException("Tag \""+tag+"\" can not be created within this node \""+getTagName()+"\".");

			if (((MutableXmlNode)getXmlRoot()).fillWithEmptyElements)	{
				child = getDTDUtil().createElementRecursive(tag);
			}
			else	{
				child = getDTDUtil().createElement(tag);
			}
		}

		MutableXmlNode xn = (MutableXmlNode)createXmlNode(child);
		xn.setInsertPosition(pos);
		
		return xn;
	}


	/**
		Clone a node when copy happens.
		Use getInsertPosition() to determine the index where to insert the cloned tree node.
	*/
	public XmlNode cloneNode(boolean deep)	{
		Node n = getW3CNode().cloneNode(deep);

		MutableXmlNode xn = (MutableXmlNode)createXmlNode(n);

		int idx = getParent().getIndex(this);
		xn.setInsertPosition(idx + 1);

		return xn;
	}


	/** Returns a deep clone (with all children). */
	public Object clone()	{
		return cloneNode(true);
	}



	/**
		These nodes will be insertable everywhere: comment, processing instruction
	*/
	public boolean isInsertableEverywhere()	{
		return getNodeType() == Node.COMMENT_NODE || getNodeType() == Node.PROCESSING_INSTRUCTION_NODE;
	}

	/**
		Returns the insertable view position in a folder.
		@param tag XML Tag of element type to insert
		@return -1 if element not insertable, else insert position, 0-n
	*/
	public int getInsertablePosition(String tag)	{
		Point p = isElementInsertable(tag);
		//System.err.println("... element "+tag+" is insertable in "+getTagName()+" at point "+p);
		return p == null ? -1 : p.x;
	}

	/**
		Returns the real position where the passed tag is insertable into this element.
		This is called when an element is created.
		@param tag name to be inserted
		@desiredPosition position of selected or mouse-over item
		@return position where the tag can be inserted, or -1 if not at all insrtable
	*/
	public int getRealInsertPosition(String tag, int desiredPosition)	{
		Point p = isElementInsertable(tag, desiredPosition);
		//System.err.println("... element "+tag+" is insertable in "+getTagName()+" for desired position "+desiredPosition+" at point "+p);
		return p == null ? -1 : p.x;
	}

	/**
		Check if node is insertable into this one.
		Returns the biggest possible insert position.
		For mixed content this will always return the last child position.
		@return index pair (x = visual position, y = xml node position)
			if the node with the passed tag-name can
			be inserted at position into this one, else null.
	*/
	private Point isElementInsertable(String tag)	{
		int cnt = getChildCount();	// ensure children are read
		return isElementInsertable(tag, cnt, false);
	}

	/**
		Check if node is insertable into this one at specified position.
		This is called to calculate the DOM-Node position when an element is created.
		@return index pair (x = visual position, y = xml node position)
			if the node with the passed tag-name can
			be inserted at the passed position into this one, else null.
	*/
	private Point isElementInsertable(String tag, int insertPos)	{
		return isElementInsertable(tag, insertPos, true);
	}


	private Point isElementInsertable(String tag, final int insertPos, final boolean posValid)	{
		//System.err.println("isElementInsertable, tag "+tag+", insertPos "+insertPos+" posValid "+posValid);

		if (canHoldChildren() == false || getDTDUtil() == null || getDTDUtil().isElementDeclaredAsEMPTY(getTagName()))	{
			//System.err.println("... can not hold children or is EMPTY");
			return null;
		}

		DTDUtil dtdutil = getDTDUtil();
		Point p = null;

		boolean isPrimitive =
				isEntityInsertionTag(tag) ||
				dtdutil.isCDATA(tag) ||
				dtdutil.isPCDATA(tag);

		// allow comments, or anything in content type ANY, or PCDATA when element has mixed content
		if (isPrimitive || dtdutil.isComment(tag) || dtdutil.isElementDeclaredAsANY(getTagName()))	{
			if (isPrimitive && dtdutil.hasElementPCDATA(getTagName()) == false)	// no mixed content
				return null;

			p = new Point(insertPos, insertPos);

			if (showAllSubNodes() == false)	// simple mode
				p.y = getDTDUtil().getNodePosition(getW3CNode(), insertPos, getConfiguredNodeTypes());

			return p;
		}

		if (posValid)	{
			p = dtdutil.isElementInsertable((Element)getW3CNode(), tag, insertPos, getConfiguredNodeTypes());
		}
		else	{
			// find the biggest insert position (always append node to siblings)
			for (int i = getChildCount(); p == null && i >= 0; i--)	{	// loop over count+1
				p = dtdutil.isElementInsertable((Element)getW3CNode(), tag, i, getConfiguredNodeTypes());
			}
		}

		return p;
	}


	private boolean canHoldChildren()	{	// true wen ELEMENT_NODE oder DOCUMENT_NODE
		return getNodeType() == Node.ELEMENT_NODE || getNodeType() == Node.DOCUMENT_NODE;
	}


	private void setInsertPosition(int insertPosition)	{
		this.insertPosition = insertPosition;
	}

	/** Returns the last used insert position within folder. Used when inserting node into tree model. */
	public int getInsertPosition()	{
		return insertPosition;
	}


	// create empty elements, recursive
	private void fillWithEmptyElements()	{
		MutableXmlNode n = (MutableXmlNode)firstValidElement(this);
		int cc = n.getChildCount();

		// fill node, if no children are present
		if (cc <= 0)	{
			n.init();	// get all insertable elements

			Node parent = n.getW3CNode();
			List insertable = n.getInsertableWithinTags();
			if (insertable != null)	{
				for (int i = 0; i < insertable.size(); i++)	{
					String tag = (String) insertable.get(i);
					Node child = getDTDUtil().createElementRecursive(tag);
					parent.appendChild(child);
				}

				// invalidate child list so that next list call will create tree nodes
				n.children = null;
			}
		}
	}


	/** Checks if the passed name is a valid target name for a processing instruction. */
	public boolean checkPITarget(String target)	{
		return getDTDUtil().checkPITarget(target);
	}

}