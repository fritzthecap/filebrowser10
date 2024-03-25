package fri.gui.mvc.view.swing;

import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import fri.gui.mvc.view.Selection;
import fri.gui.mvc.model.Model;

/**
	DefaultSwingView is a committable and closeable JPanel.
	It stores the model and provides do-nothing implementations
	for interface methods.

	@author  Ritzberger Fritz
*/

public class DefaultSwingView extends JPanel implements
	SwingView
{
	protected Model model;
	protected Selection selection;
	protected JComponent sensorComponent;
	protected JScrollPane scrollPane;


	/** Creates a panel with BorderLayout. */
	public DefaultSwingView()	{
		super(new BorderLayout());
	}
	
	
	/** Returns this panel. */
	public JComponent getAddableComponent()	{
		return this;
	}

	/** Returns the sensor JComponent if setSensorComponent() has been called at least once, else null. */
	public JComponent getSensorComponent()	{
		return sensorComponent;
	}

	/**
		Adds the passed JComponent to <i>BorderLayout.CENTER</i>,
		embedded in a JScrollPane.
		Removes any previously installed sensor JComponent.
	*/
	protected void setSensorComponent(JComponent sensorComponent)	{
		if (this.scrollPane != null)
			remove(this.scrollPane);
			
		add(this.scrollPane = new JScrollPane(this.sensorComponent = sensorComponent), BorderLayout.CENTER);
	}

	/** Implements View. Returns the data model stored by setModel(). */
	public Model getModel()	{
		return model;
	}

	/**
		Implements View. Calls commit(), which flushes the current Model, and sets the new Model.
		Override this to create a Swing model adapter and set it to the Swing view.
		Do not forget to call <i>super.setModel(m)</i> in first source line!
	*/
	public void setModel(Model model)	{
		commit();	// flush the old Model before setting a new one
		this.model = model;
	}

	/** Implements View, does nothing. This method e.g. could call <i>RefreshTable.refresh(table)</i>. */
	public void refresh()	{
	}

	/** Implements Commitable, does nothing. This method e.g. could call <i>CommitTable.commit(table)</i>. */
	public void commit()	{
	}

	
	/** Override this convenience method (that returns null) to create a Selection. */
	protected Selection createSelection()	{
		return null;
	}

	/**
		Returns the member variable selection, if not null, else evaluates that
		variable by calling <i>selection = createSelection()</i>.
	*/
	public Selection getSelection()	{
		if (selection == null)
			selection = createSelection();
		
		return selection;
	}


	
	/** Implements Closeable. Calls commit(), which flushes data to the Model, and returns true. */
	public boolean close()	{
		commit();
		return true;
	}

}



/*
	protected List views;

	/** Returns all contained Views. *
	public List getViewList()	{
		return views;
	}
	
	/** Implements HierarchicalView. Adds another view to this one. Calls addViewToList() and addViewToContainer(). *
	public void addView(DefaultSwingView view)	{
		addViewToList((view);
		addViewToContainer(view);
	}

	/** Manages a list of contained views and adds the passed View to it. *
	protected void addViewToList(DefaultSwingView view)	{
		if (views == null)
			views = new ArrayList();
		views.add(view);
	}
	
	/** Adds to BorderLayout.CENTER when there is no sensorComponent, else to BorderLayout.SOUTH. *
	protected void addViewToContainer(DefaultSwingView view)	{
		add(view, getSensorComponent() == null ? BorderLayout.CENTER : BorderLayout.SOUTH);
	}
	
	/**
		Implements View.
		This implementation loops all contained Views and refreshes them.
		This method e.g. could call <i>RefreshTable.refresh(table)</i>.
	*
	public void refresh()	{
		for (int i = 0; views != null && i < views.size(); i++)
			((DefaultSwingView)views.get(i)).refresh();
	}

	/**
		Implements Committable.
		This implementation loops all contained Views and commits them.
		This method e.g. could call <i>RefreshTable.refresh(table)</i>.
	*
	public void commit()	{
		for (int i = 0; views != null && i < views.size(); i++)
			((DefaultSwingView)views.get(i)).commit();
	}

	/** Implements Closeable. Calls commit(), which flushes data to the Model, and returns true. *
	public boolean close()	{
		commit();
		
		for (int i = 0; views != null && i < views.size(); i++)
			if (((DefaultSwingView)views.get(i)).close() == false)
				return false;
		
		return true;
	}

*/