package fri.gui.swing.document.textfield.mask;

import fri.gui.swing.document.textfield.MaskingElement;

/**
	TextField mask that allows each character only once.
	Can be used for input of character sets. The only
	character that is allowed more than once is "\",
	as it is used for escaping e.g. "\t".
*/

public class UniqueCharacterMask extends MaskingElement
{
	public boolean checkCharacter(char c, int i)	{
		String s = getTrueText();
		return s.indexOf(c) < 0 || c == '\\';
	}

}