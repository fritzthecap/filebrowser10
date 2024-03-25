package fri.gui.awt.component;

import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Window;
import fri.util.text.TextUtil;

/**
	Tries to create a type-unique name from a Window. If it is a class derived from Frame
	or Dialog the class name will be returned (assuming that it is unique), else the class
	name with appended title will be returned (hoping it is unique enough).
*/

public abstract class WindowTypeName
{
	/** Returns a unique name for a Window Object (made of its classname, plus title for Frame and Dialog delegates). */
	public static String windowTypeName(Window window)	{
		String type = window.getClass().getName();
		if (type.startsWith("java.") == false && type.startsWith("javax.") == false)
			return type;	// not a delegate of java.awt. or javax.swing.
				
		// is delegate window: JFrame, JDialog, Frame, Dialog, add title to make a unique typename
		String title = (window instanceof Dialog)
			? ((Dialog)window).getTitle()
			: (window instanceof Frame)
				? ((Frame)window).getTitle()
				: null;
		
		if (title != null)
			type = type+"_"+TextUtil.makeIdentifier(title);
			
		return type;
	}
	
	private WindowTypeName()	{}	// do not instantiate

}
