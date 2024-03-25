package fri.gui.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import fri.util.os.OS;

public abstract class BugFixes
{
	/** Workaround for JDC Bug 4298592 */
	public static void fixInputMethodProblem(JComponent componentToFix)	{
		if (OS.isAboveJava13)
			return;
			
		//System.err.println("fixing 4298592 menu input method problem for "+componentToFix.getClass());
		Component [] comps = componentToFix.getComponents();

		for (int i = 0; i < comps.length; i++)	{
			Component comp = comps[i];

			if (comp instanceof AbstractButton)	{
				//System.err.println("-> fixing problem for "+((AbstractButton)comp).getText());
				comp.enableInputMethods(false);
			}
			if (comp instanceof JComponent)	{
				if (comp instanceof JMenu)
					comp = ((JMenu)comp).getPopupMenu();

				fixInputMethodProblem((JComponent)comp);
			}
		}
	}

  /** Ensure that the popup is properly located */
  public static Point computePopupLocation(MouseEvent event, Component rel, JPopupMenu popup)	{
		if (OS.isAboveJava13)
			return event.getPoint();

    Dimension psz = popup.getSize();
    Dimension ssz = Toolkit.getDefaultToolkit().getScreenSize();
    Point gLoc = rel.getLocationOnScreen();
    Point result = new Point( event.getX(), event.getY() );

    gLoc.x += event.getX();
    gLoc.y += event.getY();

    if ( psz.width == 0 || psz.height == 0 ) {
      int items = popup.getSubElements().length;
      psz.height = ( items * 22 );
      psz.width = 100;
    }

    psz.height += 5;

    if ( (gLoc.x + psz.width) > ssz.width ) {
      result.x -= (( gLoc.x + psz.width) - ssz.width);
      if ( (gLoc.x + result.x) < 0 )
        result.x = -(gLoc.x + event.getX());
    }

    if ( (gLoc.y + psz.height) > ssz.height ) {
      result.y -= (( gLoc.y + psz.height) - ssz.height);
      if ( (gLoc.y + result.y) < 0 )
        result.y = -gLoc.y;
    }

    return result;
  }


	/**
		Compute a proper location for popup menu of a ComboBox.
		@param parentDeltaHeight height of ComboBox (when closed).
	*/
	public static Point computePopupLocation(int x, int y, Component invoker, JPopupMenu jpopupmenu, int parentDeltaHeight)
	{
		if (OS.isAboveJava13)
			return new Point(x, y);

		//System.err.println("popup relative location x = "+x+", y = "+y);
		if (x < 0)
			x = 0;	// always on left border of invoker
		if (y <= 0)
			y = parentDeltaHeight;	// default on bottom of invoker
		
		Dimension popupSize = jpopupmenu.getSize();
		
		if (popupSize.width == 0 || popupSize.height == 0) {
			popupSize = jpopupmenu.getPreferredSize();
		}
		
		if (popupSize.width == 0 || popupSize.height == 0) {
			int items = jpopupmenu.getSubElements().length;
			popupSize.height = items * parentDeltaHeight;
			popupSize.width = 100;
		}

		// calculate screen sizes and invoker location
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Point invokerLocation = invoker.getLocationOnScreen();
		//System.err.println("invoker absolute location x = "+x+", y = "+y);

		Point result = new Point( x, y );

		Point popupLocation = new Point(invokerLocation.x + x, invokerLocation.y + y);

		int rightX = popupLocation.x + popupSize.width;
		if (rightX > screenSize.width) {
			result.x -= (rightX - screenSize.width);
		}
		int leftX = invokerLocation.x + result.x;
		if (leftX < 0)	{
			result.x += (-leftX);
		}

		int bottomY = popupLocation.y + popupSize.height;
		if (bottomY > screenSize.height) {
			result.y -= (bottomY - screenSize.height);
		}
		int topY = invokerLocation.y + result.y;
		if (topY < 0 )	{
			result.y += (-topY);
		}
		
		// if opening upwards, do not hide the invoker (combobox)
		if (invokerLocation.y + result.y < invokerLocation.y)	{	// negative amount
			result.y -= parentDeltaHeight;
		}

		//System.err.println("return relative result x = "+result.x+", y = "+result.y);
		return result;
	}



	private BugFixes()	{}
}