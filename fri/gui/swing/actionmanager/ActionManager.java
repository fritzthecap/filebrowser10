package fri.gui.swing.actionmanager;

import javax.swing.*;
import java.util.*;
import java.net.URL;
import java.awt.event.*;
import java.awt.Point;

/**
		Klasse, um Maus-Aktionen in Menues und Toolbars
		mit Tastatur-Aktionen auf Java-Components und einem ActionListener
		fuer diese Aktionen zusammenzufassen zu einer "Action"
		und dieser ein Icon, einen Tooltip und einen Menue-String zuzuweisen.
		Diese Swing-Actions koennen in einen JToolBar, ein JMenu oder ein
		JPopupMenu eingefuegt werden:
		<PRE>
			----------------------------------
			Container          | Dynamisch generierter Action-Trigger
			----------------------------------
			JToolbar           | JButton
			JMenuItem/JMenu    | JMenuItem
			JPopupMenu         | JMenuItem
			----------------------------------
		</PRE>
	<P />
	Verhalten:<br>
		Actions werden als "AbstractAction" in einer Hashtable
		den uebergebenen Namen mittels "insertAction()" zugeordnet.
		Im uebergebenen ActionListener erhaelt man mit "event.getActionCommand()"
		den Namen der Action und die erzeugende JComponent mit getSource().
		Der String-Name der Action wird in allen Dienstfunktionen (visualizeAction)
		als Primaerschluessel verwendet.<BR>
		Achtung: mit insertFillableAction() eingefuegte Actions koennen erst
			befuellt werden, wenn sie in einem Tool- oder MenuBar mit
			visualizeAction() eingefuegt wurden!<br>
	<P />
	Lebenszyklus: 
	<UL>
		<LI>Programmstart: Anlegen des Managers und Einfuegen von
			Actions mittels insertAction()
		<LI>Programmstart: Zuordnen der Actions zu Toolbars, Menus,
			Popups mittels visualizeAction()
		<LI>Programmstart oder -lauf: Mit fillAction() Actions dynamisch mit Sub-Items fuellen.
		<LI>Programmlauf: En- und Disable von Actions mittels setEnabled().
		<LI>Programmlauf: dynamisch Einfuegen von SubMenu-Punkten mit
	</UL>
	<P />
	Beispiel: 
	<pre>
			...
			// create an action manager with delete and undo actions
			ActionManger am = new ActionManager(sensorComponent, actionListener);
			
			// insert actions
			am.insertAction("delete", "images/Delete.gif", "Delete selection", KeyEvent.VK_DELETE, 0);
	
			DoAction undo = new DoAction(DoAction.UNDO, "images/Undo.gif"); // pre-built action
			am.insertAction(undo, "Undo Previous Action", KeyEvent.VK_Z, InputEvent.CTRL_MASK);
			
			// render actions in various Components that support "add(Action)"
			am.visualizeAction("delete", toolbar);  // append toolbar button
			am.visualizeAction(DoAction.UNDO, toolbar);
			am.visualizeAction("delete", menu);     // append menu item
			am.visualizeAction(DoAction.UNDO, menu);
			...
		
		// The ActionListener method for all Actions in ActionManager
		public void actionPerformed(ActionEvent e)	{
			if (e.getActionCommand().equals("delete"))
				deleteSelectedItem();
			else
			if (e.getActionCommand().equals(DoAction.UNDO))
				undoPreviousAction();
		}
	</pre>
	@author Ritzberger Fritz
*/

public class ActionManager extends Hashtable
{
	public static String menuItemSeparator = ".";
	protected JComponent defaultKeySensor = null;
	protected ActionListener defaultListener = null;


	/** Empty constructor, does nothing. */
	public ActionManager()	{
	}

	/**
		Create a new manager with a default action listener.
		@param defListener default actionlistener for all actions that will be inserted.
	*/
	public ActionManager(ActionListener defListener)	{
		this(null, defListener);
	}

