package fri.util.xml.xml4j;

import java.io.*;
import java.util.*;
import java.net.URL;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.ibm.xml.parser.*;
import com.ibm.xml.parsers.NonValidatingTXDOMParser;
import fri.util.NetUtil;
import fri.util.io.BomAwareReader;
import fri.util.xml.DOMUtil;
import fri.util.error.Err;
import fri.util.collections.AggregatingHashtable;

/**
	XML4J dependent class.<br>
	Wrap the IBM TX-Parser. Parse a InputStream to a TXDocument DOM.
	Provide a DTDUtil and other utilities to handle the Document.
*/

public class ParserWrapper
{
	/** The encoding according to the default Locale of the machine. */
	public static final String encoding = DOMUtil.getEncoding(Locale.getDefault());
	
	private InputStream xmlSource;
	private String uri;
	private boolean showingAllNodes;
	private boolean doValidation;
	private boolean expandEntities;
	private DTDUtil dtdutil;
	private Hashtable errorNodes;
	private TXDocument doc;
	private byte [] bom;


	/**
		Construct a document parsing object.
		@param xmlSource the XML text to parse as an InputStream.
		@param uri optional name of XML source.
	*/
	public ParserWrapper(
		InputStream xmlSource,
		String uri,
		boolean showingAllNodes,
		boolean doValidation,
		boolean expandEntities)
		throws Exception
	{
		this.xmlSource = xmlSource;
		this.uri = uri;
		this.showingAllNodes = showingAllNodes;
		this.doValidation = doValidation;
		this.expandEntities = expandEntities;

		parse(new ParserErrorListener());
	}
	
	/**
		Reads the document and parses its XML representation.
		@param eh error listener for each error line from TX-parser, may be null,
			then errors are written to wherever parser writes errors (stdout, stderr).
		@return document parsed or null if Exception or number of errors > 0.
	*/
	private synchronized Document parse(ParserErrorListener eh)
		throws Exception
	{
		DTD dtd;
		boolean hasDTD = false;
		
		String uriToParse = uri;
		if (uriToParse.startsWith("file:") == false && uriToParse.startsWith("http:") == false && uriToParse.startsWith("ftp:") == false)
			uriToParse = NetUtil.makeURL(uriToParse).toExternalForm();

		System.err.println("URI to parse: "+uriToParse+", do validation: "+doValidation+", expand entites: "+expandEntities);
		
		if (uri.toLowerCase().endsWith(".dtd"))	{	// is a DTD
			Parser parser = new Parser(uriToParse, eh, null);
			parser.setEndBy1stError(false);

			String baseUri = uriToParse.substring(0, uriToParse.lastIndexOf("/"));	// needs the directory where DTD is to resolve entities
			dtd = readDTD(parser, baseUri);	// build a DTD using the parser
			hasDTD = true;

			doc = new TXDocument();	// create an empty document
			doc.setVersion("1.0");
			doc.setEncoding(DOMUtil.getEncoding(Locale.getDefault()));

			DefaultDTD.addDTD(dtd, doc);	// add DTD as child
			String rootElementName = dtd.getName();
			if (rootElementName == null)
				throw new Exception("No root element name could be found in "+uriToParse);
			
			doc.appendChild(doc.createElement(rootElementName));	// add root element: DOCTYPE

			// make a default name for new XML document
			uri = uri.substring(0, uri.length() - ".dtd".length()) + ".xml";
			System.err.println("Creating XML document from DTD: "+uri);
		}
		else	{	// not ".dtd"
			if (doValidation)	{
				System.err.println("Using validating parser, expand entities "+expandEntities);
				Parser parser = new Parser(uriToParse, eh, null);
				parser.setEndBy1stError(false);
				parser.setProcessNamespace(true);
				parser.setExpandEntityReferences(expandEntities);
				parser.setElementFactory(new ParserNodeFactory(showingAllNodes, doValidation));

				doc = parser.readStream(getBomAwareReader());	// parse the text

				int i = parser.getNumberOfErrors();
				if (i > 0)
					Err.log("XML Parse errors: "+i);
			}
			else	{
				System.err.println("Using non-validating parser (does not expand entities).");
				NonValidatingTXDOMParser parser = new NonValidatingTXDOMParser();
				parser.setErrorHandler(eh);
				InputSource in = new InputSource(getBomAwareReader());
				in.setSystemId(uriToParse);
				parser.parse(in);

				doc = (TXDocument)parser.getDocument();

				eh.setFallbackNode(doc);
			}
			
			dtd = doc.getDTD();	// check for DTD
			hasDTD = (dtd != null);

			if (hasDTD == false)	{	// make default DTD if necessary
				dtd = DefaultDTD.create(doc);
				System.err.println("Creating default DTD ...");
			}
		}

		eh.close();	// output all errors at once if there were errors
		
		this.dtdutil = new DTDUtil(dtd, doc, hasDTD, bom);	// allocate a DOM implementation wrapper

		// save the parsers global error variable to this instance
		this.errorNodes = (Hashtable)ParserErrorListener.errorNodes.clone();
		// clear global variable for next parser
		ParserErrorListener.errorNodes.clear();

		return doc;
	}

	private BomAwareReader getBomAwareReader()
		throws IOException
	{
		BomAwareReader r = new BomAwareReader(xmlSource);
		bom = r.getByteOrderMark();
		return r;
	}

