package fri.gui.swing.actionmanager;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.net.*;

import fri.gui.CursorUtil;
import fri.gui.swing.ComponentUtil;

/**
	Ziel:<br>
		Speicherung der Information ueber eine Action (siehe Konstruktor), die mit
		class ActionManger verwaltet wird. Weitergabe des ActionEvents
		mit dem Namen der Action (ueber getActionCommand() abrufbar)
		an den (ueber den ActionManager registrierten) Listener der Action.
		Auch vorgefertigte Action's koennen mit keypress, image und tooltip
		versehen werden.
	<P>
	Verhalten:<br>
		Der "name" der Action wird als Property in AbstractAction gespeichert.
		Alle anderen Action-Infos werden als Membervariable gespeichert.
		Der Benutzer von ActionManager sollte diese Klasse gar nicht benutzen
		muessen. Aufrufe von ActionManager steuern alle Ablauefe.

	@author  Ritzberger Fritz
*/

class ManagedAction extends AbstractAction
{
	protected String tooltip, displayTooltip;
	protected int key;
	protected int mask;
	protected ActionListener actionListener;
	protected JComponent keyboardSensor;
	private JComponent oldKeyboardsensor;
	private Action delegate;	// optional
	protected Hashtable triggerList = new Hashtable();


	/**
		Anlegen einer gemanagten AbstractAction.
		@param name Primaer-Schluessel zur Identifizierung der Action in allen
			Dienstfunktionen des package.
		@param image Name einer Bilddatei, die zur Darstellung der Action
			verwendet wird. Kann null sein. Der Name muss ein zum actionlistener
			relativer Pfad sein, d.h. er muss unterhalb des Package der actionListener-Class
			liegen (getClass.getResource() wird verwendet).
		@param tooltip Text, der im Tooltip der Action erscheinen soll.
		@param key Accelerator fuer die Action (zB KeyEvent.VK_F1).
			Wird in Menuitems rechtsbuendig neben dem Itemlabel textuell dargestellt.
		@param mask Accelerator Zusatztaste (zB InputEvent.CTRL_MASK)
		@param keyboardsensor Component, auf der der Accelerator wahrgenommen werden
			soll.
		@param actionlistener Objekt, das verstaendigt wird, wenn die Action
			ausgeloest wird.
	*/
	public ManagedAction(
		String name,
		String image,
		String tooltip,
		int key,
		int mask,
		JComponent keyboardsensor,
		ActionListener actionlistener)
	{
		this(name, image != null && actionlistener != null
				? new ImageIcon(actionlistener.getClass().getResource(image))
				: (Icon)null,
			tooltip, key, mask, keyboardsensor, actionlistener);
	}

	/**
		Anlegen einer gemanagten AbstractAction.
		@param image statt Image-Name eine URL
		@param rest see above
	*/
	public ManagedAction(
		String name,
		URL image,
		String tooltip,
		int key,
		int mask,
		JComponent keyboardsensor,
		ActionListener actionlistener)
	{
		this(name, image != null ? new ImageIcon(image) : (Icon)null, tooltip, key, mask, keyboardsensor, actionlistener);
	}

	/**
		Anlegen einer gemanagten AbstractAction.
		@param icon zu verwendendes Icon
		@param rest see above
	*/
	public ManagedAction(
		String name,
		Icon icon,
		String tooltip,
		int key,
		int mask,
		JComponent keyboardsensor,
		ActionListener actionlistener)
	{
		super(name, icon);
		this.actionListener = actionlistener;
		init(tooltip, key, mask, keyboardsensor);
	}

	/**
		Anlegen einer gemanagten AbstractAction.
		@param action vorgefertigte AbstractAction als Delegations-Objekt
		@param rest see above
	*/
	public ManagedAction(
		Action action,
		String tooltip,
		int key,
		int mask,
		JComponent keyboardsensor)
	{
		putValue(Action.SMALL_ICON, action.getValue(Action.SMALL_ICON));
		this.delegate = action;
		this.actionListener = this;
		init(tooltip, key, mask, keyboardsensor);
	}


	/**
		Anlegen einer gemanagten AbstractAction.
		@param image another ImageIcon to render this (predefined) action
		@param rest see above
	*/
	public ManagedAction(
		Action action,
		URL image,
		String tooltip)
	{
		this(action, image, tooltip, 0, 0);
	}

	public ManagedAction(
		Action action,
		Icon icon,
		String tooltip)
	{
		this(action, icon, tooltip, 0, 0);
	}

	/**
		Anlegen einer gemanagten AbstractAction.
		@param image another ImageIcon to render this (predefined) action
		@param rest see above
	*/
	public ManagedAction(
		Action action,
		URL image,
		String tooltip,
		int key,
		int mask)
	{
		this(action, image != null ? new ImageIcon(image) : null, tooltip, key, mask);
	}