	/**
		Create a new manager for a default keyboardsensor-component.
		@param defKeySensor default keyboardsensor for all actions that will be inserted.
	*/
	public ActionManager(JComponent defKeySensor)	{
		this(defKeySensor, null);
	}

	/**
		Create a new manager for a keyboardsensor-component and
		a default actionlistener.
		@param defKeySensor default keyboardsensor for all actions that will be inserted.
		@param defListener default actionlistener for all actions that will be inserted.
	*/
	public ActionManager(JComponent defKeySensor, ActionListener defListener)	{
		this.defaultKeySensor = defKeySensor;
		this.defaultListener = defListener;
	}



	public void registerAction(String name)	{
		registerAction(name, 0);
	}

	public void registerAction(String name, int key)	{
		registerAction(name, (String)null, (String)null, key, 0);
	}

	public void registerAction(String name, String image, String tooltip)	{
		registerAction(name, image, tooltip, 0, 0);
	}

	public void registerAction(String name, String image, String tooltip, int key, int mask)	{
		registerAction(name, image, tooltip, key, mask, defaultKeySensor, defaultListener, false);
	}

	public void registerAction(String name, Icon icon, String tooltip)	{
		registerAction(name, icon, tooltip, 0, 0);
	}

	public void registerAction(String name, URL image, String tooltip)	{
		registerAction(name, image, tooltip, 0, 0);
	}

	public void registerAction(String name, Icon icon, String tooltip, int key, int mask)	{
		registerAction(name, icon, tooltip, key, mask, false);
	}

	public void registerAction(
		String name,
		URL image,
		String tooltip,
		int key,
		int mask)
	{
		if (get(name) != null)
			throw new IllegalArgumentException("Action-identifier already contained");

		Action a = new ManagedAction(name, image, tooltip, key, mask, defaultKeySensor, defaultListener);

		put(name, a);
	}

	private void registerAction(String name, String image, String tooltip, int key, int mask, boolean fillable)	{
		registerAction(name, image, tooltip, key, mask, defaultKeySensor, defaultListener, fillable);
	}

	public void registerAction(String name, String image, String tooltip, int key, int mask, JComponent keyboardsensor, ActionListener actionlistener)	{
		registerAction(name, image, tooltip, key, mask, keyboardsensor, actionlistener, false);
	}

	public void registerAction(
		String name,
		String image,
		String tooltip,
		int key,
		int mask,
		JComponent keyboardsensor,
		ActionListener actionlistener,
		boolean fillable)
	{
		if (get(name) != null)
			throw new IllegalArgumentException("Action-identifier already contained");

		ManagedAction a = fillable
				? new FillableManagedAction(name, image, tooltip, key, mask, keyboardsensor, actionlistener)
				: new ManagedAction(name, image, tooltip, key, mask, keyboardsensor, actionlistener);

		put(name, a);
	}

	public void registerAction(
		String name,
		Icon icon,
		String tooltip,
		int key,
		int mask,
		boolean fillable)
	{
		if (get(name) != null)
			throw new IllegalArgumentException("Action-identifier already contained");

		ManagedAction a = fillable
				? new FillableManagedAction(name, icon, tooltip, key, mask, defaultKeySensor, defaultListener)
				: new ManagedAction(name, icon, tooltip, key, mask, defaultKeySensor, defaultListener);

		put(name, a);
	}


	public void registerFillableAction(String name)	{
		registerFillableAction(name, (String)null, null, 0, 0);
	}

	public void registerFillableAction(String name, String image, String tooltip)	{
		registerFillableAction(name, image, tooltip, 0, 0);
	}

	public void registerFillableAction(String name, String image, String tooltip, int key, int mask)	{
		registerAction(name, image, tooltip, key, mask, true);
	}

	public void registerFillableAction(String name, Icon icon, String tooltip)	{
		registerFillableAction(name, icon, tooltip, 0, 0);
	}

