package fri.gui.swing.util;

import java.awt.*;
import javax.swing.*;

/**
	Static Utilities are:
	Edit method for a JMenuBar
*/

public abstract class MenuBarUtil
{
	private MenuBarUtil()	{}
	

	/**
		Insert a Menu into MenuBar at position i, beginning at Zero
		If position is bigger than Component-Count, then Menu is added.
	*/
	public static void insertMenu(JMenuBar menubar, JMenu menu, int position)	{
		int cnt = menubar.getComponentCount();
		if (cnt > 0)	{
			Component [] carr = new Component [cnt];

			// remove all Components from menubar
			for (int i = carr.length - 1; i >= 0; i--)	{
				carr[i] = menubar.getComponent(i);
				menubar.remove(carr[i]);
			}
			
			// Add all and insert this menu at position in menubar
			int j = 0;
			for (int i = 0; i < carr.length + 1; i++, j++)	{
				if (i == position)	{
					menubar.add(menu);
					j--;
				}
				else
					menubar.add(carr[j]);
			}
		}
		else	{		
			// add this menu to menubar
			menubar.add(menu);
		}
	}
	
	
	/**
		Returns -1 if menu not contained, else index of menu within the passed menubar.
	*/
	public static int getMenuIndex(JMenuBar menubar, JMenu menu)	{
		int pos = -1;
		for (int i = 0; i < menubar.getComponentCount(); i++)	{
			if (menubar.getComponent(i) == menu)
				pos = i;
		}
		return pos;
	}
	
}

