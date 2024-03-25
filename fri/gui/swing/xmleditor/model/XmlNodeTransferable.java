package fri.gui.swing.xmleditor.model;

import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import org.w3c.dom.*;

/**
	Implementation of interface Transferable for serializable XML nodes.
	Needed for Drag 'n Drop between XML editor windows.

	@author Ritzberger Fritz
*/

public class XmlNodeTransferable implements
	Transferable,
	ClipboardOwner
{
	public static final DataFlavor xmlNodeFlavor = new DataFlavor(SerializableNode.class, "W3C-Node");  		
	public static final DataFlavor[] flavors = {
		xmlNodeFlavor,
	};
	private static final List flavorList = Arrays.asList(flavors);
	private List data;


	public XmlNodeTransferable(List data) {
		this.data = data;
	}

	public synchronized DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor ) {
		return flavorList.contains(flavor);
	}
	
	public synchronized Object getTransferData(DataFlavor flavor)
		throws UnsupportedFlavorException, IOException
	{
		if (flavor.equals(xmlNodeFlavor))	{
			return this.data;
		}
		else	{
			throw new UnsupportedFlavorException (flavor);
		}
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}
	
	public String toString() {
		return "XmlNodeTransferable";
	}




	/** Minimal implementation of serializable w3c Node. */
	public static class SerializableNode implements
		Node,
		Attr,
		Serializable
	{
		private short type;
		private String name;
		private String value;
		private boolean hasChildNodes;
		private Vector children;
		private Vector attributes;
		private Node parent;

		public SerializableNode(Node delegate)	{
			type = delegate.getNodeType();
			name = delegate.getNodeName();
			value = delegate.getNodeValue();
			hasChildNodes = delegate.hasChildNodes();

			NamedNodeMap attr = delegate.getAttributes();
			for (int i = 0; attr != null && i < attr.getLength(); i++)	{
				Node n = attr.item(i);
				SerializableNode sn = new SerializableNode(n);

				if (attributes == null)
					attributes = new SerializableNamedNodeMap();
				((NamedNodeMap)attributes).setNamedItem(sn);
			}

			NodeList chldr = delegate.getChildNodes();
			for (int i = 0; chldr != null && i < chldr.getLength(); i++)	{
				Node n = chldr.item(i);
				SerializableNode sn = new SerializableNode(n);

				if (children == null)
					children = new SerializableNodeList();
				appendChild(sn);
				sn.parent = this;
			}
		}

		// Node implementation
		public Node appendChild(Node newChild)	{
			children.add(newChild);
			return newChild;
		}
		public Node cloneNode(boolean deep)	{
			return null;	// not meant for this
		}
		public NamedNodeMap getAttributes()	{
			return (NamedNodeMap)attributes;
		}
		public NodeList getChildNodes()	{
			return (NodeList)children;
		}
		public Node getFirstChild()	{
			return (Node)children.get(0);
		}
		public Node getLastChild()	{
			return (Node)children.get(children.size() - 1);
		}
		public Node getNextSibling()	{
			return null;	// not meant for this
		}
		public String getNodeName()	{
			return name;
		}
		public short getNodeType()	{
			return type;
		}
		public String getNodeValue()	{
			return value;
		}
		public Document getOwnerDocument()	{
			return null;	// not meant for this
		}
		public Node getParentNode()	{
			return parent;
		}
		public Node getPreviousSibling()	{
			return null;	// not meant for this
		}
		public boolean hasChildNodes()	{
			return hasChildNodes;
		}
		public Node insertBefore(Node newChild, Node refChild)	{
			if (refChild != null)	{
				int i = children.indexOf(refChild);
				children.add(i, newChild);	// must throw IndexOutOfBoundsException
			}
			else	{
				appendChild(newChild);
			}
			return newChild;
		}
		public Node removeChild(Node oldChild)	{
			children.remove(oldChild);
			return oldChild;
		}
		public Node replaceChild(Node newChild, Node oldChild)	{
			int i = children.indexOf(oldChild);
			children.set(i, newChild);
			return newChild;
		}
		public void setNodeValue(String nodeValue)	{
		}

		// Attr implementation
		public String getName()	{
			return getNodeName();
		}
		public boolean getSpecified()	{
			return getValue() != null;
		}
		public String getValue()	{
			return getNodeValue();
		}
		public void setValue(String value)	{
			this.value = value;
		}
		public Element getOwnerElement ()	{
			return null;
		}
		public void normalize() 	{
		}
		public boolean isSupported(String feature, String version)	{
			return false;
		}
		public String getNamespaceURI() 	{
			return null;
		}
		public String getPrefix()	{
			return null;
		}
		public void setPrefix(String prefix)	{
		}
		public String getLocalName()	{
			return getNodeType() == Node.ELEMENT_NODE || getNodeType() == Node.ATTRIBUTE_NODE ? getNodeName() : null;
		}
		public boolean hasAttributes()	{
			return attributes != null;
		}

		// String in JDK 1.4
		//public String getFeature(String s, String t)	{
		// Object new in JDK 1.5
		public Object getFeature(String s, String t)	{
			return null;
		}
		public boolean isEqualNode(Node other)	{
			return equals(other);
		}
		public boolean isSameNode(Node other)	{
			return equals(other);
		}
		public String lookupPrefix(String namespaceURI)	{
			return null;
		}
		public String lookupNamespaceURI(String uri)	{
			return null;
		}
		public boolean isDefaultNamespace(String namespaceURI)	{
			return false;
		}
		public Object getUserData(String key)	{
			return null;
		}
		// Following argument type is not in JDK 1.4
		public Object setUserData(String key, Object data, org.w3c.dom.UserDataHandler handler)	{
			return null;
		}
		public String getTextContent()	{
			return null;
		}
		public void setTextContent(String c)	{
		}
		public short compareDocumentPosition(Node other)	{
			return (short) 0;
		}
		public String getBaseURI()	{
			return null;
		}

		// Attr, new in JDK 1.5
		public boolean isId()	{
			return name.equals("id");
		}
		// Following argument type is not in JDK 1.4
		public org.w3c.dom.TypeInfo getSchemaTypeInfo()	{
			return null;
		}
	}

	/** Minimal implementation of serializable NodeList (element children). */
	public static class SerializableNodeList extends Vector implements
		NodeList,
		Serializable
	{
		public int getLength()	{
			return size();
		}
		public Node item(int index)	{
			return (Node)get(index);
		}
	}

	/** Minimal implementation of serializable NamedNodeMap (attributes). */
	public static class SerializableNamedNodeMap extends SerializableNodeList implements
		NamedNodeMap,
		Serializable
	{
		private Vector names;

		public Node getNamedItem(String name)	{
			int i = names != null ? names.indexOf(name) : -1;
			return i < size() && i >= 0 ? (Node)get(i) : null;
		}
		public Node removeNamedItem(String name)	{
			return null;	// not meant for this
		}
		public Node setNamedItem(Node arg)	{
			if (names == null)	{
				names = new Vector();
			}
			names.add(arg.getNodeName());
			Node n = new SerializableNode(arg);
			add(n);
			return n;
		}
		public Node getNamedItemNS(String namespaceURI, String localName) 	{
			return null;
		}
		public Node setNamedItemNS(Node arg)	{
			return null;
		}
		public Node removeNamedItemNS(String namespaceURI, String localName)	{
			return null;
		}
	}

}
