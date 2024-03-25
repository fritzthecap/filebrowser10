package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.*;
import javax.swing.JTable;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import fri.gui.CursorUtil;
import fri.gui.swing.BugFixes;

/**
	Target: a dynamic popup-menu that installs context-specific items
		when it is shown.
		It listenes to these dynamic items instead of the default-listener.
*/

public class TreeEditPopup extends JPopupMenu implements
	ActionListener
{
	private TreeEditController tc;
	private JMenuItem origMenuItem;
	private JMenuItem selectAllItem = new JMenuItem("Select All");
	private JMenuItem printAsText = new JMenuItem("Print Contents As Text");
	private int subIndex;
	private LaunchGroup currGroup;
	private OpenLauncher launcher;
	private String label;
	private boolean tableMode = false;	// starting with tree
	private JMenuItem sortItem, refreshItem, expandRecursive;
	

	/**
		Create an empty popup menu.
		@param tc tree edit controller to request selection in tree.
		@param label label of menuitem to substitute dynamically
		@param subIndex index of item to substitute
	*/
	public TreeEditPopup(TreeEditController tc, String label, int subIndex)	{
		this.tc = tc;
		this.subIndex = subIndex;
		this.label = label;
		selectAllItem.addActionListener(tc);
		printAsText.addActionListener(tc);
	}


	/**
		Substitute contex-sensitive and popup the menu.
		@param x x-coordinate of popup
		@param y y-coordinate of popup
	*/
	public void show(int x, int y, Component c)	{
		// multiple open events are rendered in popup
		boolean done = false;
		NetNode [] n = tc.getSelectedNodes();

		if (n != null && n.length > 0)	{
			Component cursorComponent = tc.getCursorComponent();
			CursorUtil.setWaitCursor(cursorComponent);
			try	{
				launcher = new OpenLauncher(tc.getCursorComponent(), tc, n, tc.getOpenCommands());
				
				if (launcher.getUndefined() == null)	{	// has no undefined items
					LaunchGroups launchGroups = launcher.getLaunchGroups();
					currGroup = null;
					
					// take only first of many launch groups or do nothing
					if (launchGroups.size() > 0)	{
						if (launchGroups.size() > 1)	// warn because of multiple not renderable launch groups
							Toolkit.getDefaultToolkit().beep();
							
						currGroup = (LaunchGroup)launchGroups.elementAt(0);					
						// substitute menu
						removeOriginalItem();
						buildLauncherItems();
						
						done = true;
					}	// end if launch groups > 0
						
				}	// end if undefined null
			}
			finally	{
				CursorUtil.resetWaitCursor(cursorComponent);
			}

		}	// end if selected > 0 
		
		if (!done)	{
			restoreMenu(); 
		}
		
		setComponentContext(c);

		BugFixes.fixInputMethodProblem(this);
		
		super.show(c, x, y);
	}


	private void restoreMenu()	{
		JMenu menu = null;
		Component c = getComponent(subIndex);
		//System.err.println("component at "+subIndex+" is "+c.getClass());
		if (c instanceof JMenu)
			menu = (JMenu)c;
			
		if (menu == null)	// no menu was substituted
			return;
			
		System.err.println("restoring menu item "+origMenuItem.getClass());
		try { remove(menu); } catch (Exception e) {} // Swing BUG
		insert(origMenuItem, subIndex);
		
		// workaround menu bug: removes action listener when removed from parent
		origMenuItem.removeActionListener(tc);	// not to cumulate when when bug is fixed
		origMenuItem.addActionListener(tc);
	}


	private void removeOriginalItem()	{
		Component c = getComponent(subIndex);
		if (c instanceof JMenu == false)
			origMenuItem = (JMenuItem)c;
			
		try { remove(c); } catch (Exception e) {} // Swing BUG
	}
	


	private void buildLauncherItems()	{
		JMenu menu = new JMenu(label);	// main item "open"
		JMenuItem [] items = launcher.buildMenuItems(currGroup, this);
		for (int j = 0; j < items.length; j++)	{
			menu.add(items[j]);	// action command already set to menuitem text
		}
		insert(menu, subIndex);
	}


	// interface ActionListener
	
	public void actionPerformed(ActionEvent e)	{
		Component c = tc.getCursorComponent();
		CursorUtil.setWaitCursor(c);
		try	{
			System.err.println("TreeEditPopup.actionPerformed "+e.getActionCommand());
			launcher.startCommandWithPattern(currGroup, e.getActionCommand());
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
	}
	
	
	// add or remove items according to component on which launched
	private void setComponentContext(Component c)	{
		boolean isTable = c instanceof JTable;
		int i = getComponentCount();
		
		if (!tableMode && isTable)	{
			tableMode = true;
			expandRecursive = (JMenuItem)getComponent(i - 1);
			remove(i - 1);	// "Expand Recursive" item
			sortItem = (JMenuItem)getComponent(i - 2);
			remove(i - 2);	// "Sort" item
			refreshItem = (JMenuItem)getComponent(3);
			remove(3);	// "refresh" item
			add(selectAllItem);	// "Select All" item
			add(printAsText);	// "Print As Text" item
		}
		else
		if (tableMode && !isTable)	{
			tableMode = false;
			remove(i - 1);	// "Print As Text" item
			remove(i - 2);	// "Select All" item
			insert(refreshItem, 3);	// "refresh" item
			add(sortItem);	// "Sort" item
			add(expandRecursive);	// "Expand Recursive" item
		}
	}
	
}