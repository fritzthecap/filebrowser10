package fri.gui.awt.colorchoice;

import java.awt.*;

/** Zusatzfunktionen zur Klasse Color */

public abstract class ColorUtil
{
	/** Ist die uebergebene Farbe eine "dunkle" Farbe? */
	public static boolean isBright(Color c)	{
		float [] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
		if (hsb[2] < 0.5)
			return false;
		if (hsb[1] < 0.5)
			return true;
		if (c.getRed() < 150 && c.getGreen() < 150 && c.getBlue() < 150)
			return false;
		return true;
	}

	/** Liefert schwarz oder weiss als Zeichenfarbe,
			je nachdem ob eine helle oder dunkle Farbe uebergeben wird. */
	public static Color getDrawColor(Color c)	{
		return isBright(c) ? Color.black : Color.white;
	}
}