package fri.gui.swing.diff;

import java.awt.Color;

public abstract class DiffColors
{
	public static Color INSERTED = new Color(0xBBFFBB);//0xCCFFCC);	// light green
	public static Color CHANGED = new Color(0xCCFFFF);	// light blue
	public static Color DELETED = new Color(0xFFDADA);	// pink red

	private DiffColors()	{}	// do not instantiate

}