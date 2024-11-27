package fri.gui.swing.actionmanager;

import java.util.*;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import java.awt.event.MouseEvent;
import fri.gui.swing.BugFixes;

/**
	Ziel:<BR>
	<blockquote>
		Eine zentral verwaltete Action soll kontextabhaengige
		dynamisch erzeugte Untermenuepunkte aufnehmen koennen und
		diese generisch mit einer dem Behaelter (Toolbar, Menu)
		angepassten Methode visualisieren koennen.<br>
		<b>Beispiel</b>: Es sollen abhaengig von der Selektion in einer TreeView
			verschiedene Popup-Menupunkte verwendet werden, wobei diese
			im Toolbar als ein am Button aufklappendes Popu-Menu, im
			Menue als ein Sub-Menue erscheinen. Der ActionListener erhaelt
			ein zusammengesetztes ActionCommand, wenn ein Item ausgeloest wird:
			"staticItem.dynamicItem".
	</blockquote>
	Verhalten:<BR>
	<blockquote>
		Es koennen sowohl hierarchische Strukturen dynamisch eingefuegt
		werden (MenuTree) als auch nur eine einzige Menu-Ebene (String[]).
		Die Action unterteilt sich dann in diese Struktur mittels Sub-Menus
		und liefert dem ActionListener z.B. das ActionCommand
		"actionName.path1.path2.leaf", wenn etwas ausgewaehlt wurde.<br>
		Ein <b>Menu-Separator</b> kann durch Uebergabe des Wertes "null" erzeugt werden.
	</blockquote>
	Lebenszyklus:<BR>
	<blockquote>
		Bei Programmstart werden mittels eines ActionManagers und der
		Methode insertFillableAction() FillableManagedAction's
		erzeugt. Ohne Aufruf von fillAction() bleibt diese FillableAction
		ein einfacher JMenuItem.
		Wird ihr ueber fillAction() 1-n Ausloeser zugewiesen, unterteilt
		sich diese Action visuell in Sub-Menus.<br>
		<b>ACHTUNG</b>: Die Action muss mittels visualizeAction() in allen
		Tool- und MenuBars dargestellt worden sein, bevor sie mit fillAction()
		befuellt werden kann!<br>
		Wird eine dynamisch eingefuegte Action dann ausgeloest, wird dem
		installierten ActionListener im actionCommand des Events die
		mit "." verketteten actionCommands aller ausgeloesten MenuItems uebergeben.
		Menuitems, die nicht zum ActionManager gehoeren, werden in
		diesem Namen allerdings nicht aufgelistet.<br>
		Z.B.: actionCommand "new.address" kann vom "Edit"-Hauptmenu stammen,
		in dem in der FillableManagedAction "new" dynamisch der Item "address"
		eingefuellt wurde.
	</blockquote>
		
	@version $Revision: 1.35 $ <BR>
	@author  $Author: fr $ - Ritzberger Fritz <BR>
	@see ActionManager
	@see ManagedAction
*/

public class FillableManagedAction extends ManagedAction
{
	public final String menuItemSeparator;
	private Hashtable addList = new Hashtable();
	private Vector delList = new Vector();
	private String [] currentItems = null;
	private boolean [] enabled = null;
	private boolean allDisabled;
	private JPopupMenu popup = null;
	private boolean doNotCheck = false;
	private MenuTree currentMenuTree = null;
	private Point popupPoint;


	/** Calls superclass constructor and sets menuItemSeparator. */
	public FillableManagedAction(
		String name,
		String image,
		String tooltip,
		int key,
		int mask,
		JComponent keyboardsensor,
		ActionListener actionlistener)
	{
		super(name, image, tooltip, key, mask, keyboardsensor, actionlistener);
		this.menuItemSeparator = ActionManager.menuItemSeparator;
	}

	/** Calls superclass constructor and sets menuItemSeparator. */
	public FillableManagedAction(
		String name,
		Icon icon,
		String tooltip,
		int key,
		int mask,
		JComponent keyboardsensor,
		ActionListener actionlistener)
	{
		super(name, icon, tooltip, key, mask, keyboardsensor, actionlistener);
		this.menuItemSeparator = ActionManager.menuItemSeparator;
	}



