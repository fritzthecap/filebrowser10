package fri.gui.swing.hexeditor;

import java.io.File;
import javax.swing.JTable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import fri.gui.mvc.view.swing.PopupMouseListener;
import fri.gui.swing.mdi.MdiFrame;
import fri.gui.swing.util.CommitTable;
import fri.gui.swing.editor.EditFileManager;
import fri.gui.swing.editor.EditController;
import fri.gui.swing.editor.EditorTextHolder;

/**
	The lifecycle manager for opened hex files.
	Creates a HexEditTable as editor, commits any open cell editor before closing.
*/

public class HexEditFileManager extends EditFileManager
{
	public HexEditFileManager(File file, EditController controller, PopupMouseListener popupListener)	{
		super(file, controller, popupListener);
	}

	/**
		Returns a HexEditTable.
	*/
	protected EditorTextHolder createEditArea(File file)	{
		return new HexTable(file);
	}


	/**
		Overridden to commit open cell editors before frame checks for changed data.
	*/
	public void closing(MdiFrame ic, PropertyChangeEvent e)
		throws PropertyVetoException
	{
		CommitTable.commit((JTable)editor.getTextComponent());	// end cell editor
		
		super.closing(ic, e);
	}
	
}