package fri.gui.swing.splitpane;

import java.awt.event.*;
import java.beans.*;
import javax.swing.JSplitPane;

/**
	This class is a ComponentListener to relocate the divider
	when a parent frame is sizing: keep proportion of the divider to 0.5.
*/

public class SymmetryListener implements
	PropertyChangeListener,	// to save previous divider location
	ComponentListener	// keep proportion
{
	private double dividerLocation = 0.5;
	private JSplitPane splitPane;
	private boolean changing = false;


	/** Install the symmetry listener to passed SpitPane. */
	public SymmetryListener(JSplitPane splitPane)	{
		this.splitPane = splitPane;
		//splitPane.setDividerLocation(dividerLocation);
		splitPane.addPropertyChangeListener(this);	// to store proportion
		splitPane.addComponentListener(this);	// to keep proportion when sizing
	}


	/** Interface PropertyChangeListener: if divider location changes, calculate divider location to half of width. */
	public void propertyChange(PropertyChangeEvent e)	{
		if (changing == false && e.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY))	{
			double dl = SplitPane.getProportionalDividerLocation(splitPane);
			if (dl > 0.1)
				dividerLocation = dl;
			//System.err.println("SymmetryListener.propertyChange, pending location "+dl+", divider location "+dividerLocation);
			//Thread.dumpStack();
		}
	}
	
	// interface ComponentListener
	
	/** Implements ComponentListener to relocate splitpane when sizing. */
	public void componentResized(ComponentEvent e)	{
		//System.err.println("SymmetryListener.componentResized, having divider location "+dividerLocation);
		changing = true;
		splitPane.setDividerLocation(dividerLocation);
		changing = false;
	}
	public void componentMoved(ComponentEvent e)	{
	}
	public void componentShown(ComponentEvent e)	{
	}
	public void componentHidden(ComponentEvent e)	{
	}

}