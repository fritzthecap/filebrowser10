package fri.util.xml.xml4j;

import org.w3c.dom.*;
import com.ibm.xml.parser.*;

/**
	XML4J dependent class.
	Error indication. Overrides factory methods to store
	the current node for retrieval by the error listener.
*/

class ParserNodeFactory extends TXDocument
{
	static Node currentNode;
	
	private boolean showAllTextNodes;
	private boolean doValidation;

	/**
		Factory with methods that store the current node (error indication).
		@param showAllTextNodes true if even text nodes should be indicated as error nodes,
			false if only element nodes should be in error list.
		@param doValidation control parser validation, works only with xml4j 1.1.9, not with 2.0.15
	*/
	public ParserNodeFactory(boolean showAllTextNodes, boolean doValidation)	{
		super();
		this.showAllTextNodes = showAllTextNodes;
		this.doValidation = doValidation;
	}

	/** Overridden to control validation. Works only with xml4j 1.1.9, not with 2.0.15. */
	public boolean isCheckValidity()	{
		return doValidation;
	}

	/** Overridden to store current node. */
	public Element createElement(String name)
		throws DOMException
	{
		Element e = super.createElement(name);
		currentNode = e;
		return e;
	}

	/** Overridden to store current node. */
	public Comment createComment(String data)	{
		Comment e = super.createComment(data);
		currentNode = e;
		return e;
	}

	/** Overridden to store current node. */
	public ProcessingInstruction createProcessingInstruction(String name, String data)
		throws DOMException
	{
		ProcessingInstruction e = super.createProcessingInstruction(name, data);
		if (showAllTextNodes)
			currentNode = e;
		return e;
	}

	/** Overridden to store current node. */
    public DocumentFragment createDocumentFragment()	{
		DocumentFragment e = super.createDocumentFragment();
		currentNode = e;
		return e;
    }

	/** Overridden to store current node. */
	public TXText createTextNode(String data, boolean isIgnorableWhitespace)	{
		TXText e = super.createTextNode(data, isIgnorableWhitespace);
		if (showAllTextNodes)
			currentNode = e;
		return e;
	}

	/** Overridden to store current node. */
	public TXText createTextNode(char[] charArray, int offset, int length, boolean isIgnorableWhitespace)	{
		TXText e = super.createTextNode(charArray, offset, length, isIgnorableWhitespace);
		if (showAllTextNodes)
			currentNode = e;
		return e;
	}

	/** Overridden to store current node. */
	public CDATASection createCDATASection(String data)
		throws DOMException
	{
		CDATASection e = super.createCDATASection(data);
		if (showAllTextNodes)
			currentNode = e;
		return e;
	}

	/** Overridden to store current node. */
	public EntityReference createEntityReference(String name)
		throws DOMException
	{
		EntityReference e = super.createEntityReference(name);
		if (showAllTextNodes)
			currentNode = e;
		return e;
	}

}