	public ManagedAction(
		Action action,
		Icon icon,
		String tooltip,
		int key,
		int mask)
	{
		super((String)action.getValue(Action.NAME), icon);
		action.putValue(Action.SMALL_ICON, getValue(Action.SMALL_ICON));
		this.delegate = action;
		this.actionListener = this;
		init(tooltip, key, mask, null);
	}



	private void init(
		String tooltip,
		int key,
		int mask,
		JComponent keyboardsensor)
	{
		this.key = key;
		this.mask = mask;
		this.tooltip = tooltip;
		this.displayTooltip = buildTooltiptext(tooltip);
		changeKeyboardSensor(keyboardsensor);
		putValue(Action.SHORT_DESCRIPTION, tooltip);
		//putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key, mask));
		if (delegate != null)
			delegate.putValue(Action.SHORT_DESCRIPTION, tooltip);
	}


	private String buildTooltiptext(String tooltip)	{
		return key > 0 ? tooltip+". "+getKeyText(key, mask) : tooltip;
	}


	/** common dataless method to build a string for a accelerator */
	public static String getKeyText(int key, int modifiers)	{
		String acceleratorText = "";
		if (key <= 0)
			return acceleratorText;
		if (modifiers > 0)	{
			acceleratorText = KeyEvent.getKeyModifiersText(modifiers);
			acceleratorText += "+";
		}
		acceleratorText += KeyEvent.getKeyText(key);
		return acceleratorText;
	}


	/**
		@return das delegate-Objekt, falls eine vorgefertigte Action
			verwendet wurde, sonst die Action selbst. Zum Einfuegen in 
			Menues, Toolbars etc gedacht.
	*/
	protected Action getAction()	{
		return delegate == null ? this : delegate;
	}


	/** Delegation an "getValue(Action.NAME).toString()" */
	public String getName()	{
		if (delegate != null)
			return delegate.getValue(Action.NAME).toString();
		return getValue(Action.NAME).toString();
	}


	/** Setzen eines (neuen) Keyboard-Sensors fuer diese Action */
	protected void changeKeyboardSensor(JComponent keyboardsensor)	{
		if (keyboardsensor != this.keyboardSensor && this.keyboardSensor != null && actionListener != null && key > 0)	{
			KeyStroke k = KeyStroke.getKeyStroke(key, mask);
			
			// unregister old sensor
			// this.keyboardSensor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(KeyStroke.getKeyStroke(key, mask));
			this.keyboardSensor.getInputMap().remove(k);
			this.keyboardSensor.getActionMap().remove(getName());
		}
		
		this.keyboardSensor = keyboardsensor;
		
		if (this.keyboardSensor != null && actionListener != null && key > 0)	{
			oldKeyboardsensor = keyboardsensor;

			// register new sensor
			KeyStroke k = KeyStroke.getKeyStroke(key, mask);
			
			// keyboardsensor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(k, getName());
			keyboardsensor.getInputMap().put(k, getName());
			keyboardsensor.getActionMap().put(getName(), this /*actionListener*/);
		}
	}

	/** Unregister the keystroke and set the keyboard sensor null if "on" is false,
			else restore to old value that was saved at previous call. */
	protected void setKeyboardSensor(boolean on)	{
		if (on)	{
			if (oldKeyboardsensor != null && oldKeyboardsensor != keyboardSensor)
				changeKeyboardSensor(oldKeyboardsensor);
		}
		else	{
			if (keyboardSensor != null)
				oldKeyboardsensor = keyboardSensor;
			changeKeyboardSensor(null);
		}
	}

	/**
		Insert a new trigger (button in toolbar, item in menu or popup)
		into this action. All these triggers can be modified dynamic and
		generic by adding submenu-items or showing a popup at action performed.
	*/
	protected void insertTriggerList(JComponent c)	{
		if (c instanceof JMenuItem)	{
			MenuElement menu = (MenuElement)c.getParent();
			int pos = getMenuItemPosition(menu, (JMenuItem)c);
			triggerList.put(c, new Integer(pos));	// the menuitem and its position
		}
		else
		if (c instanceof AbstractButton)	{
			triggerList.put(c, this);	// the button and its actionlistener
		}
		else	{
			Thread.dumpStack();
			System.err.println("FEHLER: insertTriggerList: unknown instanceof: "+c.getClass());
		}
	}


	private int getMenuItemPosition(MenuElement menu, JMenuItem item)	{
		MenuElement[] me = menu.getSubElements();
		for (int i = 0; i < me.length; i++)	{
			if (((JMenuItem)me[i]).equals(item))
				return i;
		}
		return -1;
	}



