package fri.util.xml;

import java.util.*;
import org.w3c.dom.*;
import fri.util.LocaleToEncoding;
import fri.util.text.Replace;

/**
 	Methods to edit a XML-document.
 	This methods are strongy bound to rendering logic of
 	<i>fri.gui.swing.xmleditor.model</i> logic.

	@author  Ritzberger Fritz
*/

public abstract class DOMUtil
{
	/** The fixed CDATA start sequence. */
	public final static String CDATA = "<![CDATA[";
	/** The fixed CDATA end sequence. */
	public final static String CDEND = "]]>";

	private static String warning;


	/**
		Returns children of passed node. List type is Node.
		Optionally some Node types can be passed as filters.
		The list is empty if no children exist, null is never returned.
		This method DOES NOT ignore space-only text nodes!
		@param node Node whose children to retrieve
		@param nodeTypes null or array of wanted Node types, e.g. Node.ELEMENT_NODE, Node.TEXT_NODE, ...
		@return list of Nodes that are optionally of passed types
	*/
	public static Vector getChildList(Node node, short [] nodeTypes)	{
		NodeList nl = node.getChildNodes();
		if (nl == null)
			return new Vector(0);
		
		int len = nl.getLength();
		Vector v = new Vector(len);
		
		for (int i = 0; i < len; i++)	{
			Node n = nl.item(i);
			if (n == null)
				continue;	// null child

			if (nodeTypes == null)	{
				v.addElement(n);	// take all types of nodes
			}
			else	{
				boolean found = false;
				for (int j = 0; found == false && j < nodeTypes.length; j++)	{
					if (nodeTypes[j] == n.getNodeType())	{
						v.addElement(n);
						found = true;
					}
				}
			}
		}
		
		return v;
	}


	/** Returns the reference child at given position, null when no children. */
	public static Node getChildAt(Node node, int pos)	{
		NodeList nl = node.getChildNodes();
		return nl != null ? nl.item(pos) : null;
	}



	/** Returns count of children, zero if none. */
	public static int getChildCount(Node node)	{
		NodeList nl = node.getChildNodes();
		return nl != null ? nl.getLength() : 0;
	}



	/** Removes all empty text nodes from passed node. */
	public static void removeEmptyTextNodes(Node n)	{
		removeNodes(n, true);
	}

	private static void removeAllChildren(Node n)	{
		removeNodes(n, false);
	}

	private static void removeNodes(Node node, boolean onlyEmptyTextNodes)	{
		NodeList nl = node.getChildNodes();
		if (nl == null)
			return;
			
		for (int i = nl.getLength() - 1; i >= 0; i--)	{
			Node n = nl.item(i);
			
			if (n != null && (onlyEmptyTextNodes == false || n.getNodeType() == Node.TEXT_NODE))	{
				boolean doRemove = true;

				if (onlyEmptyTextNodes)	{
					String s = n.getNodeValue();
				
					if (s != null && s.trim().length() > 0)
						doRemove = false;
				}

				if (doRemove)
					node.removeChild(n);
			}
		}
	}
	
	private static void removeLeadingTextChildren(Node node)	{
		NodeList nl = node.getChildNodes();
		if (nl == null)
			return;
			
		Vector v = new Vector(nl.getLength());
		boolean stop = false;

		for (int i = 0; !stop && i < nl.getLength(); i++)	{
			Node n = nl.item(i);
			
			if (n != null)
				if (isShownChild(n))
					stop = true;
				else
					v.add(n);
		}

		for (int i = v.size() - 1; i >= 0; i--)	{
			Node n = (Node)v.get(i);
			node.removeChild(n);
		}
	}


