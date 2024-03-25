package fri.gui.awt.clipboard;

/**
	Darsteller fuer die Funktionalitaet der Clipboard-Keys Ctrl-C, -V, -X.
	Das implementierende Objekt kann ein TextField oder eine TextArea sein.
	Die JDK-TextAreas implementieren alle diese Funktionen.
*/

public interface ClipTextRenderer
{
	/**
		Ersetzen einer Text-Region durch den uebergebenen Text.
		@param pasteText einzufuegender Text
		@param selectionStart Beginn-Position (Offset) des zu ersetzenden Textes
		@param selectionEnd End-Position des zu ersetzenden Textes
	*/
	public void replaceRange(String pasteText, int selectionStart, int selectionEnd);

	/**
		Einfuegen des uebergebenen Textes an einer bezeichneten Position.
		@param pasteText einzufuegender Text
		@param caretPosition Position (Offset), an der eingefuegt werden soll
	*/
	public void insert(String pasteText, int caretPosition);
}