	private byte [] getByteOrderMark()	{
		return bom;
	}
	
	/**
		Returns the DTDUtil for the parsed document.
	*/
	public DTDUtil getDTDUtil()	{
		return dtdutil;
	}

	/**
		Returns a Map of (Node)node -> (String)errorMessage that holds
		all nodes with errors.
	*/
	public Hashtable getErrorNodes()	{
		return errorNodes;
	}

	/**
		Returns the real name where to store the document to.
		Could have been constructed from the dtd name.
	*/
	public String getURI()	{
		return uri;
	}



	private DTD readDTD(Parser parser, final String baseUri)
		throws IOException
	{
		parser.setExpandEntityReferences(true);
		//parser.setWarningRedefinedEntity(false);
		parser.setEndBy1stError(false);
		parser.setEntityResolver(new EntityResolver()	{
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				// xml4j seems to add the entity source in quest to the current working directory
				String workingDir = System.getProperty("user.dir").replace(File.separatorChar, '/');
				while (workingDir.endsWith("/"))
					workingDir = workingDir.substring(0, workingDir.length() - 1);
				
				// get the relative entity path: try to find the current directory within systemId
				int index = systemId.indexOf(workingDir);
				String name = (index >= 0)
					? systemId.substring(index + workingDir.length())
					: systemId.substring(systemId.lastIndexOf("/"));	// what systemId has parser made? Try with last name, will be "/something.mod"
					
				String url = baseUri+name;	// we search at the baseUri plus entity source name
				System.err.println("trying to resolve URL: "+url);
				System.err.println("... from systemId "+systemId);
				System.err.println("... and baseUri   "+baseUri);
				return new InputSource(new URL(url).openStream());
			}
		});
		DTD dtd = parser.readDTDStream(getBomAwareReader());
		//dtd.expandEntityReferences();

		// search first element declaration
		String doctype = findRootElementDecl(dtd.externalElements());
		if (doctype == null)
			doctype = findRootElementDecl(dtd.getElementDeclarations());

		// set 'DOCTYPE' property
		dtd.setName(doctype);

		// set 'SYSTEM "doctype.dtd"' identifier
		dtd.setExternalID(new ExternalID(uri));

		return dtd;
	}


	// Recursively search the DTD for the root element that is contained in no element list
	private String findRootElementDecl(Enumeration e)	{
		String doctype = null;
		AggregatingHashtable graph = new AggregatingHashtable();
		
		while (e.hasMoreElements())	{
			Object o = e.nextElement();

			if (o instanceof ElementDecl)	{
				ElementDecl decl = (ElementDecl)o;
				
				if (decl.getContentType() == ElementDecl.MODEL_GROUP)	{
					ContentModel cm = decl.getXML4JContentModel();
					CMNode cmNode = cm.getContentModelNode();
					listCMNode(decl.getName(), cmNode, graph);
				}
				
				if (doctype == null)
					doctype = decl.getName();	// be sure to have at least one element as root
			}
		}
		
		if (graph.size() > 0)	{	// get root element out of graph
			doctype = null;
			ArrayList otherRoots = new ArrayList();
			
			// find the elementdecl that is contained in no childlist
			for (Enumeration e1 = graph.keys(); e1.hasMoreElements(); )	{
				String key = (String)e1.nextElement();
				boolean found = false;
				
				for (Enumeration e2 = graph.elements(); found == false && e2.hasMoreElements(); )	{
					List v = (List) e2.nextElement();
					if (v.indexOf(key) >= 0)
						found = true;
				}
				
				if (found == false)	{
					if (doctype != null)
						otherRoots.add(key);
					else
						doctype = key;
				}
			}
			
			if (otherRoots.size() > 0)	{
				//Err.warning("The DTD contains more than one root: "+doctype+", others are "+otherRoots);
				otherRoots.add(0, doctype);
				Object chosen = Err.choose("More than one root element found, please choose one:", otherRoots);
				if (chosen != null)
					doctype = (String) chosen;
			}
		}
		
		System.err.println("DOCTYPE is "+doctype);
		return doctype;
	}
	

	private void listCM2opNode(String key, CM2op cm2Node, AggregatingHashtable graph)	{
		listCMNode(key, cm2Node.getLeft(), graph);
		listCMNode(key, cm2Node.getRight(), graph);
	}
	
	private void listCMNode(String key, CMNode cm, AggregatingHashtable graph)	{
		if (cm instanceof CM2op)	{
			CM2op cm2Node = (CM2op)cm;
			listCM2opNode(key, cm2Node, graph);
		}
		else
		if (cm instanceof CM1op)	{
			CM1op cm1Node = (CM1op)cm;
			CMNode cmNode = cm1Node.getNode();
			listCMNode(key, cmNode, graph);
		}
		else
		if (cm instanceof CMLeaf)	{
			CMLeaf cmLeaf = (CMLeaf)cm;
			if (cmLeaf.getName().equals(DTD.CM_PCDATA) == false)	// do not take #PCDATA
				graph.put(key, cmLeaf.getName());
		}
	}
	



	public static void main(String [] args)
		throws Exception
	{
		String uri = args[0];
		InputStream is = new FileInputStream(uri);
		new ParserWrapper(is, uri, true, false, false);	// parses XML
	}

}