	/**
		Returns true if all primitive nodes (text, CDATA, entity refs) are
		before first element child, or if no element children are present.
		Space-only text nodes are ignored.
	*/
	public static boolean canShowLeadingChildrenInElement(Node node)	{
		if (node.getNodeType() != Node.ELEMENT_NODE)
			return true;
			
		NodeList nl = node.getChildNodes();
		if (nl == null)
			return true;
		
		boolean checkForMixed = false;
		
		for (int i = 0; i < nl.getLength(); i++)	{
			Node n = nl.item(i);
			boolean significant = (n != null);
			boolean shownChild = significant && isShownChild(n);

			if (significant)	{
				if (n.getNodeType() == Node.TEXT_NODE)	{	// check if space-only
					String s = n.getNodeValue();

					if (s == null || s.trim().length() <= 0)
						significant = false;
				}
				else
				if (shownChild)	{	// now allow no more primitive nodes
					checkForMixed = true;
				}
			}

			if (significant && checkForMixed && shownChild == false)	{
				return false;	// mixed content present
			}
		}
		
		return true;
	}

	private static boolean isShownChild(Node n)	{
		return
			n.getNodeType() == Node.ELEMENT_NODE ||
			n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE ||
			n.getNodeType() == Node.COMMENT_NODE;
	}

	/**
		Retrieves the text from first text/cdata/entityref children of passed node.
		An entity reference is shown with its meta-symbols and name.
		@exception IllegalArgumentException if passed Node is not of type ELEMENT.
		@return the text from textchild of passed elementNode.
	*/
	public static String getLeadingChildrenValue(Element elementNode)	{
		return getElementTextNodeValue(elementNode, true);
	}

	/**
		Retrieve the catenized text from all children of passed node. Entity references
		are inserted with their meta-symbols and name.
		@return the text from textchildren of passed elementNode.
		@exception IllegalArgumentException if passed Node is not of type ELEMENT or has ELEMENT children.
	*/
	public static String getElementTextNodeValue(Element elementNode)	{
		return getElementTextNodeValue(elementNode, false);
	}

	private static String getElementTextNodeValue(Element elementNode, boolean onlyLeading)	{
		checkElementTextNode(elementNode, onlyLeading);

		NodeList nl = elementNode.getChildNodes();
		if (nl == null || nl.getLength() <= 0)
			return null;

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < nl.getLength(); i++)	{
			Node n = nl.item(i);

			if (n != null)	{
				switch (n.getNodeType())	{
				case Node.TEXT_NODE:
					sb.append(n.getNodeValue());
					break;
				case Node.ENTITY_REFERENCE_NODE:
					sb.append("&"+n.getNodeName()+";");
					break;
				case Node.CDATA_SECTION_NODE:
					sb.append(CDATA+n.getNodeValue()+CDEND);
					break;
				default:
					if (onlyLeading)
						return sb.toString();
					break;
				}
			}
		}

