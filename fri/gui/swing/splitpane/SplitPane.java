package fri.gui.swing.splitpane;

import java.awt.*;
import java.beans.*;
import javax.swing.*;
import fri.util.props.ClassProperties;
import fri.util.application.Closeable;

/**
	Bugfix for JSplitPane that does not respect setDividerLocation()
	before the pane was layouted and painted on screen.
	
	Additionally the divider location is made persistent if close()
	is called when window closes.
*/

public class SplitPane extends JSplitPane implements
	PropertyChangeListener,
	Closeable
{
	private boolean neverDone = true;
	private boolean persistent = false;
	private double dividerLocation = 0.3;
	private Class callerClass = null;


	public SplitPane()	{
		super();
	}

	public SplitPane(int orientation)	{
		super(orientation);
		init();
	}

	public SplitPane(int orientation, Component left, Component right)	{
		super(orientation, left, right);
		init();
	}
	
	public SplitPane(Class callerClass, int orientation)	{
		super(orientation);
		this.callerClass = callerClass;
		init();
	}
	
	public SplitPane(Class callerClass, int orientation, Component left, Component right)	{
		super(orientation, left, right);
		this.callerClass = callerClass;
		init();
	}

	
	private void init()	{
		// load persistent divider location
		load();
		// listen for divider changes
		addPropertyChangeListener(this);
	}

	private void load()	{
		// load persistent divider location
		String div = ClassProperties.get(getPropClass(), "dividerLocation");
		if (div != null)	{
			try	{
				double d = Double.valueOf(div).doubleValue() + 0.1;
				dividerLocation = Math.min(Math.max(0.03, d), 0.9);
				persistent = true;
				//System.err.println("SplitPane got persistent divider location: "+dividerLocation);
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
	}

	
	private Class getPropClass()	{
		return callerClass == null ? getClass() : callerClass;
	}
	
	public void setPropClass(Class propClass)	{
		this.callerClass = propClass;
		load();
	}
	

	public void setDividerLocation(int dl)	{
		//System.err.println("SplitPane.setDividerLocation(int) was called: "+dl);
		super.setDividerLocation(dl);
	}
	
	/** Overridden to store divider location privately if not read from persistence. */
	public void setDividerLocation(double dl)	{
		if (neverDone == false)	{	// not at start
			super.setDividerLocation(dl);
		}
		else	// at start
		if (persistent == false)	{	// when no persistence
			this.dividerLocation = dl;
		}
		
		//System.err.println("SplitPane.setDividerLocation(double): "+dl);
		//Thread.dumpStack();
	}
	
	
	/** Set divider location at least here. */
	public void paintComponent(Graphics g)	{
		if (neverDone)	{
			//System.err.println("SplitPane.paintComponent, super.setDividerLocation: "+dividerLocation);
			super.setDividerLocation(dividerLocation);
			neverDone = false;
		}
		super.paintComponent(g);
	}


	/** Listen for divider changes. */
	public void propertyChange(PropertyChangeEvent e)	{
		if (e.getPropertyName().equals(DIVIDER_LOCATION_PROPERTY))	{
			double dl = SplitPane.getProportionalDividerLocation(this);
			if (dl > 0)	{
				dividerLocation = dl;
				//System.err.println("SplitPane.propertyChange: "+DIVIDER_LOCATION_PROPERTY+": "+dividerLocation);
			}
		}
	}


	static double getProportionalDividerLocation(JSplitPane splitPane)	{
		double dim = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ? (double)splitPane.getWidth() : (double)splitPane.getHeight();
		dim -= (double)splitPane.getDividerSize();
		//System.err.println("SplitPane.getProportionalDividerLocation, int divider location "+splitPane.getDividerLocation()+", dimension "+dim);
		if (dim > 0)
			return (double)splitPane.getDividerLocation() / dim;
		return -1d;
	}


	/** Implemented to store divider location to persistence. */
	public boolean close()	{
		double dl = getProportionalDividerLocation(this);
		ClassProperties.put(getPropClass(), "dividerLocation", Double.toString(dl - 0.1));
		ClassProperties.store(getPropClass());
		return true;
	}
	


	/** Test main 
	public static void main(String [] args)	{
		JFrame f = new JFrame("SplitPane");
		JTextArea ta1 = new JTextArea();
		JTextArea ta2 = new JTextArea();
		SplitPane sp = new SplitPane(JSplitPane.HORIZONTAL_SPLIT, ta1, ta2);
		sp.setDividerLocation(0.2);
		f.getContentPane().add(sp);
		f.setSize(400, 200);
		f.setVisible(true);
	}
	*/
	
}