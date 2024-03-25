package fri.gui.swing.editor;

import java.io.File;
import fri.gui.mvc.view.swing.PopupMouseListener;

/**
	The lifecycle manager for opened text files.
*/

public class TextEditFileManager extends EditFileManager
{
	public TextEditFileManager(File file, EditController controller, PopupMouseListener popupListener)	{
		super(file, controller, popupListener);
	}


	/**
		Returns a Component that renders the passed Object.
		This Component will then be added to container on CENTER.
	*/
	protected EditorTextHolder createEditArea(File file)	{
		TextEditArea edi = new TextEditArea(file);
		
		edi.setWrapStyleWord(Config.getWrapLines());
		edi.setLineWrap(Config.getWrapLines());
		edi.setTabSize(Config.getTabSize());
		edi.setEncoding(Config.getEncoding());
		
		return edi;
	}
	
}