		return sb.toString();
	}

	/**
		Replace all leading entity refs or text or CDATA by nodes created from passed new text.
		This method substitutes WINWORD characters not contained in UNICODE by simple characters.
		@exception IllegalArgumentException if passed Node is not of type ELEMENT.
	*/
	public static void setLeadingChildrenValue(Element elementNode, Document document, String newText)	{
		setElementTextNodeValue(elementNode, document, newText, true);
	}

	/**
		Replace all leading primitive nodes by nodes created from passed new text.
		This method substitutes some non-Unicode WINWORD characters.
		@exception IllegalArgumentException if passed Node is not of type ELEMENT or has ELEMENT children.
	*/
	public static void setElementTextNodeValue(Element elementNode, Document document, String newText)	{
		setElementTextNodeValue(elementNode, document, newText, false);
	}

	private static void setElementTextNodeValue(
		Element elementNode,
		Document document,
		String newText,
		boolean onlyLeading)
	{
		checkElementTextNode(elementNode, onlyLeading);
		//System.err.println("getting new text "+newText);

		if (onlyLeading)	// simple mode, not leaf
			removeLeadingTextChildren(elementNode);
		else	// simple mode, leaf
			removeAllChildren(elementNode);

		if (newText == null || newText.length() <= 0)
			return;

		// substiute some non-Unicode WINWORD characters
		newText = UnicodeJDBCWorkaround.filterChars(newText, DOMUtil.getEncoding(Locale.getDefault()));

		StringBuffer buf = new StringBuffer();	// node text buffer
		Node refChild = elementNode.getFirstChild();	// insertion reference child
		int len = newText.length();
		
		for (int pos = 0; pos < len; pos++)	// loop through characters of new text
		{
			char c = newText.charAt(pos);

			if (c == CDATA.charAt(0) &&	// handle CDATA sections
					pos + CDATA.length() <= len &&
					newText.substring(pos, pos + CDATA.length()).equals(CDATA))
			{
				flushToTextNode(document, elementNode, buf, refChild);

				pos += CDATA.length();
				boolean stop = false;

				for (; stop == false && pos < len; pos++)	{	// collect CDATA while not CDEND
					char c1 = newText.charAt(pos);

					stop = c1 == CDEND.charAt(0) && 
							pos + CDEND.length() <= len &&
							newText.substring(pos, pos + CDEND.length()).equals(CDEND);

					if (stop == false)
						buf.append(c1);
				}

				String s = buf.toString();	// insert CDATA section node
				elementNode.insertBefore(document.createCDATASection(s), refChild);
				//System.err.println("inserting before "+refChild+" CDATA section "+s);
				buf.setLength(0);

				if (stop)	// CDEND found
					pos += CDEND.length() - 2;	// loop incremented too far, outer loop will increment 1
			}
			else
			if (c == '&')	// check for entity reference
			{
				char c1 = (char)0;
				int i = pos + 1;
				
				for (; i < len && (c1 = newText.charAt(i)) != ';' && Character.isWhitespace(c1) == false; i++)
					;
			
				if (i > pos + 1 && c1 == ';')	{	// entity reference identified
					flushToTextNode(document, elementNode, buf, refChild);

					String s = newText.substring(pos + 1, i);	// insert entity reference node
					elementNode.insertBefore(document.createEntityReference(s), refChild);
					//System.err.println("inserting before "+refChild+" entity reference "+s+", i is "+i+", len is "+len);
					pos = i;
				}
				else	{
					buf.append(c);	// was no entity reference
				}
			}
			else	// is normal text
			{
				buf.append(c);
			}

		}	// end for all characters

		flushToTextNode(document, elementNode, buf, refChild);	// clean up
	}

	private static void flushToTextNode(Document document, Element elementNode, StringBuffer buf, Node refChild)	{
		if (buf.length() > 0)	{
			String s = buf.toString();
			elementNode.insertBefore(document.createTextNode(s), refChild);
			//System.err.println("inserting before "+refChild+" text "+s);
			buf.setLength(0);
		}
	}

	// check internal logic: do not allow call of ElementText methods when conditions are not fulfilled
	private static void checkElementTextNode(Element e, boolean allowElementChildren)	{
		if (e.getNodeType() != Node.ELEMENT_NODE)
			throw new IllegalArgumentException("Node is not ELEMENT_NODE: "+nodeType(e));

		if (!allowElementChildren && getChildList(e, new short [] { Node.ELEMENT_NODE }).size() > 0)
			throw new IllegalArgumentException("Node has ELEMENT children: "+e.getNodeName()+"="+e.getNodeValue());
	}



	/** Gets the value text from passed node. */
	public static String getNodeValue(Node node)	{
		String s = node.getNodeValue();
		return s;
	}

	/**
		Sets the passed value to the node.
		Substitutes some non-Unicode WINWORD characters.
		@return null if no substitution of PI or COMMENT occured, else the approptiate warning.
	*/
	public static String setNodeValue(Node node, String newText)	{
		warning = null;

		newText = UnicodeJDBCWorkaround.filterChars(newText, DOMUtil.getEncoding(Locale.getDefault()));

		if (node.getNodeType() == Node.COMMENT_NODE)	{
			newText = checkComment(newText);
		}
		else
		if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE)	{
			newText = checkPI(newText);
		}
		else
		if (node.getNodeType() == Node.CDATA_SECTION_NODE)	{
			newText = checkCDATA(newText);
		}

		node.setNodeValue(newText);

		return warning;
	}


	private static String checkComment(String newText)	{
		if (newText.indexOf("--") >= 0)	{
			newText = replaceAndWarn("--", "\\-\\-", newText, "comment");
		}
		return newText;
	}

	private static String checkPI(String newText)	{
		if (newText.indexOf("?>") >= 0)	{
			newText = replaceAndWarn("?>", "\\?\\>", newText, "processing instruction");
		}
		return newText;
	}

	private static String checkCDATA(String newText)	{
		if (newText.indexOf("]]>") >= 0)	{
			newText = replaceAndWarn("]]>", "\\]\\]\\>", newText, "CDATA section");
		}
		return newText;
	}

	private static String replaceAndWarn(String s, String t, String text, String warnLabel)	{
		text = Replace.replace(text, s, t);
		warning = "Occurences of \""+s+"\" in "+warnLabel+" have been replaced by \""+t+"\".";
		return text;
	}


	/**
		Get a text from an XML-Node attribute like &lt;SOME_ELEMENT desc="Hallo Welt"&gt;
		Well known entities are substituted in the returned text.
		@param node XML Node that maybe holds the attribute
		@param attribName of the attribute to extract text from
	*/
	public static String getAttributeText(Element node, String attribName)	{
		String s = node.getAttribute(attribName);
		return s;
	}


	/**
		Set a new text to the attribute of a XML node.
		Removes the attribute when newText is null or empty.
		Substitutes some non-Unicode WINWORD characters.
		@param attribName name of the attribute
		@param newText new text to store in the attribute
	*/
	public static void setAttributeText(Element node, String attribName, String newText)	{
		if (newText == null || newText.length() <= 0)	{
			if (node.getAttributeNode(attribName) != null)
				node.removeAttribute(attribName);
		}
		else	{
			newText = UnicodeJDBCWorkaround.filterChars(newText, DOMUtil.getEncoding(Locale.getDefault()));
			node.setAttribute(attribName, newText);
		}
	}
	

  

	/* Returns true if anywhere in node or any child node (recursively) is a non-empty text. */
	public static boolean hasAnyTexts(Node node)	{
		Vector v = DOMUtil.getChildList(node, null);

		for (int i = 0; i < v.size(); i++)	{
			Node n = (Node)v.get(i);

			if (n.getNodeType() == Node.TEXT_NODE ||
					n.getNodeType() == Node.COMMENT_NODE ||
					n.getNodeType() == Node.ENTITY_REFERENCE_NODE ||
					n.getNodeType() == Node.CDATA_SECTION_NODE)
			{
				if (n.getNodeValue().length() > 0)
					return true;
			}
			else
			if (n.getNodeType() == Node.ELEMENT_NODE)	{
				if (hasAnyTexts(n))
					return true;
			}
		}

		return false;
	}


	/**
		Returns a technical String represantion of passed node type.
		<pre>
			ELEMENT_NODE = 1
			ATTRIBUTE_NODE = 2
			TEXT_NODE = 3
			CDATA_SECTION_NODE = 4
			ENTITY_REFERENCE_NODE = 5
			ENTITY_NODE = 6
			PROCESSING_INSTRUCTION_NODE = 7
			COMMENT_NODE = 8
			DOCUMENT_NODE = 9
			DOCUMENT_TYPE_NODE = 10
			DOCUMENT_FRAGMENT_NODE = 11
			NOTATION_NODE = 12		
		</pre>
	*/
	public static String nodeType(Node node)	{
		int type = node.getNodeType();
		switch (type) {
			case Node.ATTRIBUTE_NODE: return "ATTRIBUTE_NODE";
			case Node.ENTITY_NODE: return "ENTITY_NODE";
			case Node.COMMENT_NODE:  return "COMMENT_NODE";
			case Node.DOCUMENT_TYPE_NODE:  return "DOCUMENT_TYPE_NODE";
			case Node.DOCUMENT_FRAGMENT_NODE:  return "DOCUMENT_FRAGMENT_NODE";
			case Node.NOTATION_NODE:  return "NOTATION_NODE";
			case Node.DOCUMENT_NODE:  return "DOCUMENT_NODE";
			case Node.ELEMENT_NODE:  return "ELEMENT_NODE";
			case Node.ENTITY_REFERENCE_NODE:  return "ENTITY_REFERENCE_NODE";
			case Node.CDATA_SECTION_NODE:  return "CDATA_SECTION_NODE";
			case Node.TEXT_NODE:  return "TEXT_NODE";
			case Node.PROCESSING_INSTRUCTION_NODE:  return "PROCESSING_INSTRUCTION_NODE";
		}
		throw new IllegalArgumentException("Unknown node type: "+type);
	}


	/**
		Mapping of a Locale to the encodings of different languages.
	*/
	public static String getEncoding(Locale loc)	{
		return LocaleToEncoding.encoding(loc);
	}



	private DOMUtil()	{}	// do not construct


	/**
		Replace &lt; and &amp; and " and ' by well-known entities
		if &amp; is not part of an entitity-reference or character-reference.
	*
	private static String textToXml(String str) {
		if (str == null)
			return null;

		StringBuffer buf = new StringBuffer();
		int len = str.length();
		
		for (int pos = 0; pos < len; pos++) {
			char c = str.charAt(pos);

			switch (c) {
			default:
				buf.append(c);
				break;

			case '<':
				buf.append("&lt;");
				break;

			case '"':
				buf.append("&quot;");
				break;

			case '\'':
				buf.append("&apos;");
				break;

			case '&':
				int subLen = 0;
				char c1 = (char)0;
				int i = pos + 1;
				
				for (; i < len && (c1 = str.charAt(i)) != ';' && Character.isWhitespace(c1) == false; i++)	{
					subLen++;
				}
			
				if (subLen > 0 && c1 == ';')	{	// entity identified
					buf.append(c);
				}
				else	{
					buf.append("&amp;");
				}
				break;
			}
		}

		return buf.toString();
	}

	/**
		Replace well-known entitity-references and character-references
		by their character peer (generate display text).
		Well-known entities are &amp;quot; &amp;apos; &amp;lt; &amp;gt; &amp;amp;
	*
	private static String xmlToText(String str) {
		if (str == null)
			return null;

		StringBuffer buf = new StringBuffer();
		int len = str.length();
		
		for (int pos = 0; pos < len; pos++) {
			char c = str.charAt(pos);

			switch (c) {
			default:
				buf.append(c);
				break;

			case '&':
				char rep = c;

				StringBuffer sb = new StringBuffer();
				char c1 = (char)0;
				int i = pos + 1;
				
				for (; i < len && (c1 = str.charAt(i)) != ';' && Character.isWhitespace(c1) == false; i++)	{
					sb.append(c1);
				}
			
				if (sb.length() > 0 && c1 == ';')	{	// entity identified
					int oldpos = pos;	// remember current position
					pos = i;	// set new position
					String s = sb.toString();
						
					if (s.equals("lt"))
						rep = '<';
					else
					if (s.equals("gt"))
						rep = '>';
					else
					if (s.equals("amp"))
						rep = '&';
					else
					if (s.equals("apos"))
						rep = '\'';
					else
					if (s.equals("quot"))
						rep = '"';
					else
					if (s.startsWith("#") && s.length() > 1)	{	// try numeric
						try	{
							if (s.charAt(1) == 'x')
								rep = (char)Integer.parseInt(s.substring(2), 16);
							else
								rep = (char)Integer.parseInt(s.substring(1));
						}
						catch (NumberFormatException e)	{
								pos = oldpos;	// not a wellknown entity
						}
					}
					else	{	// not a wellknown entity
						pos = oldpos;
					}
				}

				buf.append(rep);
				break;
			}
		}

		return buf.toString();
	}
	*/
}
