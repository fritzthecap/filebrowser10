package fri.gui.swing.xmleditor.controller;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import fri.util.error.Err;
import fri.gui.swing.xmleditor.view.XmlTreeTable;
import fri.gui.swing.xmleditor.model.*;

/**
	Encapsulates functionality in conjunction with reloading
	a text or URI into the XML tree view.
*/

class DocumentReloader implements
	DocumentLoader
{
	private XmlTreeTable treetable;


	DocumentReloader(XmlTreeTable treetable)	{
		if (treetable == null)
			throw new IllegalArgumentException("treetable is null");
		this.treetable = treetable;
	}



	/**
		Implements DocumentLoader: parses and loads the passed text as XML document.
		If text is null, the original URI within the treeview is reloaded.
		@return true if no IO error occured.
	*/
	public boolean loadDocument(String text)	{
		return loadDocument(text, false);
	}

	/**
		Implements DocumentLoader: parses (validates) the passed text as XML document.
		@return true if no parse error occured.
	*/
	public boolean validateDocument(String text)	{
		if (text == null)
			return false;
		MutableXmlNode root = constructNewRoot(text, true);
		return root != null && root.hasDocumentErrors() == false;
	}



	/**
		For validation this returns the whole XML document as String,
		which can be used to create a new root (parsing that text),
		or for editing it as a text and reload then using this DocumentReloader.
		@param withDefaultDTD if true, the artificial DefaultDTD will be included
		@return null if an IO error occured, else the document as String
	*/
	public String getDocumentAsString(boolean withDefaultDTD)	{
		try	{
			return getXmlRoot().getDocumentAsString(withDefaultDTD);
		}
		catch (Exception e)	{
			e.printStackTrace();
			return null;
		}
	}

	/**
		Parse passed text and provide a new root node.
		If text is null, build the root from the original URI in the current view root.
		Provide a visual dialog when parser errors occured.
		@return null when the URI could not be loaeded (not because of parse errors!)
			else the new root (that was NOT yet set into the view).
	*/
	public MutableXmlNode constructNewRoot(String text, boolean validate)	{
		MutableXmlNode root = null;
		try	{
			InputStream is = null;
			if (text != null)
				is = new ByteArrayInputStream(text.getBytes());
			root = new MutableXmlNode(getXmlRoot().getURI(), is, treetable.getConfiguration());
		}
		catch (Exception e)	{
			Err.error(e);
		}
		return root;
	}

	/**
		Retrieve the XML text and re-parse it,
		load into tree-view if no IO error occured.
	*/
	public void reloadFromMemory()	{
		String text = getDocumentAsString(false);

		if (text != null)	{	// do not pass null as this means reload from URI
			loadDocument(text, true);
		}
	}

	/**
		Re-read the Document from its URI and load it into the treeview.
		@return true if no IO error occured.
	*/
	public boolean reloadFromURI()	{
		return loadDocument(null, true);
	}

	private boolean loadDocument(String text, boolean evenWhenErrors)	{
		MutableXmlNode root = constructNewRoot(text, treetable.getConfiguration().validate);

		boolean ret = (root != null && root.hasDocumentErrors() == false);

		if (root != null && (ret || evenWhenErrors))	{
			treetable.setRoot(root);
		}

		return ret;
	}

	private XmlNode getXmlRoot()	{
		XmlTreeTableModel m = (XmlTreeTableModel)treetable.getTreeTableModel();
		return (XmlNode)m.getRoot();
	}

}