package fri.gui.swing.document.textfield.mask;

import fri.util.file.ValidFilename;
import fri.gui.swing.document.textfield.MaskingElement;

/**
	TextField mask that allows only alphanumeric characters and underscore ('_'),
	at first position only letter or '_'.
*/

public class FilenameMask extends MaskingElement
{
	public boolean checkCharacter(char c, int i)	{
		return ValidFilename.checkFileCharacter(c);
	}

}