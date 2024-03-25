package fri.gui.swing.filebrowser;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import java.beans.PropertyChangeListener;

/**
	Helper class to use ActionMap and InputMap of JComponent.
	It takes an action listener and an action-name and calls
	the listener with an ActionEvent returning this action name
	from getActionCommand().
*/

public class ActionListenerDelegate implements Action //extends AbstractAction
{
	private ActionListener al;
	private String actionName;
	
	/** Create a delegate to action listener with predefined action name. */
	public ActionListenerDelegate(ActionListener al, String actionName)	{
		this.al = al;
		this.actionName = actionName;
	}
	
	/** Implements Action to delegate to action listener with predefined action name. */
	public void actionPerformed(ActionEvent e)	{
		ActionEvent e2 = new ActionEvent(e.getSource(), e.getID(), actionName, e.getModifiers());
		al.actionPerformed(e2);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener)	{
	}
	public Object getValue(String key)	{
		return null;
	}
	public boolean isEnabled()	{
		return true;
	}
	public void putValue(String key, Object value)	{
	}
	public void removePropertyChangeListener(PropertyChangeListener listener)	{
	}
	public void setEnabled(boolean b)	{
	}
}