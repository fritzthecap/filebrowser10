package fri.gui.swing.filebrowser;

/**
	Implementers can match a File to some criteria like time, size, name etc.
*/

public interface SearchPattern
{
	public boolean match(SearchFile f);
}
