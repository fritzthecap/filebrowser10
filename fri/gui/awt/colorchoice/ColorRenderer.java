package fri.gui.awt.colorchoice;

import java.awt.*;

/** Der Client des Farbdarsteller-Objektes implementiert dieses
		interface, um Änderungen der Farb-Scrollbars zu empfangen.
*/
public interface ColorRenderer
{
	/** Eine neue Farbe wurde eingestellt. @param c die neue Farbe. */
	public void changeColor(Color c);
}