	/**
		Default Implementierung, die nichts tut.
		@exception RuntimeException wenn auf dieser (nicht fuellbaren) Ebene aufgerufen.
	*/
	public boolean fillAction(String [] menuNames, ActionListener listener)	{
		throw new RuntimeException("WARNUNG: fillAction kann nur fuer FillableAction aufgerufen werden!");
	}
	/**
		Default Implementierung, die nichts tut.
		@exception RuntimeException wenn auf dieser (nicht fuellbaren) Ebene aufgerufen.
	*/
	public boolean fillAction(String [] menuNames, ActionListener listener, boolean [] enabled)	{
		throw new RuntimeException("WARNUNG: fillAction kann nur fuer FillableAction aufgerufen werden!");
	}
	/**
		Default Implementierung, die nichts tut.
		@exception RuntimeException wenn auf dieser (nicht fuellbaren) Ebene aufgerufen.
	*/
	public boolean fillAction(MenuTree menu, ActionListener listener)	{
		throw new RuntimeException("WARNUNG: fillAction kann nur fuer FillableAction aufgerufen werden!");
	}



	// interface ActionListener, delegate to the registered ActionListener

	public void actionPerformed(ActionEvent e)	{
		System.err.println("ManagedAction actionPerformed, name "+getName());
		// setting wait cursor
		Component window = ComponentUtil.getWindowForActionSource(e.getSource());
		if (window != null)
			CursorUtil.setWaitCursor(window);
		
		try	{
			// perform the action
			if (delegate != null)	{
				delegate.actionPerformed(e);
			}
			else
			if (actionListener != null && actionListener != this)	{
				actionListener.actionPerformed(new ActionEvent(e.getSource(), e.getID(), getName()));
			}
			else	{
				System.err.println("FEHLER: Kein ActionListener vorhanden in Action "+getName());
			}
		}
		finally	{
			// resetting default cursor
			if (window != null)
				CursorUtil.resetWaitCursor(window);
		}
	}



	/** Set the action and its optional delegate en- or disabled */
	public void setEnabled(boolean enable)	{
		super.setEnabled(enable);
		if (delegate != null)
			delegate.setEnabled(enable);

		// class "Action" is good, but be very sure
		for (Enumeration e = triggerList.keys(); e.hasMoreElements(); )	{
			JComponent c = (JComponent)e.nextElement();
			if (c instanceof JButton)	{
				JButton b = (JButton)c;
				b.setEnabled(enable);
			}
			else
			if (c instanceof JMenuItem)	{
				JMenuItem m = (JMenuItem)c;
				m.setEnabled(enable);
			}
			else	{
				System.err.println("unknown type of action trigger: "+c.getClass());
				Thread.dumpStack();
			}
		}
	}
	
	
	/** Change the name of the action and its optional delegate. @return old name. */
	protected String setName(String newName)	{
		String old = new String(getName());

		putValue(Action.NAME, newName);

		if (delegate != null)
			delegate.putValue(Action.NAME, newName);

		// Take text away from toolbar buttons
		for (Enumeration e = triggerList.keys(); e.hasMoreElements(); )	{
			JComponent c = (JComponent)e.nextElement();
			if (c instanceof JButton)	{
				JButton b = (JButton)c;
				b.setText(null);
			}
		}

		return old;
	}


	/** Set the tooltip of a named action (to another language) */
	protected String setTooltip(String newTooltip)	{
		tooltip = buildTooltiptext(newTooltip);
		putValue(Action.SHORT_DESCRIPTION, tooltip);

		if (delegate != null)
			delegate.putValue(Action.SHORT_DESCRIPTION, tooltip);

		// set new tooltip text into toolbar buttons
		for (Enumeration e = triggerList.keys(); e.hasMoreElements(); )	{
			JComponent c = (JComponent)e.nextElement();
			if (c instanceof JButton)	{
				JButton b = (JButton)c;
				b.setToolTipText(tooltip);
			}
		}
		return newTooltip;
	}


	/** @return the list of all triggers, means buttons and menuitems */		
	public Enumeration getTriggers()	{
		return triggerList.keys();
	}
	
	
	/** Set the ActionListener to an(other) object */
	public void setActionListener(ActionListener al)	{
		this.actionListener = al;
		if (delegate != null)
			System.err.println("delegate is not null, action listener in \""+getName()+"\" was set to "+al);
	}
	

	/** Set all Buttons in trigger list to selected state */
	public void setPressed(boolean selected)	{
		for (Enumeration e = triggerList.keys(); e.hasMoreElements(); )	{
			JComponent c = (JComponent)e.nextElement();
			if (c instanceof JButton)	{
				JButton b = (JButton)c;
				b.setBorderPainted(selected);
			}
		}
	}

}