	/** Returns the currently created popup-menu or null */
	public JPopupMenu getPopupMenu()	{
		return popup;
	}



	private void beginFilling()	{
		if (popup != null && popup.isVisible())
			popup.setVisible(false);

		addList.clear();
		delList.clear();
	}
	
	private void endFilling()	{
		// delete old items after enumeration, avoid infinite loop
		for (Enumeration e = delList.elements(); e.hasMoreElements(); )	{
			Object key = e.nextElement();
			triggerList.remove(key);
		}
		// insert new items after enumeration, avoid infinite loop
		for (Enumeration e = addList.keys(); e.hasMoreElements(); )	{
			Object key = e.nextElement();
			Object value = addList.get(key);
			triggerList.put(key, value);	// store the new menu holders
		}
	}
	
	
	/**
		Fill this action dynamically with submenu-items and set them enabled
		according to the passed boolean array.
	*/
	public boolean fillAction(
		String [] menuNames,
		ActionListener listener,
		boolean [] enabled)
	{
		if (isSameEnabling(enabled) &&
				isSameItems(menuNames) &&
				isSameActionListener(listener))	{
			return menuNames != null && menuNames.length > 0;
		}

		doNotCheck = true;	// checking was made

		this.enabled = enabled;
		boolean ret = fillAction(menuNames, listener);

		doNotCheck = false;	// next call can be without enabling

		return ret;
	}

	/**
		Fill this action dynamically with submenu-items.
	*/
	public synchronized boolean fillAction(
		String [] menuNames,
		ActionListener listener)
	{
		//System.err.println("FillableAction.fillAction");
		if (doNotCheck == false &&
				isSameItems(menuNames) &&
				isSameActionListener(listener))	{
			return menuNames != null && menuNames.length > 0;
		}
		return substituteAllTriggers(true, menuNames, null, listener);
	}

	/**
		Fill this action dynamically with a submenu-tree.
		Returns true if there is at least one submenu item.
	*/
	public boolean fillAction(MenuTree menuTree, ActionListener listener)	{
		if (currentMenuTree != null && currentMenuTree.equals(menuTree))	{
			return currentMenuTree.size() > 0;
		}
		return substituteAllTriggers(false, null, menuTree, listener);
	}

	private boolean substituteAllTriggers(
		boolean doMenuNames,	// both could be null!
		String [] menuNames,
		MenuTree menuTree,
		ActionListener listener)
	{
		beginFilling();
		boolean ret = false;
		currentItems = menuNames;
		currentMenuTree = menuTree;

		// prepare filling buttons with only one popup menu
		if (doMenuNames)
			popup = buildPopupMenu(menuNames, listener);
		else
			popup = buildPopupMenu(menuTree, listener);
		
		// loop all triggers of this action and fill each one
		for (Enumeration e = triggerList.keys(); e.hasMoreElements(); )	{
			JComponent c = (JComponent)e.nextElement();

			if (c instanceof JButton)	{
				ret = setButtonSubMenu(c, listener);
			}
			else
			if (c instanceof JMenuItem)	{
				if (doMenuNames)
					ret = setMenuSubMenu(c, menuNames, listener);
				else
					ret = setMenuSubTree(c, menuTree, listener);
			}
			else
				System.err.println("FEHLER: unbekannte Component in ManagedAction.fillAction "+getName());
		}
		
		endFilling();
		return ret;
	}


	/** Set a popup point upon keyboard sensor component for keypress popup choice. */
	public void setPopupPoint(Point popupPoint)	{
		this.popupPoint = popupPoint;
	}
	
	/** Overridden to open popup choice on keypress (F4) when <i>popupPoint</i> was set by client. */
	public void actionPerformed(ActionEvent e)	{
		System.err.println("FillableManagedAction actionPerformed, name "+getName());
		if (popupPoint != null && getPopupMenu() != null && keyboardSensor != null)	{
			showPopupActionMenu(keyboardSensor, popupPoint.x, popupPoint.y);
		}
		else	{
			super.actionPerformed(e);
		}
	}
	
	
	private void showPopupActionMenu(JComponent eventSource, int x, int y)	{
		MouseEvent event = new MouseEvent(eventSource, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false);
		Point p = BugFixes.computePopupLocation(event, eventSource, getPopupMenu());
		getPopupMenu().show(eventSource, p.x, p.y);
	}



