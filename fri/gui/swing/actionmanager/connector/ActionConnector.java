package fri.gui.swing.actionmanager.connector;

import java.lang.reflect.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComponent;
import fri.util.text.TextUtil;
import fri.gui.mvc.view.Selection;
import fri.gui.swing.actionmanager.ActionManager;

/**
	Extends ActionManager to implement a ActionListener for
	all ManagedActions in this ActionManager.
	The callbacks for the Actions are connected by self-reflection:
	find the callback-method for an Action by looking for a
	method with the same name plus prefix "cb_" in this class,
	spaces and special characters are replaced by "_":
	<pre>
		public final static String ACTION_DELETE = "Delete";
		public final static String ACTION_DELETE = "Save As ...";
		...
		public void cb_Delete(Object selection)	{
			...
		}
		public void cb_Save_As____(Object selection)	{
			...
		}
	</pre>
	The action callback method gets the selection as the only argument.
	The type of the selection is defined by sub-classes.
	<p>
	If a method can not be implemented as the action name is only known
	at runtime ("fillable" actions), then a fallback ActionListener can
	be used to receive those callbacks.

  @author  Ritzberger Fritz
*/

public abstract class ActionConnector extends ActionManager implements
	ActionListener
{
	protected ActionListener fallback = null;
	protected Selection selection;
	protected ActionEvent currentActionEvent;

	/** Convenience do-nothing constructor. */
	public ActionConnector()	{
		this(null, null, null);
	}
	
	/**
		Create a connection between a collection of Actions,
		their keyboard-sensor, and their callbacks.
		@param sensor keyboard sensor, e.g. a JTree, JTable etc.
		@param selection object to request selected data from
		@param fallback ActionListener to call if action command was not found by introspection
	*/
	public ActionConnector(
		JComponent sensor,
		Selection selection,
		ActionListener fallback)
	{
		super();

		this.defaultKeySensor = sensor;
		this.defaultListener = this;
		this.fallback = fallback;
		this.selection = selection;
	}


	/**
		Returns an object that can retrieve the selection from some sensor view.
	*/
	public Selection getSelection()	{
		return selection;
	}	

	/** Encapsulates the key sensor of superclass. */
	protected JComponent getKeySensor()	{
		return defaultKeySensor;
	}

	/**
		Returns the ActionEvent that resulted from last call to actionPerformed().
		This can be used by subclasses to retrieve the action-source in the
		cb_callback() procedure.
	*/
	protected ActionEvent getCurrentActionEvent()	{
		return currentActionEvent;
	}
	
	
	/**
		Central ActionListener for all ManagedActions in this ActionManager,
		as here the data selection can be retrieved.
		The command is delegated to a method with the same name
		as the command, this is done with reflection methods.
		A subclass of this one implements the command methods.
		The passed selection is retrieved by Selection.getSelectedObject();
	*/
	public void actionPerformed(ActionEvent e)	{
		currentActionEvent = e;
		
		String command = e.getActionCommand();	// must be "copy" or "XML_Editor" or ...

		// turn non-method-chars to underscore
		command = TextUtil.makeIdentifier(command);
		command = "cb_"+command;
		
		Object selected = selection != null ? selection.getSelectedObject() : null;

		System.err.println("ActionConnector.actionPerformed "+command);	//+", selected "+selected);

		// search for a matching method to the command
		Class c = getClass();
		Class [] paramTypes = new Class [] { Object.class };
		Method m = null;
		try	{
			m = c.getMethod(command, paramTypes);
		}
		catch (Exception e1)	{
			// for dynamically created menu items this is not an error!
		}
		
		// action command was found as method name
		if (m != null)	{		
			try	{
				m.invoke(this, new Object[] { selected });
			}
			catch (Exception e2)	{
				handleInvokeException(e2);
			}
		}
		else	{	// action command was not found as method name
			fallback.actionPerformed(e);
		}
		
		currentActionEvent = null;	// reset to prevent use by other methods than the current callback
	}

	
	/** Called from actionPerformed when exception is thrown by method.invoke(). */
	protected void handleInvokeException(Throwable e)	{
		e.printStackTrace();
		if (e instanceof OutOfMemoryError)
			throw (OutOfMemoryError) e;
	}

}
