package fri.gui.swing.xmleditor.view;

/**
	Implementers create internal containers rendering XML documents from an URI.
*/

public interface FrameCreator
{
	/** Creates a editor rendering passed document URI. */
	public void createEditor(String uri);

	/** Brings an editor Component to front. */
	public void setSelectedEditor(Object editor);

	/** Sets a new title for an editor (after "Save As"). */
	public void setRenderedEditorObject(Object editor, Object uri);
	
	/** Closes the container window of all internal containers. */
	public void closeContainerWindow();
}