	// Methods to reduce memory usage. Buffer settings and
	// allocate new Menus only when settings changed.

	// Decide if Item Set has changed
	private boolean isSameItems(String [] names)	{
		if (names == null && this.currentItems == null)
			return true;

		if (names == null && this.currentItems != null ||
				names != null && this.currentItems == null)
			return false;
			
		if (names.length != this.currentItems.length)
			return false;

		for (int i = 0; i < currentItems.length; i++)
			if (names[i].equals(this.currentItems[i]) == false)
				return false;

		
		return true;
	}
	
	
	// Decide if Enabling has changed
	private boolean isSameEnabling(boolean [] enabled)	{
		if (enabled == null && this.enabled == null)	{
			return true;
		}

		if (enabled == null && this.enabled != null ||
				enabled != null && this.enabled == null)	{
			return false;
		}
			
		if (enabled.length != this.enabled.length)	{
			return false;
		}

		for (int i = 0; i < enabled.length; i++)	{
			if (enabled[i] != this.enabled[i])	{
				return false;
			}
		}
		
		return true;
	}


	// Decide if ActionListener has changed
	private boolean isSameActionListener(ActionListener actionListener)	{
		if (actionListener == null && this.actionListener == null)
			return true;

		if (actionListener == null && this.actionListener != null ||
				actionListener != null && this.actionListener == null)
			return false;
			
		if (actionListener != this.actionListener)
			return false;
			
		return true;
	}



	// add an action listener to the button, that shows a popup menu with new items
	
