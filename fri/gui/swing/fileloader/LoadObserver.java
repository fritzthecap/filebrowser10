package fri.gui.swing.fileloader;

/**
	Interface between FileLoader in background and foreground GUI.
	The FileLoader calls the implementer with true before loading
	and with false when loading was finished.
*/

public interface LoadObserver
{
	/**
		Implementer receives start (true) and end (false) of loading from FileLoader.
		@param loading true at start, false at end of file loading.
	*/
	public void setLoading(boolean loading);
}