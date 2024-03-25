package fri.gui.swing.xmleditor.model;

/**
	Implementers parse and load a XML document from a text String.
	This is needed by DocumentEditDialog to save its changes.
*/

public interface DocumentLoader
{
	/**
		Parse and load a new document from passed text.
		Returns true if document could be parsed without errors
		(when validating, else always true).
		@param text string to build document from
		@return true if document could be parsed,
			or always true if parser was set non-validating
	*/
	public boolean loadDocument(String text);
	/**
		Parse and a new document from passed text.
		Returns true if document could be parsed without errors
		(when validating, else always true).
		@param text string to build document from
		@return true if document could be parsed,
			or always true if parser was set non-validating
	*/
	public boolean validateDocument(String text);
}