	private boolean setButtonSubMenu(JComponent c, ActionListener listener)	{
		final JButton btn = (JButton)c;
		Icon icon = btn.getIcon();	// Bug jdk1.3
		String text = btn.getText();

		// get old listener and remove it
		ActionListener l = (ActionListener)triggerList.get(c);
		btn.removeActionListener(l);

		if (popup != null)	{	// popup is valid, add it to button
			// add new action listener that pops up the new menu
			l = new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					System.err.println("actionPerformed in action "+getName());
					Dimension d = btn.getSize();
					showPopupActionMenu(btn, d.width/2, d.height/2);
				}
			};
			btn.addActionListener(l);
			triggerList.put(c, l);	// store new listener
		}
		else	{	// if null, then reset button: remove submenu
			btn.addActionListener(this);
			triggerList.put(c, this);	// store this listener
		}

		btn.setIcon(icon);	// Bug jdk1.3
		btn.setText(text);
		btn.setToolTipText(tooltip);
		btn.setEnabled(allDisabled == false && popup != null);

		return popup != null;
	}



	// @return a dynamically created popup menu
	
	private JPopupMenu buildPopupMenu(String [] menuNames, ActionListener listener)	{
		return buildPopupMenu(buildMenu(menuNames, listener));
	}


	private JPopupMenu buildPopupMenu(MenuTree menuTree, ActionListener listener)	{
		return buildPopupMenu(menuTree != null ? menuTree.getAsJMenuItems(this, listener) : null);
	}


	private JPopupMenu buildPopupMenu(JMenuItem [] array)	{
		JPopupMenu pop = null;
		for (int i = 0; array != null && i < array.length; i++)	{
			if (pop == null)
				pop = new JPopupMenu();
				
			if (array[i] == null)
				pop.addSeparator();
			else
				pop.add(array[i]);
		}
		return pop;
	}


	private boolean setMenuSubTree(JComponent c, MenuTree menuTree, ActionListener listener)	{
		return setMenuSubMenu(c, menuTree != null ? menuTree.getAsJMenuItems(this, listener) : null, listener);
	}

	private boolean setMenuSubMenu(JComponent c, String [] menuNames, ActionListener listener)	{
		return setMenuSubMenu(c, buildMenu(menuNames, listener), listener);
	}
	
	// replace a menu item by a menu and fill it with new dynamical created items
	private boolean setMenuSubMenu(JComponent c, JMenuItem [] items, ActionListener listener)	{
		//System.err.println("setMenuSubMenu, Menu-Item \""+c.getName()+"\"");
		JMenuItem mi = (JMenuItem)c;
		// get parent menu
		Component p = (Component)mi.getParent();
		JPopupMenu parent = null;
		if (p instanceof JMenu)	{
			parent = ((JMenu)p).getPopupMenu();
		}
		else
		if (p instanceof JPopupMenu)	{
			parent = (JPopupMenu)p;
		}
		else	{
			System.err.println("FEHLER: setMenuSubMenu, unbekannte Menu-Struktur in \""+getName()+"\", parent class "+(p != null ? p.getClass().getName() : "null!"));
			//Thread.dumpStack();
			return false;
		}
		
		// find the menu item in parent list
		Component [] comps = parent.getComponents();
		Component rc = null;
		int pos = 0;
		for (int j = 0; rc == null && j < comps.length; j++)	{
			if (comps[j].equals(mi))
				rc = (Component)comps[j];
			else
				pos++;
		}
		
		//try	{ parent.remove(rc); } catch (Exception e)	{}	// Swing BUG
		parent.remove(rc);

		// substitute it by a menu
		JMenuItem menuItem = (items != null && items.length > 0)
				? new JMenu(mi.getText())
				: createMenuItem(getName(), null, listener);
				
		// add items to the new menu
		for (int i = 0; items != null && i < items.length; i++)	{
			JMenu menu = (JMenu)menuItem;
			if (items[i] == null)
				menu.addSeparator();
			else
				menu.add(items[i]);
			//System.err.println("  adding "+items[i].getText());
		}
		
		//System.err.println("  inserting "+menu.getText()+" at "+pos);
		parent.insert(menuItem, pos);

		delList.add(c);	// remove old menu from trigger list
		addList.put(menuItem, Integer.valueOf(pos));	// store the new menu to trigger list

		//System.err.println((items != null ? "set" : "reset")+" MenuSubMenu in action \""+getName()+"\"");
		if (items == null || allDisabled)
			menuItem.setEnabled(false);	// no contents, hide it
			
		return items != null && allDisabled == false;
	}



	private JMenuItem [] buildMenu(String [] menuNames, ActionListener listener)	{
		JMenuItem [] array = null;

		// disable container if all elements are disabled
		allDisabled = enabled != null;
		//allDisabled = true;

		for (int i = 0; menuNames != null && i < menuNames.length; i++)	{
			if (array == null)
				array = new JMenuItem [menuNames.length];

			JMenuItem mi = createMenuItem(menuNames[i], getName(), listener);

			if (mi != null && enabled != null && enabled[i] == false)
				mi.setEnabled(false);

			array[i] = mi;

			if (enabled != null && enabled[i] == true)
				allDisabled = false;
		}

		return array;
	}



	/**
		Create a menu item with passed label and an adequate action command.
	*/
	protected JMenuItem createMenuItem(String label, String actionPath, ActionListener al)	{
		if (label == null)
			return null;
			
		JMenuItem mi = new JMenuItem(label);
		mi.setActionCommand(catenizeActionCommand(actionPath, menuItemSeparator, label));
		//System.err.println("setting action command >"+mi.getActionCommand()+"<");
		ActionListener l = al != null ? al : this;
		mi.addActionListener(l);
		return mi;
	}

	
	/**
		Concatenize two menu strings for an action command, by inserting "." between them.
		Diese Methode wird von FillableManagedAction und MenuTree benutzt.
	*/
	protected static String catenizeActionCommand(String first, String sep, String second)	{
		if (first != null)
			return first+sep+second;
		return second;
	}


	/**
		Change the name of the action and its optional delegate.
		@return old name.
	*/
	protected String setName(String newName)	{
		String old = super.setName(newName);
		// Set new text in filled menus as their triggers were inactivated
		for (Enumeration e = triggerList.keys(); e.hasMoreElements(); )	{
			JComponent c = (JComponent)e.nextElement();
			if (c instanceof JMenuItem)	{
				// MenuItems are substituted and so cannot be internationalized by applications
				JMenuItem m = (JMenuItem)c;
				m.setText(newName);
			}
		}
		return old;
	}

}