	public void registerFillableAction(String name, Icon icon, String tooltip, int key, int mask)	{
		registerAction(name, icon, tooltip, key, mask, true);
	}



	public void registerAction(Action a, URL image, String tooltip)	{
		String name = a.getValue(Action.NAME).toString();
		if (get(name) != null)
			throw new IllegalArgumentException("Action-identifier already contained");

		a = new ManagedAction(a, image, tooltip);

		put(name, a);
	}

	public void registerAction(Action a, String tooltip, int key, int mask)	{
		registerAction(a, tooltip, key, mask, defaultKeySensor);
	}

	public void registerAction(Action a, URL image, String tooltip, int key, int mask)	{
		String name = a.getValue(Action.NAME).toString();
		if (get(name) != null)
			throw new IllegalArgumentException("Action-identifier already contained");

		a = new ManagedAction(a, image, tooltip, key, mask);

		put(name, a);
	}

	public void registerAction(Action a, Icon icon, String tooltip, int key, int mask)	{
		String name = a.getValue(Action.NAME).toString();
		if (get(name) != null)
			throw new IllegalArgumentException("Action-identifier already contained");

		a = new ManagedAction(a, icon, tooltip, key, mask);

		put(name, a);
	}

	public void registerAction(Action a, String tooltip, int key, int mask, JComponent keyboardsensor)	{
		String name = a.getValue(Action.NAME).toString();
		if (get(name) != null)
			throw new IllegalArgumentException("Action-identifier already contained");

		a = new ManagedAction(a, tooltip, key, mask, keyboardsensor);

		put(name, a);
	}



