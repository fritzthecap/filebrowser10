package fri.gui.swing.actionmanager.connector;

import java.awt.Window;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import fri.gui.mvc.util.swing.EventUtil;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.model.Model;
import fri.gui.mvc.view.View;
import fri.gui.mvc.view.swing.SwingView;
import fri.gui.swing.ComponentUtil;

/**
	Controller base class. Implement <i>insertActions()</i> to add specific actions.
	Implement <i>setEnabledActions()</i> to render a new View's selection state.
	
	@author Fritz Ritzberger
*/

public abstract class AbstractSwingController extends ActionConnector implements
	Controller
{
	private SwingView view;
	
	/** Do-nothing constructor. */
	public AbstractSwingController()	{
	}
	
	/** Sets the passed View. */
	public AbstractSwingController(SwingView view)	{
		setView(view);
	}
	
	/** Sets the passed View and fallback ActionListener for runtime-defined actions. */
	public AbstractSwingController(SwingView view, ActionListener fallback)	{
		setView(view);
		this.fallback = fallback;
	}
	

	/** Returns the current View's Model or null if View is null. */
	public Model getModel()	{
		return getView() != null ? getView().getModel() : null;
	}
	
	/** Sets a new Model into this controller's View. Throws NullPointerExeption if View is null! */
	public void setModel(Model model)	{
		getView().setModel(model);
	}
	

	/** Returns the current View of this controller. */
	public View getView()	{
		return view;
	}
	
	protected SwingView getSwingView()	{
		return view;
	}
	
	/** Set a View into this controller. Changes the keyboard sensor Component and the Selection. */
	public void setView(View view)	{
		this.view = (SwingView)view;
		
		if (getView() != null)	{
			changeAllKeyboardSensors(getSwingView().getSensorComponent());
			this.selection = getView().getSelection();
			
			if (size() <= 0)
				insertActions();

			setEnabledActions();
		}
		else	{
			this.selection = null;
			changeAllKeyboardSensors(null);
		}
	}

	/**
		This gets called from <i>setView()</i> when <i>size()</i> is zero.
		Subclasses should add all Actions now to this ActionManager by <i>insertAction(...)</i>.
	*/
	protected abstract void insertActions();


	/**
		This gets called from <i>setView()</i> after <i>insertActions()</i>.
		Subclasses should enable their Actions according to the new View's selection state.
		This implementation does nothing.
	*/
	protected void setEnabledActions()	{
	}



	/**
		Implements Closeable: closes the SwingView when not null.
		@return false if view not null and <i>view.close()</i> returned null, else true.
	*/
	public boolean close()	{
		return getView() == null || getSwingView().close();
	}


	/** Called from actionPerformed when exception is thrown by method.invoke(). */
	protected void handleInvokeException(Throwable e)	{
		error(e);
		super.handleInvokeException(e);
	}

	public void error(Throwable e)	{
		String msg = e.getMessage();
		if ((msg == null || msg.trim().length() <= 0) && e.getCause() != null)	{
			msg = e.getCause().getMessage();
			if (msg == null || msg.trim().length() <= 0)
				msg = e.getCause().toString();
		}
		error(msg);
	}

	public void error(final String msg)	{
		Runnable r = new Runnable()	{
			public void run()	{
				JOptionPane.showMessageDialog(
						getDialogParent(),
						msg,
						"Error",
						JOptionPane.ERROR_MESSAGE);
			}
		};
		EventUtil.invokeSynchronous(r);
	}

	public Window getDialogParent()	{
		return ComponentUtil.getWindowForComponent(
				getView() != null
					? getSwingView().getAddableComponent()
					: getKeySensor());
	}

}
