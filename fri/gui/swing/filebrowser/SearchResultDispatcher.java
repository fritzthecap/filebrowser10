package fri.gui.swing.filebrowser;

import java.io.File;
import gnu.regexp.REMatch;
import fri.util.observer.CancelProgressObserver;

/**
	Receives found line sets from SearchContentPattern. Can interrupt search.
*/

public interface SearchResultDispatcher extends CancelProgressObserver
{
	/**
		Receiving all lines found in passed file, and search pattern informations.
		This is for displaying search result and further working with file (view/edit).
	*/
	public void showGrepResult(
		File file,
		int numberOfMatches,
		String displayTextLines,
		String searchPattern,
		String searchSyntax,
		boolean ignoreCase,
		boolean wordMatch);
		
	/**
		Returns true if dispatcher wants to replace search patterns
		and create a substituted file.
	*/
	public boolean isReplacing();

	/**
		This method gets called when a new file was opened for search.
	*/
	public void openReplacement(SearchFile comingFile);

	/**
		This happens when replacing a single-line-pattern.
		Recieve one line, and its newline sequence.
		This method gets called for each file line. If no pattern was found,
		matches is null. If wordMatch is true, the found positions must be adjusted
		as the word boundary pattern includes start and end character, except
		at begin/end of line and begin/end of file.
	*/
	public void replaceLine(String line, String newline, REMatch [] matches, boolean wordMatch);

	/**
		This happens when replacing a multi-line-pattern.
		Recieve the whole text, and its newline sequence.
		This method gets called once for each file. If no pattern was found,
		matches is null. If wordMatch is true, the found positions must be adjusted
		as the word boundary pattern includes start and end character, except
		at begin/end of line and begin/end of file.
	*/
	public void replaceText(String text, String newline, REMatch [] matches, boolean wordMatch);

	/**
		This method gets called when a file was closed after search.
		The argument is the same as in preceeding open-call.
	*/
	public void closeReplacement(SearchFile passedFile);

}