	/**
		Set (centralized) all connected menuitems and buttons to the given state
		@param name identifier for the action
		@param value true = enabled, false = disabled
		@exception NoSuchElementException when action name not found
	*/
	public void setEnabled(String name, boolean value)
	{
		Action a;
		if ((a = (Action)get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);
			
		a.setEnabled(value);
		
		((ManagedAction)a).setKeyboardSensor(value);
	}

	/**
		Set (centralized) all connected menuitems and buttons to the given state
		@param name identifier for the action
		@param value true = enabled, false = disabled
		@exception NoSuchElementException when action name not found
	*/
	public boolean getEnabled(String name)
	{
		Action a;
		if ((a = (Action)get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);
		return a.isEnabled();
	}


	/**
		Fill the action with exactly one level of sub-menuitems.
		The Action must already have been visualized in some Tool- or MenuBar!
		Filling will be done generic by adding a popup menu to toolbar-buttons
		and a sub-menu to menu-items.
		@param name identifier for the action
		@param menus array of menu-labels. This names will be appended to
			their parent action command by a "."
			(filling "open" into "File" creates a action command "File.open")
		@exception NoSuchElementException when action name not found
	*/
	public boolean fillAction(String name, String [] menus)
	{
		return fillAction(name, menus, null);
	}

	/**
		Fill the action with exactly one level of sub-menuitems.
		The Action must already have been visualized in some Tool- or MenuBar!
		Set the items enabled according to passed boolean array.
		@param name identifier for the action
		@param menus array of menu-labels. This names will be appended to
			their parent action command by a "."
			(filling "open" into "File" creates an action command "File.open")
		@param enabled array for enabled/disabled setting of menuitems
		@exception NoSuchElementException when action name not found
	*/
	public boolean fillAction(String name, String [] menus, boolean [] enabled)
	{
		return fillAction(name, menus, enabled, defaultListener);
	}


	/**
		Fill the action with exactly one level of sub-menuitems.
		The Action must already have been visualized in some Tool- or MenuBar!
		This will be done generic by adding a popup menu to toolbar-buttons
		and a sub-menu to menu-items.
		@param begin see above
		@param listener new action listener other than default,
			one for all new menu-items
		@exception NoSuchElementException when action name not found
	*/
	public boolean fillAction(
		String name,
		String [] menus,
		boolean [] enabled,
		ActionListener listener)
	{
		//System.err.println("ActionManager.fillAction "+name);
		ManagedAction a;
		if ((a = (ManagedAction)get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);
			
		boolean ret = a.fillAction(menus, listener, enabled);
		setEnabled(name, ret);
		return ret;
	}

	/**
		Fill the action with a tree of sub-menuitems.
		The Action must already have been visualized in some Tool- or MenuBar!
		@param name identifier for the action
		@param menus tree of menu-labels. This names will be appended to
			their parent action command by a "."
			(filling "open" into "File" creates an action command "File.open")
		@exception NoSuchElementException when action name not found
	*/
	public boolean fillAction(String name, MenuTree menus)
	{
		//System.err.println("ActionManager.fillAction "+name);
		ManagedAction a;
		if ((a = (ManagedAction)get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);
		
		boolean ret = a.fillAction(menus, defaultListener);
		setEnabled(name, ret);
		return ret;
	}



	/**
		Render the action in another Component like JToolBar, JMenu, JPopupMenu.
		@param action action to render
		@param comp Component where the action will be added (=rendered) to end
		@exception NoSuchElementException when action name not found
	*/
	public AbstractButton visualizeAction(
		Action action,
		JComponent comp)
	{
		return visualizeAction((String)action.getValue(Action.NAME), comp, true);
	}


	/**
		Render the action in another Component like JToolBar, JMenu, JPopupMenu.
		@param name identifier of action
		@param comp Component where the action will be added (=rendered) to end
		@exception NoSuchElementException when action name not found
	*/
	public AbstractButton visualizeAction(
		String name,
		JComponent comp)

	{
		return visualizeAction(name, comp, true);
	}


	/**
		Render the action in another Component like JToolBar, JMenu, JPopupMenu.
		@param begin see above
		@param withIcon use the icon for the created Component
		@exception NoSuchElementException when action name not found
	*/
	public AbstractButton visualizeAction(
		String name,
		JComponent comp,
		boolean withIcon)
	{
		return visualizeAction(name, comp, withIcon, -1);
	}


	/**
		Render the action in another Component like JToolBar, JMenu, JPopupMenu.
		@param begin see above
		@param menuPos position where menuitem should be inserted, -1 for add to tail.
			This parameter is only valid for JMenu, as there is no adequate method in
			JToolBar and a void returning insert() in JPopupMenu.
		@exception NoSuchElementException when action name not found
	*/
	public AbstractButton visualizeAction(
		String name,
		JComponent comp,
		boolean withIcon,
		int menuPos)
	{
		return visualizeAction(
			name,
			comp,
			withIcon,
			menuPos,
			null);
	}
	

	/**
		Render the action in another Component like JToolBar, JMenu, JPopupMenu.
		@param begin see above
		@param checkable null if not an checkable action (default), else holds the
			setting of the checkable menuitem / toggle button.
		@exception NoSuchElementException when action name not found
	*/
	public AbstractButton visualizeAction(
		String name,
		JComponent comp,
		boolean withIcon,
		int menuPos,
		Boolean checkable)
	{
		Object o;
		ManagedAction a;
		if ((o = get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);

		a = (ManagedAction)o;
		Action action = a.getAction();
		
		if (comp instanceof JToolBar || comp instanceof JPanel)	// ignores menu position
		{
			AbstractButton btn;
			
			if (checkable == null)	{
				if (comp instanceof JToolBar)	{
					btn = ((JToolBar)comp).add(action);
				}
				else	{
					btn = new JButton();
					btn.setAction(action);
					comp.add(btn);
				}
			}
			else	{
				btn = new JCheckBox(action);
				btn.setSelected(checkable.booleanValue());
				comp.add(btn);
			}
			
			btn.setToolTipText(language(a.tooltip));
			btn.setBorderPainted(false);
			btn.setAlignmentY(JComponent.CENTER_ALIGNMENT);
			btn.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			
			init(btn, withIcon, a);

			return btn;
		}
		else
		if (comp instanceof JPopupMenu || comp instanceof JMenu)
		{
			JMenuItem mi;
			
			if (checkable == null)	{
				if (comp instanceof JPopupMenu)
					if (menuPos > -1)	{	// insert
						mi = new JMenuItem(action);
						((JPopupMenu)comp).insert(mi, menuPos);
					}
					else
						mi = ((JPopupMenu)comp).add(action);
				else	// instanceof JMenu
					if (menuPos > -1)	// insert
						mi = ((JMenu)comp).insert(action, menuPos);
					else
						mi = ((JMenu)comp).add(action);
			}
			else	{
				mi = new JCheckBoxMenuItem(action);
				mi.setSelected(checkable.booleanValue());			

				if (comp instanceof JPopupMenu)	// Popup does not support position
					((JPopupMenu)comp).add(mi);
				else	// instanceof JMenu
					if (menuPos > -1)	// insert
						((JMenu)comp).insert(mi, menuPos);
					else
						((JMenu)comp).add(mi);
			}
			
			mi.setMnemonic(a.getName().charAt(0));
			if (a.key > 0)
				mi.setAccelerator(KeyStroke.getKeyStroke(a.key, a.mask));

			init(mi, withIcon, a);
			
			return mi;
		}
		
		System.err.println("FEHLER: Kann Action nicht visualisieren in: "+comp.getClass());
		return null;
	}


	private void init(AbstractButton ab, boolean withIcon, ManagedAction a)	{
		if (withIcon && ab.getIcon() != null)	{
			if (ab instanceof JButton)
				ab.setText("");	// no text on toolbar buttons with icon
				
			// workaround SGI problem with GraphicContext
			Icon icon = ab.getIcon();
			if (icon != null)
				ab.setDisabledIcon(new ImageIcon(GrayFilter.createDisabledImage(((ImageIcon)icon).getImage())));
		}
		else	{
			ab.setIcon(null);
		}

		// do internationalization
		if (ab.getText() != null && ab.getText().length() > 0)	{
			ab.setText(language(ab.getText()));
		}

		if (ab instanceof JButton && a.tooltip != null && a.tooltip.length() > 0)	{
			ab.setToolTipText(language(a.tooltip));
		}
		
		a.insertTriggerList(ab);
	}


	/**
		Render the checkable action. The callback gets the state of the item
		by using <code>ActionManager.isChecked(actionEvent.getSource());</code>.<p>
		visualizeCheckableAction() calls
		<code>visualizeAction(name, menu, false, -1, new Boolean(checked));</code>
		@param name identifier of checkable action.
		@param comp menu where the action will be added (rendered).
		@param checked true if the checkbox should be selected from start.
		@exception NoSuchElementException when action name not found.
	*/
	public AbstractButton visualizeCheckableAction(
		String name,
		boolean checked,
		JComponent comp)
	{
		return visualizeAction(name, comp, false, -1, new Boolean(checked));
	}


	/**
		Change dynamically the keyboard sensor for the given component.
		@param name identifier for the action
		@param listener new keyboard sensor
		@exception NoSuchElementException when action name not found
	*/
	public void changeKeyboardSensor(String name, JComponent newSensor)	{
		ManagedAction a;
		if ((a = (ManagedAction)get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);

		a.changeKeyboardSensor(newSensor);
	}


	/**
		Change dynamically the keyboard sensor in all actions.
		This can be used for focus changes, if all actions are
		keyboard-triggered by the same Component.
		@param listener new global keyboard sensor
	*/
	public void changeAllKeyboardSensors(JComponent newSensor)	{
		//System.err.println("changeAllKeyboardSensors for "+newSensor.getClass());
		for (Enumeration e = elements(); e.hasMoreElements(); )	{
			ManagedAction a = (ManagedAction)e.nextElement();
			a.changeKeyboardSensor(newSensor);
		}
		defaultKeySensor = newSensor;
	}


	/* set the named action (to another language)
	*/
	private String setName(String oldName, String newName)	{
		ManagedAction a;
		if ((a = (ManagedAction)get(oldName)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+oldName);

		remove(oldName);
		String s = a.setName(newName);
		put(newName, a);
	
		return s;
	}
	
	
	/** set the tooltip of a named action (to another language)
	*/
	private String setTooltip(String name, String newTooltip)	{
		ManagedAction a;
		if ((a = (ManagedAction)get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);

		return a.setTooltip(newTooltip);
	}


	/**
		Set all Actions disabled (useful for start state).
	*/
	public void setAllDisabled()	{
		for (Enumeration e = elements(); e.hasMoreElements(); )	{
			ManagedAction a = (ManagedAction)e.nextElement();
			a.setEnabled(false);
		}
	}


	/**
		Returns the popup from a FillableManagedAction, if present.
		@exception NoSuchElementException when action name not found
		@exception ClassCastException when action is not a FillableManagedAction
	*/
	public JPopupMenu getPopupMenu(String name)	{
		ManagedAction a;
		if ((a = (ManagedAction)get(name)) == null || a instanceof FillableManagedAction == false)
			throw new NoSuchElementException("FillableAction-identifier not found: "+name);
			
		FillableManagedAction fa = (FillableManagedAction)a;
		return fa.getPopupMenu();
	}


	/**
		Set all action triggers selected. This is needed when editing rich text
		and the cursor is on a text that was attributed bold: all bold-switches
		have to be set armed then!
	*/
	public void setPressed(String name, boolean selected)	{
		ManagedAction a;
		if ((a = (ManagedAction)get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);
		
		a.setPressed(selected);	
	}
	
	
	/**
		Returns the selected state of a checkable menuitem or checkbox.
	*/
	public static boolean isChecked(Object actionSource)	{
		if (actionSource instanceof AbstractButton)	{
			return ((AbstractButton)actionSource).isSelected();
		}
		System.err.println("Unknown action source: "+actionSource.getClass());
		Thread.dumpStack();
		return false;
	}


	/** Translate the text of some button or menuitem to another language. Default this returns the passed text. */
	protected String language(String label)	{
		return label;
	}


	/** Set a popup point for a FillableManagedAction. This is needed for keypresses. */
	public void setPopupPoint(String name, Point popupPoint)	{
		FillableManagedAction a;
		if ((a = (FillableManagedAction)get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);
		
		a.setPopupPoint(popupPoint);
	}


	/** service method for internationalization: switch the language of an action */
	public static String switchLanguage(String oldName, String newName, ActionManager am)	{
		if (am != null && oldName != null)	{
			am.setName(oldName, newName);
		}
		return newName;
	}


	/** service method for internationalization: switch the language of a tooltip */
	public static String switchTooltipLanguage(String actionName, String newTooltip, ActionManager am)	{
		if (am != null && actionName != null)	{
			am.setTooltip(actionName, newTooltip);
		}
		return newTooltip;		
	}



	/* set the action-listener of a named action (to an(other) object)
	private void setActionListener(String name, ActionListener al)	{
		ManagedAction a;
		if ((a = (ManagedAction)get(name)) == null)
			throw new NoSuchElementException("Action-identifier not found: "+name);

		a.setActionListener(al);
	}
	*/

	/* test main
	public static void main(String [] args)	{
		ActionListener al = new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				System.err.println("action performed, command "+e.getActionCommand()+", source "+e.getSource());
			}
		};
		ActionManager am = new ActionManager(al);
		am.registerAction("Perform Checkable Action");
		JFrame f = new JFrame();
		JPanel p = new JPanel();
		f.getContentPane().add(p);
		am.visualizeCheckableAction("Perform Checkable Action", true, p);
		f.pack();
		f.show();
	}
	*/
}
